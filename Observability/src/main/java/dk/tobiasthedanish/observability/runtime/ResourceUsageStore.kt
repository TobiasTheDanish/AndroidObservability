package dk.tobiasthedanish.observability.runtime

import androidx.annotation.VisibleForTesting
import dk.tobiasthedanish.observability.session.SessionManager
import dk.tobiasthedanish.observability.storage.Database
import dk.tobiasthedanish.observability.storage.MemoryUsageEntity
import dk.tobiasthedanish.observability.utils.IdFactory
import dk.tobiasthedanish.observability.utils.Logger
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.atomic.AtomicBoolean

internal interface ResourceUsageStore : MemoryUsageListener {
    fun flush()
}

private const val TAG = "ResourceUsageStoreImpl"

internal class ResourceUsageStoreImpl(
    private val db: Database,
    private val sessionManager: SessionManager,
    private val idFactory: IdFactory,
    private val logger: Logger = Logger(TAG),
): ResourceUsageStore {
    @VisibleForTesting
    val memoryUsageQueue by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        LinkedBlockingQueue<MemoryUsageEntity>(10)
    }
    private var isFlushing = AtomicBoolean(false)

    override fun flush() {
        if (isFlushing.compareAndSet(false, true)) {
            logger.debug("Resource usage flush started")
            try {
                val memoryUsageList = mutableListOf<MemoryUsageEntity>()
                memoryUsageQueue.drainTo(memoryUsageList)

                if (memoryUsageList.isEmpty()) {
                    logger.debug("No memory usage to flush")
                    return
                }

                val failed = db.insertMemoryUsages(memoryUsageList)
                if(failed > 0) {
                    logger.error("Failed to insert $failed memory usages")
                    if (failed < memoryUsageList.size) {
                        logger.info("Successfully inserted ${memoryUsageList.size-failed} memory usages")
                    }
                } else {
                    logger.info("Successfully inserted ${memoryUsageList.size} memory usages")
                }
            } finally {
                isFlushing.set(false)
            }

        } else {
            logger.debug("Resource usage flush already in progress")
        }
    }

    override fun onReceive(usage: MemoryUsage) {
        val entity = MemoryUsageEntity(
            id = idFactory.uuid(),
            sessionId = sessionManager.getSessionId(),
            freeMemory = usage.freeMemory,
            usedMemory = usage.usedMemory,
            totalMemory = usage.totalMemory,
            maxMemory = usage.maxMemory,
            availableHeapSpace = usage.availableHeapSpace,
            createdAt = usage.createdAt
        )

        if (!memoryUsageQueue.offer(entity)) {
            db.createMemoryUsage(entity)
            flush()
        }
    }
}