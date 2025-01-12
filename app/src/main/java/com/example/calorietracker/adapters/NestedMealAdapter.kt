package com.example.calorietracker.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.navigation.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.example.calorietracker.R
import com.example.calorietracker.models.Meal

class NestedMealAdapter(private var meals: MutableList<Meal>) : RecyclerView.Adapter<NestedMealAdapter.MealViewHolder>() {

    class MealViewHolder(val row: View) : RecyclerView.ViewHolder(row) {
        val mealName: TextView = row.findViewById(R.id.mealNameTV)
        val mealServing: TextView = row.findViewById(R.id.mealServingTV)
        val mealCalories: TextView = row.findViewById(R.id.mealCaloriesTV)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MealViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.nested_meal_row, parent, false)
        val holder = MealViewHolder(view)

        holder.row.setOnClickListener {
            val meal = meals[holder.adapterPosition]
            val navController = holder.itemView.findNavController()
            val action = HomeFragmentDirections.actionHomeFragmentToEditMealFragment(meal.mealId)
            navController.navigate(action)
        }

        return holder
    }

    override fun onBindViewHolder(holder: MealViewHolder, position: Int) {
        val meal = meals[position]
        holder.mealName.text = meal.mealName
        holder.mealCalories.text = meal.macros.calories.toString()

        val serving = "${meal.amount} ${meal.servingType}"
        holder.mealServing.text = serving
    }

    override fun getItemCount(): Int {
        return meals.size
    }

    /**
     * Metodo per aggiornare i pasti nell'adapter.
     */
    fun updateMeals(newMeals: MutableList<Meal>) {
        meals.clear() // Pulisci i vecchi dati
        meals.addAll(newMeals) // Aggiungi i nuovi dati
        notifyDataSetChanged() // Notifica i cambiamenti all'adapter
    }
}