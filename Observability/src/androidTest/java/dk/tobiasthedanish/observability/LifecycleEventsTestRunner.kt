package dk.tobiasthedanish.observability

import android.app.Application
import android.app.Instrumentation
import android.util.Log
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import dk.tobiasthedanish.observability.utils.CleanupService
import dk.tobiasthedanish.observability.utils.CleanupServiceImpl
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
import dk.tobiasthedanish.observability.storage.DatabaseImpl
import dk.tobiasthedanish.observability.time.AndroidTimeProvider
import dk.tobiasthedanish.observability.time.TimeProvider
import dk.tobiasthedanish.observability.utils.IdFactory
import dk.tobiasthedanish.observability.utils.IdFactoryImpl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

internal class LifecycleEventsTestRunner {
    private val instrumentation: Instrumentation = InstrumentationRegistry.getInstrumentation()
    private val application = instrumentation.context.applicationContext as Application
    private val device = UiDevice.getInstance(instrumentation)
    private val testEventTracker = TestEventTracker()

    fun wakeup() {
        device.wakeUp()
    }

    fun initObservability() {
        val config = object : ObservabilityConfigInternal {
            private val database = DatabaseImpl(application)
            private val idFactory: IdFactory = IdFactoryImpl()
            private val sessionStore = SessionStoreImpl(
                application.dataStore,
                CoroutineScope(Dispatchers.IO)
            )

            override val cleanupService: CleanupService = CleanupServiceImpl(testEventTracker, database)
            override val timeProvider: TimeProvider = AndroidTimeProvider()
            override val sessionManager: SessionManager = SessionManagerImpl(
                timeProvider = timeProvider,
                idFactory = idFactory,
                sessionStore = sessionStore,
                db = database,
            )
            override val lifecycleManager: LifecycleManager = LifecycleManager(application)
            override val navigationManager: NavigationManager = NavigationManagerImpl()
            override val activityLifecycleCollector: ActivityLifecycleCollector = ActivityLifecycleCollector(
                lifecycleManager = lifecycleManager,
                eventTracker = testEventTracker,
                timeProvider = timeProvider
            )
            override val appLifecycleCollector: AppLifecycleCollector = AppLifecycleCollector(
                lifecycleManager = lifecycleManager,
                eventTracker = testEventTracker,
                timeProvider = timeProvider
            )
            override val unhandledExceptionCollector: UnhandledExceptionCollector = UnhandledExceptionCollector(
                eventTracker = testEventTracker,
                timeProvider = timeProvider,
            )
            override val navigationCollector: NavigationCollector = NavigationCollectorImpl(testEventTracker, timeProvider)

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
        return testEventTracker.eventsEncountered.any { event ->
            event == type
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
}