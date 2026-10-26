package io.github.sanitised.st

import java.util.UUID

/**
 * Represents a SillyTavern Character card.
 */
data class StCharacter(
    val avatar: String, // e.g. "Seraphina.png"
    val name: String,
    val description: String = "",
    val personality: String = "",
    val firstMessage: String = "",
    val scenario: String = "",
    val creatorNotes: String = "",
    val tags: List<String> = emptyList(),
    val dateLastChat: Long = 0L,
    val chatSize: Long = 0L
)

/**
 * Represents a single message in a chat history.
 */
data class StChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val isUser: Boolean,
    val text: String,
    val sendDate: String = "",
    val isGenerating: Boolean = false
)

/**
 * Represents a chat file session for a character.
 */
data class StChatSession(
    val fileName: String,
    val characterAvatar: String,
    val previewText: String = "",
    val lastDate: String = ""
)

/**
 * Represents the AI engine, model, and key configuration for SillyTavern.
 */
data class AiSettings(
    val provider: String = "openrouter",
    val model: String = "anthropic/claude-3.5-sonnet",
    val apiKey: String = "",
    val hasExistingKey: Boolean = false,
    val customUrl: String = "",
    val temperature: Double = 0.9,
    val maxTokens: Int = 800
)
