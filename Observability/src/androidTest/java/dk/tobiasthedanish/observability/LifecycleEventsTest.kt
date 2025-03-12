package dk.tobiasthedanish.observability

import android.app.Application
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.util.filter
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dk.tobiasthedanish.observability.events.EventTypes
import dk.tobiasthedanish.observability.lifecycle.ActivityLifecycleCollector
import dk.tobiasthedanish.observability.lifecycle.ActivityLifecycleEvent
import dk.tobiasthedanish.observability.lifecycle.LifecycleManager
import dk.tobiasthedanish.observability.time.AndroidTimeProvider
import dk.tobiasthedanish.observability.time.TimeProvider
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LifecycleEventsTest {
    @Test
    fun activityLifecycleEventsTest() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val application = instrumentation.context.applicationContext as Application

        val config = object : ObservabilityConfigInternal {
            val testEventTracker = TestEventTracker()
            override val timeProvider: TimeProvider = AndroidTimeProvider()
            override val lifecycleManager: LifecycleManager = LifecycleManager(application)
            override val activityLifecycleCollector: ActivityLifecycleCollector = ActivityLifecycleCollector(lifecycleManager, testEventTracker, timeProvider)
        }

        val observabilityInternal = ObservabilityInternal(config)
        observabilityInternal.init()
        observabilityInternal.start()

        ActivityScenario.launch(TestActivity::class.java).use {
            it.moveToState(Lifecycle.State.RESUMED)

            var events = config.testEventTracker.eventsForType(EventTypes.LIFECYCLE_ACTIVITY)?.filter { item ->
                    when (item) {
                        is ActivityLifecycleEvent -> item.className == TestActivity::class.java.name
                        else -> false
                    }
                }
            Assert.assertNotNull("No event received...", events)
            Assert.assertEquals(2, events?.size)

            it.moveToState(Lifecycle.State.DESTROYED)
            events = config.testEventTracker.eventsForType(EventTypes.LIFECYCLE_ACTIVITY)?.filter { item ->
                    when (item) {
                        is ActivityLifecycleEvent -> item.className == TestActivity::class.java.name
                        else -> false
                    }
                }
            Assert.assertNotNull("No event received...", events)
            Assert.assertEquals("Expected 2 but got ${events?.size}.\nEvents: $events", 2, events?.size)
        }
    }
}