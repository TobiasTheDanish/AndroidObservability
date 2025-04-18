package dk.tobiasthedanish.observability.lifecycle

import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import dk.tobiasthedanish.observability.Observability
import dk.tobiasthedanish.observability.events.EventTypes
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class LifecycleEventsTest {
    private val mockWebServer: MockWebServer = MockWebServer()

    companion object {
        private val runner: LifecycleEventsTestRunner = LifecycleEventsTestRunner()
        @JvmStatic
        @BeforeClass
        fun before() {
            runner.disableUncaughtExceptionHandler()
            runner.initObservability()
        }
    }


    @Before
    fun setup() {
        mockWebServer.start(8080)
        mockWebServer.enqueue(MockResponse().setResponseCode(201))

    }

    @After
    fun teardown() {
        mockWebServer.shutdown()
        runner.teardown()
    }

    @Test
    @LargeTest
    fun activityLifecycleEventsTest() {
        ActivityScenario.launch(TestActivity::class.java).use {
            it.moveToState(Lifecycle.State.RESUMED)

            it.moveToState(Lifecycle.State.DESTROYED)
            triggerExport()
            Assert.assertTrue(
                "Web server did NOT track 'lifecycle_activity' event",
                didTrackEvent(EventTypes.LIFECYCLE_ACTIVITY)
            )
        }
    }

    @Test
    @LargeTest
    fun appLifecycleEventsTest() {
        // This function is hella flaky if emulator is running slow due to pressHome function
        ActivityScenario.launch(TestActivity::class.java).use { scenario ->
            scenario.moveToState(Lifecycle.State.RESUMED)

            runner.pressHome()
            triggerExport()

            Assert.assertTrue(
                "Web server did NOT track 'lifecycle_app' event",
                didTrackEvent(EventTypes.LIFECYCLE_APP)
            )
        }

    }

    @Test
    @LargeTest
    fun unhandledExceptionTest() {
        ActivityScenario.launch(TestActivity::class.java).use { scenario ->
            scenario.moveToState(Lifecycle.State.RESUMED)
            scenario.onActivity {
                runner.crashApp()
                //triggerExport()
            }
        }

        Assert.assertTrue(
            "No Unhandled exception event was tracked by server",
            didTrackEvent(EventTypes.UNHANDLED_EXCEPTION)
        )
    }

    @Test
    @LargeTest
    fun navigationEventTest() {
        ActivityScenario.launch(TestActivity::class.java).use {
            it.moveToState(Lifecycle.State.RESUMED)
            it.onActivity {
                runner.navigate()
            }
            it.moveToState(Lifecycle.State.DESTROYED)
            triggerExport()
        }

        Assert.assertTrue(
            "No navigation event was tracked by server",
            didTrackEvent(EventTypes.NAVIGATION)
        )
    }

    private fun triggerExport() {
        Observability.triggerExport()
    }

    private fun didTrackEvent(type: String): Boolean {
        //val request = mockWebServer.takeRequest()
        val request = mockWebServer.takeRequest(2000, TimeUnit.MILLISECONDS)
        Log.d("didTrackEvent", "latest request: $request")
        val body = request?.body?.readUtf8()
        Log.d("didTrackEvent", "latest request body: $body")
        return body != null && body.containsEvent(type)
    }

    private fun String.containsEvent(eventType: String): Boolean {
        return contains("\"type\":\"$eventType\"")
    }
}