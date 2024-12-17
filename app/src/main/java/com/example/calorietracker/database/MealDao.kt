package com.example.calorietracker.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.calorietracker.models.Meal
import com.google.firebase.Timestamp

@Dao
interface MealDao {
    @Query("SELECT * FROM meals")
    fun getAllMeals(): List<Meal>

    @Insert
    fun insertMeal(vararg meal: Meal)

    @Update
    fun updateMeal(meal: Meal)

    @Delete
    fun deleteMeal(meal: Meal)

    @Query("SELECT * FROM meals WHERE creationDate = :date")
    fun getMealsByDate(date: String): List<Meal>

    @Query("SELECT * FROM meals WHERE mealId = :id")
    fun getMealById(id: String): Meal

}