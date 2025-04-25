package dk.tobiasthedanish.observability.tracing

import dk.tobiasthedanish.observability.utils.IdFactoryImpl
import org.junit.Assert.*
import org.junit.Before

import org.junit.Test
import org.mockito.kotlin.mock

class TraceImplTest {
    private val mockTimeProvider = FakeTimeProvider()
    private val mockTraceCollector = mock<TraceCollector>()
    private val idFactory = IdFactoryImpl()
    private val factory = TraceFactoryImpl(mockTimeProvider, mockTraceCollector, idFactory)
    private lateinit var trace: TraceImpl

    @Before
    fun setup() {
        trace = factory.createTrace("TestTrace")
    }

    @Test
    fun setParent() {
        val child = factory.createTrace("Test Trace")
        val oldGroupId = child.groupId
        assertNull(child.parentId)
        assertTrue(oldGroupId.isNotBlank())

        child.setParent(trace)
        assertNotNull(child.parentId)
        assertEquals(trace.traceId, child.parentId)
        assertNotEquals(oldGroupId, child.groupId)
        assertEquals(trace.groupId, child.groupId)
    }

    @Test
    fun start() {
        trace = factory.createTrace("Test Trace")
        trace.start()

        assertNotEquals(0, trace.startTime)
        assertFalse(trace.hasEnded())
    }

    @Test
    fun end() {
        trace = factory.createTrace("Test Trace")

        trace.end()
        assertNotEquals(0, trace.endTime)
        assertTrue(trace.hasEnded())
    }
}