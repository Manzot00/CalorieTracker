package com.example.calorietracker.models

import androidx.lifecycle.MutableLiveData

data class MealCategory(
    val name: String = "",
    val meals: MutableLiveData<MutableList<Meal>> = MutableLiveData(mutableListOf()),
    var isExpandable: Boolean = false
)

object MealCategories {
    val categories = listOf(
        MealCategory("Breakfast", MutableLiveData(mutableListOf())),
        MealCategory("Lunch", MutableLiveData(mutableListOf())),
        MealCategory("Dinner", MutableLiveData(mutableListOf())),
        MealCategory("Snacks", MutableLiveData(mutableListOf()))
    )
    // Variabile di stato per verificare se i dati sono già caricati
    var isDataLoaded = false

    /**
     * Aggiungi un nuovo pasto a una categoria specifica.
     */
    fun addMealToCategory(categoryName: String, meal: Meal) {
        val category = categories.find { it.name == categoryName }
        category?.meals?.value?.let {
            it.add(meal)
            category.meals.postValue(it) // Aggiorna il LiveData
        }
    }

    /**
     * Rimuovi un pasto da una categoria specifica.
     */
    fun removeMealFromCategory(categoryName: String, meal: Meal) {
        val category = categories.find { it.name == categoryName }
        category?.meals?.value?.let {
            it.remove(meal)
            category.meals.postValue(it) // Aggiorna il LiveData
        }
    }

    /**
     * Modifica un pasto in una categoria specifica.
     */
    fun editMealInCategory(categoryName: String, oldMeal: Meal, newMeal: Meal) {
        // Rimuovi il pasto dalla vecchia categoria
        val oldCategory = categories.find { it.name == oldMeal.mealCategory }
        oldCategory?.meals?.value?.let {
            it.remove(oldMeal)
            oldCategory.meals.postValue(it) // Aggiorna il LiveData
        }

        // Aggiungi il pasto alla nuova categoria
        val newCategory = categories.find { it.name == newMeal.mealCategory }
        newCategory?.meals?.value?.let {
            it.add(newMeal)
            newCategory.meals.postValue(it) // Aggiorna il LiveData
        }
    }

    /**
     * Calcola il totale delle calorie di tutti i pasti.
     */
    fun getTotalCalories(): Int {
        var totalCalories = 0
        categories.forEach { category ->
            category.meals.value?.forEach { meal ->
                totalCalories += meal.macros.calories?.toInt() ?: 0
            }
        }
        return totalCalories
    }

    /**
     * Calcola il totale delle proteine di tutti i pasti.
     */
    fun getTotalProteins(): Int {
        var totalProteins = 0
        categories.forEach { category ->
            category.meals.value?.forEach { meal ->
                totalProteins += meal.macros.protein?.toInt() ?: 0
            }
        }
        return totalProteins
    }

    /**
     * Calcola il totale dei carboidrati di tutti i pasti.
     */
    fun getTotalCarbs(): Int {
        var totalCarbs = 0
        categories.forEach { category ->
            category.meals.value?.forEach { meal ->
                totalCarbs += meal.macros.carbohydrate?.toInt() ?: 0
            }
        }
        return totalCarbs
    }

    /**
     * Calcola il totale dei grassi di tutti i pasti.
     */
    fun getTotalFats(): Int {
        var totalFats = 0
        categories.forEach { category ->
            category.meals.value?.forEach { meal ->
                totalFats += meal.macros.fat?.toInt() ?: 0
            }
        }
        return totalFats
    }

    /**
     * Pulisci tutti i dati delle categorie.
     */
    fun clear() {
        categories.forEach { category ->
            category.meals.value?.clear()
            category.meals.postValue(category.meals.value)
        }
    }
}
