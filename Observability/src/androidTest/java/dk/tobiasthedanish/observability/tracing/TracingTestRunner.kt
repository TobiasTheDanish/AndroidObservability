package dk.tobiasthedanish.observability.tracing

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
import dk.tobiasthedanish.observability.exception.UnhandledExceptionCollector
import dk.tobiasthedanish.observability.lifecycle.ActivityLifecycleCollector
import dk.tobiasthedanish.observability.lifecycle.AppLifecycleCollector
import dk.tobiasthedanish.observability.lifecycle.LifecycleManager
import dk.tobiasthedanish.observability.navigation.NavigationCollector
import dk.tobiasthedanish.observability.navigation.NavigationCollectorImpl
import dk.tobiasthedanish.observability.navigation.NavigationManager
import dk.tobiasthedanish.observability.navigation.NavigationManagerImpl
import dk.tobiasthedanish.observability.session.SessionManager
import dk.tobiasthedanish.observability.session.SessionManagerImpl
import dk.tobiasthedanish.observability.session.SessionStoreImpl
import dk.tobiasthedanish.observability.session.dataStore
import dk.tobiasthedanish.observability.storage.Constants
import dk.tobiasthedanish.observability.storage.DatabaseImpl
import dk.tobiasthedanish.observability.time.AndroidTimeProvider
import dk.tobiasthedanish.observability.time.TimeProvider
import dk.tobiasthedanish.observability.utils.CleanupService
import dk.tobiasthedanish.observability.utils.CleanupServiceImpl
import dk.tobiasthedanish.observability.utils.IdFactory
import dk.tobiasthedanish.observability.utils.IdFactoryImpl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

class TracingTestRunner {
    private val instrumentation: Instrumentation = InstrumentationRegistry.getInstrumentation()
    private val application = instrumentation.context.applicationContext as Application
    private val device = UiDevice.getInstance(instrumentation)
    private val database = DatabaseImpl(application)
    private val idFactory: IdFactory = IdFactoryImpl()
    private val sessionStore = SessionStoreImpl(
        application.dataStore,
        CoroutineScope(Dispatchers.IO)
    )
    private val eventStore: EventStore = EventStoreImpl(db = database, idFactory = idFactory)
    private lateinit var traceStore: TraceStore

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

            override val cleanupService: CleanupService = CleanupServiceImpl(database)
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

    fun didStoreTrace(groupId: String): Boolean {
        return tracesStoredCount(groupId) > 0
    }

    fun tracesStoredCount(groupId: String): Int {
        try {
            traceStore.flush()
            database.readableDatabase.rawQuery(
                """
                    SELECT COUNT(${Constants.DB.TraceTable.COL_TRACE_ID}) AS count 
                    FROM ${Constants.DB.TraceTable.NAME} 
                    WHERE ${Constants.DB.TraceTable.COL_GROUP_ID} = ?
                """.trimIndent(),
                arrayOf(groupId)
            ).use {
                if (it.moveToFirst()) {
                    val count = it.getInt(it.getColumnIndex("count"))
                    return count
                } else {
                    Log.e("tracesStoredCount", "Cursor was empty")
                    return -1
                }
            }
        } catch (e: SQLiteException) {
            Log.e("tracesStoredCount", "Database error", e)
            return -1
        }
    }

    fun logTraces() {
        try {
            traceStore.flush()
            database.readableDatabase.rawQuery(
                """
                    SELECT ${Constants.DB.TraceTable.COL_TRACE_ID}, ${Constants.DB.TraceTable.COL_GROUP_ID}, ${Constants.DB.TraceTable.COL_NAME}  
                    FROM ${Constants.DB.TraceTable.NAME} 
                """.trimIndent(),
                arrayOf()
            ).use {
                while (it.moveToNext()) {
                    val traceId = it.getString(it.getColumnIndex(Constants.DB.TraceTable.COL_TRACE_ID))
                    val groupId = it.getString(it.getColumnIndex(Constants.DB.TraceTable.COL_GROUP_ID))
                    val name = it.getString(it.getColumnIndex(Constants.DB.TraceTable.COL_NAME))

                    Log.d("logTraces", "name: $name, groupId: $groupId, traceId: $traceId")
                }
            }
        } catch (e: SQLiteException) {
            Log.e("logTraces", "Database error", e)
        }
    }
}