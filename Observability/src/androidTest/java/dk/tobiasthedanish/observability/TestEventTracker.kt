package dk.tobiasthedanish.observability

import dk.tobiasthedanish.observability.events.EventTracker
import org.jetbrains.annotations.TestOnly

@TestOnly
class TestEventTracker: EventTracker {
    private val trackingMap = HashMap<String, MutableList<Any>>()

    override fun <T: Any> track(data: T, timeStamp: Long, type: String) {
        if (trackingMap.containsKey(type)) {
            trackingMap[type]?.apply {
                add(data)
                toMutableList()
            }
        } else {
            trackingMap[type] = mutableListOf(data)
        }
    }

    @TestOnly
    fun eventsForType(type: String): MutableList<Any>? {
        val res = trackingMap[type]?.toMutableList()

        trackingMap[type]?.clear()

        return res
    }

    @TestOnly
    fun clear() {
        trackingMap.clear()
    }
}