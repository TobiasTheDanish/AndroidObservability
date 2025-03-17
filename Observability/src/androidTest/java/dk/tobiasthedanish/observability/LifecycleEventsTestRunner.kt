package dk.tobiasthedanish.observability

import android.app.Application
import android.app.Instrumentation
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import dk.tobiasthedanish.observability.utils.CleanupService
import dk.tobiasthedanish.observability.utils.CleanupServiceImpl
import dk.tobiasthedanish.observability.exception.UnhandledExceptionCollector
import dk.tobiasthedanish.observability.lifecycle.ActivityLifecycleCollector
import dk.tobiasthedanish.observability.lifecycle.AppLifecycleCollector
import dk.tobiasthedanish.observability.lifecycle.LifecycleManager
import dk.tobiasthedanish.observability.time.AndroidTimeProvider
import dk.tobiasthedanish.observability.time.TimeProvider

internal class LifecycleEventsTestRunner {
    private val instrumentation: Instrumentation = InstrumentationRegistry.getInstrumentation()
    private val application = instrumentation.context.applicationContext as Application
    private val device = UiDevice.getInstance(instrumentation)
    private val testEventTracker = TestEventTracker()
    private lateinit var observabilityInternal: ObservabilityInternal

    fun initObservability() {
        val config = object : ObservabilityConfigInternal {
            override val cleanupService: CleanupService = CleanupServiceImpl(testEventTracker)
            override val timeProvider: TimeProvider = AndroidTimeProvider()
            override val lifecycleManager: LifecycleManager = LifecycleManager(application)
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
        }

        observabilityInternal = ObservabilityInternal(config)
        observabilityInternal.init()
        observabilityInternal.start()
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
        device.pressHome()
        device.waitForIdle()
    }
}