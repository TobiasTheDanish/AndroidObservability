package dk.tobiasthedanish.observability

import android.app.Application
import dk.tobiasthedanish.observability.utils.CleanupService
import dk.tobiasthedanish.observability.utils.CleanupServiceImpl
import dk.tobiasthedanish.observability.events.EventStore
import dk.tobiasthedanish.observability.events.EventStoreImpl
import dk.tobiasthedanish.observability.events.EventTracker
import dk.tobiasthedanish.observability.events.EventTrackerImpl
import dk.tobiasthedanish.observability.exception.UnhandledExceptionCollector
import dk.tobiasthedanish.observability.lifecycle.ActivityLifecycleCollector
import dk.tobiasthedanish.observability.lifecycle.AppLifecycleCollector
import dk.tobiasthedanish.observability.lifecycle.AppLifecycleListener
import dk.tobiasthedanish.observability.lifecycle.LifecycleManager
import dk.tobiasthedanish.observability.navigation.NavigationCollector
import dk.tobiasthedanish.observability.navigation.NavigationCollectorImpl
import dk.tobiasthedanish.observability.navigation.NavigationManager
import dk.tobiasthedanish.observability.navigation.NavigationManagerImpl
import dk.tobiasthedanish.observability.session.SessionManager
import dk.tobiasthedanish.observability.session.SessionManagerImpl
import dk.tobiasthedanish.observability.session.SessionStore
import dk.tobiasthedanish.observability.session.SessionStoreImpl
import dk.tobiasthedanish.observability.session.dataStore
import dk.tobiasthedanish.observability.storage.Database
import dk.tobiasthedanish.observability.storage.DatabaseImpl
import dk.tobiasthedanish.observability.time.AndroidTimeProvider
import dk.tobiasthedanish.observability.time.TimeProvider
import dk.tobiasthedanish.observability.utils.IdFactory
import dk.tobiasthedanish.observability.utils.IdFactoryImpl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

internal interface ObservabilityConfigInternal {
    val sessionManager: SessionManager
    val timeProvider: TimeProvider
    val lifecycleManager: LifecycleManager
    val navigationManager: NavigationManager
    val activityLifecycleCollector: ActivityLifecycleCollector
    val appLifecycleCollector: AppLifecycleCollector
    val unhandledExceptionCollector: UnhandledExceptionCollector
    val navigationCollector: NavigationCollector
    val cleanupService: CleanupService
}

internal class ObservabilityConfigInternalImpl(application: Application) :
    ObservabilityConfigInternal {
    private val database: Database = DatabaseImpl(application)
    private val idFactory: IdFactory = IdFactoryImpl()
    private val eventStore: EventStore = EventStoreImpl(db = database, idFactory = idFactory)
    private val sessionStore: SessionStore = SessionStoreImpl(
        dataStore = application.dataStore,
        externalScope = CoroutineScope(Dispatchers.IO)
    )
    override val timeProvider: TimeProvider = AndroidTimeProvider()
    override val sessionManager: SessionManager = SessionManagerImpl(
        timeProvider = timeProvider,
        idFactory = idFactory,
        sessionStore = sessionStore,
        db = database,
    )
    private val eventTracker: EventTracker = EventTrackerImpl(
        eventStore = eventStore,
        sessionManager = sessionManager
    )
    override val cleanupService: CleanupService = CleanupServiceImpl(
        database = database
    )
    override val navigationManager: NavigationManager = NavigationManagerImpl()
    override val lifecycleManager: LifecycleManager = LifecycleManager(application)
    override val navigationCollector: NavigationCollector = NavigationCollectorImpl(
        eventTracker = eventTracker,
        timeProvider = timeProvider
    )
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
    private val sessionManager by lazy { config.sessionManager }
    private val lifecycleManager by lazy { config.lifecycleManager }
    private val activityLifecycleCollector by lazy { config.activityLifecycleCollector }
    private val appLifecycleCollector by lazy { config.appLifecycleCollector }
    private val unhandledExceptionCollector by lazy { config.unhandledExceptionCollector }
    private val navigationCollector by lazy { config.navigationCollector }
    private val navigationManager by lazy { config.navigationManager }
    private val cleanupService by lazy { config.cleanupService }

    private var isStarted: Boolean = false
    private val startLock = Any()

    fun init() {
        sessionManager.init()
        lifecycleManager.addListener(this)
        lifecycleManager.register()
        navigationManager.addCollector(navigationCollector)
    }

    fun start() {
        synchronized(startLock) {
            if (!isStarted) {
                activityLifecycleCollector.register()
                appLifecycleCollector.register()
                unhandledExceptionCollector.register()
                navigationManager.register()
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
                navigationManager.unregister()
                isStarted = false
            }
        }
    }

    fun onNavigation(route: String) {
        if (isStarted) {
            navigationManager.onNavigation(route)
        }
    }

    override fun onAppForeground() {
        sessionManager.onAppForeground()
        synchronized(startLock) {
            if (isStarted) {
                // Exporter is useful here
            }
        }
    }

    override fun onAppBackground() {
        sessionManager.onAppBackground()
        synchronized(startLock) {
            if (isStarted) {
                cleanupService.clearData()

            }
        }
    }
}