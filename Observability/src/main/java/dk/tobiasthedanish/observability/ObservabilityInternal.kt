package dk.tobiasthedanish.observability

import android.app.Application
import dk.tobiasthedanish.observability.events.EventTracker
import dk.tobiasthedanish.observability.events.EventTrackerImpl
import dk.tobiasthedanish.observability.lifecycle.ActivityLifecycleCollector
import dk.tobiasthedanish.observability.lifecycle.LifecycleManager
import dk.tobiasthedanish.observability.time.AndroidTimeProvider
import dk.tobiasthedanish.observability.time.TimeProvider

internal interface ObservabilityConfigInternal {
    val timeProvider: TimeProvider
    val lifecycleManager: LifecycleManager
    val activityLifecycleCollector: ActivityLifecycleCollector
}

internal class ObservabilityConfigInternalImpl(application: Application) : ObservabilityConfigInternal {
    private val eventTracker: EventTracker = EventTrackerImpl()
    override val timeProvider: TimeProvider = AndroidTimeProvider()
    override val lifecycleManager: LifecycleManager = LifecycleManager(application)
    override val activityLifecycleCollector: ActivityLifecycleCollector = ActivityLifecycleCollector(lifecycleManager, eventTracker, timeProvider)
}

internal class ObservabilityInternal(config: ObservabilityConfigInternal) {
    private val lifecycleManager by lazy { config.lifecycleManager }
    private val activityLifecycleCollector by lazy { config.activityLifecycleCollector }

    private var isStarted: Boolean = false
    private val startLock = Any()

    fun init() {
        lifecycleManager.register()
    }

    fun start() {
        synchronized(startLock) {
            if (!isStarted) {
                activityLifecycleCollector.register()
                isStarted = true
            }
        }
    }

    fun stop() {
        synchronized(startLock) {
            if (!isStarted) {
                activityLifecycleCollector.unregister()
                isStarted = false
            }
        }
    }
}