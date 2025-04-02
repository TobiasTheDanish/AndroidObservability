package dk.tobiasthedanish.observability.lifecycle

import android.app.Application
import android.app.Instrumentation
import android.database.sqlite.SQLiteException
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
import dk.tobiasthedanish.observability.tracing.TraceStore
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
    private val scheduler: Scheduler = SchedulerImpl(Executors.newSingleThreadScheduledExecutor())
    private val idFactory: IdFactory = IdFactoryImpl()
    private val localPreferencesDataStore = LocalPreferencesDataStoreImpl(
        dataStore = application.dataStore,
        idFactory = idFactory
    )
    private val sessionStore = SessionStoreImpl(
        localPreferencesDataStore,
        CoroutineScope(Dispatchers.IO)
    )
    private val eventStore: EventStore = EventStoreImpl(db = database, idFactory = idFactory)
    private lateinit var traceStore: TraceStore

    fun wakeup() {
        device.wakeUp()
    }

    fun initObservability() {
        val timeProvider: TimeProvider = AndroidTimeProvider()
        val sessionManager = SessionManagerImpl(
            timeProvider = timeProvider,
            idFactory = idFactory,
            sessionStore = sessionStore,
            db = database,
        )
        traceStore = TraceStoreImpl(sessionManager, database)

        val config = object : ObservabilityConfigInternal {
            override val timeProvider: TimeProvider = timeProvider
            override val sessionManager: SessionManager = sessionManager
            override val traceCollector: TraceCollector = TraceCollectorImpl(traceStore = traceStore)
            override val traceFactory: TraceFactory = TraceFactoryImpl(
                timeProvider, traceCollector, idFactory
            )

            private val eventTracker: EventTracker = EventTrackerImpl(eventStore = eventStore, sessionManager)
            private val ticker: Ticker = TickerImpl(scheduler)
            private val manifestReader = ManifestReaderImpl(application)

            override val cleanupService: CleanupService = CleanupServiceImpl(database)
            override val exporter: Exporter = ExporterImpl(ticker)
            override val configService: ConfigService = ConfigServiceImpl(manifestReader)
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
        Thread.setDefaultUncaughtExceptionHandler { _, _ ->
            // Disable default exception handler to prevent crash dialog
        }
    }

    fun crashApp() {
        Thread.getDefaultUncaughtExceptionHandler()!!.uncaughtException(
            Thread.currentThread(),
            Exception("Test exception"),
        )
    }

    fun didTrackEvent(type: String): Boolean {
        return trackedEventCount(type) > 0
    }

    fun trackedEventCount(type: String): Int {
        try {
            eventStore.flush()
            database.readableDatabase.rawQuery(
                """
                    SELECT COUNT(${Constants.DB.EventTable.COL_ID}) AS count 
                    FROM ${Constants.DB.EventTable.NAME} 
                    WHERE ${Constants.DB.EventTable.COL_TYPE} = ?
                """.trimIndent(),
                arrayOf(type)
            ).use {
                if (it.moveToFirst()) {
                    val count = it.getInt(it.getColumnIndex("count"))
                    return count
                } else {
                    Log.e("didTrackEvent", "Cursor was empty")
                    return -1
                }
            }
        } catch (e: SQLiteException) {
            Log.e("didTrackEvent", "Database error", e)
            return -1
        }
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
        database.writableDatabase.delete(Constants.DB.EventTable.NAME, null, null)
    }
}