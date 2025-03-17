package dk.tobiasthedanish.observability.session

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dk.tobiasthedanish.observability.storage.Database
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

val Context.dataStore: DataStore<Preferences> by preferencesDataStore("dk.tobiasthedanish.observability")

internal interface SessionStore {
    suspend fun getRecent(): Session?
    fun setRecent(session: Session)
    fun updateLastEventTime(t: Long)
    fun setSessionCrashed()
}

internal class SessionStoreImpl(
    private val dataStore: DataStore<Preferences>,
    private val externalScope: CoroutineScope,
): SessionStore {
    private val sessionIdKey = stringPreferencesKey("sessionId")
    private val sessionCreatedAtKey = longPreferencesKey("sessionCreatedAt")
    private val sessionLastEventKey = longPreferencesKey("sessionLastEvent")
    private val sessionCrashedKey = booleanPreferencesKey("sessionCrashed")

    override suspend fun getRecent(): Session? {
        return externalScope.async {
            dataStore.data.map { data ->
                val id = data[sessionIdKey] ?: return@map null
                val createdAt = data[sessionCreatedAtKey] ?: return@map null
                val lastEventTime = data[sessionLastEventKey] ?: 0
                val crashed = data[sessionCrashedKey] ?: false

                Session(
                    id = id,
                    createdAt = createdAt,
                    lastEventTime = lastEventTime,
                    crashed = crashed
                )
            }.first()
        }.await()
    }

    override fun setRecent(session: Session) {
        externalScope.launch {
            dataStore.edit { data ->
                data[sessionIdKey] = session.id
                data[sessionCreatedAtKey] = session.createdAt
                data[sessionLastEventKey] = session.lastEventTime
                data[sessionCrashedKey] = session.crashed
            }
        }
    }

    override fun updateLastEventTime(t: Long) {
        externalScope.launch {
            dataStore.edit { data ->
                data[sessionLastEventKey] = t
            }
        }
    }

    override fun setSessionCrashed(): Unit = runBlocking {
        dataStore.edit { data ->
            data[sessionCrashedKey] = true
        }
    }
}