package com.gandhasiri.app.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "patrol_logs")
data class PatrolLog(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val timestamp: Long,
    val status: String,
    val notes: String
)
