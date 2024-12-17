package com.example.calorietracker.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.calorietracker.models.Meal

@Database(entities = [Meal::class], version = 2)
@TypeConverters(TimestampConverter::class)
abstract class LocalDatabase : RoomDatabase() {
    abstract fun getMealDao(): MealDao

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