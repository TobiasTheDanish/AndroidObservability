package dk.tobiasthedanish.observability.events

import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import dk.tobiasthedanish.observability.Observability
import dk.tobiasthedanish.observability.trackEvent
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
class CustomEventTest {
    private val mockWebServer: MockWebServer = MockWebServer()

    companion object {
        private val runner: CustomEventTestRunner = CustomEventTestRunner()
        @JvmStatic
        @BeforeClass
        fun before() {
            Observability.stop()
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
    fun testTrackCustomEvent() {
        val nonNullData = CustomEventTestData(title = "Romeo and Juliet", description = "A love story by William Shakespeare")
        Observability.trackEvent(nonNullData)

        val nullData = CustomEventTestData(title = "Hamlet")
        Observability.trackEvent(nullData)

        triggerExport()

        val bodies = aggregateRequests()
        Assert.assertTrue(bodies.didTrackRequest("POST", "/api/v1/collection"))
        Assert.assertTrue(bodies.didTrackEvent(EventTypes.CUSTOM))
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