package dk.tobiasthedanish.observability.runtime

import dk.tobiasthedanish.observability.storage.MemoryUsageEntity
import dk.tobiasthedanish.observability.utils.IdFactoryImpl
import dk.tobiasthedanish.observability.utils.Logger
import org.junit.Assert.*

import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock

class ResourceUsageStoreImplTest {
    private lateinit var resourceUsageStoreImpl: ResourceUsageStoreImpl
    private val db = FakeDatabase()
    private val mockLogger = mock<Logger>()

    @Before
    fun setUp() {
        resourceUsageStoreImpl = ResourceUsageStoreImpl(
            db = db,
            sessionManager = FakeSessionManager(),
            idFactory = IdFactoryImpl(),
            logger = mockLogger
        )
    }

    @Test
    fun givenQueueIsNotEmptyWhenStoreIsFlushedThenQueueIsSentToDb() {
        // GIVEN
        resourceUsageStoreImpl.onReceive(FakeMemoryInspector.memoryUsage)

        // WHEN
        resourceUsageStoreImpl.flush()

        // THEN
        val data = db.dataMap[MemoryUsageEntity::class.java.name]

        assertNotNull(data)
        assertEquals(1, data?.size)
        val expectedUsage = FakeMemoryInspector.memoryUsage
        val actualUsage = data?.get(0) as MemoryUsageEntity

        assertEquals(expectedUsage.freeMemory, actualUsage.freeMemory)
        assertEquals(expectedUsage.usedMemory, actualUsage.usedMemory)
        assertEquals(expectedUsage.maxMemory, actualUsage.maxMemory)
        assertEquals(expectedUsage.totalMemory, actualUsage.totalMemory)
        assertEquals(expectedUsage.availableHeapSpace, actualUsage.availableHeapSpace)
    }

    @Test
    fun givenQueueIsEmptyWhenUsageIsReceivedThenUsageIsAddedToQueue() {
        // GIVEN
        resourceUsageStoreImpl.memoryUsageQueue.clear()

        // WHEN
        resourceUsageStoreImpl.onReceive(FakeMemoryInspector.memoryUsage)

        // THEN
        assertEquals(1, resourceUsageStoreImpl.memoryUsageQueue.size)
    }
}