package com.example.calorietracker.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.calorietracker.models.Meal

@Dao
interface MealDao {
    @Query("SELECT * FROM meals")
    suspend fun getAllMeals(): List<Meal>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllMeals(meals: List<Meal>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMeal(meal: Meal)

    @Update
    suspend fun updateMeal(meal: Meal)

    @Delete
    suspend fun deleteMeal(meal: Meal)

    @Query("SELECT * FROM meals WHERE creationDate = :date")
    suspend fun getMealsByDate(date: String): List<Meal>

    @Query("SELECT * FROM meals WHERE mealId = :id")
    suspend fun getMealById(id: String): Meal

    @Query("SELECT * FROM meals WHERE lastUpdated = :lastUpdated")
    suspend fun getMealsByLastUpdated(lastUpdated: String): List<Meal>

}