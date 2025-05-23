package dk.tobiasthedanish.observability.lifecycle

import kotlinx.serialization.Serializable

@Serializable
internal data class ActivityLifecycleEvent(
    val type: String,
    val className: String,
)

internal object ActivityLifecycleEventType {
    const val CREATED: String = "created"
    const val RESUMED: String = "resumed"
    const val PAUSED: String = "paused"
    const val DESTROYED: String = "destroyed"
}

@Serializable
internal data class AppLifecycleEvent(
    val type: String,
)

internal object AppLifecycleEventType {
    const val FOREGROUND: String = "foreground"
    const val BACKGROUND: String = "background"
}
