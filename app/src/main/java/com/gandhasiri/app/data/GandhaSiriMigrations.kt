package com.gandhasiri.app.data

import android.util.Log
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

private const val TAG = "GandhaSiri"

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        Log.d(TAG, "DB migration 1→2")
        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `tree_measurements` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `treeId` TEXT NOT NULL,
                `girthCm` REAL NOT NULL,
                `measuredAt` INTEGER NOT NULL
            )
            """.trimIndent()
        )
    }
}

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(database: SupportSQLiteDatabase) {
        Log.d(TAG, "DB migration 2→3")
        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `alert_logs` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `type` TEXT NOT NULL,
                `timestamp` INTEGER NOT NULL,
                `note` TEXT NOT NULL
            )
            """.trimIndent()
        )
    }
}

// SQLite doesn't support adding a UNIQUE constraint via ALTER TABLE,
// so trees is recreated with the new schema.
val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(database: SupportSQLiteDatabase) {
        Log.d(TAG, "DB migration 3→4")
        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `trees_new` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `treeId` TEXT,
                `photoPath` TEXT NOT NULL,
                `latitude` REAL NOT NULL,
                `longitude` REAL NOT NULL,
                `girthCm` REAL NOT NULL,
                `ageYears` INTEGER NOT NULL,
                `notes` TEXT NOT NULL,
                `createdAt` INTEGER NOT NULL,
                `updatedAt` INTEGER NOT NULL
            )
            """.trimIndent()
        )
        database.execSQL(
            """
            INSERT INTO `trees_new`
                (`id`, `treeId`, `photoPath`, `latitude`, `longitude`, `girthCm`, `ageYears`, `notes`, `createdAt`, `updatedAt`)
            SELECT
                `id`, NULL, `photoPath`, `latitude`, `longitude`, `girthCm`, `ageYears`, `notes`, `createdAt`, `createdAt`
            FROM `trees`
            """.trimIndent()
        )
        database.execSQL("DROP TABLE `trees`")
        database.execSQL("ALTER TABLE `trees_new` RENAME TO `trees`")
        database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_trees_treeId` ON `trees` (`treeId`)")
    }
}

val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(database: SupportSQLiteDatabase) {
        Log.d(TAG, "DB migration 4→5")
        database.execSQL("ALTER TABLE `trees` ADD COLUMN `aiEstimate` TEXT")
        database.execSQL("ALTER TABLE `trees` ADD COLUMN `aiTimestamp` INTEGER")
    }
}

val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(database: SupportSQLiteDatabase) {
        Log.d(TAG, "DB migration 5→6")
        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `patrol_logs` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `timestamp` INTEGER NOT NULL,
                `status` TEXT NOT NULL,
                `notes` TEXT NOT NULL
            )
            """.trimIndent()
        )
    }
}

val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // No-op: version bump only
    }
}

val MIGRATION_7_8 = object : Migration(7, 8) {
    override fun migrate(database: SupportSQLiteDatabase) {
        Log.d(TAG, "DB migration 7→8")
        database.execSQL("ALTER TABLE `trees` ADD COLUMN `projectedGrowthJson` TEXT")
    }
}
