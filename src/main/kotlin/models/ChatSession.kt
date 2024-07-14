package models

data class ChatSession(
    val sessionId: String,
    var customerId: String,
    var agentId: String?,
    val messages: MutableList<Message> = mutableListOf()
)
