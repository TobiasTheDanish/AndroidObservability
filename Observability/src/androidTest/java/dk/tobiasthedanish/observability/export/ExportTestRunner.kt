package dk.tobiasthedanish.observability.export

import android.app.Application
import android.app.Instrumentation
import androidx.test.platform.app.InstrumentationRegistry
import dk.tobiasthedanish.observability.Observability
import dk.tobiasthedanish.observability.ObservabilityConfig
import dk.tobiasthedanish.observability.ObservabilityConfigInternal
import dk.tobiasthedanish.observability.events.EventStore
import dk.tobiasthedanish.observability.events.EventStoreImpl
import dk.tobiasthedanish.observability.events.EventTracker
import dk.tobiasthedanish.observability.events.EventTrackerImpl
import dk.tobiasthedanish.observability.utils.CleanupService
import dk.tobiasthedanish.observability.utils.CleanupServiceImpl
import dk.tobiasthedanish.observability.exception.UnhandledExceptionCollector
import dk.tobiasthedanish.observability.http.HttpClientFactory
import dk.tobiasthedanish.observability.http.InternalHttpClientImpl
import dk.tobiasthedanish.observability.installation.InstallationManagerImpl
import dk.tobiasthedanish.observability.lifecycle.ActivityLifecycleCollector
import dk.tobiasthedanish.observability.lifecycle.AppLifecycleCollector
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
import dk.tobiasthedanish.observability.scheduling.Ticker
import dk.tobiasthedanish.observability.scheduling.TickerImpl
import dk.tobiasthedanish.observability.session.SessionManager
import dk.tobiasthedanish.observability.session.SessionManagerImpl
import dk.tobiasthedanish.observability.session.SessionStoreImpl
import dk.tobiasthedanish.observability.storage.Constants
import dk.tobiasthedanish.observability.storage.DatabaseImpl
import dk.tobiasthedanish.observability.time.AndroidTimeProvider
import dk.tobiasthedanish.observability.time.TimeProvider
import dk.tobiasthedanish.observability.tracing.TraceCollector
import dk.tobiasthedanish.observability.tracing.TraceCollectorImpl
import dk.tobiasthedanish.observability.tracing.TraceFactory
import dk.tobiasthedanish.observability.tracing.TraceFactoryImpl
import dk.tobiasthedanish.observability.tracing.TraceStoreImpl
import dk.tobiasthedanish.observability.utils.ConfigService
import dk.tobiasthedanish.observability.utils.ConfigServiceImpl
import dk.tobiasthedanish.observability.utils.IdFactory
import dk.tobiasthedanish.observability.utils.IdFactoryImpl
import dk.tobiasthedanish.observability.utils.LocalPreferencesDataStoreImpl
import dk.tobiasthedanish.observability.utils.ManifestReaderImpl
import dk.tobiasthedanish.observability.utils.dataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import java.util.concurrent.Executors
import kotlin.time.Duration

class ExportTestRunner {
    private val instrumentation: Instrumentation = InstrumentationRegistry.getInstrumentation()
    private val application = instrumentation.context.applicationContext as Application
    private val database = DatabaseImpl(application)
    private val testScheduler = TestScheduler()

    fun initObservability() {
        val config = object : ObservabilityConfigInternal {
            private val manifestReader = ManifestReaderImpl(application)
            override val configService: ConfigService = ConfigServiceImpl(manifestReader, ObservabilityConfig())
            private val scheduler: Scheduler = SchedulerImpl(Executors.newSingleThreadScheduledExecutor(), CoroutineScope(Dispatchers.IO))
            private val ticker: Ticker = TickerImpl(testScheduler)
            private val idFactory: IdFactory = IdFactoryImpl()
            private val localPreferencesDataStore = LocalPreferencesDataStoreImpl(
                dataStore = application.dataStore,
            )
            private val sessionStore = SessionStoreImpl(
                localPreferencesDataStore,
                database,
                CoroutineScope(Dispatchers.IO)
            )
            override val eventStore: EventStore = EventStoreImpl(db = database, idFactory = idFactory, configService)

            override val timeProvider: TimeProvider = AndroidTimeProvider()
            override val sessionManager: SessionManager = SessionManagerImpl(
                timeProvider = timeProvider,
                idFactory = idFactory,
                sessionStore = sessionStore,
                db = database,
                configService = configService,
            )
            override val traceStore = TraceStoreImpl(sessionManager, database, configService)
            override val resourceUsageStore: ResourceUsageStore = ResourceUsageStoreImpl(
                database,
                sessionManager,
                idFactory,
            )
            override val traceCollector: TraceCollector = TraceCollectorImpl(traceStore = traceStore)
            override val resourceUsageCollector: ResourceUsageCollector = ResourceUsageCollectorImpl(
                ticker = TickerImpl(
                    testScheduler
                ),
                timeProvider = timeProvider,
                memoryInspector = AndroidMemoryInspector(Runtime.getRuntime())
            )
            override val traceFactory: TraceFactory = TraceFactoryImpl(
                timeProvider, traceCollector, idFactory
            )
            private val httpService = InternalHttpClientImpl(HttpClientFactory.client, env = configService,)
            override val installationManager = InstallationManagerImpl(
                preferencesDataStore = localPreferencesDataStore,
                idFactory = idFactory,
                scheduler = scheduler,
                httpService = httpService,
                timeProvider = timeProvider,
            )
            override val cleanupService: CleanupService = CleanupServiceImpl(database, sessionManager)
            override val exporter: Exporter = ExporterImpl(
                ticker = ticker,
                httpService = httpService,
                database = database,
                sessionManager = sessionManager,
                installationManager = installationManager,
                scheduler = scheduler,
                configService = configService,
            )
            override val eventTracker: EventTracker = EventTrackerImpl(eventStore = eventStore, sessionManager, exporter = exporter)
            override val lifecycleManager: LifecycleManager = LifecycleManager(application)
            override val navigationManager: NavigationManager = NavigationManagerImpl()
            override val activityLifecycleCollector: ActivityLifecycleCollector = ActivityLifecycleCollector(
                lifecycleManager = lifecycleManager,
                eventTracker = eventTracker,
                timeProvider = timeProvider
            )
            override val appLifecycleCollector: AppLifecycleCollector = AppLifecycleCollector(
                lifecycleManager = lifecycleManager,
                eventTracker = eventTracker,
                timeProvider = timeProvider
            )
            override val unhandledExceptionCollector: UnhandledExceptionCollector = UnhandledExceptionCollector(
                eventTracker = eventTracker,
                timeProvider = timeProvider,
            )
            override val navigationCollector: NavigationCollector = NavigationCollectorImpl(eventTracker, timeProvider)
        }

        Observability.initInstrumentationTest(config)
    }

    fun advanceTimeBy(time: Duration) {
        testScheduler.advanceTimeBy(time)
    }

    fun teardown() {
        Observability.triggerExport()
        database.writableDatabase.delete(Constants.DB.MemoryUsageTable.NAME, null, null)
    }
}