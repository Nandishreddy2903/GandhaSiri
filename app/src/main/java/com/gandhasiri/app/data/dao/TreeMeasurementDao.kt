package com.gandhasiri.app.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.gandhasiri.app.data.entities.TreeMeasurement
import kotlinx.coroutines.flow.Flow

@Dao
interface TreeMeasurementDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(measurement: TreeMeasurement): Long

    @Query("SELECT * FROM tree_measurements")
    suspend fun getAll(): List<TreeMeasurement>

    @Query("SELECT * FROM tree_measurements WHERE id = :id")
    suspend fun getById(id: Int): TreeMeasurement?
    
    @Query("SELECT * FROM tree_measurements WHERE treeId = :treeId ORDER BY measuredAt ASC")
    fun getByTreeId(treeId: String): Flow<List<TreeMeasurement>>

    @Query("DELETE FROM tree_measurements")
    suspend fun deleteAll()
}
