package models

data class Message(
    val messageId: String,
    val content: String,
    val senderId: String,
    val timestamp: Long
)
