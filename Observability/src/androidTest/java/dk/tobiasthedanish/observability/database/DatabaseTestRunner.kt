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
import dk.tobiasthedanish.observability.storage.TraceEntity
import dk.tobiasthedanish.observability.time.AndroidTimeProvider
import dk.tobiasthedanish.observability.tracing.InternalTrace
import dk.tobiasthedanish.observability.tracing.TraceCollector
import dk.tobiasthedanish.observability.tracing.TraceFactoryImpl
import dk.tobiasthedanish.observability.tracing.TraceStatus
import dk.tobiasthedanish.observability.utils.IdFactoryImpl

internal class DatabaseTestRunner {
    private val instrumentation: Instrumentation = InstrumentationRegistry.getInstrumentation()
    private val application = instrumentation.context.applicationContext as Application

    private val idFactory = IdFactoryImpl()
    private val timeProvider = AndroidTimeProvider()
    val traceFactory = TraceFactoryImpl(
        timeProvider = timeProvider,
        idFactory = idFactory,
        traceCollector = object : TraceCollector {
            override fun onStart(trace: InternalTrace) {
            }

            override fun onEnded(trace: InternalTrace) {
                createTrace(trace)
            }

            override fun register() {
            }

            override fun unregister() {
            }

        }
    )
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
        database.deleteOldExportedSessions("")
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

    fun createTrace(trace: InternalTrace, sessionId: String? = null): TraceEntity {
        val errorMessage = when(trace.status) {
            is TraceStatus.Ok -> null
            is TraceStatus.Error -> (trace.status as TraceStatus.Error).message
        }

        val entity = TraceEntity(
            traceId = trace.traceId,
            groupId = trace.groupId,
            parentId = trace.parentId,
            sessionId = sessionId ?: this.sessionId,
            name = trace.name,
            status = trace.status.name,
            errorMessage = errorMessage,
            startTime = trace.startTime,
            endTime = trace.endTime,
            hasEnded = trace.hasEnded(),
        )

        database.createTrace(entity)

        return entity
    }

    fun getTrace(traceId: String): TraceEntity? {
        return database.getTrace(traceId)
    }

    fun clearData() {
        database.writableDatabase.delete(Constants.DB.EventTable.NAME, null, null)
        database.writableDatabase.delete(Constants.DB.SessionTable.NAME, null, null)
    }
}