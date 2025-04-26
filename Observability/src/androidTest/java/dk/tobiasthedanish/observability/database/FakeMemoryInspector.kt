package dk.tobiasthedanish.observability.database

import dk.tobiasthedanish.observability.runtime.MemoryInspector

class FakeMemoryInspector: MemoryInspector {
    override fun freeMemory(): Long {
        return 0L
    }

    override fun usedMemory(): Long {
        return 0L
    }

    override fun totalMemory(): Long {
        return 0L
    }

    override fun maxMemory(): Long {
        return 0L
    }

    override fun availableHeapSpace(): Long {
        return 0L
    }
}