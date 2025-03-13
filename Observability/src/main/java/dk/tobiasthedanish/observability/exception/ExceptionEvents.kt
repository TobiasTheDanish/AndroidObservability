package dk.tobiasthedanish.observability.exception

import kotlinx.serialization.Serializable

@Serializable
internal data class ExceptionEvent(
    val threadName: String,
    val handled: Boolean,
    val exceptionUnits: List<ExceptionUnit>
)