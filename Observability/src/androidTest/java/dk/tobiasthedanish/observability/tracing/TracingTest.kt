package dk.tobiasthedanish.observability.tracing

import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.filters.LargeTest
import org.junit.Assert
import org.junit.Before
import org.junit.Test

class TracingTest {
    private val runner = TracingTestRunner()

    @Before
    fun setup() {
        runner.setup()
    }

    @LargeTest
    @Test
    fun testTracing() {
        runner.initObservability()

        ActivityScenario.launch(TraceTestActivity::class.java).use { scenario ->
            scenario.moveToState(Lifecycle.State.RESUMED)

            scenario.onActivity {
                runner.fetchData()
                runner.pressHome()
            }
        }

        runner.logTraces()
        Assert.assertTrue("No trace stored with group id ${TraceTestActivity.rootTrace?.groupId ?: "INVALID"}", runner.didStoreTrace(TraceTestActivity.rootTrace?.groupId ?: "INVALID"))
        Assert.assertEquals(3, runner.tracesStoredCount(TraceTestActivity.rootTrace?.groupId ?: "INVALID"))
    }
}