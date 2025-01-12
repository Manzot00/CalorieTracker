package com.example.calorietracker.adapters

import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.calorietracker.models.MealCategory
import android.view.LayoutInflater
import android.widget.ImageButton
import android.widget.ImageView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.navigation.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.calorietracker.R

class MealCategoryAdapter (private val data: List<MealCategory>) : RecyclerView.Adapter<MealCategoryAdapter.CategoryViewHolder>() {

    class CategoryViewHolder(row: View) : RecyclerView.ViewHolder(row) {
        val mealCategoryName: TextView = row.findViewById(R.id.mealCategoryNameTV)
        val mealsRecyclerView: RecyclerView = row.findViewById(R.id.mealsRV)
        val addBtn: ImageButton = row.findViewById(R.id.addMealBtn)
        val mealCategoryExpand: ConstraintLayout = row.findViewById(R.id.totalMealsLayout)
        val mealsExpandImage: ImageView = row.findViewById(R.id.mealsExpandImage)
        val totalMealsTV : TextView = row.findViewById(R.id.totalMealsTV)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.meal_category_row, parent, false)
        return CategoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        val category = data[position]
        holder.mealCategoryName.text = category.name

        // Imposta il layout del RecyclerView annidato
        holder.mealsRecyclerView.layoutManager = LinearLayoutManager(holder.itemView.context)
        val nestedAdapter = NestedMealAdapter(category.meals.value ?: mutableListOf())
        holder.mealsRecyclerView.adapter = nestedAdapter

        // Evita osservazioni multiple
        if (!category.meals.hasObservers()) {
            val lifecycleOwner = holder.itemView.findViewTreeLifecycleOwner()
            if (lifecycleOwner != null) {
                // Osserva i cambiamenti nel LiveData dei pasti
                category.meals.observe(lifecycleOwner) { updatedMeals ->
                    nestedAdapter.updateMeals(updatedMeals) // Aggiorna l'adapter annidato
                    Log.d("MealLists", "${updatedMeals.map { it.mealName }}")

                    // Aggiorna il testo e la visibilità in base al numero di pasti
                    holder.totalMealsTV.text = "${updatedMeals.size} meals"
                    holder.mealCategoryExpand.isVisible = updatedMeals.isNotEmpty()
                }
            }
        }

        holder.addBtn.setOnClickListener {
            val navController = holder.itemView.findNavController()
            val action = HomeFragmentDirections.actionHomeFragmentToSearchFragment(data[position].name)
            navController.navigate(action)
        }

        val isExpandable: Boolean = data[position].isExpandable
        holder.mealsRecyclerView.isVisible = isExpandable
        holder.totalMealsTV.text = "${data[position].meals.value?.size} meals"
        holder.mealCategoryExpand.isVisible = data[position].meals.value?.isNotEmpty() ?: false
        holder.mealsExpandImage.setImageResource(
            when (isExpandable) {
                true -> android.R.drawable.arrow_up_float
                false -> android.R.drawable.arrow_down_float
            }
        )

        holder.mealCategoryExpand.setOnClickListener {
            data[position].isExpandable = !data[position].isExpandable
            notifyItemChanged(position)
        }
    }

    override fun getItemCount(): Int {
        return data.size
    }
}