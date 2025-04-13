package dk.tobiasthedanish.observability.utils

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dk.tobiasthedanish.observability.session.Session
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map


val Context.dataStore: DataStore<Preferences> by preferencesDataStore("dk.tobiasthedanish.observability")

internal interface LocalPreferencesDataStore {
    suspend fun getRecent(): Session?
    suspend fun setRecent(session: Session)
    suspend fun updateLastEventTime(t: Long)
    suspend fun setSessionCrashed()

    suspend fun getInstallationId(): String?
    suspend fun setInstallationId(id: String)
}

internal class LocalPreferencesDataStoreImpl(
    private val dataStore: DataStore<Preferences>,
): LocalPreferencesDataStore {
    private val installationIdKey = stringPreferencesKey("installationId")
    private val sessionIdKey = stringPreferencesKey("sessionId")
    private val sessionCreatedAtKey = longPreferencesKey("sessionCreatedAt")
    private val sessionLastEventKey = longPreferencesKey("sessionLastEvent")
    private val sessionCrashedKey = booleanPreferencesKey("sessionCrashed")

    override suspend fun getRecent(): Session? {
        return dataStore.data.map { data ->
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
    }

    override suspend fun setRecent(session: Session) {
        dataStore.edit { data ->
            data[sessionIdKey] = session.id
            data[sessionCreatedAtKey] = session.createdAt
            data[sessionLastEventKey] = session.lastEventTime
            data[sessionCrashedKey] = session.crashed
        }
    }

    override suspend fun updateLastEventTime(t: Long) {
        dataStore.edit { data ->
            data[sessionLastEventKey] = t
        }
    }

    override suspend fun setSessionCrashed() {
        dataStore.edit { data ->
            data[sessionCrashedKey] = true
        }
    }

    override suspend fun getInstallationId(): String? {
        val id = dataStore.data.map { data ->
            data[installationIdKey]
        }.first()

        return id
    }

    override suspend fun setInstallationId(id: String) {
        dataStore.edit { data ->
            data[installationIdKey] = id
        }
    }
}