package dk.tobiasthedanish.observability.runtime

import dk.tobiasthedanish.observability.time.TimeProvider
import dk.tobiasthedanish.observability.utils.Logger
import org.junit.Assert.*
import org.junit.Before

import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

class ResourceUsageCollectorImplTest {
    private lateinit var resourceUsageCollector: ResourceUsageCollector
    private val mockTimeProvider = mock<TimeProvider> {
        on { now() }.doReturn(1234)
    }
    private val mockLogger = mock<Logger>()

    @Before
    fun setup() {
        resourceUsageCollector = ResourceUsageCollectorImpl(
            ticker = FakeTicker(),
            memoryInspector = FakeMemoryInspector(),
            timeProvider = mockTimeProvider,
            logger = mockLogger,
        )
    }

    @Test
    fun givenListenerAddedWhenRegisterCalledThenListenerReceivesUsage() {
        // GIVEN
        val memoryUsages = mutableListOf<MemoryUsage>()
        resourceUsageCollector.addListener(
            listener = object : MemoryUsageListener {
                override fun onReceive(usage: MemoryUsage) {
                    memoryUsages.add(usage)
                }
            }
        )

        // WHEN
        resourceUsageCollector.register()

        // THEN
        assertEquals(1, memoryUsages.size)
        val expectedUsage = FakeMemoryInspector.memoryUsage
        val actualUsage = memoryUsages[0]

        assertEquals(expectedUsage.freeMemory, actualUsage.freeMemory)
        assertEquals(expectedUsage.usedMemory, actualUsage.usedMemory)
        assertEquals(expectedUsage.maxMemory, actualUsage.maxMemory)
        assertEquals(expectedUsage.totalMemory, actualUsage.totalMemory)
        assertEquals(expectedUsage.availableHeapSpace, actualUsage.availableHeapSpace)
    }
}