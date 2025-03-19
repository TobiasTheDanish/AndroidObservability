package dk.tobiasthedanish.observability.storage

import android.database.sqlite.SQLiteDatabase
import android.util.Log

private const val TAG = "DBMigration"

internal object DBMigration {
    fun run(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        try {
            db.beginTransaction()
            try {
                for (version in oldVersion + 1..newVersion) {
                    when (version) {
                        Constants.DB.Versions.V2 -> migrateToV2(db)
                        else -> Log.e(
                            TAG,
                            "No migration found for version $version",
                        )
                    }
                }
                db.setTransactionSuccessful()
            } finally {
                db.endTransaction()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Unable to migrate from v$oldVersion -> v$newVersion")
        }
    }

    private fun migrateToV2(db: SQLiteDatabase) {
        Constants.SQL.Migrations.V2.forEach {
            db.execSQL(it)
        }
    }
}