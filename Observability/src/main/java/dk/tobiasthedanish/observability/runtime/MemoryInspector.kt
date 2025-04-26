package dk.tobiasthedanish.observability.runtime

internal interface MemoryInspector {
    /**
     * Returns the amount of free memory available to the JVM, measured in bytes.
     */
    fun freeMemory(): Long
    /**
     * Returns the amount of memory used by the JVM, measured in bytes.
     */
    fun usedMemory(): Long
    /**
     * Returns the total memory in to the JVM, measured in bytes.
     */
    fun totalMemory(): Long
    /**
     * Returns the max amount of memory the JVM will attempt to use, measured in bytes.
     * If not limit this returns [Long.MAX_VALUE]
     */
    fun maxMemory(): Long

    /**
     * Returns the amount of memory available on the heap, measured in bytes.
     * This can be used as an indication of how close the JVM is to being out of memory.
     */
    fun availableHeapSpace(): Long
}

internal class AndroidMemoryInspector(private val runtime: Runtime): MemoryInspector {
    override fun freeMemory(): Long {
        return runtime.freeMemory()
    }

    override fun usedMemory(): Long {
        return runtime.totalMemory() - runtime.freeMemory()
    }

    override fun totalMemory(): Long {
        return runtime.totalMemory()
    }

    override fun maxMemory(): Long {
        return runtime.maxMemory()
    }

    override fun availableHeapSpace(): Long {
        return maxMemory() - usedMemory()
    }
}