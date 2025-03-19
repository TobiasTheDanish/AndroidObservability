package dk.tobiasthedanish.observability.storage

internal data class SessionEntity(
    val id: String,
    val createdAt: Long,
    val crashed: Boolean = false,
    val exported: Boolean = false,
)

internal data class EventEntity(
    val id: String,
    val serializedData: String,
    val type: String,
    val createdAt: Long,
    val sessionId: String
)