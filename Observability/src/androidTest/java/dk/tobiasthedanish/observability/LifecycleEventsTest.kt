package dk.tobiasthedanish.observability

import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import dk.tobiasthedanish.observability.events.EventTypes
import org.junit.After
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

    @After
    fun teardown() {
        runner.teardown()
    }

    @Test
    @LargeTest
    fun activityLifecycleEventsTest() {
        runner.initObservability()

        ActivityScenario.launch(TestActivity::class.java).use {
            it.moveToState(Lifecycle.State.RESUMED)

            Assert.assertTrue(runner.didTrackEvent(EventTypes.LIFECYCLE_ACTIVITY))
            Assert.assertTrue("Less than the expected amount of 4 event were tracked", 2 <= runner.trackedEventCount(EventTypes.LIFECYCLE_ACTIVITY))

            it.moveToState(Lifecycle.State.DESTROYED)
            Assert.assertTrue("Less than the expected amount of 4 event were tracked", 4 <= runner.trackedEventCount(EventTypes.LIFECYCLE_ACTIVITY))
        }
    }

    @Test
    @LargeTest
    fun appLifecycleEventsTest() {
        // This function is hella flaky if emulator is running slow due to pressHome function
        runner.initObservability()

        ActivityScenario.launch(TestActivity::class.java).use { scenario ->
            scenario.moveToState(Lifecycle.State.RESUMED)
            Assert.assertTrue(runner.didTrackEvent(EventTypes.LIFECYCLE_APP))
            Assert.assertEquals(1, runner.trackedEventCount(EventTypes.LIFECYCLE_APP))

            runner.pressHome()
            Assert.assertEquals(2, runner.trackedEventCount(EventTypes.LIFECYCLE_APP))
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