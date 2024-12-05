package com.example.calorietracker.models

data class FoodSearchResponse(
    val brand_name: String = "",
    val food_name: String = "",
    val food_description: String = "",
    val food_type: String = "",
    val food_id: Long = 0,
    val food_url: String = "",
)
