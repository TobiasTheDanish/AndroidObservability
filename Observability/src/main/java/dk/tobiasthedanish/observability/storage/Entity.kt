package dk.tobiasthedanish.observability.storage

internal data class ExportEntity(
    val sessionEntity: SessionEntity?,
    val eventEntities: List<EventEntity>,
    val traceEntities: List<TraceEntity>,
)

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

internal data class TraceEntity(
    val groupId: String,
    val traceId: String,
    val parentId: String?,
    val sessionId: String,
    val name: String,
    val status: String,
    val errorMessage: String?,
    val startTime: Long,
    val endTime: Long,
    val hasEnded: Boolean,
)