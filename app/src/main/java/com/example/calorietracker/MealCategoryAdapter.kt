package com.example.calorietracker

import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.calorietracker.models.MealCategory
import android.view.LayoutInflater

class MealCategoryAdapter (private val data: List<MealCategory>) : RecyclerView.Adapter<MealCategoryAdapter.CategoryViewHolder>() {

    class CategoryViewHolder(row: View) : RecyclerView.ViewHolder(row) {
        val mealCategoryName: TextView = row.findViewById(R.id.mealCategoryNameTV)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.meal_category_row, parent, false)
        return CategoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        holder.mealCategoryName.text = data[position].name
    }

    override fun getItemCount(): Int {
        return data.size
    }
}