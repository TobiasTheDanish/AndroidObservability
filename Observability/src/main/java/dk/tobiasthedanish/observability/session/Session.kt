package dk.tobiasthedanish.observability.session

import kotlinx.serialization.Serializable

@Serializable
data class Session(
    val id: String,
    val createdAt: Long,
    val lastEventTime: Long = 0,
    val crashed: Boolean = false,
)