package dk.tobiasthedanish.observability

import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import dk.tobiasthedanish.observability.events.EventTypes
import dk.tobiasthedanish.observability.lifecycle.ActivityLifecycleEventType
import dk.tobiasthedanish.observability.lifecycle.AppLifecycleEventType
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LifecycleEventsTest {
    private lateinit var runner: LifecycleEventsTestRunner

    @Before
    fun setup() {
        runner = LifecycleEventsTestRunner()
        runner.wakeup()
    }

    @Test
    @LargeTest
    fun activityLifecycleEventsTest() {
        runner.initObservability()

        ActivityScenario.launch(TestActivity::class.java).use {
            it.moveToState(Lifecycle.State.RESUMED)

            Assert.assertTrue(runner.didTrackEvent(ActivityLifecycleEventType.CREATED))
            Assert.assertTrue(runner.didTrackEvent(ActivityLifecycleEventType.RESUMED))
            Assert.assertFalse(runner.didTrackEvent(ActivityLifecycleEventType.PAUSED))
            Assert.assertFalse(runner.didTrackEvent(ActivityLifecycleEventType.DESTROYED))

            it.moveToState(Lifecycle.State.DESTROYED)
            Assert.assertTrue(runner.didTrackEvent(ActivityLifecycleEventType.PAUSED))
            Assert.assertTrue(runner.didTrackEvent(ActivityLifecycleEventType.DESTROYED))
        }
    }

    @Test
    @LargeTest
    fun appLifecycleEventsTest() {
        // This function is hella flaky if emulator is running slow due to pressHome function
        runner.initObservability()

        ActivityScenario.launch(TestActivity::class.java).use { scenario ->
            scenario.moveToState(Lifecycle.State.RESUMED)
            Assert.assertTrue(runner.didTrackEvent(AppLifecycleEventType.FOREGROUND))
            Assert.assertFalse(runner.didTrackEvent(AppLifecycleEventType.BACKGROUND))

            runner.pressHome()
            Assert.assertTrue(runner.didTrackEvent(AppLifecycleEventType.BACKGROUND))
        }

    }

    @Test
    @LargeTest
    fun unhandledExceptionTest() {
        runner.disableUncaughtExceptionHandler()
        runner.initObservability()
        ActivityScenario.launch(TestActivity::class.java).use { scenario ->
            scenario.moveToState(Lifecycle.State.RESUMED)
            scenario.onActivity {
                runner.crashApp()
            }
        }

        Assert.assertTrue("No Unhandled exception event was tracked", runner.didTrackEvent(EventTypes.UNHANDLED_EXCEPTION))
    }

    @Test
    @LargeTest
    fun navigationEventTest() {
        runner.initObservability()

        ActivityScenario.launch(TestActivity::class.java).use {
            it.moveToState(Lifecycle.State.RESUMED)
            it.onActivity {
                runner.navigate()
            }
        }

        Assert.assertTrue("No navigation event was tracked", runner.didTrackEvent(EventTypes.NAVIGATION))
    }
}