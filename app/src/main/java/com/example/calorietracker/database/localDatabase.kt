package com.example.calorietracker.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.calorietracker.models.Meal

@Database(entities = [Meal::class], version = 1)
@TypeConverters(TimestampConverter::class)
abstract class localDatabase : RoomDatabase() {
    abstract fun getMealDao(): MealDao

    companion object {
        @Volatile
        private var INSTANCE: localDatabase? = null
        fun getInstance(context: Context): localDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    localDatabase::class.java,
                    "local_database"
                ).build()
            }
        }
    }
}