package com.example.calorietracker.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "weights")
data class Weight(
    @PrimaryKey val weightId: String,
    val weight: Double,
    val date: String,
    val userId: String
)
