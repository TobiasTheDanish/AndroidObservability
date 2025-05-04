package dk.tobiasthedanish.observability

import android.app.Application
import android.util.Log
import dk.tobiasthedanish.observability.utils.CleanupService
import dk.tobiasthedanish.observability.utils.CleanupServiceImpl
import dk.tobiasthedanish.observability.events.EventStore
import dk.tobiasthedanish.observability.events.EventStoreImpl
import dk.tobiasthedanish.observability.events.EventTracker
import dk.tobiasthedanish.observability.events.EventTrackerImpl
import dk.tobiasthedanish.observability.events.EventTypes
import dk.tobiasthedanish.observability.exception.ExceptionEventFactory
import dk.tobiasthedanish.observability.exception.UnhandledExceptionCollector
import dk.tobiasthedanish.observability.export.Exporter
import dk.tobiasthedanish.observability.export.ExporterImpl
import dk.tobiasthedanish.observability.http.HttpClientFactory
import dk.tobiasthedanish.observability.http.InternalHttpClientImpl
import dk.tobiasthedanish.observability.installation.InstallationManager
import dk.tobiasthedanish.observability.installation.InstallationManagerImpl
import dk.tobiasthedanish.observability.lifecycle.ActivityLifecycleCollector
import dk.tobiasthedanish.observability.lifecycle.AppLifecycleCollector
import dk.tobiasthedanish.observability.lifecycle.AppLifecycleListener
import dk.tobiasthedanish.observability.lifecycle.LifecycleManager
import dk.tobiasthedanish.observability.navigation.NavigationCollector
import dk.tobiasthedanish.observability.navigation.NavigationCollectorImpl
import dk.tobiasthedanish.observability.navigation.NavigationManager
import dk.tobiasthedanish.observability.navigation.NavigationManagerImpl
import dk.tobiasthedanish.observability.runtime.AndroidMemoryInspector
import dk.tobiasthedanish.observability.runtime.ResourceUsageCollector
import dk.tobiasthedanish.observability.runtime.ResourceUsageCollectorImpl
import dk.tobiasthedanish.observability.runtime.ResourceUsageStore
import dk.tobiasthedanish.observability.runtime.ResourceUsageStoreImpl
import dk.tobiasthedanish.observability.scheduling.Scheduler
import dk.tobiasthedanish.observability.scheduling.SchedulerImpl
import dk.tobiasthedanish.observability.scheduling.TickerImpl
import dk.tobiasthedanish.observability.session.SessionManager
import dk.tobiasthedanish.observability.session.SessionManagerImpl
import dk.tobiasthedanish.observability.session.SessionStore
import dk.tobiasthedanish.observability.session.SessionStoreImpl
import dk.tobiasthedanish.observability.storage.Database
import dk.tobiasthedanish.observability.storage.DatabaseImpl
import dk.tobiasthedanish.observability.time.AndroidTimeProvider
import dk.tobiasthedanish.observability.time.TimeProvider
import dk.tobiasthedanish.observability.tracing.Trace
import dk.tobiasthedanish.observability.tracing.TraceCollector
import dk.tobiasthedanish.observability.tracing.TraceCollectorImpl
import dk.tobiasthedanish.observability.tracing.TraceFactory
import dk.tobiasthedanish.observability.tracing.TraceFactoryImpl
import dk.tobiasthedanish.observability.tracing.TraceStore
import dk.tobiasthedanish.observability.tracing.TraceStoreImpl
import dk.tobiasthedanish.observability.utils.ConfigService
import dk.tobiasthedanish.observability.utils.ConfigServiceImpl
import dk.tobiasthedanish.observability.utils.IdFactory
import dk.tobiasthedanish.observability.utils.IdFactoryImpl
import dk.tobiasthedanish.observability.utils.LocalPreferencesDataStoreImpl
import dk.tobiasthedanish.observability.utils.ManifestReader
import dk.tobiasthedanish.observability.utils.ManifestReaderImpl
import dk.tobiasthedanish.observability.utils.dataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.jetbrains.annotations.TestOnly
import java.util.concurrent.Executors

