package dk.tobiasthedanish.observability.events

internal class Event<T: Any>(val data: T, val type: String, val timestamp: Long, val sessionId: String) {
}