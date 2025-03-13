package dk.tobiasthedanish.observability

import android.app.Application
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dk.tobiasthedanish.observability.events.EventTypes
import dk.tobiasthedanish.observability.lifecycle.ActivityLifecycleCollector
import dk.tobiasthedanish.observability.lifecycle.ActivityLifecycleEvent
import dk.tobiasthedanish.observability.lifecycle.AppLifecycleCollector
import dk.tobiasthedanish.observability.lifecycle.AppLifecycleEvent
import dk.tobiasthedanish.observability.lifecycle.AppLifecycleEventType
import dk.tobiasthedanish.observability.lifecycle.LifecycleManager
import dk.tobiasthedanish.observability.time.AndroidTimeProvider
import dk.tobiasthedanish.observability.time.TimeProvider
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LifecycleEventsTest {
    companion object {
        val testEventTracker = TestEventTracker()
        internal lateinit var observabilityInternal: ObservabilityInternal

        @JvmStatic
        @BeforeClass
        fun setupSuite() {
            val instrumentation = InstrumentationRegistry.getInstrumentation()
            val application = instrumentation.context.applicationContext as Application

            val config = object : ObservabilityConfigInternal {
                val testEventTracker = LifecycleEventsTest.testEventTracker
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
            }

            observabilityInternal = ObservabilityInternal(config)
            observabilityInternal.init()
        }
    }

    @Before
    fun setupTest() {
        observabilityInternal.start()
    }

    @After
    fun teardownTest() {
        observabilityInternal.stop()
        testEventTracker.clear()
    }

    @Test
    fun activityLifecycleEventsTest() {
        ActivityScenario.launch(TestActivity::class.java).use {
            it.moveToState(Lifecycle.State.RESUMED)

            var events = testEventTracker.eventsForType(EventTypes.LIFECYCLE_ACTIVITY)?.filter { item ->
                    when (item) {
                        is ActivityLifecycleEvent -> item.className == TestActivity::class.java.name
                        else -> false
                    }
                }
            Assert.assertNotNull("No event received...", events)
            Assert.assertEquals(2, events?.size)

            it.moveToState(Lifecycle.State.DESTROYED)
            events = testEventTracker.eventsForType(EventTypes.LIFECYCLE_ACTIVITY)?.filter { item ->
                    when (item) {
                        is ActivityLifecycleEvent -> item.className == TestActivity::class.java.name
                        else -> false
                    }
                }
            Assert.assertNotNull("No event received...", events)
            Assert.assertEquals("Expected 2 but got ${events?.size}.\nEvents: $events", 2, events?.size)
        }
    }

    @Test
    fun appLifecycleEventsTest() {
        ActivityScenario.launch(TestActivity::class.java).use {
            it.moveToState(Lifecycle.State.RESUMED)
            val events = testEventTracker.eventsForType(EventTypes.LIFECYCLE_APP)
            Assert.assertNotNull("No event received...", events)
            Assert.assertEquals(1, events?.size)
            val event = events?.get(0)
            Assert.assertNotNull(event)
            Assert.assertTrue(event is AppLifecycleEvent)
            Assert.assertEquals(AppLifecycleEventType.FOREGROUND, (event as AppLifecycleEvent).type)
            it.moveToState(Lifecycle.State.DESTROYED)
        }

        val events = testEventTracker.eventsForType(EventTypes.LIFECYCLE_APP)
        Assert.assertNotNull("No event received...", events)
        Assert.assertEquals(1, events?.size)
        val event = events?.get(0)
        Assert.assertNotNull(event)
        Assert.assertTrue(event is AppLifecycleEvent)
        Assert.assertEquals(AppLifecycleEventType.BACKGROUND, (event as AppLifecycleEvent).type)
    }
}