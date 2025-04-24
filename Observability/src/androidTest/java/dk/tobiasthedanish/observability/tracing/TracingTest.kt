package dk.tobiasthedanish.observability.tracing

import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.filters.LargeTest
import dk.tobiasthedanish.observability.Observability
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import java.util.ArrayList
import java.util.concurrent.TimeUnit

class TracingTest {
    private val runner = TracingTestRunner()
    private val mockWebServer = MockWebServer()

    @Before
    fun setup() {
        runner.setup()
        mockWebServer.start(8080)
        mockWebServer.enqueue(MockResponse().setResponseCode(201))
        mockWebServer.enqueue(MockResponse().setResponseCode(201))
    }

    @After
    fun teardown() {
        mockWebServer.shutdown()
    }

    @LargeTest
    @Test
    fun testTracing() {
        runner.initObservability()

        ActivityScenario.launch(TraceTestActivity::class.java).use { scenario ->
            scenario.moveToState(Lifecycle.State.RESUMED)

            scenario.onActivity {
                runner.fetchData()
            }
            scenario.moveToState(Lifecycle.State.DESTROYED)
            triggerExport()

            val bodies = aggregateRequests()
            Assert.assertTrue(
                "No trace stored with group id ${TraceTestActivity.rootTrace?.groupId ?: "INVALID"}",
                bodies.didTrackTrace(TraceTestActivity.rootTrace?.groupId ?: "INVALID")
            )
            Assert.assertTrue(
                "No collection export request tracked",
                bodies.didTrackRequest("post", "/api/v1/collection")
            )
        }
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

    private fun List<String>.didTrackTrace(type: String): Boolean {
        val bodies = this

        return bodies.any { body -> body.containsTrace(type) }
    }

    private fun List<String>.didTrackRequest(method: String, path: String): Boolean {
        val bodies = this
        val expected = "${method.uppercase()} $path"

        return bodies.any { body ->
            Log.d("didTrackRequest", "expected: $expected, actual: $body")

            body.containsRequest(expected)
        }
    }

    private fun String.containsTrace(groupId: String): Boolean {
        return contains("\"groupId\":\"$groupId\"")
    }

    private fun String.containsRequest(expected: String): Boolean {
        return contains(expected)
    }
}