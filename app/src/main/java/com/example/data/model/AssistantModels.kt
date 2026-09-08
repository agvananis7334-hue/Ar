package com.example.data.model

enum class AssistantStatus {
    IDLE,
    LISTENING,
    THINKING,
    SPEAKING,
    ERROR
}

enum class LanguageMode(val displayName: String, val code: String) {
    AUTO("Auto (Gujarati/English)", "auto"),
    GUJARATI("ગુજરાતી (Gujarati)", "gu"),
    ENGLISH("English", "en")
}

data class AssistantMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val sender: MessageSender,
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val actionBadge: String? = null,
    val isOfflineExecuted: Boolean = true
)

enum class MessageSender {
    USER,
    SIRI
}

data class CommandExecutionResult(
    val spokenText: String,
    val displayText: String = spokenText,
    val actionType: ActionType = ActionType.INFO,
    val actionBadge: String? = null,
    val isSuccess: Boolean = true,
    val payload: String? = null
)

enum class ActionType {
    INFO,
    FLASHLIGHT,
    BATTERY,
    TIME_DATE,
    TIMER,
    ALARM,
    VOLUME,
    OPEN_APP,
    CALL,
    CALCULATOR,
    JOKE
}

data class QuickSuggestion(
    val label: String,
    val query: String,
    val iconEmoji: String
)

data class ActiveTimer(
    val initialSeconds: Int,
    var remainingSeconds: Int,
    val label: String = "Timer",
    var isRunning: Boolean = true
)
