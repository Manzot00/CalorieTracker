package com.example.calorietracker.models

data class FoodDetailResponse(
    val food_id: Long,
    val food_name: String,
    val food_type: String,
    val food_url: String,
    val brand_name: String? = null, // Presente solo se food_type è "Brand"
    val servings: List<Serving>
)

data class Serving(
    val serving_id: Long,
    val serving_description: String,
    val serving_url: String,
    val metric_serving_amount: Double?,
    val metric_serving_unit: String?,
    val number_of_units: Double?,
    val measurement_description: String?,
    val calories: Double?,
    val carbohydrate: Double?,
    val protein: Double?,
    val fat: Double?,
    val saturated_fat: Double?,
    val polyunsaturated_fat: Double?,
    val monounsaturated_fat: Double?,
    val cholesterol: Double?,
    val sodium: Double?,
    val potassium: Double?,
    val fiber: Double?,
    val sugar: Double?,
    val vitamin_a: Double?,
    val vitamin_c: Double?,
    val calcium: Double?,
    val iron: Double?,
    val trans_fat: Double?,
    val added_sugars: Double?,
    val vitamin_d: Double?
)
