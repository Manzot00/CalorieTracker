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

    @Query("SELECT * FROM weights WHERE userId = :userId")
    suspend fun getAllWeights(userId: String): List<Weight>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllWeights(weights: List<Weight>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWeight(weight: Weight)

    @Update
    suspend fun updateWeight(weight: Weight)

    @Delete
    suspend fun deleteWeight(weight: Weight)

    @Query("SELECT * FROM weights WHERE date = :date AND userId = :userId")
    suspend fun getWeightByDate(date: String, userId: String): List<Weight>

    @Query("SELECT COUNT(*) FROM weights WHERE date = :date AND userId = :userId")
    suspend fun isWeightRecorded(date: String, userId: String): Int

    @Query("SELECT * FROM weights WHERE userId = :userId ORDER BY date DESC LIMIT 1 ")
    suspend fun getLatestWeight(userId: String): Weight?

}