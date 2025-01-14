package com.example.calorietracker

import android.content.ContentValues.TAG
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.calorietracker.api.RetrofitClient
import com.example.calorietracker.database.LocalDatabase
import com.example.calorietracker.database.MealDao
import com.example.calorietracker.databinding.FragmentEditMealBinding
import com.example.calorietracker.models.FoodDetailResponse
import com.example.calorietracker.models.Macro
import com.example.calorietracker.models.Meal
import com.example.calorietracker.models.MealCategories
import com.example.calorietracker.utils.isInternetAvailable
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class EditMealFragment : Fragment() {

    private var _binding: FragmentEditMealBinding? = null
    private val binding get() = _binding!!
    private var food: FoodDetailResponse? = null
    private var meal: Meal? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        _binding = FragmentEditMealBinding.inflate(inflater, container, false)
        return binding.root
    }

    private val args: EditMealFragmentArgs by navArgs()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (!isInternetAvailable(requireContext())) {
            Toast.makeText(requireContext(), "No internet connection", Toast.LENGTH_SHORT).show()
            findNavController().popBackStack()
        }

        val localDatabase = LocalDatabase.getInstance(requireContext())
        val mealDao = localDatabase.getMealDao()

        val adapter = ArrayAdapter(requireContext(),
            android.R.layout.simple_spinner_item,
            MealCategories.categories.map { it.name })
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.mealCategorySpinner.adapter = adapter

        getMealDetails(mealDao)

        binding.mealServingTypeSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedServing = food?.servings?.get(position)
                selectedServing?.let { serving ->

                    // Update UI elements
                    binding.mealCaloriesTV.text = serving.calories?.toString() ?: ""
                    binding.mealProteinTV.text = serving.protein?.toString() ?: ""
                    binding.mealCarbsTV.text = serving.carbohydrate?.toString() ?: ""
                    binding.mealFatsTV.text = serving.fat?.toString() ?: ""
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // Gestisci la quantità di cibo
        binding.mealAmountET.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                binding.saveMealBtn.isEnabled = (s?.isNotEmpty() ?: false)
                val amount = s.toString().toDoubleOrNull() ?: 0.0 // Default to 1.0 if invalid input
                val selectedServing = food?.servings?.find { it.serving_description == binding.mealServingTypeSpinner.selectedItem.toString() }

                selectedServing?.let { serving ->
                    val newCalories = serving.calories?.times(amount)
                    val newProtein = serving.protein?.times(amount)
                    val newCarbs = serving.carbohydrate?.times(amount)
                    val newFat = serving.fat?.times(amount)

                    // Update UI elements
                    binding.mealCaloriesTV.text = newCalories?.toString() ?: ""
                    binding.mealProteinTV.text = newProtein?.toString() ?: ""
                    binding.mealCarbsTV.text = newCarbs?.toString() ?: ""
                    binding.mealFatsTV.text = newFat?.toString() ?: ""
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        binding.deleteMealBtn.setOnClickListener {
            lifecycleScope.launch(Dispatchers.IO) {
                mealDao.deleteMeal(meal!!)
                withContext(Dispatchers.Main) {
                    MealCategories.removeMealFromCategory(meal!!.mealCategory, meal!!)
                    Toast.makeText(requireContext(), "Meal deleted", Toast.LENGTH_SHORT).show()
                    findNavController().navigate(R.id.homeFragment)
                }
            }
        }

        binding.saveMealBtn.setOnClickListener {
            val amount = binding.mealAmountET.text.toString().toDouble()
            val selectedServing = food?.servings?.find { it.serving_description == binding.mealServingTypeSpinner.selectedItem.toString() }
            val mealCategory = binding.mealCategorySpinner.selectedItem.toString()

            val macros = Macro(
                calories = selectedServing?.calories?.times(amount),
                carbohydrate = selectedServing?.carbohydrate?.times(amount),
                protein = selectedServing?.protein?.times(amount),
                fat = selectedServing?.fat?.times(amount),
                saturatedFat = selectedServing?.saturated_fat?.times(amount),
                polyunsaturatedFat = selectedServing?.polyunsaturated_fat?.times(amount),
                monounsaturatedFat = selectedServing?.monounsaturated_fat?.times(amount),
                cholesterol = selectedServing?.cholesterol?.times(amount),
                sodium = selectedServing?.sodium?.times(amount),
                potassium = selectedServing?.potassium?.times(amount),
                fiber = selectedServing?.fiber?.times(amount),
                sugar = selectedServing?.sugar?.times(amount),
                vitaminA = selectedServing?.vitamin_a?.times(amount),
                vitaminC = selectedServing?.vitamin_c?.times(amount),
                calcium = selectedServing?.calcium?.times(amount),
                iron = selectedServing?.iron?.times(amount),
                transFat = selectedServing?.trans_fat?.times(amount),
                addedSugars = selectedServing?.added_sugars?.times(amount),
                vitaminD = selectedServing?.vitamin_d?.times(amount)
            )

            val updatedMeal = meal?.copy(
                amount = amount,
                mealCategory = mealCategory,
                servingType = selectedServing?.measurement_description ?: "",
                macros = macros,
                lastUpdated = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            )

            lifecycleScope.launch(Dispatchers.IO) {
                mealDao.updateMeal(updatedMeal!!)
                withContext(Dispatchers.Main) {
                    MealCategories.editMealInCategory(meal!!.mealCategory, meal!!, updatedMeal)
                    Toast.makeText(requireContext(), "Meal updated", Toast.LENGTH_SHORT).show()
                    findNavController().navigate(R.id.homeFragment)
                }
            }
        }

    }

    private fun getMealDetails(mealDao: MealDao) {
        lifecycleScope.launch(Dispatchers.IO) {
            binding.fetchingMealProgress.visibility = View.VISIBLE
            meal = mealDao.getMealById(args.mealId)
            withContext(Dispatchers.Main) {
                binding.mealAmountET.setText(meal!!.amount.toString())
                binding.mealCategorySpinner.setSelection(MealCategories.categories.indexOfFirst { it.name == meal!!.mealCategory })
            }

            try {
                val user = FirebaseAuth.getInstance().currentUser
                val token = user?.getIdToken(false)?.await()?.token

                val response = RetrofitClient.myAPIService.getFood(meal!!.foodId,"Bearer $token")
                when (response.code()) {
                    200 -> {
                        val foodDetailResponse = response.body()
                        if (foodDetailResponse != null) {
                            food = foodDetailResponse
                            val servings = food?.servings ?: emptyList()

                            withContext(Dispatchers.Main) {
                                val servingAdapter = ArrayAdapter(requireContext(),
                                    android.R.layout.simple_spinner_item,
                                    servings.map { it.serving_description })
                                servingAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                                binding.mealServingTypeSpinner.adapter = servingAdapter
                                // Trigger initial selection
                                binding.mealServingTypeSpinner.setSelection(servings.indexOfFirst { it.measurement_description == meal!!.servingType })

                                binding.mealNameTV.text = food?.food_name
                                binding.mealBrandNameTV.text = food?.brand_name
                                binding.mealBrandNameTV.isVisible = food?.brand_name != null
                            }
                        }
                    }
                    401 -> {
                        // Unauthorized
                        Log.e(TAG, "Unauthorized request: ${response.errorBody()?.string()}")
                        withContext(Dispatchers.Main) {
                            Toast.makeText(
                                requireContext(),
                                "Unauthorized request",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                    404 -> {
                        // Forbidden
                        Log.e(TAG, "Forbidden request: ${response.errorBody()?.string()}")
                        withContext(Dispatchers.Main) {
                            Toast.makeText(
                                requireContext(),
                                "Forbidden request",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                    500 -> {
                        // Internal server error
                        Log.e(TAG, "Internal server error: ${response.errorBody()?.string()}")
                        withContext(Dispatchers.Main) {
                            Toast.makeText(
                                requireContext(),
                                "Internal server error",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                    else -> {
                        // Unexpected error
                        Log.e(TAG, "Unexpected error: ${response.errorBody()?.string()}")
                        withContext(Dispatchers.Main) {
                            Toast.makeText(requireContext(), "Unexpected error", Toast.LENGTH_SHORT)
                                .show()
                        }
                    }
                }
            }catch (e: Exception) {
                e.printStackTrace()
            }finally {
                withContext(Dispatchers.Main) {
                    binding.fetchingMealProgress.visibility = View.GONE
                }
            }

        }
    }

}