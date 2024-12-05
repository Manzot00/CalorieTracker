package com.example.calorietracker.models

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class SharedViewModelMealCategory : ViewModel() {
    private val _mealCategories = MutableLiveData<List<MealCategory>>()
    val mealCategories: LiveData<List<MealCategory>> = _mealCategories

    fun setMealCategories(categories: List<MealCategory>) {
        _mealCategories.value = categories
    }

}