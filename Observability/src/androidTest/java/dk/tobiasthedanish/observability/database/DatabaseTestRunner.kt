package dk.tobiasthedanish.observability.database

import android.app.Application
import android.app.Instrumentation
import android.content.ContentValues
import androidx.test.platform.app.InstrumentationRegistry
import dk.tobiasthedanish.observability.events.EventTypes
import dk.tobiasthedanish.observability.storage.Constants
import dk.tobiasthedanish.observability.storage.DatabaseImpl
import dk.tobiasthedanish.observability.storage.EventEntity
import dk.tobiasthedanish.observability.storage.SessionEntity
import dk.tobiasthedanish.observability.time.AndroidTimeProvider
import dk.tobiasthedanish.observability.utils.IdFactoryImpl

internal class DatabaseTestRunner {
    private val instrumentation: Instrumentation = InstrumentationRegistry.getInstrumentation()
    private val application = instrumentation.context.applicationContext as Application

    private val idFactory = IdFactoryImpl()
    private val timeProvider = AndroidTimeProvider()
    private val database = DatabaseImpl(application)
    lateinit var sessionId: String
        private set

    fun setup() {
        sessionId = idFactory.uuid()

        var values = ContentValues().apply {
            put(Constants.DB.SessionTable.COL_ID, sessionId)
            put(Constants.DB.SessionTable.COL_CREATED_AT, timeProvider.now())
            put(Constants.DB.SessionTable.COL_CRASHED, 0)
        }
        database.writableDatabase.insert(Constants.DB.SessionTable.NAME, null, values)

        values = ContentValues().apply {
            put(Constants.DB.EventTable.COL_ID, idFactory.uuid())
            put(Constants.DB.EventTable.COL_CREATED_AT, timeProvider.now())
            put(Constants.DB.EventTable.COL_TYPE, EventTypes.UNHANDLED_EXCEPTION)
            put(Constants.DB.EventTable.COL_SERIALIZED_DATA, "")
            put(Constants.DB.EventTable.COL_SESSION_ID, sessionId)
        }
        database.writableDatabase.insert(Constants.DB.EventTable.NAME, null, values)

        values = ContentValues().apply {
            put(Constants.DB.EventTable.COL_ID, idFactory.uuid())
            put(Constants.DB.EventTable.COL_CREATED_AT, timeProvider.now())
            put(Constants.DB.EventTable.COL_TYPE, EventTypes.LIFECYCLE_APP)
            put(Constants.DB.EventTable.COL_SERIALIZED_DATA, "")
            put(Constants.DB.EventTable.COL_SESSION_ID, sessionId)
        }
        database.writableDatabase.insert(Constants.DB.EventTable.NAME, null, values)

        values = ContentValues().apply {
            put(Constants.DB.EventTable.COL_ID, idFactory.uuid())
            put(Constants.DB.EventTable.COL_CREATED_AT, timeProvider.now())
            put(Constants.DB.EventTable.COL_TYPE, EventTypes.LIFECYCLE_ACTIVITY)
            put(Constants.DB.EventTable.COL_SERIALIZED_DATA, "")
            put(Constants.DB.EventTable.COL_SESSION_ID, sessionId)
        }
        database.writableDatabase.insert(Constants.DB.EventTable.NAME, null, values)
    }

    fun getSession(sessionId: String): SessionEntity? {
        return database.getSession(sessionId)
    }

    fun setSessionCrashed(sessionId: String) {
        database.setSessionCrashed(sessionId)
    }

    fun setSessionExported(sessionId: String) {
        database.setSessionExported(sessionId)
    }

    fun deleteExportedSessions() {
        database.deleteExportedSessions()
    }

    fun createSession(): SessionEntity {
        val entity = SessionEntity(
            id = idFactory.uuid(),
            createdAt = timeProvider.now(),
            crashed = false,
        )

        database.createSession(entity)
        return entity
    }

    fun getEvent(eventId: String): EventEntity? {
        return database.getEvent(eventId)
    }

    fun createEvent(type: String, data: String, sessionId: String? = null): EventEntity {
        val entity = EventEntity(
            id = idFactory.uuid(),
            createdAt = timeProvider.now(),
            sessionId = sessionId ?: this.sessionId,
            serializedData = data,
            type = type
        )

        database.createEvent(entity)

        return entity
    }

    fun clearData() {
        database.writableDatabase.delete(Constants.DB.EventTable.NAME, null, null)
        database.writableDatabase.delete(Constants.DB.SessionTable.NAME, null, null)
    }
}