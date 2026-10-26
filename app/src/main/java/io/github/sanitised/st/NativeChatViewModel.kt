package io.github.sanitised.st

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class NativeChatViewModel : ViewModel() {

    private val _characters = MutableStateFlow<List<StCharacter>>(emptyList())
    val characters: StateFlow<List<StCharacter>> = _characters.asStateFlow()

    private val _selectedCharacter = MutableStateFlow<StCharacter?>(null)
    val selectedCharacter: StateFlow<StCharacter?> = _selectedCharacter.asStateFlow()

    private val _messages = MutableStateFlow<List<StChatMessage>>(emptyList())
    val messages: StateFlow<List<StChatMessage>> = _messages.asStateFlow()

    private val _isLoadingCharacters = MutableStateFlow(false)
    val isLoadingCharacters: StateFlow<Boolean> = _isLoadingCharacters.asStateFlow()

    private val _isLoadingMessages = MutableStateFlow(false)
    val isLoadingMessages: StateFlow<Boolean> = _isLoadingMessages.asStateFlow()

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    private val _activeChatFileName = MutableStateFlow<String?>(null)
    val activeChatFileName: StateFlow<String?> = _activeChatFileName.asStateFlow()

    private val _chatSessions = MutableStateFlow<List<StChatSession>>(emptyList())
    val chatSessions: StateFlow<List<StChatSession>> = _chatSessions.asStateFlow()

    private val _userName = MutableStateFlow("User")
    val userName: StateFlow<String> = _userName.asStateFlow()

    private val _userPersona = MutableStateFlow("")
    val userPersona: StateFlow<String> = _userPersona.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _fontSizeSp = MutableStateFlow(15)
    val fontSizeSp: StateFlow<Int> = _fontSizeSp.asStateFlow()

    private val _themeAccent = MutableStateFlow("violet") // "violet", "cyan", "emerald", "amber", "crimson"
    val themeAccent: StateFlow<String> = _themeAccent.asStateFlow()

    private val _showQuickActions = MutableStateFlow(true)
    val showQuickActions: StateFlow<Boolean> = _showQuickActions.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    private val _aiSettings = MutableStateFlow(AiSettings())
    val aiSettings: StateFlow<AiSettings> = _aiSettings.asStateFlow()

    private var currentBaseUrl: String = "http://127.0.0.1:8000"
    private var localCharacterDirs: List<File> = emptyList()

    fun setConfig(baseUrl: String, localDirs: List<File>) {
        currentBaseUrl = baseUrl.trimEnd('/')
        localCharacterDirs = localDirs
        loadAiSettings()
    }

    fun loadAiSettings() {
        viewModelScope.launch {
            try {
                _aiSettings.value = NativeChatApi.getAiSettings(currentBaseUrl)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun saveAiSettings(newSettings: AiSettings, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            val success = NativeChatApi.saveAiSettings(currentBaseUrl, newSettings)
            if (success) {
                _aiSettings.value = newSettings.copy(
                    apiKey = "",
                    hasExistingKey = newSettings.apiKey.isNotBlank() || newSettings.hasExistingKey
                )
            }
            onComplete(success)
        }
    }

    fun saveApiKeyDirect(provider: String, apiKey: String, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
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
            val success = NativeChatApi.writeSecret(currentBaseUrl, secretKeyName, apiKey.trim())
            if (success) {
                _aiSettings.value = _aiSettings.value.copy(hasExistingKey = true)
            }
            onComplete(success)
        }
    }

    fun fetchAvailableModels(source: String, onResult: (List<String>) -> Unit) {
        viewModelScope.launch {
            val list = NativeChatApi.fetchAvailableModels(currentBaseUrl, source)
            onResult(list)
        }
    }

    fun loadCharacters(initialAvatar: String? = null) {
        viewModelScope.launch {
            _isLoadingCharacters.value = true
            _errorMessage.value = null
            try {
                var list = NativeChatApi.getCharacters(currentBaseUrl, localCharacterDirs)
                if (list.isEmpty()) {
                    // Retry once after 1.5s in case Node.js server was completing startup
                    delay(1500)
                    list = NativeChatApi.getCharacters(currentBaseUrl, localCharacterDirs)
                }

                _characters.value = list
                if (list.isNotEmpty()) {
                    val target = if (!initialAvatar.isNullOrBlank()) {
                        list.firstOrNull { it.avatar == initialAvatar } ?: list.first()
                    } else {
                        _selectedCharacter.value ?: list.first()
                    }
                    selectCharacter(target)
                }
            } catch (e: Exception) {
                _errorMessage.value = "Failed to load characters: ${e.localizedMessage}"
            } finally {
                _isLoadingCharacters.value = false
            }
        }
        loadAiSettings()
    }

    fun selectCharacter(character: StCharacter) {
        _selectedCharacter.value = character
        viewModelScope.launch {
            _isLoadingMessages.value = true
            try {
                val chats = NativeChatApi.getCharacterChats(currentBaseUrl, character.avatar)
                _chatSessions.value = chats
                if (chats.isNotEmpty()) {
                    val latestChat = chats.first()
                    _activeChatFileName.value = latestChat.fileName
                    val history = NativeChatApi.getChatMessages(currentBaseUrl, character.avatar, latestChat.fileName)
                    if (history.isNotEmpty()) {
                        _messages.value = history
                    } else {
                        initNewChatMessages(character)
                    }
                } else {
                    createNewChat(character)
                }
            } catch (e: Exception) {
                initNewChatMessages(character)
            } finally {
                _isLoadingMessages.value = false
            }
        }
    }

    fun loadChatSessions() {
        val character = _selectedCharacter.value ?: return
        viewModelScope.launch {
            try {
                val sessions = NativeChatApi.getCharacterChats(currentBaseUrl, character.avatar)
                _chatSessions.value = sessions
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun selectChatSession(session: StChatSession, onComplete: (() -> Unit)? = null) {
        val character = _selectedCharacter.value ?: return
        _activeChatFileName.value = session.fileName
        _isLoadingMessages.value = true
        viewModelScope.launch {
            try {
                val history = NativeChatApi.getChatMessages(currentBaseUrl, character.avatar, session.fileName)
                _messages.value = history
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoadingMessages.value = false
                onComplete?.invoke()
            }
        }
    }

    fun switchSwipe(messageIndex: Int, newSwipeId: Int) {
        val list = _messages.value.toMutableList()
        if (messageIndex !in list.indices) return
        val msg = list[messageIndex]
        if (msg.isUser || msg.swipes.isEmpty()) return
        val clamped = newSwipeId.coerceIn(0, msg.swipes.lastIndex)
        list[messageIndex] = msg.copy(
            text = msg.swipes[clamped],
            swipeId = clamped
        )
        _messages.value = list
        saveCurrentChat(messagesToSave = list)
    }

    fun saveUserPersona(name: String, persona: String) {
        _userName.value = name.trim().ifBlank { "User" }
        _userPersona.value = persona.trim()
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setFontSize(size: Int) {
        _fontSizeSp.value = size.coerceIn(12, 22)
    }

    fun setThemeAccent(accent: String) {
        _themeAccent.value = accent
    }

    fun toggleQuickActions() {
        _showQuickActions.value = !_showQuickActions.value
    }

    private fun initNewChatMessages(character: StCharacter) {
        val list = mutableListOf<StChatMessage>()
        if (character.firstMessage.isNotBlank()) {
            list.add(
                StChatMessage(
                    name = character.name,
                    isUser = false,
                    text = character.firstMessage,
                    sendDate = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).format(Date()),
                    swipes = listOf(character.firstMessage),
                    swipeId = 0
                )
            )
        }
        _messages.value = list
    }

    fun createNewChat(character: StCharacter? = _selectedCharacter.value): Boolean {
        val targetChar = character ?: _characters.value.firstOrNull() ?: return false
        _selectedCharacter.value = targetChar
        val timeStamp = SimpleDateFormat("yyyy-MM-dd @HH'h' mm'm' ss's'", Locale.US).format(Date())
        val newFileName = timeStamp
        _activeChatFileName.value = newFileName
        initNewChatMessages(targetChar)
        saveCurrentChat()
        loadChatSessions()
        return true
    }

    fun sendMessage(userText: String, userName: String = _userName.value) {
        val character = _selectedCharacter.value ?: return
        val trimmed = userText.trim()
        if (trimmed.isBlank() || _isGenerating.value) return

        val userMessage = StChatMessage(
            name = userName,
            isUser = true,
            text = trimmed,
            sendDate = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).format(Date())
        )

        val tempPlaceholder = StChatMessage(
            name = character.name,
            isUser = false,
            text = "",
            isGenerating = true
        )

        val updatedList = _messages.value + userMessage
        _messages.value = updatedList + tempPlaceholder
        _isGenerating.value = true
        _errorMessage.value = null

        viewModelScope.launch {
            saveCurrentChat(messagesToSave = updatedList)

            val result = NativeChatApi.generateReply(
                baseUrl = currentBaseUrl,
                character = character,
                history = updatedList,
                userMessage = trimmed,
                userName = userName,
                userPersona = _userPersona.value,
                onStatusUpdate = { _statusMessage.value = it }
            )

            result.onSuccess { replyText ->
                val assistantMessage = StChatMessage(
                    name = character.name,
                    isUser = false,
                    text = replyText,
                    sendDate = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).format(Date()),
                    swipes = listOf(replyText),
                    swipeId = 0
                )
                val finalList = updatedList + assistantMessage
                _messages.value = finalList
                saveCurrentChat(messagesToSave = finalList)
                _statusMessage.value = null
            }.onFailure { err ->
                _messages.value = updatedList
                _errorMessage.value = err.message ?: "Failed to generate reply"
                _statusMessage.value = null
            }

            _isGenerating.value = false
        }
    }

    fun regenerateLastMessage(userName: String = _userName.value) {
        val character = _selectedCharacter.value ?: return
        val list = _messages.value
        if (list.isEmpty() || _isGenerating.value) return

        val lastAssistantMsg = list.lastOrNull() ?: return
        if (lastAssistantMsg.isUser) return

        val listWithoutLast = list.dropLast(1)
        val lastUserMsg = listWithoutLast.lastOrNull { it.isUser } ?: return
        val prompt = lastUserMsg.text

        val currentSwipes = if (lastAssistantMsg.swipes.isNotEmpty()) {
            lastAssistantMsg.swipes.toMutableList()
        } else {
            mutableListOf(lastAssistantMsg.text)
        }

        val tempPlaceholder = StChatMessage(
            name = character.name,
            isUser = false,
            text = "",
            isGenerating = true
        )

        _messages.value = listWithoutLast + tempPlaceholder
        _isGenerating.value = true
        _errorMessage.value = null

        viewModelScope.launch {
            val result = NativeChatApi.generateReply(
                baseUrl = currentBaseUrl,
                character = character,
                history = listWithoutLast,
                userMessage = prompt,
                userName = userName,
                userPersona = _userPersona.value,
                onStatusUpdate = { _statusMessage.value = it }
            )

            result.onSuccess { replyText ->
                currentSwipes.add(replyText)
                val newSwipeId = currentSwipes.lastIndex
                val assistantMessage = lastAssistantMsg.copy(
                    name = character.name,
                    isUser = false,
                    text = replyText,
                    swipes = currentSwipes,
                    swipeId = newSwipeId,
                    sendDate = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).format(Date())
                )
                val finalList = listWithoutLast + assistantMessage
                _messages.value = finalList
                saveCurrentChat(messagesToSave = finalList)
                _statusMessage.value = null
            }.onFailure { err ->
                _messages.value = list
                _errorMessage.value = err.message ?: "Failed to regenerate reply"
                _statusMessage.value = null
            }

            _isGenerating.value = false
        }
    }

    fun continueLastMessage(userName: String = _userName.value) {
        val character = _selectedCharacter.value ?: return
        val list = _messages.value
        if (list.isEmpty() || _isGenerating.value) return
        val lastAssistantMsg = list.lastOrNull()
        if (lastAssistantMsg == null || lastAssistantMsg.isUser) return

        val existingText = lastAssistantMsg.text
        val tempPlaceholder = StChatMessage(
            name = character.name,
            isUser = false,
            text = existingText,
            isGenerating = true
        )

        val listWithoutLast = list.dropLast(1)
        _messages.value = listWithoutLast + tempPlaceholder
        _isGenerating.value = true
        _errorMessage.value = null

        viewModelScope.launch {
            val result = NativeChatApi.generateReply(
                baseUrl = currentBaseUrl,
                character = character,
                history = list,
                userMessage = "[Continue your previous response directly from where you left off. Do not repeat what you already wrote.]",
                userName = userName,
                userPersona = _userPersona.value,
                onStatusUpdate = { _statusMessage.value = it }
            )

            result.onSuccess { continuationText ->
                val combinedText = if (existingText.isNotBlank()) {
                    "$existingText $continuationText"
                } else {
                    continuationText
                }
                val updatedSwipes = if (lastAssistantMsg.swipes.isNotEmpty()) {
                    val sw = lastAssistantMsg.swipes.toMutableList()
                    val idx = lastAssistantMsg.swipeId.coerceIn(0, sw.lastIndex)
                    sw[idx] = combinedText
                    sw
                } else {
                    listOf(combinedText)
                }
                val updatedMessage = lastAssistantMsg.copy(
                    text = combinedText.trim(),
                    swipes = updatedSwipes
                )
                val finalList = listWithoutLast + updatedMessage
                _messages.value = finalList
                saveCurrentChat(messagesToSave = finalList)
                _statusMessage.value = null
            }.onFailure { err ->
                _messages.value = list
                _errorMessage.value = err.message ?: "Failed to continue reply"
                _statusMessage.value = null
            }

            _isGenerating.value = false
        }
    }

    fun retryLastMessage(userName: String = _userName.value) {
        val character = _selectedCharacter.value ?: return
        val list = _messages.value
        val lastUserMsg = list.lastOrNull { it.isUser } ?: return
        val userPrompt = lastUserMsg.text

        // Clean any failed trailing assistant messages
        val cleanedList = if (list.isNotEmpty() && !list.last().isUser) {
            list.dropLast(1)
        } else {
            list
        }

        val tempPlaceholder = StChatMessage(
            name = character.name,
            isUser = false,
            text = "",
            isGenerating = true
        )

        _messages.value = cleanedList + tempPlaceholder
        _isGenerating.value = true
        _errorMessage.value = null

        viewModelScope.launch {
            val result = NativeChatApi.generateReply(
                baseUrl = currentBaseUrl,
                character = character,
                history = cleanedList,
                userMessage = userPrompt,
                userName = userName,
                userPersona = _userPersona.value,
                onStatusUpdate = { _statusMessage.value = it }
            )

            result.onSuccess { replyText ->
                val assistantMessage = StChatMessage(
                    name = character.name,
                    isUser = false,
                    text = replyText,
                    sendDate = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).format(Date()),
                    swipes = listOf(replyText),
                    swipeId = 0
                )
                val finalList = cleanedList + assistantMessage
                _messages.value = finalList
                saveCurrentChat(messagesToSave = finalList)
                _statusMessage.value = null
            }.onFailure { err ->
                _messages.value = cleanedList
                _errorMessage.value = err.message ?: "Failed to generate reply"
                _statusMessage.value = null
            }

            _isGenerating.value = false
        }
    }

    fun deleteMessage(index: Int) {
        val list = _messages.value.toMutableList()
        if (index in list.indices) {
            list.removeAt(index)
            _messages.value = list
            saveCurrentChat()
        }
    }

    fun editMessage(index: Int, newText: String) {
        val list = _messages.value.toMutableList()
        if (index in list.indices) {
            val old = list[index]
            val updatedSwipes = if (old.swipes.isNotEmpty()) {
                val sw = old.swipes.toMutableList()
                val idx = old.swipeId.coerceIn(0, sw.lastIndex)
                sw[idx] = newText
                sw
            } else {
                listOf(newText)
            }
            list[index] = old.copy(text = newText, swipes = updatedSwipes)
            _messages.value = list
            saveCurrentChat()
        }
    }

    private fun saveCurrentChat(messagesToSave: List<StChatMessage> = _messages.value) {
        val character = _selectedCharacter.value ?: return
        val fileName = _activeChatFileName.value ?: return
        viewModelScope.launch {
            NativeChatApi.saveChat(
                baseUrl = currentBaseUrl,
                avatarUrl = character.avatar,
                fileName = fileName,
                userName = _userName.value,
                characterName = character.name,
                messages = messagesToSave
            )
        }
    }

    fun dismissError() {
        _errorMessage.value = null
    }
}
