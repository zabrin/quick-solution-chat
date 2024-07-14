package models

data class User(
    val userId: String,
    val username: String,
    val role: String // Customer or JobRad
)
