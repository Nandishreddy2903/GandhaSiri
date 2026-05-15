package com.gandhasiri.app.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.gandhasiri.app.data.entities.AlertLog
import kotlinx.coroutines.flow.Flow

@Dao
interface AlertLogDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(alertLog: AlertLog): Long

    @Query("SELECT * FROM alert_logs ORDER BY timestamp DESC")
    suspend fun getAll(): List<AlertLog>

    @Query("SELECT * FROM alert_logs ORDER BY timestamp DESC")
    fun getLatestFlow(): Flow<List<AlertLog>>

    @Query("SELECT COUNT(*) FROM alert_logs WHERE timestamp >= :monthStart")
    fun getCountThisMonth(monthStart: Long): Flow<Int>

    @Query("SELECT * FROM alert_logs WHERE id = :id")
    suspend fun getById(id: Int): AlertLog?

    @Query("DELETE FROM alert_logs")
    suspend fun deleteAll()
}
