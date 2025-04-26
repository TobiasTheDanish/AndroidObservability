package dk.tobiasthedanish.observability.export

import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import dk.tobiasthedanish.observability.Observability
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
import kotlin.time.Duration.Companion.seconds

@RunWith(AndroidJUnit4::class)
class ExportTest {
    private val mockWebServer: MockWebServer = MockWebServer()

    companion object {
        private val runner: ExportTestRunner = ExportTestRunner()
        @JvmStatic
        @BeforeClass
        fun before() {
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
    fun testMemoryUsageExport() {
        runner.advanceTimeBy(20.seconds)

        triggerExport()

        val bodies = aggregateRequests()
        Assert.assertTrue(
            "Web server did NOT track collection request",
            bodies.didTrackRequest("POST", "/api/v1/resources/memory")
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
}