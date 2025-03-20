package dk.tobiasthedanish.observability.storage

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log

internal interface Database {
    fun createSession(session: SessionEntity)
    fun getSession(sessionId: String): SessionEntity?
    fun setSessionCrashed(sessionId: String)
    fun setSessionExported(sessionId: String)

    fun createEvent(event: EventEntity)
    fun getEvent(eventId: String): EventEntity?
    fun insertEvents(events: List<EventEntity>): Boolean

    fun deleteExportedSessions()
}

private const val TAG = "DatabaseImpl"

internal class DatabaseImpl(
    context: Context,
): SQLiteOpenHelper(
    context,
    Constants.DB.NAME,
    null,
    Constants.DB.Versions.V2,
), Database {
    override fun onCreate(db: SQLiteDatabase) {
        try {
            db.execSQL(Constants.SQL.CREATE_SESSION_TABLE)
            db.execSQL(Constants.SQL.CREATE_EVENTS_TABLE)
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
            }
        } catch (e: SQLiteException) {
            Log.e(TAG, "Failed to insert session", e)
        }
    }

    override fun getSession(sessionId: String): SessionEntity? {
        var res: SessionEntity? = null
        readableDatabase.rawQuery(Constants.SQL.GET_SESSION, arrayOf(sessionId)).use {
            if(it.moveToFirst()) {
                val idIndex = it.getColumnIndex(Constants.DB.SessionTable.COL_ID)
                val id = it.getString(idIndex)

                val createdAtIndex = it.getColumnIndex(Constants.DB.SessionTable.COL_CREATED_AT)
                val createdAt = it.getLong(createdAtIndex)

                val crashedIndex = it.getColumnIndex(Constants.DB.SessionTable.COL_CRASHED)
                val crashed = it.getInt(crashedIndex) == 1

                val exportedIndex = it.getColumnIndex(Constants.DB.SessionTable.COL_EXPORTED)
                val exported = it.getInt(exportedIndex) == 1

                res = SessionEntity(id, createdAt, crashed, exported)
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
            if(it.moveToFirst()) {
                val idIndex = it.getColumnIndex(Constants.DB.EventTable.COL_ID)
                val id = it.getString(idIndex)

                val createdAtIndex = it.getColumnIndex(Constants.DB.EventTable.COL_CREATED_AT)
                val createdAt = it.getLong(createdAtIndex)

                val typeIndex = it.getColumnIndex(Constants.DB.EventTable.COL_TYPE)
                val type = it.getString(typeIndex)

                val serializedDataIndex = it.getColumnIndex(Constants.DB.EventTable.COL_SERIALIZED_DATA)
                val serializedData = it.getString(serializedDataIndex)

                val sessionIdIndex = it.getColumnIndex(Constants.DB.EventTable.COL_SESSION_ID)
                val sessionId = it.getString(sessionIdIndex)

                res = EventEntity(
                    id = id,
                    createdAt = createdAt,
                    type = type,
                    serializedData = serializedData,
                    sessionId = sessionId,
                )
            }
        }

        return res
    }

    override fun insertEvents(events: List<EventEntity>): Boolean {
        writableDatabase.beginTransaction()
        try {
            events.forEach { event ->
                val values = ContentValues().apply {
                    put(Constants.DB.EventTable.COL_ID, event.id)
                    put(Constants.DB.EventTable.COL_CREATED_AT, event.createdAt)
                    put(Constants.DB.EventTable.COL_TYPE, event.type)
                    put(Constants.DB.EventTable.COL_SESSION_ID, event.sessionId)
                    put(Constants.DB.EventTable.COL_SERIALIZED_DATA, event.serializedData)
                }
                if(writableDatabase.insert(Constants.DB.EventTable.NAME, null, values) == -1L) {
                    return false
                }
            }

            writableDatabase.setTransactionSuccessful()
            return true
        } catch (e: SQLiteException) {
            Log.e(TAG, "Error inserting events in db", e)
            return false
        } finally {
            writableDatabase.endTransaction()
        }
    }

    override fun deleteExportedSessions() {
        try {
            val result = writableDatabase.delete(
                Constants.DB.SessionTable.NAME,
                "${Constants.DB.SessionTable.COL_EXPORTED} = 1",
                null
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
}