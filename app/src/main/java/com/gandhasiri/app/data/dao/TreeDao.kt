package com.gandhasiri.app.data.dao

import kotlinx.coroutines.flow.Flow

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.gandhasiri.app.data.entities.Tree

@Dao
interface TreeDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertInternal(tree: Tree): Long

    @Update
    suspend fun update(tree: Tree)

    @Transaction
    suspend fun insertAndGenerateId(tree: Tree): Long {
        val id = insertInternal(tree)
        if (id > 0) {
            val generatedTreeId = String.format(java.util.Locale.US, "GSIRI-%04d", id)
            val updatedTree = tree.copy(id = id.toInt(), treeId = generatedTreeId)
            update(updatedTree)
        }
        return id
    }

    @Query("SELECT * FROM trees ORDER BY createdAt DESC")
    fun getAll(): Flow<List<Tree>>

    @Query("SELECT * FROM trees WHERE id = :id")
    suspend fun getById(id: Int): Tree?
    
    @Query("SELECT * FROM trees WHERE treeId = :treeId")
    suspend fun getByTreeId(treeId: String): Tree?

    @Query("SELECT * FROM trees WHERE treeId = :treeId")
    fun getTreeByIdFlow(treeId: String): Flow<Tree?>

    @androidx.room.Delete
    suspend fun delete(tree: Tree)

    @Query("DELETE FROM trees")
    suspend fun deleteAll()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMeasurement(measurement: com.gandhasiri.app.data.entities.TreeMeasurement)

    @Query("UPDATE trees SET girthCm = :newGirth, updatedAt = :updatedAt WHERE treeId = :treeId")
    suspend fun updateTreeGirth(treeId: String, newGirth: Double, updatedAt: Long)

    @Query("UPDATE trees SET projectedGrowthJson = :json, updatedAt = :updatedAt WHERE treeId = :treeId")
    suspend fun updateProjectedGrowth(treeId: String, json: String, updatedAt: Long)

    @Query("UPDATE trees SET photoPath = :path, updatedAt = :updatedAt WHERE treeId = :treeId")
    suspend fun updatePhotoPath(treeId: String, path: String, updatedAt: Long)

    @Query("SELECT * FROM tree_measurements WHERE treeId = :treeId ORDER BY measuredAt ASC")
    fun getMeasurementsForTree(treeId: String): Flow<List<com.gandhasiri.app.data.entities.TreeMeasurement>>

    @Query("SELECT * FROM trees WHERE treeId = :treeId")
    fun getTreeById(treeId: String): Flow<Tree?>

    @Transaction
    suspend fun insertTreeWithMeasurement(tree: Tree, measurementDao: TreeMeasurementDao): String {
        val id = insertInternal(tree)
        if (id > 0) {
            val generatedTreeId = String.format(java.util.Locale.US, "GSIRI-%04d", id.toInt())
            val updatedTree = tree.copy(id = id.toInt(), treeId = generatedTreeId)
            update(updatedTree)
            
            // Preload initial measurement
            val firstMeasurement = com.gandhasiri.app.data.entities.TreeMeasurement(
                treeId = generatedTreeId,
                girthCm = tree.girthCm,
                measuredAt = tree.createdAt
            )
            measurementDao.insert(firstMeasurement)
            return generatedTreeId
        }
        throw Exception("Failed to insert tree")
    }
}
