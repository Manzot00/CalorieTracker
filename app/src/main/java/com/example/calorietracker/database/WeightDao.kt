package com.example.calorietracker.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.calorietracker.models.Weight

@Dao
interface WeightDao {

    @Query("SELECT * FROM weights")
    suspend fun getAllWeights(): List<Weight>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllWeights(weights: List<Weight>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWeight(weight: Weight)

    @Update
    suspend fun updateWeight(weight: Weight)

    @Delete
    suspend fun deleteWeight(weight: Weight)

    @Query("SELECT * FROM weights WHERE date = :date")
    suspend fun getWeightByDate(date: String): List<Weight>

    @Query("SELECT COUNT(*) FROM weights WHERE date = :date")
    suspend fun isWeightRecorded(date: String): Int

    @Query("SELECT * FROM weights ORDER BY date DESC LIMIT 1")
    suspend fun getLatestWeight(): Weight?

}