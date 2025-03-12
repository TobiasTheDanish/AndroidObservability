package dk.tobiasthedanish.observability.events

internal interface EventTracker {
    fun <T: Any> track(
        data: T,
        timeStamp: Long,
        type: String,
    )
}

internal class EventTrackerImpl(): EventTracker {
    override fun <T: Any> track(data: T, timeStamp: Long, type: String) {
        TODO("Not yet implemented")
    }
}