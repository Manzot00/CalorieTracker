package com.example.calorietracker

import android.app.DatePickerDialog
import android.content.Context
import android.icu.util.Calendar
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.calorietracker.database.LocalDatabase
import com.example.calorietracker.database.MealDao
import com.example.calorietracker.databinding.FragmentHomeBinding
import com.example.calorietracker.models.DailyGoals
import com.example.calorietracker.models.MealCategories
import com.example.calorietracker.models.SelectedDay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private lateinit var localDatabase : LocalDatabase
    private lateinit var mealDao : MealDao
    private var currentWeek: List<String> = emptyList()

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
        localDatabase = LocalDatabase.getInstance(requireContext())
        mealDao = localDatabase.getMealDao()

        // Inizializza con la data corrente se non è impostata
        if (SelectedDay.selectedDate.value.isNullOrEmpty()) {
            SelectedDay.updateSelectedDate(
                SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            )
        }

        // Calcola la settimana iniziale
        currentWeek = getWeekDates(SelectedDay.selectedDate.value ?: "")

        // Imposta l'adapter per il RecyclerView
        val calendarAdapter = CalendarAdapter(currentWeek, mealDao)
        binding.calendarRecyclerView.layoutManager = GridLayoutManager(requireContext(), 7)
        binding.calendarRecyclerView.adapter = calendarAdapter

        SelectedDay.selectedDate.observe(viewLifecycleOwner) { newDate ->
            binding.selectedDateTV.text = newDate

            // Calcola la settimana della nuova data
            val newWeek = getWeekDates(newDate)

            // Aggiorna l'adapter solo se la settimana è diversa
            if (newWeek != currentWeek) {
                currentWeek = newWeek // Aggiorna la settimana corrente
                calendarAdapter.updateDays(newWeek) // Aggiorna l'adapter con i nuovi giorni
            } else {
                // Notifica l'adapter anche se la settimana è la stessa
                calendarAdapter.notifyDataSetChanged()
            }

            // Aggiorna i pasti relativi alla nuova data
            lifecycleScope.launch(Dispatchers.IO) {
                MealCategories.clear()
                mealDao.getMealsByDate(newDate).forEach {
                    MealCategories.addMealToCategory(it.mealCategory, it)
                }
            }
        }

        // Imposta l'adapter per le categorie di pasti
        binding.mealCategoriesRV.layoutManager = LinearLayoutManager(requireContext())
        val mealCategoryAdapter = MealCategoryAdapter(MealCategories.categories)
        binding.mealCategoriesRV.adapter = mealCategoryAdapter
        // Osserva i cambiamenti nelle categorie di pasti e notifica l'adapter
        MealCategories.categories.forEach { category ->
            category.meals.observe(viewLifecycleOwner) {
                binding.mealCategoriesRV.adapter?.notifyDataSetChanged() // Notifica i cambiamenti
            }
        }

        val dailyGoals = getDailyGoals()
        binding.targetCaloriesTV.text = "Target kcals: ${dailyGoals.calorieGoal}"
        MealCategories.categories.forEach { category ->
            category.meals.observe(viewLifecycleOwner) {
                // Aggiorna la UI ogni volta che cambia una categoria
                updateUI(dailyGoals)
            }
        }

        binding.datePickerBtn.setOnClickListener {
            openDatePicker(SelectedDay.selectedDate.value ?: "")
        }
    }

    /*private fun populateMealCategories(mealDao: MealDao, adapter: MealCategoryAdapter) {
        // Ottieni la data corrente
        val calendar = Calendar.getInstance()
        val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)

        lifecycleScope.launch(Dispatchers.IO) {
            // Recupera i pasti dal database
            val meals = mealDao.getMealsByDate(currentDate)
            Log.d("MealLists", "${meals.map { it.mealName }}")

            // Aggiungi i pasti alle categorie
            meals.forEach { meal ->
                MealCategories.addMealToCategory(meal.mealCategory, meal)
            }

            // Imposta la variabile di stato a true
            MealCategories.isDataLoaded = true

            withContext(Dispatchers.Main) {
                // Notifica all'adapter che i dati sono cambiati
                adapter.notifyDataSetChanged()
                val dailyGoals = getDailyGoals()
                binding.targetCaloriesTV.text = "Target kcals: ${dailyGoals.calorieGoal}"
                binding.remainingCalsTV.text = "${dailyGoals.calorieGoal - MealCategories.getTotalCalories()} kcals\n remaining"
                binding.proteinTV.text = "${MealCategories.getTotalProteins()}/${dailyGoals.proteinGoal}g protein"
                binding.carbsTV.text = "${MealCategories.getTotalCarbs()}/${dailyGoals.carbGoal}g carbs"
                binding.fatsTV.text = "${MealCategories.getTotalFats()}/${dailyGoals.fatGoal}g fats"
                val ringChart = binding.ringChart
                ringChart.max = dailyGoals.calorieGoal
                ringChart.progress = MealCategories.getTotalCalories()
            }
        }
    }*/

    private fun getDailyGoals(): DailyGoals{
        val sharedPref = requireActivity().getSharedPreferences("daily_goals", Context.MODE_PRIVATE)
        val dailyGoals = DailyGoals(
            sharedPref.getInt("calorie_goal", -1),
            sharedPref.getInt("protein_goal", -1),
            sharedPref.getInt("carb_goal", -1),
            sharedPref.getInt("fat_goal", -1),
            sharedPref.getFloat("water_goal", -1.0f).toDouble()
        )
        return dailyGoals
    }

    private fun getWeekDates(date: String): List<String> {
        // Converte la data passata come argomento in un oggetto Date
        val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val inputDate = dateFormatter.parse(date) ?: return emptyList() // Gestisce i casi di input non valido

        // Ottieni un'istanza di Calendar basata sulla data fornita
        val calendar = Calendar.getInstance()
        calendar.time = inputDate

        // Trova il giorno della settimana della data fornita (1 = Domenica, 2 = Lunedì, ..., 7 = Sabato)
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)

        // Sposta il calendario al primo giorno della settimana (Lunedì)
        val offsetToMonday = if (dayOfWeek == Calendar.SUNDAY) -6 else Calendar.MONDAY - dayOfWeek
        calendar.add(Calendar.DAY_OF_MONTH, offsetToMonday)

        // Crea una lista di stringhe per la settimana della data fornita
        val weekDates = mutableListOf<String>()

        for (i in 0 until 7) {
            val dateString = dateFormatter.format(calendar.time) // Formatta la data come stringa
            weekDates.add(dateString)
            calendar.add(Calendar.DAY_OF_MONTH, 1) // Vai al giorno successivo
        }

        return weekDates
    }

    // Funzione per aggiornare la UI
    private fun updateUI(dailyGoals: DailyGoals) {
        binding.remainingCalsTV.text = "${dailyGoals.calorieGoal - MealCategories.getTotalCalories()} kcals\n remaining"
        binding.proteinTV.text = "${MealCategories.getTotalProteins()}/${dailyGoals.proteinGoal}g protein"
        binding.carbsTV.text = "${MealCategories.getTotalCarbs()}/${dailyGoals.carbGoal}g carbs"
        binding.fatsTV.text = "${MealCategories.getTotalFats()}/${dailyGoals.fatGoal}g fats"

        val ringChart = binding.ringChart
        ringChart.max = dailyGoals.calorieGoal
        ringChart.progress = MealCategories.getTotalCalories()
    }

    private fun openDatePicker(selectedDate: String) {
        // Formatta la data passata come parametro
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val calendar = Calendar.getInstance()

        // Converte la data passata in Calendar
        val date = dateFormat.parse(selectedDate)
        if (date != null) {
            calendar.time = date
        }

        // Ottieni anno, mese e giorno
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        // Crea il DatePickerDialog
        val datePickerDialog = DatePickerDialog(
            requireContext(),
            R.style.CustomDatePickerDialogTheme,
            { _, selectedYear, selectedMonth, selectedDay ->
                // Aggiorna SelectedDay.selectedDate con la nuova data
                val newSelectedDate = String.format(
                    Locale.getDefault(),
                    "%04d-%02d-%02d",
                    selectedYear,
                    selectedMonth + 1, // Mesi partono da 0
                    selectedDay
                )
                SelectedDay.updateSelectedDate(newSelectedDate)
            },
            year,
            month,
            day
        )

        // Mostra il DatePickerDialog
        datePickerDialog.show()
    }
}