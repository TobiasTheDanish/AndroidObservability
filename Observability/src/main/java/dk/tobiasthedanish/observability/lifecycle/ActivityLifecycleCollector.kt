package dk.tobiasthedanish.observability.lifecycle

import android.app.Activity
import android.os.Bundle
import dk.tobiasthedanish.observability.collector.Collector
import dk.tobiasthedanish.observability.events.EventTracker
import dk.tobiasthedanish.observability.events.EventTypes
import dk.tobiasthedanish.observability.time.TimeProvider

internal class ActivityLifecycleCollector(
    private val lifecycleManager: LifecycleManager,
    private val eventTracker: EventTracker,
    private val timeProvider: TimeProvider,
): ActivityLifecycleListener, Collector {
    override fun register() {
        lifecycleManager.addListener(this)
    }

    override fun unregister() {
        lifecycleManager.removeListener(this)
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        eventTracker.track(
            timeStamp = timeProvider.now(),
            type = EventTypes.LIFECYCLE_ACTIVITY,
            data = ActivityLifecycleEvent(
                type = ActivityLifecycleEventType.CREATED,
                className = activity.javaClass.name,
            )
        )
    }

    override fun onActivityResumed(activity: Activity) {
        eventTracker.track(
            timeStamp = timeProvider.now(),
            type = EventTypes.LIFECYCLE_ACTIVITY,
            data = ActivityLifecycleEvent(
                type = ActivityLifecycleEventType.RESUMED,
                className = activity.javaClass.name,
            )
        )
    }

    override fun onActivityPaused(activity: Activity) {
        eventTracker.track(
            timeStamp = timeProvider.now(),
            type = EventTypes.LIFECYCLE_ACTIVITY,
            data = ActivityLifecycleEvent(
                type = ActivityLifecycleEventType.PAUSED,
                className = activity.javaClass.name,
            )
        )
    }

    override fun onActivityDestroyed(activity: Activity) {
        eventTracker.track(
            timeStamp = timeProvider.now(),
            type = EventTypes.LIFECYCLE_ACTIVITY,
            data = ActivityLifecycleEvent(
                type = ActivityLifecycleEventType.DESTROYED,
                className = activity.javaClass.name,
            )
        )
    }
}