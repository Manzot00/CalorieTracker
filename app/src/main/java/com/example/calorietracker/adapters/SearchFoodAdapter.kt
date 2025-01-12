package com.example.calorietracker.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.navigation.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.example.calorietracker.R
import com.example.calorietracker.models.FoodSearchResponse

class SearchFoodAdapter(private val data: List<FoodSearchResponse>, private val mealCategory: String) : RecyclerView.Adapter<SearchFoodAdapter.SearchFoodViewHolder>() {

    class SearchFoodViewHolder(val row: View) : RecyclerView.ViewHolder(row) {
        val foodName: TextView = row.findViewById(R.id.searchFoodName)
        val foodDescription: TextView = row.findViewById(R.id.searchFoodDescription)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SearchFoodViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.food_search_row, parent, false)
        val holder = SearchFoodViewHolder(view)

        holder.row.setOnClickListener {
            val navController = holder.row.findNavController()
            val action = SearchFragmentDirections.actionSearchFragmentToAddFoodFragment(data[holder.adapterPosition].food_id, mealCategory)
            navController.navigate(action)
        }

        return holder
    }

    override fun onBindViewHolder(holder: SearchFoodViewHolder, position: Int) {
        val food = data[position]
        val foodName = if (food.brand_name.isNullOrEmpty()) {
            food.food_name // Solo il nome del cibo
        } else {
            "${food.food_name} (${food.brand_name})" // Nome del cibo e brand
        }
        holder.foodName.text = foodName
        holder.foodDescription.text = food.food_description
    }

    override fun getItemCount(): Int {
        return data.size
    }
}