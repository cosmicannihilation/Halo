package com.example.testapplication
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "heart_rate_readings")
data class HeartRateReading(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val timestamp: Long,
    val heartRate: Int,
    val status: String
)
