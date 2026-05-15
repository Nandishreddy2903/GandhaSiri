package com.gandhasiri.app.data

import android.content.Context
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.gandhasiri.app.data.dao.AlertLogDao
import com.gandhasiri.app.data.dao.PatrolLogDao
import com.gandhasiri.app.data.dao.TreeDao
import com.gandhasiri.app.data.dao.TreeMeasurementDao
import com.gandhasiri.app.data.entities.AlertLog
import com.gandhasiri.app.data.entities.PatrolLog
import com.gandhasiri.app.data.entities.Tree
import com.gandhasiri.app.data.entities.TreeMeasurement

@Database(
    entities = [Tree::class, TreeMeasurement::class, AlertLog::class, PatrolLog::class],
    version = 8,
    exportSchema = true
)
abstract class GandhaSiriDatabase : RoomDatabase() {

    abstract fun treeDao(): TreeDao
    abstract fun treeMeasurementDao(): TreeMeasurementDao
    abstract fun alertLogDao(): AlertLogDao
    abstract fun patrolLogDao(): PatrolLogDao

    companion object {
        @Volatile private var INSTANCE: GandhaSiriDatabase? = null

        fun getDatabase(context: Context): GandhaSiriDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    GandhaSiriDatabase::class.java,
                    "gandhasiri_database"
                )
                .addMigrations(
                    MIGRATION_1_2,
                    MIGRATION_2_3,
                    MIGRATION_3_4,
                    MIGRATION_4_5,
                    MIGRATION_5_6,
                    MIGRATION_6_7,
                    MIGRATION_7_8
                )
                .addCallback(object : RoomDatabase.Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        Log.d("GandhaSiri", "DB created (v${db.version})")
                    }
                    override fun onOpen(db: SupportSQLiteDatabase) {
                        super.onOpen(db)
                        Log.d("GandhaSiri", "DB opened (v${db.version})")
                    }
                })
                .build()
                .also { INSTANCE = it }
            }
        }
    }
}
