package dk.tobiasthedanish.observability.utils

import dk.tobiasthedanish.observability.session.SessionManager
import dk.tobiasthedanish.observability.storage.Database

internal interface CleanupService {
    fun clearData()
}

internal class CleanupServiceImpl(
    private val database: Database,
    private val sessionManager: SessionManager,
): CleanupService {
    override fun clearData() {
        database.deleteOldExportedSessions(sessionManager.getSessionId())
    }
}