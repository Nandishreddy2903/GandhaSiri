package com.gandhasiri.app.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "alert_logs")
data class AlertLog(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val type: String,
    val timestamp: Long,
    val note: String
)