internal interface ObservabilityConfigInternal {
    val installationManager: InstallationManager
    val sessionManager: SessionManager
    val traceFactory: TraceFactory
    val timeProvider: TimeProvider
    val lifecycleManager: LifecycleManager
    val navigationManager: NavigationManager
    val activityLifecycleCollector: ActivityLifecycleCollector
    val appLifecycleCollector: AppLifecycleCollector
    val unhandledExceptionCollector: UnhandledExceptionCollector
    val navigationCollector: NavigationCollector
    val traceCollector: TraceCollector
    val resourceUsageCollector: ResourceUsageCollector
    val cleanupService: CleanupService
    val exporter: Exporter
    val configService: ConfigService
    val eventTracker: EventTracker
    val eventStore: EventStore
    val traceStore: TraceStore
    val resourceUsageStore: ResourceUsageStore
}

internal class ObservabilityConfigInternalImpl(application: Application) :
    ObservabilityConfigInternal {
    private val database: Database = DatabaseImpl(application)
    private val idFactory: IdFactory = IdFactoryImpl()
    private val scheduler: Scheduler = SchedulerImpl(Executors.newSingleThreadScheduledExecutor(), CoroutineScope(Dispatchers.IO))
    private val localPreferencesDataStore = LocalPreferencesDataStoreImpl(
        dataStore = application.dataStore,
    )
    private val sessionStore: SessionStore = SessionStoreImpl(
        dataStore = localPreferencesDataStore,
        db = database,
        externalScope = CoroutineScope(Dispatchers.IO)
    )

    private val manifestReader: ManifestReader = ManifestReaderImpl(application)
    override val timeProvider: TimeProvider = AndroidTimeProvider()
    override val configService: ConfigService = ConfigServiceImpl(manifestReader)
    override val resourceUsageCollector: ResourceUsageCollector = ResourceUsageCollectorImpl(
        ticker = TickerImpl(scheduler),
        memoryInspector = AndroidMemoryInspector(Runtime.getRuntime()),
        timeProvider = timeProvider,
    )
    override val sessionManager: SessionManager = SessionManagerImpl(
        timeProvider = timeProvider,
        idFactory = idFactory,
        sessionStore = sessionStore,
        db = database,
    )
    override val resourceUsageStore: ResourceUsageStore = ResourceUsageStoreImpl(
        db = database,
        sessionManager = sessionManager,
        idFactory = idFactory
    )
    private val httpService = InternalHttpClientImpl(
        client = HttpClientFactory.client,
        env = configService,
    )
    override val installationManager: InstallationManager = InstallationManagerImpl(
        preferencesDataStore = localPreferencesDataStore,
        idFactory = idFactory,
        scheduler = scheduler,
        httpService = httpService,
    )
    override val exporter: Exporter = ExporterImpl(
        ticker = TickerImpl(scheduler),
        httpService = httpService,
        database = database,
        sessionManager = sessionManager,
        installationManager = installationManager,
        scheduler = scheduler,
    )
    override val eventStore: EventStore = EventStoreImpl(db = database, idFactory = idFactory)
    override val eventTracker: EventTracker = EventTrackerImpl(
        eventStore = eventStore,
        sessionManager = sessionManager,
        exporter = exporter,
    )
    override val traceStore: TraceStore = TraceStoreImpl(
        sessionManager = sessionManager,
        db = database,
    )
    override val traceCollector: TraceCollector = TraceCollectorImpl(
        traceStore = traceStore,
    )
    override val traceFactory: TraceFactory = TraceFactoryImpl(
        idFactory = idFactory,
        traceCollector = traceCollector,
        timeProvider = timeProvider,
    )
    override val cleanupService: CleanupService = CleanupServiceImpl(
        database = database,
        sessionManager = sessionManager,
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
    private val timeProvider by lazy { config.timeProvider }
    private val installationManager by lazy { config.installationManager }
    private val sessionManager by lazy { config.sessionManager }
    private val lifecycleManager by lazy { config.lifecycleManager }
    private val navigationManager by lazy { config.navigationManager }
    private val activityLifecycleCollector by lazy { config.activityLifecycleCollector }
    private val appLifecycleCollector by lazy { config.appLifecycleCollector }
    private val unhandledExceptionCollector by lazy { config.unhandledExceptionCollector }
    private val navigationCollector by lazy { config.navigationCollector }
    private val traceCollector by lazy { config.traceCollector }
    private val resourceUsageCollector by lazy { config.resourceUsageCollector }
    private val traceFactory by lazy { config.traceFactory }
    private val cleanupService by lazy { config.cleanupService }
    private val exporter by lazy { config.exporter }
    private val configService by lazy { config.configService }
    private val eventTracker by lazy { config.eventTracker }
    private val eventStore by lazy { config.eventStore }
    private val traceStore by lazy { config.traceStore }
    private val resourceUsageStore by lazy { config.resourceUsageStore }

    private var isInitialized: Boolean = false
    private var isStarted: Boolean = false
    private val startLock = Any()

    fun init() {
        installationManager.init()
        sessionManager.init()
        if (!configService.init()) {
            Log.e("", "Initializing Observability failed! Will not start SDK.")
            return
        }

        lifecycleManager.addListener(this)
        lifecycleManager.register()
        navigationManager.addCollector(navigationCollector)
        isInitialized = true
    }

    fun start() {
        synchronized(startLock) {
            if (isInitialized && !isStarted) {
                activityLifecycleCollector.register()
                appLifecycleCollector.register()
                unhandledExceptionCollector.register()
                traceCollector.register()
                navigationManager.register()
                exporter.register()
                resourceUsageCollector.addListener(resourceUsageStore)
                resourceUsageCollector.register()
                isStarted = true
            }
        }
    }

    fun stop() {
        synchronized(startLock) {
            if (isInitialized && isStarted) {
                activityLifecycleCollector.unregister()
                appLifecycleCollector.unregister()
                unhandledExceptionCollector.unregister()
                traceCollector.unregister()
                navigationManager.unregister()
                exporter.unregister()
                resourceUsageCollector.removeListener(resourceUsageStore)
                resourceUsageCollector.unregister()
                isStarted = false
            }
        }
    }

    fun getInstallationId(): String {
        return installationManager.installationId
    }

    fun onNavigation(route: String) {
        if (isStarted) {
            navigationManager.onNavigation(route)
        }
    }

    fun exceptionHandled(thread: Thread, throwable: Throwable) {
        val event = ExceptionEventFactory.create(thread, throwable, true)

        eventTracker.track(event, timeProvider.now(), EventTypes.EXCEPTION)
    }

    fun createTrace(name: String): Trace {
        return traceFactory.createTrace(name)
    }

    fun startTrace(name: String): Trace {
        return traceFactory.startTrace(name)
    }

    @TestOnly
    internal fun triggerExport() {
        eventStore.flush()
        traceStore.flush()
        resourceUsageStore.flush()
        exporter.export(sessionManager.getSessionId())
    }

    @TestOnly
    internal fun getSessionId(): String {
        return sessionManager.getSessionId()
    }

    override fun onAppForeground() {
        sessionManager.onAppForeground()
        synchronized(startLock) {
            if (isStarted) {
                exporter.resume()
                resourceUsageCollector.register()
            }
        }
    }

    override fun onAppBackground() {
        sessionManager.onAppBackground()
        synchronized(startLock) {
            if (isStarted) {
                resourceUsageCollector.unregister()
                exporter.pause()
                cleanupService.clearData()
            }
        }
    }
}