package dk.tobiasthedanish.observability.storage

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import androidx.core.database.getStringOrNull

internal interface Database {
    fun createSession(session: SessionEntity)
    fun getSession(sessionId: String): SessionEntity?
    fun setSessionCrashed(sessionId: String)
    fun setSessionExported(sessionId: String)

    fun createEvent(event: EventEntity)
    fun getEvent(eventId: String): EventEntity?
    /**
     * @return number of FAILED events
     */
    fun insertEvents(events: List<EventEntity>): Int
    fun setEventExported(eventId: String)

    fun createTrace(trace: TraceEntity)
    fun getTrace(traceId: String): TraceEntity?
    /**
     * @return number of FAILED traces
     */
    fun insertTraces(traces: List<TraceEntity>): Int
    fun setTraceExported(traceId: String)

    fun getMemoryUsage(id: String): MemoryUsageEntity?
    fun createMemoryUsage(data: MemoryUsageEntity)
    fun insertMemoryUsages(data: List<MemoryUsageEntity>): Int
    fun setMemoryUsageExported(id: String)

    fun getDataForExport(sessionId: String): ExportEntity

    fun deleteOldExportedSessions(currentSessionId: String)
}

private const val TAG = "DatabaseImpl"

internal class DatabaseImpl(
    context: Context,
) : SQLiteOpenHelper(
    context,
    Constants.DB.NAME,
    null,
    Constants.DB.Versions.V1,
), Database {
    override fun onCreate(db: SQLiteDatabase) {
        try {
            db.execSQL(Constants.SQL.CREATE_SESSION_TABLE)
            db.execSQL(Constants.SQL.CREATE_EVENTS_TABLE)
            db.execSQL(Constants.SQL.CREATE_TRACE_TABLE)
            db.execSQL(Constants.SQL.CREATE_MEMORY_USAGE_TABLE)
        } catch (e: SQLiteException) {
            Log.e(TAG, "Failed to create database", e)
        }
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        DBMigration.run(db, oldVersion, newVersion)
    }

    override fun onConfigure(db: SQLiteDatabase) {
        super.onConfigure(db)

        db.setForeignKeyConstraintsEnabled(true)
    }

    override fun createSession(session: SessionEntity) {
        try {
            val values = ContentValues().apply {
                put(Constants.DB.SessionTable.COL_ID, session.id)
                put(Constants.DB.SessionTable.COL_CREATED_AT, session.createdAt)
                put(Constants.DB.SessionTable.COL_CRASHED, if (session.crashed) 1 else 0)
                put(Constants.DB.SessionTable.COL_EXPORTED, if (session.exported) 1 else 0)
            }
            val result = writableDatabase.insert(Constants.DB.SessionTable.NAME, null, values)
            if (result == -1L) {
                Log.e(TAG, "Failed to insert session")
            } else {
                Log.d(TAG, "Inserted session ${session.id}")
            }
        } catch (e: SQLiteException) {
            Log.e(TAG, "Failed to insert session", e)
        }
    }

    override fun getSession(sessionId: String): SessionEntity? {
        var res: SessionEntity? = null
        readableDatabase.rawQuery(Constants.SQL.GET_SESSION, arrayOf(sessionId)).use {
            if (it.moveToFirst()) {
                res = readSession(it)
            }
        }

        return res
    }

    override fun setSessionCrashed(sessionId: String) {
        try {
            val values = ContentValues().apply {
                put(Constants.DB.SessionTable.COL_CRASHED, 1)
            }

            val result = writableDatabase.update(
                Constants.DB.SessionTable.NAME,
                values,
                "${Constants.DB.SessionTable.COL_ID} = ?",
                arrayOf(sessionId)
            )

            if (result == -1) {
                Log.e(TAG, "Failed to set session crash for session: $sessionId")
            }
        } catch (e: SQLiteException) {
            Log.e(TAG, "Failed to set session crash for session: $sessionId", e)
        }
    }

    override fun setSessionExported(sessionId: String) {
        try {
            val values = ContentValues().apply {
                put(Constants.DB.SessionTable.COL_EXPORTED, 1)
            }

            val result = writableDatabase.update(
                Constants.DB.SessionTable.NAME,
                values,
                "${Constants.DB.SessionTable.COL_ID} = ?",
                arrayOf(sessionId)
            )

            if (result == -1) {
                Log.e(TAG, "Failed to set session exported for session: $sessionId")
            }
        } catch (e: SQLiteException) {
            Log.e(TAG, "Failed to set session exported for session: $sessionId", e)
        }
    }

    override fun createEvent(event: EventEntity) {
        try {
            val values = ContentValues().apply {
                put(Constants.DB.EventTable.COL_ID, event.id)
                put(Constants.DB.EventTable.COL_CREATED_AT, event.createdAt)
                put(Constants.DB.EventTable.COL_TYPE, event.type)
                put(Constants.DB.EventTable.COL_SESSION_ID, event.sessionId)
                put(Constants.DB.EventTable.COL_SERIALIZED_DATA, event.serializedData)
            }
            val result = writableDatabase.insert(Constants.DB.EventTable.NAME, null, values)
            if (result == -1L) {
                Log.e(TAG, "Failed to insert event")
            }
        } catch (e: SQLiteException) {
            Log.e(TAG, "Failed to insert event", e)
        }
    }

    override fun getEvent(eventId: String): EventEntity? {
        var res: EventEntity? = null

        readableDatabase.rawQuery(Constants.SQL.GET_EVENT, arrayOf(eventId)).use {
            if (it.moveToFirst()) {
                res = readEvent(it)
            }
        }

        return res
    }

    @SuppressLint("UseKtx")
    override fun insertEvents(events: List<EventEntity>): Int {
        var failedCount = 0
        for (event in events) {
            try {
                val values = ContentValues().apply {
                    put(Constants.DB.EventTable.COL_ID, event.id)
                    put(Constants.DB.EventTable.COL_CREATED_AT, event.createdAt)
                    put(Constants.DB.EventTable.COL_TYPE, event.type)
                    put(Constants.DB.EventTable.COL_SESSION_ID, event.sessionId)
                    put(Constants.DB.EventTable.COL_SERIALIZED_DATA, event.serializedData)
                }
                if (writableDatabase.insert(Constants.DB.EventTable.NAME, null, values) == -1L) {
                    failedCount += 1
                }
            } catch (e: SQLiteException) {
                Log.e(TAG, "Error inserting events in db", e)
                failedCount += 1
            }
        }

        return failedCount
    }

    override fun setEventExported(eventId: String) {
        try {
            val values = ContentValues().apply {
                put(Constants.DB.EventTable.COL_EXPORTED, 1)
            }

            val result = writableDatabase.update(
                Constants.DB.EventTable.NAME,
                values,
                "${Constants.DB.EventTable.COL_ID} = ?",
                arrayOf(eventId)
            )

            if (result == -1) {
                Log.e(TAG, "Failed to set event exported for event: $eventId")
            }
        } catch (e: SQLiteException) {
            Log.e(TAG, "Failed to set event exported for event: $eventId", e)
        }
    }

    override fun createTrace(trace: TraceEntity) {
        try {
            val values = ContentValues().apply {
                put(Constants.DB.TraceTable.COL_TRACE_ID, trace.traceId)
                put(Constants.DB.TraceTable.COL_GROUP_ID, trace.groupId)
                put(Constants.DB.TraceTable.COL_PARENT_ID, trace.parentId)
                put(Constants.DB.TraceTable.COL_SESSION_ID, trace.sessionId)
                put(Constants.DB.TraceTable.COL_NAME, trace.name)
                put(Constants.DB.TraceTable.COL_STATUS, trace.status)
                put(Constants.DB.TraceTable.COL_ERROR_MESSAGE, trace.errorMessage)
                put(Constants.DB.TraceTable.COL_STARTED_AT, trace.startTime)
                put(Constants.DB.TraceTable.COL_ENDED_AT, trace.endTime)
                put(Constants.DB.TraceTable.COL_HAS_ENDED, if (trace.hasEnded) 1 else 0)
            }
            val result = writableDatabase.insert(Constants.DB.TraceTable.NAME, null, values)
            if (result == -1L) {
                Log.e(TAG, "Failed to insert trace")
            }
        } catch (e: SQLiteException) {
            Log.e(TAG, "Failed to insert trace", e)
        }
    }

    override fun getTrace(traceId: String): TraceEntity? {
        var res: TraceEntity? = null

        readableDatabase.rawQuery(Constants.SQL.GET_TRACE, arrayOf(traceId)).use {
            if (it.moveToFirst()) {
                res = readTrace(it)
            }
        }

        return res
    }

    @SuppressLint("UseKtx")
    override fun insertTraces(traces: List<TraceEntity>): Int {
        var failedCount = 0
        for (trace in traces) {
            try {
                val values = ContentValues().apply {
                    put(Constants.DB.TraceTable.COL_TRACE_ID, trace.traceId)
                    put(Constants.DB.TraceTable.COL_GROUP_ID, trace.groupId)
                    put(Constants.DB.TraceTable.COL_PARENT_ID, trace.parentId)
                    put(Constants.DB.TraceTable.COL_SESSION_ID, trace.sessionId)
                    put(Constants.DB.TraceTable.COL_NAME, trace.name)
                    put(Constants.DB.TraceTable.COL_STATUS, trace.status)
                    put(Constants.DB.TraceTable.COL_ERROR_MESSAGE, trace.errorMessage)
                    put(Constants.DB.TraceTable.COL_STARTED_AT, trace.startTime)
                    put(Constants.DB.TraceTable.COL_ENDED_AT, trace.endTime)
                    put(Constants.DB.TraceTable.COL_HAS_ENDED, if (trace.hasEnded) 1 else 0)
                }

                if (writableDatabase.insert(Constants.DB.TraceTable.NAME, null, values) == -1L) {
                    Log.e(TAG, "Failed to insert trace(${trace.name})")
                    failedCount += 1
                }
            } catch (e: SQLiteException) {
                Log.e(TAG, "Failed to insert traces", e)
                failedCount += 1
            }
        }

        return failedCount
    }

    override fun setTraceExported(traceId: String) {
        try {
            val values = ContentValues().apply {
                put(Constants.DB.TraceTable.COL_EXPORTED, 1)
            }

            val result = writableDatabase.update(
                Constants.DB.TraceTable.NAME,
                values,
                "${Constants.DB.TraceTable.COL_TRACE_ID} = ?",
                arrayOf(traceId)
            )

            if (result == -1) {
                Log.e(TAG, "Failed to set trace exported for trace: $traceId")
            }
        } catch (e: SQLiteException) {
            Log.e(TAG, "Failed to set trace exported for trace: $traceId", e)
        }
    }

    override fun getMemoryUsage(id: String): MemoryUsageEntity? {
        var res: MemoryUsageEntity? = null

        Log.d(TAG, "Reading memory usage with id $id")
        readableDatabase.rawQuery(Constants.SQL.GET_MEMORY_USAGE, arrayOf(id)).use {
            if (it.moveToFirst()) {
                res = readMemoryUsage(it)
            } else {
                Log.e(TAG, "Reading memory usage $id failed!")
            }
        }

        return res
    }

    override fun createMemoryUsage(data: MemoryUsageEntity) {
        Log.d(TAG, "Creating memory usage with id ${data.id}")
        val result = insertMemoryUsage(data)
        if (result == -1L) {
            Log.e(TAG, "Creating memory usage ${data.id} failed!")
        }
    }

    override fun insertMemoryUsages(data: List<MemoryUsageEntity>): Int {
        var failedCount = 0
        for (usage in data) {
            Log.d(TAG, "Creating memory usage with id ${usage.id}")
            val result = insertMemoryUsage(usage)
            if (result == -1L) {
                Log.e(TAG, "Creating memory usage failed!")
                failedCount += 1
            }
        }
        return failedCount
    }

    override fun setMemoryUsageExported(id: String) {
        try {
            val values = ContentValues().apply {
                put(Constants.DB.MemoryUsageTable.COL_EXPORTED, 1)
            }

            val result = writableDatabase.update(
                Constants.DB.MemoryUsageTable.NAME,
                values,
                "${Constants.DB.MemoryUsageTable.COL_ID} = ?",
                arrayOf(id)
            )

            if (result == -1) {
                Log.e(TAG, "Failed to set memory usage exported for memory usage: $id")
            }
        } catch (e: SQLiteException) {
            Log.e(TAG, "Failed to set memory usage exported for memory usage: $id", e)
        }
    }

    private fun insertMemoryUsage(data: MemoryUsageEntity): Long {
        try {
            val values = ContentValues().apply {
                this.put(Constants.DB.MemoryUsageTable.COL_ID, data.id)
                this.put(Constants.DB.MemoryUsageTable.COL_SESSION_ID, data.sessionId)
                this.put(Constants.DB.MemoryUsageTable.COL_FREE_MEMORY, data.freeMemory)
                this.put(Constants.DB.MemoryUsageTable.COL_USED_MEMORY, data.usedMemory)
                this.put(Constants.DB.MemoryUsageTable.COL_TOTAL_MEMORY, data.totalMemory)
                this.put(Constants.DB.MemoryUsageTable.COL_MAX_MEMORY, data.maxMemory)
                this.put(Constants.DB.MemoryUsageTable.COL_AVAILABLE_HEAP_SPACE, data.availableHeapSpace)
                this.put(Constants.DB.MemoryUsageTable.COL_EXPORTED, data.exported)
            }

            return writableDatabase.insert(Constants.DB.MemoryUsageTable.NAME, null, values)
        } catch (e: SQLiteException) {
            Log.e(TAG, "Failed to insert memory usage", e)
            return -1L
        }
    }

    override fun getDataForExport(sessionId: String): ExportEntity {
        val session: SessionEntity? = try {
            readableDatabase.rawQuery(Constants.SQL.GET_SESSION_FOR_EXPORT, arrayOf(sessionId))
                .use {
                    if (it.moveToFirst()) {
                        readSession(it)
                    } else {
                        null
                    }
                }
        } catch (e: SQLiteException) {
            Log.d(TAG, "Failed to get session with id $sessionId for export")
            null
        }

        val events: List<EventEntity> = try {
            val res = mutableListOf<EventEntity>()

            readableDatabase.rawQuery(Constants.SQL.GET_EVENTS_FOR_EXPORT, arrayOf(sessionId)).use {
                while (it.moveToNext()) {
                    res.add(readEvent(it))
                }
            }

            res
        } catch (e: SQLiteException) {
            Log.d(TAG, "Failed to get events for export for sessionID $sessionId")
            emptyList()
        }

        val traces: List<TraceEntity> = try {
            val res = mutableListOf<TraceEntity>()

            readableDatabase.rawQuery(Constants.SQL.GET_TRACES_FOR_EXPORT, arrayOf(sessionId)).use {
                while (it.moveToNext()) {
                    res.add(readTrace(it))
                }
            }

            res
        } catch (e: SQLiteException) {
            Log.d(TAG, "Failed to get traces for export for sessionID $sessionId")
            emptyList()
        }

        val usages: List<MemoryUsageEntity> = try {
            val res = mutableListOf<MemoryUsageEntity>()

            readableDatabase.rawQuery(Constants.SQL.GET_MEMORY_USAGE_FOR_EXPORT, arrayOf(sessionId)).use {
                while (it.moveToNext()) {
                    res.add(readMemoryUsage(it))
                }
            }

            res
        } catch (e: SQLiteException) {
            Log.d(TAG, "Failed to get memory usages for export for sessionID $sessionId")
            emptyList()
        }

        return ExportEntity(
            sessionEntity = session,
            eventEntities = events,
            traceEntities = traces,
            memoryUsageEntities = usages,
        )
    }

    override fun deleteOldExportedSessions(currentSessionId: String) {
        try {
            val result = writableDatabase.delete(
                Constants.DB.SessionTable.NAME,
                "${Constants.DB.SessionTable.COL_ID} != ? AND ${Constants.DB.SessionTable.COL_EXPORTED} = 1",
                arrayOf(currentSessionId)
            )
            if (result == -1) {
                Log.e(TAG, "Failed to delete exported sessions")
            }
        } catch (e: SQLiteException) {
            Log.e(TAG, "Failed to delete exported sessions", e)
        }
    }

    override fun close() {
        writableDatabase.close()
        super.close()
    }

    // expects the cursor to be in a readable state
    private fun readSession(cursor: Cursor): SessionEntity {
        val idIndex = cursor.getColumnIndex(Constants.DB.SessionTable.COL_ID)
        val id = cursor.getString(idIndex)

        val createdAtIndex = cursor.getColumnIndex(Constants.DB.SessionTable.COL_CREATED_AT)
        val createdAt = cursor.getLong(createdAtIndex)

        val crashedIndex = cursor.getColumnIndex(Constants.DB.SessionTable.COL_CRASHED)
        val crashed = cursor.getInt(crashedIndex) == 1

        val exportedIndex = cursor.getColumnIndex(Constants.DB.SessionTable.COL_EXPORTED)
        val exported = cursor.getInt(exportedIndex) == 1

        return SessionEntity(id, createdAt, crashed, exported)
    }

    // expects the cursor to be in a readable state
    private fun readEvent(cursor: Cursor): EventEntity {
        val idIndex = cursor.getColumnIndex(Constants.DB.EventTable.COL_ID)
        val id = cursor.getString(idIndex)

        val createdAtIndex = cursor.getColumnIndex(Constants.DB.EventTable.COL_CREATED_AT)
        val createdAt = cursor.getLong(createdAtIndex)

        val typeIndex = cursor.getColumnIndex(Constants.DB.EventTable.COL_TYPE)
        val type = cursor.getString(typeIndex)

        val serializedDataIndex = cursor.getColumnIndex(Constants.DB.EventTable.COL_SERIALIZED_DATA)
        val serializedData = cursor.getString(serializedDataIndex)

        val sessionIdIndex = cursor.getColumnIndex(Constants.DB.EventTable.COL_SESSION_ID)
        val sessionId = cursor.getString(sessionIdIndex)

        return EventEntity(
            id = id,
            createdAt = createdAt,
            type = type,
            serializedData = serializedData,
            sessionId = sessionId,
        )
    }

    private fun readMemoryUsage(cursor: Cursor): MemoryUsageEntity {
        val idIndex = cursor.getColumnIndex(Constants.DB.MemoryUsageTable.COL_ID)
        val id = cursor.getString(idIndex)

        val sessionIdIndex = cursor.getColumnIndex(Constants.DB.MemoryUsageTable.COL_SESSION_ID)
        val sessionId = cursor.getString(sessionIdIndex)

        val usedMemoryIndex = cursor.getColumnIndex(Constants.DB.MemoryUsageTable.COL_USED_MEMORY)
        val usedMemory = cursor.getLong(usedMemoryIndex)

        val freeMemoryIndex = cursor.getColumnIndex(Constants.DB.MemoryUsageTable.COL_FREE_MEMORY)
        val freeMemory = cursor.getLong(freeMemoryIndex)

        val maxMemoryIndex = cursor.getColumnIndex(Constants.DB.MemoryUsageTable.COL_MAX_MEMORY)
        val maxMemory = cursor.getLong(maxMemoryIndex)

        val totalMemoryIndex = cursor.getColumnIndex(Constants.DB.MemoryUsageTable.COL_TOTAL_MEMORY)
        val totalMemory = cursor.getLong(totalMemoryIndex)

        val availableHeapSpaceIndex = cursor.getColumnIndex(Constants.DB.MemoryUsageTable.COL_AVAILABLE_HEAP_SPACE)
        val availableHeapSpace = cursor.getLong(availableHeapSpaceIndex)

        val exportedIndex = cursor.getColumnIndex(Constants.DB.MemoryUsageTable.COL_EXPORTED)
        val exported = cursor.getInt(exportedIndex) != 0

        return MemoryUsageEntity(
            id = id,
            sessionId = sessionId,
            freeMemory = freeMemory,
            usedMemory = usedMemory,
            totalMemory = totalMemory,
            maxMemory = maxMemory,
            availableHeapSpace = availableHeapSpace,
            exported = exported
        )
    }

    // expects the cursor to be in a readable state
    private fun readTrace(cursor: Cursor): TraceEntity {
        val idIndex = cursor.getColumnIndex(Constants.DB.TraceTable.COL_TRACE_ID)
        val id = cursor.getString(idIndex)

        val groupIdIndex = cursor.getColumnIndex(Constants.DB.TraceTable.COL_GROUP_ID)
        val groupId = cursor.getString(groupIdIndex)

        val parentIdIndex = cursor.getColumnIndex(Constants.DB.TraceTable.COL_PARENT_ID)
        val parentId = cursor.getStringOrNull(parentIdIndex)

        val statusIndex = cursor.getColumnIndex(Constants.DB.TraceTable.COL_STATUS)
        val status = cursor.getString(statusIndex)

        val createdAtIndex = cursor.getColumnIndex(Constants.DB.TraceTable.COL_STARTED_AT)
        val createdAt = cursor.getLong(createdAtIndex)

        val endedAtIndex = cursor.getColumnIndex(Constants.DB.TraceTable.COL_ENDED_AT)
        val endedAt = cursor.getLong(endedAtIndex)

        val errorMessageIndex = cursor.getColumnIndex(Constants.DB.TraceTable.COL_ERROR_MESSAGE)
        val errorMessage = cursor.getStringOrNull(errorMessageIndex)

        val sessionIdIndex = cursor.getColumnIndex(Constants.DB.TraceTable.COL_SESSION_ID)
        val sessionId = cursor.getString(sessionIdIndex)

        val nameIndex = cursor.getColumnIndex(Constants.DB.TraceTable.COL_NAME)
        val name = cursor.getString(nameIndex)

        val hasEndedIndex = cursor.getColumnIndex(Constants.DB.TraceTable.COL_HAS_ENDED)
        val hasEnded = cursor.getLong(hasEndedIndex)

        return TraceEntity(
            traceId = id,
            groupId = groupId,
            parentId = parentId,
            status = status,
            errorMessage = errorMessage,
            startTime = createdAt,
            endTime = endedAt,
            sessionId = sessionId,
            name = name,
            hasEnded = hasEnded == 1L,
        )
    }
}