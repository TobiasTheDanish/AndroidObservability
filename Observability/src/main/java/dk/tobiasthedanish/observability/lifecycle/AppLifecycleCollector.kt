package dk.tobiasthedanish.observability.lifecycle

import dk.tobiasthedanish.observability.collector.Collector
import dk.tobiasthedanish.observability.events.EventTracker
import dk.tobiasthedanish.observability.events.EventTypes
import dk.tobiasthedanish.observability.time.TimeProvider

internal class AppLifecycleCollector(
    private val lifecycleManager: LifecycleManager,
    private val timeProvider: TimeProvider,
    private val eventTracker: EventTracker,
): AppLifecycleListener, Collector {
    override fun onAppForeground() {
        eventTracker.track(
            timeStamp = timeProvider.now(),
            type = EventTypes.LIFECYCLE_APP,
            data = AppLifecycleEvent(
                type = AppLifecycleEventType.FOREGROUND
            ),
        )
    }

    override fun onAppBackground() {
        eventTracker.track(
            timeStamp = timeProvider.now(),
            type = EventTypes.LIFECYCLE_APP,
            data = AppLifecycleEvent(
                type = AppLifecycleEventType.BACKGROUND
            ),
        )
    }

    override fun register() {
        lifecycleManager.addListener(this)
    }

    override fun unregister() {
        lifecycleManager.removeListener(this)
    }
}