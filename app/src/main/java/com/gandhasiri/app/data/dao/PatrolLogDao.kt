package com.gandhasiri.app.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.gandhasiri.app.data.entities.PatrolLog
import kotlinx.coroutines.flow.Flow

@Dao
interface PatrolLogDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(patrolLog: PatrolLog): Long

    @Query("SELECT * FROM patrol_logs ORDER BY timestamp DESC")
    fun getAllFlow(): Flow<List<PatrolLog>>

    @Query("SELECT * FROM patrol_logs ORDER BY timestamp DESC LIMIT 5")
    fun getLastFiveFlow(): Flow<List<PatrolLog>>

    @Query("SELECT COUNT(*) FROM patrol_logs WHERE timestamp >= :monthStart")
    fun getCountThisMonth(monthStart: Long): Flow<Int>

    @Query("DELETE FROM patrol_logs")
    suspend fun deleteAll()
}
