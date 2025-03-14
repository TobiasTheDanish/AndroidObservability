package dk.tobiasthedanish.observability

import android.app.Application
import dk.tobiasthedanish.observability.cleanup.CleanupService
import dk.tobiasthedanish.observability.cleanup.CleanupServiceImpl
import dk.tobiasthedanish.observability.events.EventStore
import dk.tobiasthedanish.observability.events.EventStoreImpl
import dk.tobiasthedanish.observability.events.EventTracker
import dk.tobiasthedanish.observability.events.EventTrackerImpl
import dk.tobiasthedanish.observability.exception.UnhandledExceptionCollector
import dk.tobiasthedanish.observability.lifecycle.ActivityLifecycleCollector
import dk.tobiasthedanish.observability.lifecycle.AppLifecycleCollector
import dk.tobiasthedanish.observability.lifecycle.AppLifecycleListener
import dk.tobiasthedanish.observability.lifecycle.LifecycleManager
import dk.tobiasthedanish.observability.time.AndroidTimeProvider
import dk.tobiasthedanish.observability.time.TimeProvider

internal interface ObservabilityConfigInternal {
    val timeProvider: TimeProvider
    val lifecycleManager: LifecycleManager
    val activityLifecycleCollector: ActivityLifecycleCollector
    val appLifecycleCollector: AppLifecycleCollector
    val unhandledExceptionCollector: UnhandledExceptionCollector
    val cleanupService: CleanupService
}

internal class ObservabilityConfigInternalImpl(application: Application) :
    ObservabilityConfigInternal {
    private val eventStore: EventStore = EventStoreImpl()
    private val eventTracker: EventTracker = EventTrackerImpl(eventStore)
    override val cleanupService: CleanupService = CleanupServiceImpl(eventStore)
    override val timeProvider: TimeProvider = AndroidTimeProvider()
    override val lifecycleManager: LifecycleManager = LifecycleManager(application)
    override val activityLifecycleCollector: ActivityLifecycleCollector =
        ActivityLifecycleCollector(
            lifecycleManager = lifecycleManager,
            eventTracker = eventTracker,
            timeProvider = timeProvider,
        )
    override val appLifecycleCollector: AppLifecycleCollector = AppLifecycleCollector(
        lifecycleManager = lifecycleManager,
        eventTracker = eventTracker,
        timeProvider = timeProvider,
    )
    override val unhandledExceptionCollector: UnhandledExceptionCollector = UnhandledExceptionCollector(
        eventTracker = eventTracker,
        timeProvider = timeProvider,
    )
}

internal class ObservabilityInternal(config: ObservabilityConfigInternal): AppLifecycleListener {
    private val lifecycleManager by lazy { config.lifecycleManager }
    private val activityLifecycleCollector by lazy { config.activityLifecycleCollector }
    private val appLifecycleCollector by lazy { config.appLifecycleCollector }
    private val unhandledExceptionCollector by lazy { config.unhandledExceptionCollector }
    private val cleanupService by lazy { config.cleanupService }

    private var isStarted: Boolean = false
    private val startLock = Any()

    fun init() {
        lifecycleManager.addListener(this)
        lifecycleManager.register()
    }

    fun start() {
        synchronized(startLock) {
            if (!isStarted) {
                activityLifecycleCollector.register()
                appLifecycleCollector.register()
                unhandledExceptionCollector.register()
                isStarted = true
            }
        }
    }

    fun stop() {
        synchronized(startLock) {
            if (!isStarted) {
                activityLifecycleCollector.unregister()
                appLifecycleCollector.unregister()
                unhandledExceptionCollector.unregister()
                isStarted = false
            }
        }
    }

    override fun onAppForeground() {
        // maybe do something at some point
    }

    override fun onAppBackground() {
        synchronized(startLock) {
            if (isStarted) {
                cleanupService.clearData()
            }
        }
    }
}