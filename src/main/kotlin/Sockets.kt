import models.ChatSession
import models.Message
import io.ktor.server.application.*
import io.ktor.server.http.content.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.channels.consumeEach
import java.time.Duration
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.concurrent.ConcurrentHashMap

val chatSessions = ConcurrentHashMap<String, ChatSession>()
val userSessions = ConcurrentHashMap<String, WebSocketServerSession>()

fun Application.configureSockets() {
    install(WebSockets) {
        pingPeriod = Duration.ofSeconds(15)
        timeout = Duration.ofSeconds(15)
        maxFrameSize = Long.MAX_VALUE
        masking = false
    }



    routing {

        static("/static") {
            resources("static")
        }

        webSocket("/chat/{sessionId}/{userId}/{role}") {
            val sessionId = call.parameters["sessionId"] ?: return@webSocket close(CloseReason(CloseReason.Codes.CANNOT_ACCEPT, "No session ID"))
            val userId = call.parameters["userId"] ?: return@webSocket close(CloseReason(CloseReason.Codes.CANNOT_ACCEPT, "No user ID"))
            val role = call.parameters["role"] ?: return@webSocket close(CloseReason(CloseReason.Codes.CANNOT_ACCEPT, "No role"))

            val session = chatSessions.computeIfAbsent(sessionId) {
                ChatSession(sessionId, userId, null)
            }

            if (role == "jobRad") {
                userSessions[userId] = this
                session.agentId = userId
            } else {
                userSessions[userId] = this
                session.customerId = userId
            }

            send("You are connected to the chat server as $role!")

            try {
                incoming.consumeEach { frame ->
                    if (frame is Frame.Text) {
                        val receivedText = frame.readText()
                        val timestamp = DateTimeFormatter.ISO_INSTANT.format(Instant.now())
                        val message = Message(
                            messageId = java.util.UUID.randomUUID().toString(),
                            content = receivedText,
                            senderId = userId,
                            timestamp = System.currentTimeMillis()
                        )
                        session.messages.add(message)
                        chatSessions[sessionId] = session

                        val formattedMessage = "[$timestamp] User $userId: $receivedText"

                        val customerSession = session.customerId?.let { customerId -> userSessions[customerId] }
                        val agentSession = session.agentId?.let { agentId -> userSessions[agentId] }

                        customerSession?.outgoing?.send(Frame.Text(formattedMessage))
                        agentSession?.outgoing?.send(Frame.Text(formattedMessage))
                    }
                }
            } catch (e: Exception) {
                println("Error: ${e.localizedMessage}")
            } finally {
                userSessions.remove(userId)
                println("Client disconnected")
            }
        }
    }
}
