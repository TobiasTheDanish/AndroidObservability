package dk.tobiasthedanish.observability.storage

internal sealed class Constants {
    data object DB {
        const val NAME = "observability.db"

        data object Versions {
            const val V1 = 1
            const val V2 = 2
        }

        data object SessionTable {
            const val NAME = "ob_sessions"
            const val COL_ID = "id"
            const val COL_CREATED_AT = "created_at"
            const val COL_CRASHED = "crashed"
            const val COL_EXPORTED = "is_exported"
        }

        data object EventTable {
            const val NAME = "ob_events"
            const val COL_ID = "id"
            const val COL_SESSION_ID = "session_id"
            const val COL_TYPE = "type"
            const val COL_SERIALIZED_DATA = "serialized_data"
            const val COL_CREATED_AT = "created_at"
            const val COL_EXPORTED = "is_exported"
        }

        data object TraceTable {
            const val NAME = "ob_traces"
            const val COL_TRACE_ID = "trace_id"
            const val COL_SESSION_ID = "session_id"
            const val COL_GROUP_ID = "group_id"
            const val COL_PARENT_ID = "parent_id"
            const val COL_NAME = "name"
            const val COL_STATUS = "status"
            const val COL_STARTED_AT = "started_at"
            const val COL_ENDED_AT = "ended_at"
            const val COL_HAS_ENDED = "has_ended"
            const val COL_ERROR_MESSAGE = "error_message"
            const val COL_EXPORTED = "is_exported"
        }
    }

    data object SQL {

        data object Migrations {
            val V2: Array<String> = arrayOf(
//                """
//                    ALTER TABLE ${DB.SessionTable.NAME}
//                    ADD COLUMN ${DB.SessionTable.COL_EXPORTED} INTEGER DEFAULT 0
//                """.trimIndent(),
//                """
//                    ALTER TABLE ${DB.EventTable.NAME}
//                    RENAME TO ${DB.EventTable.NAME}_old
//                """.trimIndent(),
//                """
//                    CREATE TABLE IF NOT EXISTS ${DB.EventTable.NAME} (
//                        ${DB.EventTable.COL_ID} TEXT PRIMARY KEY,
//                        ${DB.EventTable.COL_SESSION_ID} TEXT NOT NULL,
//                        ${DB.EventTable.COL_TYPE} TEXT NOT NULL,
//                        ${DB.EventTable.COL_SERIALIZED_DATA} TEXT,
//                        ${DB.EventTable.COL_CREATED_AT} INTEGER NOT NULL,
//                        FOREIGN KEY (${DB.EventTable.COL_SESSION_ID})
//                            REFERENCES ${DB.SessionTable.NAME}(${DB.SessionTable.COL_ID})
//                            ON UPDATE CASCADE
//                            ON DELETE CASCADE
//                    )
//                """.trimIndent(),
//                """
//                    INSERT INTO ${DB.EventTable.NAME}
//                    SELECT * FROM ${DB.EventTable.NAME}_old
//                """.trimIndent()
            )
        }

        const val CREATE_SESSION_TABLE = """
            CREATE TABLE IF NOT EXISTS ${DB.SessionTable.NAME} (
                ${DB.SessionTable.COL_ID} TEXT PRIMARY KEY,
                ${DB.SessionTable.COL_CREATED_AT} INTEGER NOT NULL,
                ${DB.SessionTable.COL_CRASHED} INTEGER DEFAULT 0,
                ${DB.SessionTable.COL_EXPORTED} INTEGER DEFAULT 0
            )
        """

        const val GET_SESSION = """
                SELECT * FROM ${DB.SessionTable.NAME}
                WHERE ${DB.SessionTable.COL_ID} = ?
            """

        const val GET_SESSION_FOR_EXPORT = """
                SELECT * FROM ${DB.SessionTable.NAME}
                WHERE ${DB.SessionTable.COL_ID} = ? AND ${DB.SessionTable.COL_EXPORTED} = 0
            """

        const val CREATE_EVENTS_TABLE = """
            CREATE TABLE IF NOT EXISTS ${DB.EventTable.NAME} (
                ${DB.EventTable.COL_ID} TEXT PRIMARY KEY,
                ${DB.EventTable.COL_SESSION_ID} TEXT NOT NULL,
                ${DB.EventTable.COL_TYPE} TEXT NOT NULL,
                ${DB.EventTable.COL_SERIALIZED_DATA} TEXT,
                ${DB.EventTable.COL_CREATED_AT} INTEGER NOT NULL,
                ${DB.EventTable.COL_EXPORTED} INTEGER DEFAULT 0,
                FOREIGN KEY (${DB.EventTable.COL_SESSION_ID}) 
                    REFERENCES ${DB.SessionTable.NAME}(${DB.SessionTable.COL_ID})
                    ON UPDATE CASCADE
                    ON DELETE CASCADE
            )
        """

        const val GET_EVENT = """
                SELECT * FROM ${DB.EventTable.NAME}
                WHERE ${DB.EventTable.COL_ID} = ?
            """

        const val GET_EVENTS_FOR_EXPORT = """
                SELECT * FROM ${DB.EventTable.NAME}
                WHERE ${DB.EventTable.COL_SESSION_ID} = ? AND ${DB.EventTable.COL_EXPORTED} = 0
            """

        const val CREATE_TRACE_TABLE = """
            CREATE TABLE IF NOT EXISTS ${DB.TraceTable.NAME} (
                ${DB.TraceTable.COL_TRACE_ID} TEXT PRIMARY KEY,
                ${DB.TraceTable.COL_GROUP_ID} TEXT NOT NULL,
                ${DB.TraceTable.COL_SESSION_ID} TEXT NOT NULL,
                ${DB.TraceTable.COL_PARENT_ID} TEXT,
                ${DB.TraceTable.COL_NAME} TEXT NOT NULL,
                ${DB.TraceTable.COL_STATUS} TEXT NOT NULL,
                ${DB.TraceTable.COL_ERROR_MESSAGE} TEXT,
                ${DB.TraceTable.COL_STARTED_AT} INTEGER NOT NULL,
                ${DB.TraceTable.COL_ENDED_AT} INTEGER NOT NULL,
                ${DB.TraceTable.COL_HAS_ENDED} INTEGER NOT NULL,
                ${DB.TraceTable.COL_EXPORTED} INTEGER DEFAULT 0,
                FOREIGN KEY (${DB.TraceTable.COL_SESSION_ID}) 
                    REFERENCES ${DB.SessionTable.NAME}(${DB.SessionTable.COL_ID})
                    ON UPDATE CASCADE
                    ON DELETE CASCADE
            )
        """

        const val GET_TRACE = """
                SELECT * FROM ${DB.TraceTable.NAME}
                WHERE ${DB.TraceTable.COL_TRACE_ID} = ?
            """

        const val GET_TRACES_FOR_EXPORT = """
                SELECT * FROM ${DB.TraceTable.NAME}
                WHERE ${DB.TraceTable.COL_SESSION_ID} = ? AND ${DB.TraceTable.COL_EXPORTED} = 0
            """
    }
}