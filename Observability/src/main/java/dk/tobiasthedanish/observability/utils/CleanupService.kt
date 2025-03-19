package dk.tobiasthedanish.observability.utils

import dk.tobiasthedanish.observability.events.EventStore
import dk.tobiasthedanish.observability.storage.Database

internal interface CleanupService {
    fun clearData()
}

internal class CleanupServiceImpl(
    private val eventStore: EventStore,
    private val database: Database,
): CleanupService {
    override fun clearData() {
        eventStore.clear()
        database.deleteExportedSessions()
    }
}