package dk.tobiasthedanish.observability.events

import kotlinx.serialization.Serializable

@Serializable
data class CustomEventTestData(val title: String, val description: String? = null)
