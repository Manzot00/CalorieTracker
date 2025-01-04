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
import androidx.activity.addCallback
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.calorietracker.api.RetrofitClient
import com.example.calorietracker.database.LocalDatabase
import com.example.calorietracker.databinding.FragmentAddFoodBinding
import com.example.calorietracker.models.FoodDetailResponse
import com.example.calorietracker.models.Macro
import com.example.calorietracker.models.Meal
import com.example.calorietracker.models.MealCategories
import com.example.calorietracker.models.SelectedDay
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class AddFoodFragment : Fragment() {

    private var _binding: FragmentAddFoodBinding? = null
    private val binding get() = _binding!!
    private var food: FoodDetailResponse? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        _binding = FragmentAddFoodBinding.inflate(inflater, container, false)
        return binding.root
    }

    private val args: AddFoodFragmentArgs by navArgs()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val localDatabase = LocalDatabase.getInstance(requireContext())
        val mealDao = localDatabase.getMealDao()

        // Ottieni la lista di categorie e impostala come elenco di opzioni per il menu a discesa
        val adapter = ArrayAdapter(requireContext(),
            android.R.layout.simple_spinner_item,
            MealCategories.categories.map { it.name })
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.mealCategorySpinner.adapter = adapter
        val selectedCategoryIndex = MealCategories.categories.indexOfFirst { it.name == args.mealCategory }
        if (selectedCategoryIndex != -1) {
            binding.mealCategorySpinner.setSelection(selectedCategoryIndex)
        }

        getFood(args.foodId)

        binding.servingTypeSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedServing = food?.servings?.get(position)
                selectedServing?.let { serving ->

                    // Update UI elements
                    binding.foodAmountET.setText("1")
                    binding.foodCaloriesTV.text = serving.calories?.toString() ?: ""
                    binding.foodProteinTV.text = serving.protein?.toString() ?: ""
                    binding.foodCarbsTV.text = serving.carbohydrate?.toString() ?: ""
                    binding.foodFatsTV.text = serving.fat?.toString() ?: ""
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // Gestisci la quantità di cibo
        binding.foodAmountET.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                binding.saveFoodBtn.isEnabled = (s?.isNotEmpty() ?: false) &&
                        binding.servingTypeSpinner.selectedItemPosition != AdapterView.INVALID_POSITION &&
                        binding.mealCategorySpinner.selectedItemPosition != AdapterView.INVALID_POSITION
                val amount = s.toString().toDoubleOrNull() ?: 0.0 // Default to 1.0 if invalid input
                val selectedServing = food?.servings?.find { it.serving_description == binding.servingTypeSpinner.selectedItem.toString() }

                selectedServing?.let { serving ->
                    val newCalories = serving.calories?.times(amount)
                    val newProtein = serving.protein?.times(amount)
                    val newCarbs = serving.carbohydrate?.times(amount)
                    val newFat = serving.fat?.times(amount)

                    // Update UI elements
                    binding.foodCaloriesTV.text = newCalories?.toString() ?: ""
                    binding.foodProteinTV.text = newProtein?.toString() ?: ""
                    binding.foodCarbsTV.text = newCarbs?.toString() ?: ""
                    binding.foodFatsTV.text = newFat?.toString() ?: ""
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        binding.saveFoodBtn.setOnClickListener {
            val amount = binding.foodAmountET.text.toString().toDoubleOrNull() ?: 0.0
            val selectedServing = food?.servings?.find { it.serving_description == binding.servingTypeSpinner.selectedItem.toString() }
            val mealCategory = binding.mealCategorySpinner.selectedItem?.toString()

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

            if (selectedServing != null && mealCategory != null) {
                val meal = Meal(
                    mealId = UUID.randomUUID().toString(),
                    mealName = "${food?.food_name ?: ""} ${food?.brand_name?.let { "($it)" } ?: ""}",
                    mealCategory = mealCategory,
                    servingType = selectedServing.measurement_description.toString(),
                    amount = amount,
                    creationDate = SelectedDay.selectedDate.value ?: SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()),
                    macros = macros,
                    foodId = food?.food_id ?: 0,
                    userId = FirebaseAuth.getInstance().currentUser?.uid ?: "",
                    lastUpdated = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                )
                lifecycleScope.launch(Dispatchers.IO) {
                    mealDao.insertMeal(meal)
                }
                MealCategories.addMealToCategory(mealCategory, meal)
                Log.d("MealLists", "${MealCategories.categories.map { it.meals }}")

                Toast.makeText(requireContext(), "Meal added to $mealCategory", Toast.LENGTH_SHORT).show()
                val navController = findNavController()
                navController.navigate(AddFoodFragmentDirections.actionAddFoodFragmentToHomeFragment())

            } else {
                Toast.makeText(requireContext(), "Invalid input", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun getFood(foodId: Long) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                binding.fetchingFoodProgress.visibility = View.VISIBLE
                val user = FirebaseAuth.getInstance().currentUser
                val token = user?.getIdToken(false)?.await()?.token

                val response = RetrofitClient.myAPIService.getFood(foodId,"Bearer $token")
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
                                binding.servingTypeSpinner.adapter = servingAdapter
                                // Trigger initial selection
                                binding.servingTypeSpinner.setSelection(0)

                                binding.foodNameTV.text = food?.food_name
                                binding.foodBrandNameTV.text = food?.brand_name
                                binding.foodBrandNameTV.isVisible = food?.brand_name != null
                            }
                        }
                    }
                    401 -> {
                        // Unauthorized
                        Log.e(TAG, "Unauthorized request: ${response.errorBody()?.string()}")
                        Toast.makeText(requireContext(), "Unauthorized request", Toast.LENGTH_SHORT).show()
                    }
                    404 -> {
                        // Forbidden
                        Log.e(TAG, "Forbidden request: ${response.errorBody()?.string()}")
                        Toast.makeText(requireContext(), "Forbidden request", Toast.LENGTH_SHORT).show()
                    }
                    500 -> {
                        // Internal server error
                        Log.e(TAG, "Internal server error: ${response.errorBody()?.string()}")
                        Toast.makeText(requireContext(), "Internal server error", Toast.LENGTH_SHORT).show()
                    }
                    else -> {
                        // Unexpected error
                        Log.e(TAG, "Unexpected error: ${response.errorBody()?.string()}")
                        Toast.makeText(requireContext(), "Unexpected error", Toast.LENGTH_SHORT).show()
                    }
                }
            }catch (e: Exception) {
                e.printStackTrace()
            }finally {
                binding.fetchingFoodProgress.visibility = View.GONE
            }
        }
    }
}