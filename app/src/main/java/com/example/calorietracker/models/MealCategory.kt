package com.example.calorietracker.models

data class MealCategory(
    val name: String = "",
    val meal: MutableList<String> = mutableListOf()
)
