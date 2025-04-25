package dk.tobiasthedanish.observability.runtime

import dk.tobiasthedanish.observability.collector.Collector
import dk.tobiasthedanish.observability.scheduling.Ticker
import kotlin.time.Duration.Companion.seconds

internal data class MemoryUsage(
    val freeMemory: Long,
    val usedMemory: Long,
    val totalMemory: Long,
    val maxMemory: Long,
    val availableHeapSpace: Long,
)

internal interface MemoryUsageListener {
    fun onReceive(usage: MemoryUsage)
}

internal interface ResourceUsageCollector : Collector {
    fun addListener(listener: MemoryUsageListener)
    fun removeListener(listener: MemoryUsageListener)
}

internal class ResourceUsageCollectorImpl(
    private val ticker: Ticker,
    private val memoryInspector: MemoryInspector
) : ResourceUsageCollector {
    private val listeners = mutableSetOf<MemoryUsageListener>()

    override fun addListener(listener: MemoryUsageListener) {
        listeners.add(listener)
    }

    override fun removeListener(listener: MemoryUsageListener) {
        listeners.remove(listener)
    }

    override fun register() {
        ticker.start(15.seconds.inWholeMilliseconds) {
            collect()
        }
    }

    override fun unregister() {
        ticker.stop()
    }

    private fun collect() {
        val usage = MemoryUsage(
            freeMemory = memoryInspector.freeMemory(),
            usedMemory = memoryInspector.usedMemory(),
            totalMemory = memoryInspector.totalMemory(),
            maxMemory = memoryInspector.maxMemory(),
            availableHeapSpace = memoryInspector.availableHeapSpace(),
        )

        listeners.forEach { listener -> listener.onReceive(usage) }
    }
}