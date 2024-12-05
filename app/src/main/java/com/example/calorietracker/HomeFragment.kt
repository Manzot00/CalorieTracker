package com.example.calorietracker

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.calorietracker.databinding.FragmentHomeBinding
import com.example.calorietracker.models.MealCategory
import com.example.calorietracker.models.SharedViewModelMealCategory


class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private val sharedViewModel: SharedViewModelMealCategory by activityViewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val mealCategories = listOf(
            MealCategory("Breakfast", mutableListOf()),
            MealCategory("Lunch", mutableListOf()),
            MealCategory("Dinner", mutableListOf()),
            MealCategory("Snacks", mutableListOf())
        )
        sharedViewModel.setMealCategories(mealCategories)

        val rv = view.findViewById<RecyclerView>(R.id.mealCategoriesRV)
        rv.layoutManager = LinearLayoutManager(requireContext())
        rv.adapter = MealCategoryAdapter(mealCategories)

        val sharedPrefs = requireContext().getSharedPreferences("daily_goals", Context.MODE_PRIVATE)
        val calorieGoal = sharedPrefs.getInt("calorie_goal", 0)

        binding.RemainingCaloriesTV.text = calorieGoal.toString()
    }
}