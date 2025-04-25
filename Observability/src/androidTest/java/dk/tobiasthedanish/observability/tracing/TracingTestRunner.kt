package dk.tobiasthedanish.observability.tracing

import android.app.Application
import android.app.Instrumentation
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import dk.tobiasthedanish.observability.Observability
import dk.tobiasthedanish.observability.ObservabilityConfigInternal
import dk.tobiasthedanish.observability.events.EventStore
import dk.tobiasthedanish.observability.events.EventStoreImpl
import dk.tobiasthedanish.observability.events.EventTracker
import dk.tobiasthedanish.observability.events.EventTrackerImpl
import dk.tobiasthedanish.observability.exception.UnhandledExceptionCollector
import dk.tobiasthedanish.observability.export.Exporter
import dk.tobiasthedanish.observability.export.ExporterImpl
import dk.tobiasthedanish.observability.http.HttpClientFactory
import dk.tobiasthedanish.observability.http.InternalHttpClientImpl
import dk.tobiasthedanish.observability.installation.InstallationManager
import dk.tobiasthedanish.observability.installation.InstallationManagerImpl
import dk.tobiasthedanish.observability.lifecycle.ActivityLifecycleCollector
import dk.tobiasthedanish.observability.lifecycle.AppLifecycleCollector
import dk.tobiasthedanish.observability.lifecycle.LifecycleManager
import dk.tobiasthedanish.observability.navigation.NavigationCollector
import dk.tobiasthedanish.observability.navigation.NavigationCollectorImpl
import dk.tobiasthedanish.observability.navigation.NavigationManager
import dk.tobiasthedanish.observability.navigation.NavigationManagerImpl
import dk.tobiasthedanish.observability.runtime.ResourceUsageCollector
import dk.tobiasthedanish.observability.runtime.ResourceUsageStore
import dk.tobiasthedanish.observability.scheduling.Scheduler
import dk.tobiasthedanish.observability.scheduling.SchedulerImpl
import dk.tobiasthedanish.observability.scheduling.Ticker
import dk.tobiasthedanish.observability.scheduling.TickerImpl
import dk.tobiasthedanish.observability.session.SessionManager
import dk.tobiasthedanish.observability.session.SessionManagerImpl
import dk.tobiasthedanish.observability.session.SessionStoreImpl
import dk.tobiasthedanish.observability.storage.DatabaseImpl
import dk.tobiasthedanish.observability.time.AndroidTimeProvider
import dk.tobiasthedanish.observability.time.TimeProvider
import dk.tobiasthedanish.observability.utils.CleanupService
import dk.tobiasthedanish.observability.utils.CleanupServiceImpl
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

class TracingTestRunner {
    private val instrumentation: Instrumentation = InstrumentationRegistry.getInstrumentation()
    private val application = instrumentation.context.applicationContext as Application
    private val device = UiDevice.getInstance(instrumentation)
    private val database = DatabaseImpl(application)

    fun initObservability() {

        val config = object : ObservabilityConfigInternal {
            private val scheduler: Scheduler = SchedulerImpl(
                Executors.newSingleThreadScheduledExecutor(),
                CoroutineScope(Dispatchers.IO)
            )
            private val idFactory: IdFactory = IdFactoryImpl()
            private val localPreferencesDataStore = LocalPreferencesDataStoreImpl(
                dataStore = application.dataStore,
            )
            private val sessionStore = SessionStoreImpl(
                localPreferencesDataStore,
                database,
                CoroutineScope(Dispatchers.IO)
            )
            override val eventStore: EventStore =
                EventStoreImpl(db = database, idFactory = idFactory)

            override val timeProvider: TimeProvider = AndroidTimeProvider()
            override val sessionManager: SessionManager = SessionManagerImpl(
                timeProvider = timeProvider,
                idFactory = idFactory,
                sessionStore = sessionStore,
                db = database,
            )
            override val traceStore = TraceStoreImpl(sessionManager, database)
            override val resourceUsageStore: ResourceUsageStore
                get() = TODO("Not yet implemented")
            override val traceCollector: TraceCollector =
                TraceCollectorImpl(traceStore = traceStore)
            override val resourceUsageCollector: ResourceUsageCollector
                get() = TODO("Not yet implemented")
            override val traceFactory: TraceFactory = TraceFactoryImpl(
                timeProvider, traceCollector, idFactory
            )

            private val ticker: Ticker = TickerImpl(scheduler)
            private val manifestReader = ManifestReaderImpl(application)

            override val cleanupService: CleanupService =
                CleanupServiceImpl(database, sessionManager)
            override val configService: ConfigService = ConfigServiceImpl(manifestReader)
            private val httpService =
                InternalHttpClientImpl(HttpClientFactory.client, configService)
            override val installationManager: InstallationManager = InstallationManagerImpl(
                preferencesDataStore = localPreferencesDataStore,
                idFactory = idFactory,
                scheduler = scheduler,
                httpService = httpService
            )
            override val exporter: Exporter =
                ExporterImpl(ticker, httpService, database, sessionManager, installationManager, scheduler)
            private val eventTracker: EventTracker =
                EventTrackerImpl(eventStore = eventStore, sessionManager, exporter = exporter)
            override val lifecycleManager: LifecycleManager = LifecycleManager(application)
            override val navigationManager: NavigationManager = NavigationManagerImpl()
            override val activityLifecycleCollector: ActivityLifecycleCollector =
                ActivityLifecycleCollector(
                    lifecycleManager = lifecycleManager,
                    eventTracker = eventTracker,
                    timeProvider = timeProvider
                )
            override val appLifecycleCollector: AppLifecycleCollector = AppLifecycleCollector(
                lifecycleManager = lifecycleManager,
                eventTracker = eventTracker,
                timeProvider = timeProvider
            )
            override val unhandledExceptionCollector: UnhandledExceptionCollector =
                UnhandledExceptionCollector(
                    eventTracker = eventTracker,
                    timeProvider = timeProvider,
                )
            override val navigationCollector: NavigationCollector =
                NavigationCollectorImpl(eventTracker, timeProvider)
        }

        Observability.initInstrumentationTest(config)
    }

    fun setup() {
        device.wakeUp()
    }

    fun fetchData() {
        val dataButton = device.findObject(By.clickable(true))
        dataButton.click()
        device.wait(Until.hasObject(By.text("Loading")), 500)
        device.wait(Until.gone(By.text("Loading")), 5000)
        device.waitForIdle()
    }

    fun pressHome() {
        device.pressHome()
        device.waitForIdle()
    }
}