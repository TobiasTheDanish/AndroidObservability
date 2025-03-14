package dk.tobiasthedanish.observability

import android.util.Log
import dk.tobiasthedanish.observability.events.Event
import dk.tobiasthedanish.observability.events.EventStore
import dk.tobiasthedanish.observability.events.EventTracker
import dk.tobiasthedanish.observability.lifecycle.ActivityLifecycleEvent
import dk.tobiasthedanish.observability.lifecycle.AppLifecycleEvent
import org.jetbrains.annotations.TestOnly

@TestOnly
internal class TestEventTracker: EventTracker, EventStore {
    private val trackingMap = HashMap<String, MutableList<Any>>()
    private val _eventsEncountered = mutableSetOf<String>()
    val eventsEncountered: Set<String>
        get() = _eventsEncountered

    override fun <T: Any> track(data: T, timeStamp: Long, type: String) {
        when (data) {
            is ActivityLifecycleEvent -> _eventsEncountered.add(data.type)
            is AppLifecycleEvent -> _eventsEncountered.add(data.type)
            else -> _eventsEncountered.add(type)
        }
        val event = Event(data, type, timeStamp)
        store(event)
    }

    override fun <T : Any> store(event: Event<T>) {
        if (trackingMap.containsKey(event.type)) {
            trackingMap[event.type]?.apply {
                add(event.data)
                toMutableList()
            }
        } else {
            trackingMap[event.type] = mutableListOf(event.data)
        }

    }

    @TestOnly
    override fun clear() {
        trackingMap.clear()
    }
}