package dk.tobiasthedanish.observability.utils

import dk.tobiasthedanish.observability.storage.Database

internal interface CleanupService {
    fun clearData()
}

internal class CleanupServiceImpl(
    private val database: Database,
): CleanupService {
    override fun clearData() {
        database.deleteExportedSessions()
    }
}