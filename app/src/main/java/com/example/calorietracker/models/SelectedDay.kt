package com.example.calorietracker.models

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

object SelectedDay {
    private val _selectedDate = MutableLiveData<String>()
    val selectedDate: LiveData<String> get() = _selectedDate

    fun updateSelectedDate(newDate: String) {
        _selectedDate.value = newDate
    }
}