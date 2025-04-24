package dk.tobiasthedanish.observability.session

import dk.tobiasthedanish.observability.storage.Database
import dk.tobiasthedanish.observability.utils.LocalPreferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

internal interface SessionStore {
    suspend fun getRecent(): Session?
    fun setRecent(session: Session)
    fun updateLastEventTime(t: Long)

    /**
     * This expects that a recent session exists
     */
    fun setSessionCrashed()
}

internal class SessionStoreImpl(
    private val dataStore: LocalPreferencesDataStore,
    private val db: Database,
    private val externalScope: CoroutineScope,
): SessionStore {

    override suspend fun getRecent(): Session? {
        return dataStore.getRecent()
    }

    override fun setRecent(session: Session) {
        externalScope.launch {
            dataStore.setRecent(session)
        }
    }

    override fun updateLastEventTime(t: Long) {
        externalScope.launch {
            dataStore.updateLastEventTime(t)
        }
    }

    override fun setSessionCrashed() {
        externalScope.launch {
            val recent = dataStore.getRecent() ?: return@launch
            dataStore.setSessionCrashed()
            db.setSessionCrashed(recent.id)
        }
    }
}