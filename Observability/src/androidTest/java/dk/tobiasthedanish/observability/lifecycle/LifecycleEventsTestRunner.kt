package dk.tobiasthedanish.observability.lifecycle

import android.app.Application
import android.app.Instrumentation
import android.util.Log
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
import dk.tobiasthedanish.observability.utils.CleanupService
import dk.tobiasthedanish.observability.utils.CleanupServiceImpl
import dk.tobiasthedanish.observability.exception.UnhandledExceptionCollector
import dk.tobiasthedanish.observability.export.Exporter
import dk.tobiasthedanish.observability.export.ExporterImpl
import dk.tobiasthedanish.observability.http.HttpClientFactory
import dk.tobiasthedanish.observability.http.InternalHttpClientImpl
import dk.tobiasthedanish.observability.installation.InstallationManagerImpl
import dk.tobiasthedanish.observability.navigation.NavigationCollector
import dk.tobiasthedanish.observability.navigation.NavigationCollectorImpl
import dk.tobiasthedanish.observability.navigation.NavigationManager
import dk.tobiasthedanish.observability.navigation.NavigationManagerImpl
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

internal class LifecycleEventsTestRunner {
    private val instrumentation: Instrumentation = InstrumentationRegistry.getInstrumentation()
    private val application = instrumentation.context.applicationContext as Application
    private val device = UiDevice.getInstance(instrumentation)
    private val database = DatabaseImpl(application)

    fun wakeup() {
        device.wakeUp()
    }

    fun initObservability() {
        val config = object : ObservabilityConfigInternal {
            private val scheduler: Scheduler = SchedulerImpl(Executors.newSingleThreadScheduledExecutor(), CoroutineScope(Dispatchers.IO))
            private val idFactory: IdFactory = IdFactoryImpl()
            private val localPreferencesDataStore = LocalPreferencesDataStoreImpl(
                dataStore = application.dataStore,
            )
            private val sessionStore = SessionStoreImpl(
                localPreferencesDataStore,
                database,
                CoroutineScope(Dispatchers.IO)
            )
            override val eventStore: EventStore = EventStoreImpl(db = database, idFactory = idFactory)

            override val timeProvider: TimeProvider = AndroidTimeProvider()
            override val sessionManager: SessionManager = SessionManagerImpl(
                timeProvider = timeProvider,
                idFactory = idFactory,
                sessionStore = sessionStore,
                db = database,
            )
            override val traceStore = TraceStoreImpl(sessionManager, database)
            override val traceCollector: TraceCollector = TraceCollectorImpl(traceStore = traceStore)
            override val traceFactory: TraceFactory = TraceFactoryImpl(
                timeProvider, traceCollector, idFactory
            )

            private val ticker: Ticker = TickerImpl(scheduler)
            private val manifestReader = ManifestReaderImpl(application)

            override val configService: ConfigService = ConfigServiceImpl(manifestReader)
            private val httpService = InternalHttpClientImpl(HttpClientFactory.client, env = configService,)
            override val installationManager = InstallationManagerImpl(
                preferencesDataStore = localPreferencesDataStore,
                idFactory = idFactory,
                scheduler = scheduler,
                httpService = httpService,
            )
            override val cleanupService: CleanupService = CleanupServiceImpl(database, sessionManager)
            override val exporter: Exporter = ExporterImpl(
                ticker = ticker,
                httpService = httpService,
                database = database,
                sessionManager = sessionManager,
                installationManager = installationManager,
                scheduler = scheduler
            )
            private val eventTracker: EventTracker = EventTrackerImpl(eventStore = eventStore, sessionManager, exporter = exporter)
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

    fun disableUncaughtExceptionHandler() {
        Thread.setDefaultUncaughtExceptionHandler { thread, cause ->
            // Disable default exception handler to prevent crash dialog
            Log.d("LifecycleEventsTestRunner", "Thread ${thread.name} threw unhandled exception: ${cause.javaClass.name}", cause)
        }
    }

    fun crashApp() {
        Thread.getDefaultUncaughtExceptionHandler()!!.uncaughtException(
            Thread.currentThread(),
            Exception("Test exception"),
        )
    }

    fun pressHome() {
        // This function is hella flaky if emulator is running slow
        if (!device.pressHome()) {
            Log.e("LifecycleEventsTestRunner", "Pressing home failed...")
            device.pressBack()
        }

        device.waitForIdle()
    }

    fun navigate() {
        //val navigateButton = device.findObject(By.text("Navigate"))
        val navigateButton = device.findObject(By.clickable(true))
        navigateButton.click()
        device.wait(Until.hasObject(By.text("SecondaryScreen")),5000)
    }

    fun teardown() {
        Observability.triggerExport()
        database.writableDatabase.delete(Constants.DB.EventTable.NAME, null, null)
    }
}