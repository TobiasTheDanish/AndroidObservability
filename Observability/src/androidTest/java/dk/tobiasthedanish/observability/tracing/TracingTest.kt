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

            Assert.assertTrue(
                "No trace stored with group id ${TraceTestActivity.rootTrace?.groupId ?: "INVALID"}",
                !didTrackTrace(TraceTestActivity.rootTrace?.groupId ?: "INVALID") && didTrackTrace(TraceTestActivity.rootTrace?.groupId ?: "INVALID")
            )
        }
    }

    private fun triggerExport() {
        Observability.triggerExport()
    }

    private fun didTrackTrace(groupId: String): Boolean {
        //val request = mockWebServer.takeRequest()
        val request = mockWebServer.takeRequest(3000, TimeUnit.MILLISECONDS)
        Log.d("didTrackTrace", "latest request: $request")
        val body = request?.body?.readUtf8()
        Log.d("didTrackTrace", "latest request body: $body")
        return body != null && body.containsTrace(groupId)
    }

    private fun String.containsTrace(groupId: String): Boolean {
        return contains("\"groupId\":\"$groupId\"")
    }
}