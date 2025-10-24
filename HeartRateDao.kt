package com.example.testapplication

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface HeartRateDao {
    @Insert
    suspend fun insert(reading: HeartRateReading)

    @Query("SELECT COUNT(*) FROM heart_rate_readings")
    suspend fun getCount(): Int

    @Query("SELECT * FROM heart_rate_readings ORDER BY id DESC LIMIT 1")
    suspend fun getLatest(): HeartRateReading?
}
