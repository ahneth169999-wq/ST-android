package io.github.sanitised.st

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object NativeChatApi {

    /**
     * Helper to perform HTTP POST with JSON body and return response string.
     */
    private fun postJson(urlStr: String, jsonBody: String, timeoutMs: Int = 30_000): String {
        val url = URL(urlStr)
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
        conn.setRequestProperty("Accept", "application/json")
        conn.connectTimeout = 10_000
        conn.readTimeout = timeoutMs
        conn.doOutput = true

        OutputStreamWriter(conn.outputStream, StandardCharsets.UTF_8).use { writer ->
            writer.write(jsonBody)
            writer.flush()
        }

        val code = conn.responseCode
        val stream = if (code in 200..299) conn.inputStream else (conn.errorStream ?: conn.inputStream)
        val response = BufferedReader(InputStreamReader(stream, StandardCharsets.UTF_8)).use { it.readText() }

        if (code !in 200..299) {
            throw Exception("HTTP $code from $urlStr: $response")
        }
        return response
    }

    /**
     * Fetch all characters installed in local SillyTavern.
     */
    suspend fun getCharacters(baseUrl: String): List<StCharacter> = withContext(Dispatchers.IO) {
        try {
            val responseText = postJson("$baseUrl/api/characters/all", "{}")
            val array = JSONArray(responseText)
            val list = mutableListOf<StCharacter>()

            for (i in 0 until array.length()) {
                val obj = array.optJSONObject(i) ?: continue
                val name = obj.optString("name", "").takeIf { it.isNotBlank() }
                    ?: obj.optJSONObject("data")?.optString("name", "") ?: continue
                val avatar = obj.optString("avatar", "")
                val dataObj = obj.optJSONObject("data")

                val desc = dataObj?.optString("description", "") ?: obj.optString("description", "")
                val personality = dataObj?.optString("personality", "") ?: obj.optString("personality", "")
                val firstMes = dataObj?.optString("first_mes", "") ?: obj.optString("first_mes", "")
                val scenario = dataObj?.optString("scenario", "") ?: obj.optString("scenario", "")
                val creatorNotes = dataObj?.optString("creator_notes", "") ?: obj.optString("creatorcomment", "")
                val dateLastChat = obj.optLong("date_last_chat", 0L)
                val chatSize = obj.optLong("chat_size", 0L)

                val tagsList = mutableListOf<String>()
                val tagsArr = dataObj?.optJSONArray("tags") ?: obj.optJSONArray("tags")
                if (tagsArr != null) {
                    for (t in 0 until tagsArr.length()) {
                        val tag = tagsArr.optString(t, "")
                        if (tag.isNotBlank()) tagsList.add(tag)
                    }
                }

                list.add(
                    StCharacter(
                        avatar = avatar,
                        name = name,
                        description = desc,
                        personality = personality,
                        firstMessage = firstMes,
                        scenario = scenario,
                        creatorNotes = creatorNotes,
                        tags = tagsList,
                        dateLastChat = dateLastChat,
                        chatSize = chatSize
                    )
                )
            }
            list.sortedByDescending { it.dateLastChat }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    /**
     * Fetch all chats for a given character.
     */
    suspend fun getCharacterChats(baseUrl: String, avatarUrl: String): List<StChatSession> = withContext(Dispatchers.IO) {
        try {
            val req = JSONObject().apply {
                put("avatar_url", avatarUrl)
                put("simple", false)
                put("metadata", true)
            }
            val responseText = postJson("$baseUrl/api/characters/chats", req.toString())
            val array = JSONArray(responseText)
            val list = mutableListOf<StChatSession>()

            for (i in 0 until array.length()) {
                val obj = array.optJSONObject(i) ?: continue
                val fileNameWithExt = obj.optString("file_name", "")
                val fileName = fileNameWithExt.removeSuffix(".jsonl")
                if (fileName.isBlank()) continue
                val mes = obj.optString("mes", "")
                val lastMes = obj.optString("last_mes", "")
                list.add(
                    StChatSession(
                        fileName = fileName,
                        characterAvatar = avatarUrl,
                        previewText = mes,
                        lastDate = lastMes
                    )
                )
            }
            list
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    /**
     * Fetch message history for a specific chat file.
     */
    suspend fun getChatMessages(baseUrl: String, avatarUrl: String, fileName: String): List<StChatMessage> = withContext(Dispatchers.IO) {
        try {
            val req = JSONObject().apply {
                put("avatar_url", avatarUrl)
                put("file_name", fileName.removeSuffix(".jsonl"))
            }
            val responseText = postJson("$baseUrl/api/chats/get", req.toString())
            val array = JSONArray(responseText)
            val list = mutableListOf<StChatMessage>()

            for (i in 0 until array.length()) {
                val obj = array.optJSONObject(i) ?: continue
                // Skip header object that has chat_metadata
                if (obj.has("chat_metadata") && !obj.has("mes")) {
                    continue
                }
                val name = obj.optString("name", "Unknown")
                val isUser = obj.optBoolean("is_user", false)
                val mes = obj.optString("mes", "")
                val sendDate = obj.optString("send_date", "")

                list.add(
                    StChatMessage(
                        name = name,
                        isUser = isUser,
                        text = mes,
                        sendDate = sendDate
                    )
                )
            }
            list
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    /**
     * Save full chat history back to SillyTavern's storage.
     */
    suspend fun saveChat(
        baseUrl: String,
        avatarUrl: String,
        fileName: String,
        userName: String,
        characterName: String,
        messages: List<StChatMessage>
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val chatArray = JSONArray()

            // Item 0: Header
            val header = JSONObject().apply {
                put("user_name", userName)
                put("character_name", characterName)
                put("create_date", SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).format(Date()))
                put("chat_metadata", JSONObject())
            }
            chatArray.put(header)

            // Subsequent items: messages
            for (msg in messages) {
                if (msg.isGenerating || msg.text.isBlank()) continue
                val mObj = JSONObject().apply {
                    put("name", msg.name)
                    put("is_user", msg.isUser)
                    put("is_name", true)
                    put("send_date", msg.sendDate.ifBlank {
                        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).format(Date())
                    })
                    put("mes", msg.text)
                    put("extra", JSONObject())
                }
                chatArray.put(mObj)
            }

            val req = JSONObject().apply {
                put("avatar_url", avatarUrl)
                put("file_name", fileName.removeSuffix(".jsonl"))
                put("chat", chatArray)
            }

            postJson("$baseUrl/api/chats/save", req.toString())
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Fetch SillyTavern current user settings.
     */
    suspend fun getSettings(baseUrl: String): JSONObject? = withContext(Dispatchers.IO) {
        try {
            val responseText = postJson("$baseUrl/api/settings/get", "{}")
            JSONObject(responseText)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Send generation request to SillyTavern's active completion backend.
     */
    suspend fun generateReply(
        baseUrl: String,
        character: StCharacter,
        history: List<StChatMessage>,
        userMessage: String,
        userName: String = "User"
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val settings = getSettings(baseUrl)
            val mainApi = settings?.optString("main_api", "openai") ?: "openai"

            // Construct System Prompt
            val systemPromptParts = mutableListOf<String>()
            if (character.description.isNotBlank()) {
                systemPromptParts.add("Description: ${character.description}")
            }
            if (character.personality.isNotBlank()) {
                systemPromptParts.add("Personality: ${character.personality}")
            }
            if (character.scenario.isNotBlank()) {
                systemPromptParts.add("Scenario: ${character.scenario}")
            }
            systemPromptParts.add("You are roleplaying as ${character.name}. Stay in character. Respond concisely and engagingly. User's name is $userName.")
            val systemPrompt = systemPromptParts.joinToString("\n\n")

            // Construct Chat Completion Messages
            val messagesArray = JSONArray()
            messagesArray.put(JSONObject().apply {
                put("role", "system")
                put("content", systemPrompt)
            })

            // Add previous recent messages (last 20 messages for context)
            val recentHistory = history.takeLast(20)
            for (m in recentHistory) {
                if (m.isGenerating || m.text.isBlank()) continue
                messagesArray.put(JSONObject().apply {
                    put("role", if (m.isUser) "user" else "assistant")
                    put("content", m.text)
                })
            }

            // Add the new user message
            messagesArray.put(JSONObject().apply {
                put("role", "user")
                put("content", userMessage)
            })

            // Model parameters
            val model = when (mainApi) {
                "openrouter" -> settings?.optString("model_openrouter", "anthropic/claude-3.5-sonnet") ?: "anthropic/claude-3.5-sonnet"
                "claude" -> settings?.optString("model_claude", "claude-3-5-sonnet-20241022") ?: "claude-3-5-sonnet-20241022"
                else -> settings?.optString("model_openai", "gpt-4o") ?: "gpt-4o"
            }
            val temp = settings?.optDouble("temperature", 0.9) ?: 0.9
            val maxTokens = settings?.optInt("max_length", 800) ?: 800

            val requestBody = JSONObject().apply {
                put("chat_completion_source", mainApi)
                put("model", model)
                put("messages", messagesArray)
                put("temperature", temp)
                put("max_tokens", maxTokens)
                put("stream", false)
            }

            val responseText = postJson("$baseUrl/api/backends/chat-completions/generate", requestBody.toString(), timeoutMs = 90_000)
            val resObj = JSONObject(responseText)

            // Extract completion choices
            val choices = resObj.optJSONArray("choices")
            if (choices != null && choices.length() > 0) {
                val firstChoice = choices.getJSONObject(0)
                val msg = firstChoice.optJSONObject("message")
                val content = msg?.optString("content", "")?.trim()
                if (!content.isNullOrBlank()) {
                    return@withContext Result.success(content)
                }
            }

            // Fallback: check text field
            val directText = resObj.optString("text", "").trim()
            if (directText.isNotBlank()) {
                return@withContext Result.success(directText)
            }

            Result.failure(Exception("Received empty response from AI backend. Please verify your API key and model selection."))
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }
}
