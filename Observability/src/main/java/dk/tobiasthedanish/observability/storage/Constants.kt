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
        }
    }

    data object SQL {

        data object Migrations {
            val V2: Array<String> = arrayOf(
                """
                    ALTER TABLE ${DB.SessionTable.NAME}
                    ADD COLUMN ${DB.SessionTable.COL_EXPORTED} INTEGER DEFAULT 0
                """.trimIndent(),
                """
                    ALTER TABLE ${DB.EventTable.NAME}
                    RENAME TO ${DB.EventTable.NAME}_old
                """.trimIndent(),
                """
                    CREATE TABLE IF NOT EXISTS ${DB.EventTable.NAME} (
                        ${DB.EventTable.COL_ID} TEXT PRIMARY KEY,
                        ${DB.EventTable.COL_SESSION_ID} TEXT NOT NULL,
                        ${DB.EventTable.COL_TYPE} TEXT NOT NULL,
                        ${DB.EventTable.COL_SERIALIZED_DATA} TEXT,
                        ${DB.EventTable.COL_CREATED_AT} INTEGER NOT NULL,
                        FOREIGN KEY (${DB.EventTable.COL_SESSION_ID}) 
                            REFERENCES ${DB.SessionTable.NAME}(${DB.SessionTable.COL_ID})
                            ON UPDATE CASCADE
                            ON DELETE CASCADE
                    )
                """.trimIndent(),
                """
                    INSERT INTO ${DB.EventTable.NAME}
                    SELECT * FROM ${DB.EventTable.NAME}_old
                """.trimIndent()
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

        const val CREATE_EVENTS_TABLE = """
            CREATE TABLE IF NOT EXISTS ${DB.EventTable.NAME} (
                ${DB.EventTable.COL_ID} TEXT PRIMARY KEY,
                ${DB.EventTable.COL_SESSION_ID} TEXT NOT NULL,
                ${DB.EventTable.COL_TYPE} TEXT NOT NULL,
                ${DB.EventTable.COL_SERIALIZED_DATA} TEXT,
                ${DB.EventTable.COL_CREATED_AT} INTEGER NOT NULL,
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
    }
}