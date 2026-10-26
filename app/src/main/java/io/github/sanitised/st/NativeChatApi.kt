package io.github.sanitised.st

import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object NativeChatApi {

    @Volatile
    var csrfToken: String? = null
        private set

    @Volatile
    var sessionCookie: String? = null
        private set

    /**
     * Case-insensitively extracts cookies from Set-Cookie response headers.
     */
    private fun extractCookies(conn: HttpURLConnection): String? {
        val list = conn.headerFields.entries
            .filter { it.key != null && it.key.equals("Set-Cookie", ignoreCase = true) }
            .flatMap { it.value }
        if (list.isEmpty()) return null
        return list.joinToString("; ") { it.substringBefore(";") }
    }

    /**
     * Fetch CSRF token and establish cookie session with SillyTavern server.
     */
    private fun ensureCsrfAndCookie(baseUrl: String) {
        if (!csrfToken.isNullOrBlank() && !sessionCookie.isNullOrBlank()) {
            return
        }
        try {
            val url = URL("$baseUrl/csrf-token")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.connectTimeout = 6_000
            conn.readTimeout = 6_000
            if (!sessionCookie.isNullOrBlank()) {
                conn.setRequestProperty("Cookie", sessionCookie)
            }

            val code = conn.responseCode
            val cookies = extractCookies(conn)
            if (!cookies.isNullOrBlank()) {
                sessionCookie = cookies
            }

            val stream = if (code in 200..299) conn.inputStream else (conn.errorStream ?: conn.inputStream)
            val text = BufferedReader(InputStreamReader(stream, StandardCharsets.UTF_8)).use { it.readText() }
            val obj = JSONObject(text)
            csrfToken = obj.optString("token", "")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Executes HTTP POST request with CSRF token and Cookie session.
     */
    private fun postJson(urlStr: String, jsonBody: String, timeoutMs: Int = 30_000, retryOn403: Boolean = true): String {
        val baseUrl = if (urlStr.contains("/api/")) urlStr.substringBefore("/api/") else urlStr.substringBeforeLast('/')
        ensureCsrfAndCookie(baseUrl)

        val url = URL(urlStr)
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
        conn.setRequestProperty("Accept", "application/json")
        conn.connectTimeout = 10_000
        conn.readTimeout = timeoutMs

        if (!sessionCookie.isNullOrBlank()) {
            conn.setRequestProperty("Cookie", sessionCookie)
        }
        if (!csrfToken.isNullOrBlank()) {
            conn.setRequestProperty("x-csrf-token", csrfToken)
        }
        conn.doOutput = true

        OutputStreamWriter(conn.outputStream, StandardCharsets.UTF_8).use { writer ->
            writer.write(jsonBody)
            writer.flush()
        }

        val newCookies = extractCookies(conn)
        if (!newCookies.isNullOrBlank()) {
            sessionCookie = newCookies
        }

        val code = conn.responseCode
        if (code == 403 && retryOn403) {
            // CSRF token or session may have rotated; invalidate and retry once
            csrfToken = null
            sessionCookie = null
            ensureCsrfAndCookie(baseUrl)
            return postJson(urlStr, jsonBody, timeoutMs, retryOn403 = false)
        }

        val stream = if (code in 200..299) conn.inputStream else (conn.errorStream ?: conn.inputStream)
        val response = BufferedReader(InputStreamReader(stream, StandardCharsets.UTF_8)).use { it.readText() }

        if (code !in 200..299) {
            val cleanMsg = try {
                val json = JSONObject(response)
                val errObj = json.optJSONObject("error")
                errObj?.optString("message", "")?.takeIf { it.isNotBlank() }
                    ?: json.optString("message", "").takeIf { it.isNotBlank() }
                    ?: response
            } catch (_: Exception) {
                response
            }
            throw Exception(cleanMsg)
        }
        return response
    }

    /**
     * Fetch all characters installed in local SillyTavern.
     * Uses HTTP API with CSRF token and merges with local disk characters.
     */
    suspend fun getCharacters(baseUrl: String, localCharacterDirs: List<File> = emptyList()): List<StCharacter> = withContext(Dispatchers.IO) {
        val characterList = mutableListOf<StCharacter>()

        // 1. Try HTTP API (with CSRF and session cookies)
        try {
            val responseText = postJson("$baseUrl/api/characters/all", "{}")
            val array = JSONArray(responseText)

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

                characterList.add(
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
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 2. Scan local disk directories and merge any missing characters
        for (dir in localCharacterDirs) {
            if (dir.exists() && dir.isDirectory) {
                val files = dir.listFiles { _, name -> name.endsWith(".png", ignoreCase = true) } ?: continue
                for (f in files) {
                    if (characterList.none { it.avatar.equals(f.name, ignoreCase = true) }) {
                        val parsed = parseLocalPngCard(f)
                        if (parsed != null) {
                            characterList.add(parsed)
                        }
                    }
                }
            }
        }

        characterList.sortedByDescending { it.dateLastChat }
    }

    /**
     * Parse a Tavern character card PNG directly from disk.
     * Looks for tEXt chunk with keyword "chara" or "ccv3" containing Base64 JSON.
     */
    fun parseLocalPngCard(file: File): StCharacter? {
        try {
            val bytes = file.readBytes()
            if (bytes.size < 8) return null
            var offset = 8
            while (offset + 8 <= bytes.size) {
                val length = ((bytes[offset].toInt() and 0xFF) shl 24) or
                        ((bytes[offset + 1].toInt() and 0xFF) shl 16) or
                        ((bytes[offset + 2].toInt() and 0xFF) shl 8) or
                        (bytes[offset + 3].toInt() and 0xFF)
                offset += 4
                val type = String(bytes, offset, 4, StandardCharsets.US_ASCII)
                offset += 4
                if (offset + length > bytes.size) break

                if (type == "tEXt") {
                    val chunkData = bytes.copyOfRange(offset, offset + length)
                    val nullIdx = chunkData.indexOf(0.toByte())
                    if (nullIdx > 0) {
                        val keyword = String(chunkData, 0, nullIdx, StandardCharsets.ISO_8859_1)
                        if (keyword.equals("chara", ignoreCase = true) || keyword.equals("ccv3", ignoreCase = true)) {
                            val b64 = String(chunkData, nullIdx + 1, chunkData.size - nullIdx - 1, StandardCharsets.ISO_8859_1)
                            val jsonBytes = Base64.decode(b64, Base64.DEFAULT)
                            val jsonStr = String(jsonBytes, StandardCharsets.UTF_8)
                            val obj = JSONObject(jsonStr)
                            val name = obj.optString("name", "").ifBlank {
                                obj.optJSONObject("data")?.optString("name", "") ?: file.nameWithoutExtension
                            }
                            val dataObj = obj.optJSONObject("data")
                            val desc = dataObj?.optString("description", "") ?: obj.optString("description", "")
                            val personality = dataObj?.optString("personality", "") ?: obj.optString("personality", "")
                            val firstMes = dataObj?.optString("first_mes", "") ?: obj.optString("first_mes", "")
                            val scenario = dataObj?.optString("scenario", "") ?: obj.optString("scenario", "")

                            return StCharacter(
                                avatar = file.name,
                                name = name,
                                description = desc,
                                personality = personality,
                                firstMessage = firstMes,
                                scenario = scenario,
                                dateLastChat = file.lastModified()
                            )
                        }
                    }
                }
                offset += length + 4 // Skip data + CRC
            }

            // Fallback: character card with filename as name
            return StCharacter(
                avatar = file.name,
                name = file.nameWithoutExtension.replace('_', ' ').replace('-', ' '),
                dateLastChat = file.lastModified()
            )
        } catch (e: Exception) {
            return null
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
     * Fetch secret keys state from SillyTavern.
     */
    suspend fun getSecretState(baseUrl: String): JSONObject? = withContext(Dispatchers.IO) {
        try {
            val responseText = postJson("$baseUrl/api/secrets/read", "{}")
            JSONObject(responseText)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Store an API key into SillyTavern secrets.
     */
    suspend fun writeSecret(baseUrl: String, secretKey: String, value: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val req = JSONObject().apply {
                put("key", secretKey)
                put("value", value.trim())
                put("label", "NativeChat")
            }
            postJson("$baseUrl/api/secrets/write", req.toString())
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Save settings to SillyTavern settings.json.
     */
    suspend fun saveSettings(baseUrl: String, settingsObj: JSONObject): Boolean = withContext(Dispatchers.IO) {
        try {
            postJson("$baseUrl/api/settings/save", settingsObj.toString())
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Load current AI provider, model, key status, and params.
     */
    suspend fun getAiSettings(baseUrl: String): AiSettings = withContext(Dispatchers.IO) {
        val settings = getSettings(baseUrl) ?: JSONObject()
        val secretsState = getSecretState(baseUrl)

        val provider = settings.optString("main_api", "makersuite").ifBlank { "makersuite" }
        val modelKey = "model_$provider"
        var model = settings.optString(modelKey, "").ifBlank {
            settings.optString("model", "").ifBlank {
                when (provider) {
                    "makersuite" -> "gemini-3.8-flash"
                    "openrouter" -> "anthropic/claude-3.7-sonnet"
                    "claude" -> "claude-3-7-sonnet-20250219"
                    "deepseek" -> "deepseek-chat"
                    "groq" -> "llama-3.3-70b-versatile"
                    else -> "gpt-4o"
                }
            }
        }

        // Proactively upgrade deprecated Google AI Studio models to gemini-3.8-flash
        if (provider == "makersuite" && (model.contains("gemini-2.0") || model == "gemini-1.5-pro-latest")) {
            model = "gemini-3.8-flash"
            settings.put(modelKey, model)
            settings.put("model", model)
            saveSettings(baseUrl, settings)
        }

        val secretKeyName = when (provider) {
            "openrouter" -> "api_key_openrouter"
            "openai" -> "api_key_openai"
            "claude" -> "api_key_claude"
            "makersuite" -> "api_key_makersuite"
            "deepseek" -> "api_key_deepseek"
            "groq" -> "api_key_groq"
            "custom" -> "api_key_custom"
            else -> "api_key_$provider"
        }

        var hasKey = false
        val secretArr = secretsState?.optJSONArray(secretKeyName)
        if (secretArr != null && secretArr.length() > 0) {
            hasKey = true
        } else if (secretsState?.optBoolean(secretKeyName, false) == true) {
            hasKey = true
        }

        val customUrl = settings.optString("reverse_proxy", "")
        val temp = settings.optDouble("temperature", 0.9)
        val maxTokens = settings.optInt("max_length", 800)

        AiSettings(
            provider = provider,
            model = model,
            apiKey = "",
            hasExistingKey = hasKey,
            customUrl = customUrl,
            temperature = temp,
            maxTokens = maxTokens
        )
    }

    /**
     * Save AI provider, model, key, and params.
     */
    suspend fun saveAiSettings(baseUrl: String, aiSettings: AiSettings): Boolean = withContext(Dispatchers.IO) {
        try {
            // 1. If user entered a new key, save to secrets
            if (aiSettings.apiKey.isNotBlank()) {
                val secretKeyName = when (aiSettings.provider) {
                    "openrouter" -> "api_key_openrouter"
                    "openai" -> "api_key_openai"
                    "claude" -> "api_key_claude"
                    "makersuite" -> "api_key_makersuite"
                    "deepseek" -> "api_key_deepseek"
                    "groq" -> "api_key_groq"
                    "custom" -> "api_key_custom"
                    else -> "api_key_${aiSettings.provider}"
                }
                writeSecret(baseUrl, secretKeyName, aiSettings.apiKey.trim())
            }

            // 2. Update settings.json
            val currentSettings = getSettings(baseUrl) ?: JSONObject()
            currentSettings.put("main_api", aiSettings.provider)
            currentSettings.put("model_${aiSettings.provider}", aiSettings.model.trim())
            currentSettings.put("model", aiSettings.model.trim())
            currentSettings.put("temperature", aiSettings.temperature)
            currentSettings.put("max_length", aiSettings.maxTokens)
            if (aiSettings.customUrl.isNotBlank()) {
                currentSettings.put("reverse_proxy", aiSettings.customUrl.trim())
                currentSettings.put("custom_api_url", aiSettings.customUrl.trim())
            }

            saveSettings(baseUrl, currentSettings)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Query SillyTavern's backend status endpoint to fetch available models
     * for a given provider (e.g., 'makersuite' queries Google AI Studio directly).
     */
    suspend fun fetchAvailableModels(baseUrl: String, source: String): List<String> = withContext(Dispatchers.IO) {
        try {
            val req = JSONObject().apply {
                put("chat_completion_source", source)
            }
            val responseText = postJson("$baseUrl/api/backends/chat-completions/status", req.toString(), timeoutMs = 20_000)
            val resObj = JSONObject(responseText)
            val models = mutableListOf<String>()

            val dataArr = resObj.optJSONArray("data")
            if (dataArr != null) {
                for (i in 0 until dataArr.length()) {
                    val item = dataArr.opt(i)
                    if (item is JSONObject) {
                        val id = item.optString("id", "").ifBlank {
                            item.optString("name", "").removePrefix("models/")
                        }
                        if (id.isNotBlank()) models.add(id)
                    } else if (item is String && item.isNotBlank()) {
                        models.add(item.removePrefix("models/"))
                    }
                }
            }
            models
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
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
            val mainApi = settings?.optString("main_api", "makersuite")?.ifBlank { "makersuite" } ?: "makersuite"

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

            val messagesArray = JSONArray()
            messagesArray.put(JSONObject().apply {
                put("role", "system")
                put("content", systemPrompt)
            })

            val recentHistory = history.takeLast(20)
            for (m in recentHistory) {
                if (m.isGenerating || m.text.isBlank()) continue
                messagesArray.put(JSONObject().apply {
                    put("role", if (m.isUser) "user" else "assistant")
                    put("content", m.text)
                })
            }

            messagesArray.put(JSONObject().apply {
                put("role", "user")
                put("content", userMessage)
            })

            val modelKey = "model_$mainApi"
            var model = settings?.optString(modelKey, "").takeIf { !it.isNullOrBlank() }
                ?: settings?.optString("model", "").takeIf { !it.isNullOrBlank() }
                ?: when (mainApi) {
                    "makersuite" -> "gemini-3.8-flash"
                    "openrouter" -> "anthropic/claude-3.7-sonnet"
                    "claude" -> "claude-3-7-sonnet-20250219"
                    "deepseek" -> "deepseek-chat"
                    "groq" -> "llama-3.3-70b-versatile"
                    else -> "gpt-4o"
                }

            // Proactively replace deprecated gemini-2.0-flash with gemini-3.8-flash
            if (mainApi == "makersuite" && model.contains("gemini-2.0-flash")) {
                model = "gemini-3.8-flash"
                settings?.put(modelKey, model)
                settings?.put("model", model)
                if (settings != null) saveSettings(baseUrl, settings)
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
                val revProxy = settings?.optString("reverse_proxy", "")
                if (!revProxy.isNullOrBlank()) {
                    put("reverse_proxy", revProxy)
                }
            }

            val responseText = try {
                postJson("$baseUrl/api/backends/chat-completions/generate", requestBody.toString(), timeoutMs = 90_000)
            } catch (postEx: Exception) {
                val errorMsg = postEx.message.orEmpty()
                // Auto-healing: If Google recommends a specific model, e.g. "use models/gemini-3.8-flash"
                val match = Regex("use models/([a-zA-Z0-9._-]+)", RegexOption.IGNORE_CASE).find(errorMsg)
                if (match != null && mainApi == "makersuite") {
                    val recommendedModel = match.groupValues[1]
                    try {
                        val currentSettings = getSettings(baseUrl) ?: JSONObject()
                        currentSettings.put("main_api", "makersuite")
                        currentSettings.put("model_makersuite", recommendedModel)
                        currentSettings.put("model", recommendedModel)
                        saveSettings(baseUrl, currentSettings)

                        // Immediately retry with recommended model
                        requestBody.put("model", recommendedModel)
                        postJson("$baseUrl/api/backends/chat-completions/generate", requestBody.toString(), timeoutMs = 90_000)
                    } catch (_: Exception) {
                        throw postEx
                    }
                } else {
                    throw postEx
                }
            }
            val resObj = JSONObject(responseText)

            // Inspect for backend or provider error messages
            if (resObj.has("error")) {
                val errObj = resObj.optJSONObject("error")
                val msg = errObj?.optString("message", "")?.ifBlank { errObj.optString("error", "") }
                    ?: resObj.optString("error", "").takeIf { it.isNotBlank() && it != "true" }
                    ?: "Error from AI provider. Please verify your Google AI Studio key and model."
                return@withContext Result.failure(Exception(msg))
            }

            val choices = resObj.optJSONArray("choices")
            if (choices != null && choices.length() > 0) {
                val firstChoice = choices.getJSONObject(0)
                val msg = firstChoice.optJSONObject("message")
                val content = msg?.optString("content", "")?.trim()
                if (!content.isNullOrBlank()) {
                    return@withContext Result.success(content)
                }
            }

            val directText = resObj.optString("text", "").trim()
            if (directText.isNotBlank()) {
                return@withContext Result.success(directText)
            }

            Result.failure(Exception("Received empty response from AI backend. Please verify your API key and model."))
        } catch (e: Exception) {
            e.printStackTrace()
            val rawMsg = e.message.orEmpty()
            val clean = try {
                if (rawMsg.startsWith("{") && rawMsg.endsWith("}")) {
                    val j = JSONObject(rawMsg)
                    j.optJSONObject("error")?.optString("message", "")?.takeIf { it.isNotBlank() }
                        ?: j.optString("message", "").takeIf { it.isNotBlank() } ?: rawMsg
                } else rawMsg
            } catch (_: Exception) { rawMsg }
            Result.failure(Exception(clean))
        }
    }
}
