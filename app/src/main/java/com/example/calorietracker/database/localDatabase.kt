package com.example.calorietracker.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.calorietracker.models.Meal
import com.example.calorietracker.models.Weight

@Database(entities = [Meal::class, Weight::class], version = 4)
abstract class LocalDatabase : RoomDatabase() {
    abstract fun getMealDao(): MealDao
    abstract fun getWeightDao(): WeightDao

    companion object {
        @Volatile
        private var INSTANCE: LocalDatabase? = null
        fun getInstance(context: Context): LocalDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    LocalDatabase::class.java,
                    "local_database"
                ).build()
            }
        }
    }
}