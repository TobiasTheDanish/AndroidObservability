package dk.tobiasthedanish.observability.navigation

import kotlinx.serialization.Serializable

@Serializable
internal data class NavigationEvent(val route: String)