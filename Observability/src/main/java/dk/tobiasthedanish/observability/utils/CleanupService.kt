package dk.tobiasthedanish.observability.utils

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