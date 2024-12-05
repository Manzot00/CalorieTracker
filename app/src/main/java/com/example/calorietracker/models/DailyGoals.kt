package com.example.calorietracker.models

data class DailyGoals(
    val calorieGoal: Int = -1,
    val proteinGoal: Int = -1,
    val carbGoal: Int = -1,
    val fatGoal: Int = -1,
    val waterGoal: Double = -1.0
)
