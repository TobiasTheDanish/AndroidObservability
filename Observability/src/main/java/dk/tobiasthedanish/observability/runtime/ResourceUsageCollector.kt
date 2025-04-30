package dk.tobiasthedanish.observability.runtime

import dk.tobiasthedanish.observability.collector.Collector
import dk.tobiasthedanish.observability.scheduling.Ticker
import dk.tobiasthedanish.observability.time.TimeProvider
import dk.tobiasthedanish.observability.utils.Logger
import kotlin.time.Duration.Companion.seconds

internal data class MemoryUsage(
    val freeMemory: Long,
    val usedMemory: Long,
    val totalMemory: Long,
    val maxMemory: Long,
    val availableHeapSpace: Long,
    val createdAt: Long,
)

internal interface MemoryUsageListener {
    fun onReceive(usage: MemoryUsage)
}

internal interface ResourceUsageCollector : Collector {
    fun addListener(listener: MemoryUsageListener)
    fun removeListener(listener: MemoryUsageListener)
}

private const val TAG = "ResourceUsageCollectorImpl"

internal class ResourceUsageCollectorImpl(
    private val ticker: Ticker,
    private val memoryInspector: MemoryInspector,
    private val timeProvider: TimeProvider,
    private val logger: Logger = Logger(TAG)
) : ResourceUsageCollector {
    private val listeners = mutableSetOf<MemoryUsageListener>()

    override fun addListener(listener: MemoryUsageListener) {
        logger.debug("addListener called")
        listeners.add(listener)
    }

    override fun removeListener(listener: MemoryUsageListener) {
        logger.debug("removeListener called")
        listeners.remove(listener)
    }

    override fun register() {
        logger.debug("register called")
        ticker.start(15.seconds.inWholeMilliseconds) {
            collect()
        }
    }

    override fun unregister() {
        logger.debug("unregister called")
        ticker.stop()
    }

    private fun collect() {
        logger.debug("Collect called. Listeners: $listeners")
        val usage = MemoryUsage(
            freeMemory = memoryInspector.freeMemory(),
            usedMemory = memoryInspector.usedMemory(),
            totalMemory = memoryInspector.totalMemory(),
            maxMemory = memoryInspector.maxMemory(),
            availableHeapSpace = memoryInspector.availableHeapSpace(),
            createdAt = timeProvider.now(),
        )

        listeners.forEach { listener -> listener.onReceive(usage) }
    }
}