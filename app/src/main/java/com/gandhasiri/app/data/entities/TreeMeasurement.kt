package com.gandhasiri.app.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tree_measurements")
data class TreeMeasurement(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val treeId: String,
    val girthCm: Double,
    val measuredAt: Long
)
