package dk.tobiasthedanish.observability.cleanup

import dk.tobiasthedanish.observability.events.EventStore

internal interface CleanupService {
    fun clearData()
}

internal class CleanupServiceImpl(
    private val eventStore: EventStore,
): CleanupService {
    override fun clearData() {
        eventStore.clear()
    }
}