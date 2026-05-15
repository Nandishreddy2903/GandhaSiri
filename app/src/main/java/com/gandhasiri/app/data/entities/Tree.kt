package com.gandhasiri.app.data.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "trees",
    indices = [Index(value = ["treeId"], unique = true)]
)
data class Tree(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val treeId: String? = null,
    val photoPath: String,
    val latitude: Double,
    val longitude: Double,
    val girthCm: Double,
    val ageYears: Int,
    val notes: String,
    val createdAt: Long,
    val updatedAt: Long = createdAt,
    val aiEstimate: String? = null,
    val aiTimestamp: Long? = null,
    val projectedGrowthJson: String? = null
)
