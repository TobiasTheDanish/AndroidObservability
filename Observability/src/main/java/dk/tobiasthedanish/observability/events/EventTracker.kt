package dk.tobiasthedanish.observability.events

internal interface EventTracker {
    fun <T: Any> track(
        data: T,
        timeStamp: Long,
        type: String,
    )
}

internal class EventTrackerImpl(
    private val eventStore: EventStore,
): EventTracker {
    override fun <T: Any> track(data: T, timeStamp: Long, type: String) {
        val event = Event(data, type, timeStamp)
        eventStore.store(event)
    }
}