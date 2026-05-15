package com.gandhasiri.app.data.repository

import android.util.Log
import com.gandhasiri.app.data.dao.TreeDao
import com.gandhasiri.app.data.dao.TreeMeasurementDao
import com.gandhasiri.app.data.entities.Tree
import com.gandhasiri.app.data.entities.TreeMeasurement
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val TAG = "GandhaSiri"

class TreeRepository(
    private val treeDao: TreeDao,
    private val measurementDao: TreeMeasurementDao
) {
    suspend fun addMeasurement(treeId: String, girthCm: Double) {
        withContext(Dispatchers.IO) {
            try {
                val measurement = TreeMeasurement(
                    treeId = treeId,
                    girthCm = girthCm,
                    measuredAt = System.currentTimeMillis()
                )
                measurementDao.insert(measurement)
                treeDao.updateTreeGirth(treeId, girthCm, System.currentTimeMillis())
            } catch (e: Exception) {
                Log.e(TAG, "addMeasurement failed for $treeId", e)
            }
        }
    }

    suspend fun createTreeWithInitialMeasurement(tree: Tree): String? {
        return withContext(Dispatchers.IO) {
            try {
                val rowId = treeDao.insertInternal(tree)
                if (rowId <= 0) return@withContext null

                val id = rowId.toInt()
                val treeId = String.format(java.util.Locale.US, "GSIRI-%04d", id)
                treeDao.update(tree.copy(id = id, treeId = treeId))

                measurementDao.insert(
                    TreeMeasurement(
                        treeId = treeId,
                        girthCm = tree.girthCm,
                        measuredAt = tree.createdAt
                    )
                )
                Log.d(TAG, "Tree registered: $treeId")
                treeId
            } catch (e: Exception) {
                Log.e(TAG, "createTreeWithInitialMeasurement failed", e)
                null
            }
        }
    }

    suspend fun saveProjectedGrowth(treeId: String, json: String) {
        withContext(Dispatchers.IO) {
            treeDao.updateProjectedGrowth(treeId, json, System.currentTimeMillis())
        }
    }

    suspend fun updateTreePhoto(treeId: String, photoPath: String) {
        withContext(Dispatchers.IO) {
            treeDao.updatePhotoPath(treeId, photoPath, System.currentTimeMillis())
        }
    }
}
