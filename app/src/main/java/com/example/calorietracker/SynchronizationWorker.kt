package com.example.calorietracker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.calorietracker.api.RetrofitClient
import com.example.calorietracker.database.LocalDatabase
import com.example.calorietracker.database.MealDao
import com.example.calorietracker.database.WeightDao
import com.example.calorietracker.models.DailyGoals
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SynchronizationWorker(appContext: Context, workerParams: WorkerParameters) : CoroutineWorker(appContext, workerParams) {

        private val weightDao: WeightDao by lazy {
            LocalDatabase.getInstance(appContext).getWeightDao()
        }

        private val mealDao: MealDao by lazy {
            LocalDatabase.getInstance(appContext).getMealDao()
        }

        private val dailyGoals: DailyGoals = getDailyGoals()

        private fun getDailyGoals(): DailyGoals {
            val sharedPref = applicationContext.getSharedPreferences("daily_goals", Context.MODE_PRIVATE)

            return DailyGoals(
                sharedPref.getInt("calorie_goal", -1),
                sharedPref.getInt("protein_goal", -1),
                sharedPref.getInt("carb_goal", -1),
                sharedPref.getInt("fat_goal", -1),
                sharedPref.getFloat("water_goal", -1.0f).toDouble()
            )
        }


    override suspend fun doWork(): Result {

        try {
            val user = FirebaseAuth.getInstance().currentUser
            val token = user?.getIdToken(false)?.await()?.token ?: throw Exception("User token is null")
            val userId = user.uid

            // Upload meals
            val meals = mealDao.getAllMeals()
            if (meals.isNotEmpty()) {
                val mealResponse = RetrofitClient.myAPIService.uploadMeals(userId, "Bearer $token", meals)
                if (!mealResponse.isSuccessful) {
                    throw Exception("Failed to upload meals: ${mealResponse.errorBody()?.string()}")
                } else {
                    Log.d("SynchronizationWorker", "Meals uploaded successfully: ${mealResponse.body()}")
                }
            }

            // Upload weight
            val weights = weightDao.getAllWeights()
            if (weights.isNotEmpty()) {
                val weightResponse = RetrofitClient.myAPIService.uploadWeight(userId, "Bearer $token", weights)
                if (!weightResponse.isSuccessful) {
                    throw Exception("Failed to upload weight: ${weightResponse.errorBody()?.string()}")
                } else {
                    Log.d("SynchronizationWorker", "Weight uploaded successfully: ${weightResponse.body()}")
                }
            }

            // Upload daily goals
            val goalsResponse = RetrofitClient.myAPIService.updateDailyGoals(userId, "Bearer $token", dailyGoals)
            if (!goalsResponse.isSuccessful) {
                throw Exception("Failed to upload daily goals: ${goalsResponse.errorBody()?.string()}")
            } else {
                Log.d("SynchronizationWorker", "Daily goals updated successfully: ${goalsResponse.body()}")
            }

            return Result.success()
        } catch (e: Exception) {
            Log.e("SynchronizationWorker", "Synchronization failed", e)
            return Result.retry()
        }
    }
}