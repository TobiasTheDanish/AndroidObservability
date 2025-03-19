package dk.tobiasthedanish.observability.navigation

import android.util.Log
import dk.tobiasthedanish.observability.collector.Collector
import dk.tobiasthedanish.observability.events.EventTracker
import dk.tobiasthedanish.observability.events.EventTypes
import dk.tobiasthedanish.observability.time.TimeProvider
import java.util.concurrent.atomic.AtomicBoolean

internal interface NavigationCollector: Collector {
    fun onNavigation(route: String)
}

private const val TAG = "NavigationCollectorImpl"

internal class NavigationCollectorImpl(
    private val eventTracker: EventTracker,
    private val timeProvider: TimeProvider,
): NavigationCollector {
    private var enabled = AtomicBoolean(false)

    override fun register() {
        enabled.compareAndSet(false, true)
    }

    override fun unregister() {
        enabled.compareAndSet(true, false)
    }

    override fun onNavigation(route: String) {
        Log.d(TAG, "Navigation to route: $route")
        eventTracker.track(
            data = NavigationEvent(route),
            timeStamp = timeProvider.now(),
            type = EventTypes.NAVIGATION,
        )
    }
}