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
import java.util.ArrayList
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class LifecycleEventsTest {
    private val mockWebServer: MockWebServer = MockWebServer()

    companion object {
        private val runner: LifecycleEventsTestRunner = LifecycleEventsTestRunner()
        @JvmStatic
        @BeforeClass
        fun before() {
            Observability.stop()
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

            val bodies = aggregateRequests(3)
            Assert.assertTrue(
                "Web server did NOT track 'lifecycle_activity' event",
                bodies.didTrackEvent(EventTypes.LIFECYCLE_ACTIVITY)
            )
            Assert.assertTrue(
                "Web server did NOT track collection request",
                bodies.didTrackRequest("POST", "/api/v1/collection")
            )
        }
    }

    @Test
    @LargeTest
    fun appLifecycleEventsTest() {
        ActivityScenario.launch(TestActivity::class.java).use { scenario ->
            scenario.moveToState(Lifecycle.State.RESUMED)

            runner.pressHome()
            triggerExport()

            val bodies = aggregateRequests()
            Assert.assertTrue(
                "Web server did NOT track 'lifecycle_app' event",
                bodies.didTrackEvent(EventTypes.LIFECYCLE_APP)
            )
            Assert.assertTrue(
                "Web server did NOT track collection request",
                bodies.didTrackRequest("POST", "/api/v1/collection")
            )
        }

    }

    @Test
    @LargeTest
    fun unhandledExceptionTest() {
        mockWebServer.enqueue(MockResponse().setResponseCode(201))
        mockWebServer.enqueue(MockResponse().setResponseCode(201))
        ActivityScenario.launch(TestActivity::class.java).use { scenario ->
            scenario.moveToState(Lifecycle.State.RESUMED)
            scenario.onActivity {
                runner.crashApp()
                //triggerExport()
            }
        }

        val bodies = aggregateRequests(3)
        Assert.assertTrue(
            "No Unhandled exception event was tracked by server",
            bodies.didTrackEvent(EventTypes.EXCEPTION)
        )
        Assert.assertTrue(
            "No session crash request was tracked by server",
            bodies.didTrackRequest("post", "/api/v1/sessions/${Observability.getSessionId()}/crash")
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

        val bodies = aggregateRequests()
        Assert.assertTrue(
            "No navigation event was tracked by server",
            bodies.didTrackEvent(EventTypes.NAVIGATION)
        )
        Assert.assertTrue(
            "Web server did NOT track collection request",
            bodies.didTrackRequest("POST", "/api/v1/collection")
        )
    }

    private fun triggerExport() {
        Observability.triggerExport()
    }

    private fun aggregateRequests(maxRequests: Int = 5): List<String> {
        val requestList = ArrayList<String>(maxRequests)

        var count = 0
        var request = mockWebServer.takeRequest(2000, TimeUnit.MILLISECONDS)
        while (count < maxRequests && request != null) {
            Log.d("aggregateRequests", "request: $request")
            val body = request.body.readUtf8()
            Log.d("aggregateRequests", "request body: $body")
            requestList.add("$request\n$body")
            request = mockWebServer.takeRequest(2000, TimeUnit.MILLISECONDS)
            count++
        }

        return requestList
    }

    private fun List<String>.didTrackRequest(method: String, path: String): Boolean {
        val bodies = this
        val expected = "${method.uppercase()} $path"

        return bodies.any { body ->
            Log.d("didTrackRequest", "expected: $expected, actual: $body")

            body.contains(expected)
        }
    }

    private fun List<String>.didTrackEvent(type: String): Boolean {
        val bodies = this

        return bodies.any { body -> body.containsEvent(type) }
    }

    private fun String.containsEvent(eventType: String): Boolean {
        return contains("\"type\":\"$eventType\"")
    }
}