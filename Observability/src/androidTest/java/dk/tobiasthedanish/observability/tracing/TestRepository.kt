package dk.tobiasthedanish.observability.tracing

import android.util.Log
import dk.tobiasthedanish.observability.Observability
import kotlinx.coroutines.delay

class TestRepository {
    private val testDTOS = listOf(TestDTO("Test1"), TestDTO("Test2"), TestDTO("Test3"))

    suspend fun fetchData(parentTrace: Trace? = null): List<TestDTO> {
        val trace = Observability.createTrace("TestRepository.fetchData")?.apply {
            if (parentTrace != null) {
                setParent(parentTrace)
            }
            start()
        }
        Log.d("TestRepository", "Repository trace: ${trace?.traceId}")
        delay(100)
        trace?.end()
        return testDTOS
    }
}