package dk.tobiasthedanish.observability.http

import kotlinx.serialization.Serializable

@Serializable
internal data class ExportDTO(
    val session: SessionDTO?,
    val events: List<EventDTO>,
    val traces: List<TraceDTO>,
)

@Serializable
internal data class SessionDTO(
    val id: String,
    val installationId: String,
    val createdAt: Long,
    val crashed: Boolean = false,
)

@Serializable
internal data class EventDTO(
    val id: String,
    val serializedData: String,
    val type: String,
    val createdAt: Long,
    val sessionId: String
)

@Serializable
internal data class TraceDTO(
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

@Serializable
internal data class InstallationDTO(
    val id: String,
    val sdkVersion: Int,
    val model: String,
    val brand: String,
)

@Serializable
internal data class MemoryUsageDTO(
    val id: String,
    val sessionId: String,
    val installationId: String,
    val freeMemory: Long,
    val usedMemory: Long,
    val totalMemory: Long,
    val maxMemory: Long,
    val availableHeapSpace: Long,
)
