package dk.tobiasthedanish.observability.http

import android.os.Build
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
    val sdkVersion: Int = Build.VERSION.SDK_INT,
    val model: String = Build.MODEL,
    val brand: String = Build.BRAND,
)