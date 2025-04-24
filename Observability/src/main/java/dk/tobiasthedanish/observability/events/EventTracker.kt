package dk.tobiasthedanish.observability.events

import dk.tobiasthedanish.observability.export.Exporter
import dk.tobiasthedanish.observability.session.SessionManager
import dk.tobiasthedanish.observability.utils.isUnhandledException

internal interface EventTracker {
    fun <T: Any> track(
        data: T,
        timeStamp: Long,
        type: String,
    )
}

internal class EventTrackerImpl(
    private val eventStore: EventStore,
    private val sessionManager: SessionManager,
    private val exporter: Exporter,
): EventTracker {
    override fun <T: Any> track(data: T, timeStamp: Long, type: String) {
        val event = Event(
            data = data,
            type = type,
            timestamp = timeStamp,
            sessionId = sessionManager.getSessionId()
        )
        sessionManager.onEventTracked(event)
        eventStore.store(event)
        if (event.isUnhandledException()) {
            exporter.exportSessionCrash(event.sessionId)
            exporter.export(event.sessionId)
        }
    }
}