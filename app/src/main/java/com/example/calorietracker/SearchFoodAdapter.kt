package com.example.calorietracker

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.navigation.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.example.calorietracker.models.FoodSearchResponse

class SearchFoodAdapter(private val data: List<FoodSearchResponse>) : RecyclerView.Adapter<SearchFoodAdapter.SearchFoodViewHolder>() {

    class SearchFoodViewHolder(val row: View) : RecyclerView.ViewHolder(row) {
        val foodName: TextView = row.findViewById(R.id.searchFoodName)
        val foodDescription: TextView = row.findViewById(R.id.searchFoodDescription)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SearchFoodViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.food_search_row, parent, false)
        val holder = SearchFoodViewHolder(view)

        holder.row.setOnClickListener {
            val navController = holder.row.findNavController()
            val action = SearchFragmentDirections.actionSearchFragmentToAddFoodFragment(data[holder.adapterPosition].food_id)
            navController.navigate(action)
        }

        return holder
    }

    override fun onBindViewHolder(holder: SearchFoodViewHolder, position: Int) {
        holder.foodName.text = data[position].food_name
        holder.foodDescription.text = data[position].food_description
    }

    override fun getItemCount(): Int {
        return data.size
    }
}