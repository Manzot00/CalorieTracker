package com.example.calorietracker.utils

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.preferencesDataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import com.example.calorietracker.models.DailyGoals
import kotlinx.coroutines.flow.map

const val GOALS_DATASTORE = "daily_goals"

val Context.preferencesDataStore : DataStore<Preferences> by preferencesDataStore(name = GOALS_DATASTORE)

class DataStoreManager(val context: Context) {

    companion object {
        val CALORIE_GOAL = intPreferencesKey("calorie_goal")
        val PROTEIN_GOAL = intPreferencesKey("protein_goal")
        val CARB_GOAL = intPreferencesKey("carb_goal")
        val FAT_GOAL = intPreferencesKey("fat_goal")
        val WATER_GOAL = doublePreferencesKey("water_goal")
    }

    suspend fun saveToDataStore(dailyGoals: DailyGoals){
        context.preferencesDataStore.edit {
            it[CALORIE_GOAL] = dailyGoals.calorieGoal
            it[PROTEIN_GOAL] = dailyGoals.proteinGoal
            it[CARB_GOAL] = dailyGoals.carbGoal
            it[FAT_GOAL] = dailyGoals.fatGoal
            it[WATER_GOAL] = dailyGoals.waterGoal
        }
    }

    fun getFromDataStore() = context.preferencesDataStore.data.map{
        DailyGoals(
            it[CALORIE_GOAL] ?: -1,
            it[PROTEIN_GOAL] ?: -1,
            it[CARB_GOAL] ?: -1,
            it[FAT_GOAL] ?: -1,
            it[WATER_GOAL] ?: -1.0
        )
    }

    suspend fun clearDataStore() = context.preferencesDataStore.edit {
            it.clear()
        }

}