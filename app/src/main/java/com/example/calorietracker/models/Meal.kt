package com.example.calorietracker.models

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.google.firebase.Timestamp

@Entity(tableName = "meals")
data class Meal(
    @PrimaryKey val mealId: String ,
    val mealName: String,
    val mealCategory: String,
    val servingType: String,
    val amount: Double,
    val creationDate: String,
    val foodId: Long,
    val userId: String,
    val lastUpdated: String,
    @Embedded val macros: Macro
)

data class Macro(
    val calories: Double?,
    val carbohydrate: Double?,
    val protein: Double?,
    val fat: Double?,
    val saturatedFat: Double?,
    val polyunsaturatedFat: Double?,
    val monounsaturatedFat: Double?,
    val cholesterol: Double?,
    val sodium: Double?,
    val potassium: Double?,
    val fiber: Double?,
    val sugar: Double?,
    val vitaminA: Double?,
    val vitaminC: Double?,
    val calcium: Double?,
    val iron: Double?,
    val transFat: Double?,
    val addedSugars: Double?,
    val vitaminD: Double?
)
