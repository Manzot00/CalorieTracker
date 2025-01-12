package com.example.calorietracker

import android.app.DatePickerDialog
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.lifecycle.lifecycleScope
import com.example.calorietracker.database.LocalDatabase
import com.example.calorietracker.database.MealDao
import com.example.calorietracker.database.WeightDao
import com.example.calorietracker.databinding.FragmentProgressBinding
import com.example.calorietracker.models.Weight
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID

class ProgressFragment : Fragment() {

    private var _binding: FragmentProgressBinding? = null
    private val binding get() = _binding!!
    private lateinit var localDatabase : LocalDatabase
    private lateinit var mealDao : MealDao
    private lateinit var weightDao : WeightDao
    private var customStartDate: String? = null
    private var customEndDate: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProgressBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        localDatabase = LocalDatabase.getInstance(requireContext())
        mealDao = localDatabase.getMealDao()
        weightDao = localDatabase.getWeightDao()

        val calendar = Calendar.getInstance()
        val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        visualizeWeightTracker(dateFormatter)

        val adapter = ArrayAdapter.createFromResource(
            requireContext(),
            R.array.time_range, // L'array dal file strings.xml
            android.R.layout.simple_spinner_item // Layout predefinito per gli elementi
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.timeRangeSpinner.adapter = adapter

        binding.timeRangeSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                calendar.time = Date()
                val endDate = dateFormatter.format(calendar.time)
                binding.customRangeTV.visibility = View.GONE

                when (position) {
                    0 -> { // Last 7 days
                        calendar.add(Calendar.DAY_OF_MONTH, -6)
                        val startDate = dateFormatter.format(calendar.time)
                        updateChart(startDate, endDate)

                    }
                    1 -> { // Last 30 days
                        calendar.add(Calendar.DAY_OF_MONTH, -29)
                        val startDate = dateFormatter.format(calendar.time)
                        updateChart(startDate, endDate)
                    }
                    2 -> { // Last 90 days
                        calendar.add(Calendar.DAY_OF_MONTH, -89)
                        val startDate = dateFormatter.format(calendar.time)
                        updateChart(startDate, endDate)
                    }
                    3 -> { // Last 180 days
                        calendar.add(Calendar.DAY_OF_MONTH, -179)
                        val startDate = dateFormatter.format(calendar.time)
                        updateChart(startDate, endDate)
                    }
                    4 -> { // Last 365 days
                        calendar.add(Calendar.DAY_OF_MONTH, -364)
                        val startDate = dateFormatter.format(calendar.time)
                        updateChart(startDate, endDate)
                    }
                    5 -> { // Custom range
                        // Mostra il primo DatePicker per la data di inizio
                        val startDatePicker = DatePickerDialog(
                            requireContext(),
                            R.style.CustomDatePickerDialogTheme,
                            { _, startYear, startMonth, startDay ->
                                customStartDate = String.format("%04d-%02d-%02d", startYear, startMonth + 1, startDay)

                                // Mostra il secondo DatePicker per la data di fine
                                val endDatePicker = DatePickerDialog(
                                    requireContext(),
                                    R.style.CustomDatePickerDialogTheme,
                                    { _, endYear, endMonth, endDay ->
                                        customEndDate = String.format("%04d-%02d-%02d", endYear, endMonth + 1, endDay)

                                        // Aggiorna il grafico con il range selezionato
                                        updateChart(customStartDate!!, customEndDate!!)
                                        binding.customRangeTV.text = "$customStartDate - $customEndDate"
                                        binding.customRangeTV.visibility = View.VISIBLE
                                    },
                                    calendar.get(Calendar.YEAR),
                                    calendar.get(Calendar.MONTH),
                                    calendar.get(Calendar.DAY_OF_MONTH)
                                )
                                endDatePicker.setTitle("Select End Date")
                                endDatePicker.setOnCancelListener {
                                    // Se l'utente annulla, seleziona l'opzione "Last 7 days"
                                    binding.timeRangeSpinner.setSelection(0)
                                }
                                endDatePicker.show()
                            },
                            calendar.get(Calendar.YEAR),
                            calendar.get(Calendar.MONTH),
                            calendar.get(Calendar.DAY_OF_MONTH)
                        )
                        startDatePicker.setTitle("Select Start Date")
                        startDatePicker.setOnCancelListener {
                            // Se l'utente annulla, seleziona l'opzione "Last 7 days"
                            binding.timeRangeSpinner.setSelection(0)
                        }
                        startDatePicker.show()
                    }
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {
                // Non fa nulla
            }
        }

        binding.weightET.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                // Non fa nulla
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                // Non fa nulla
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                binding.saveWeightBtn.isEnabled = (s?.isNotEmpty() ?: false)
            }
        })

        binding.saveWeightBtn.setOnClickListener{
            val weight = Weight(
                weightId = UUID.randomUUID().toString(),
                weight = binding.weightET.text.toString().toDouble(),
                date = dateFormatter.format(Date()),
                userId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
            )
            lifecycleScope.launch(Dispatchers.IO) {
                weightDao.insertWeight(weight)
                withContext(Dispatchers.Main) {
                    binding.weightET.visibility = View.GONE
                    binding.saveWeightBtn.visibility = View.GONE
                    Toast.makeText(requireContext(), "Weight saved", Toast.LENGTH_SHORT).show()
                    // Aggiorna il grafico dopo aver salvato il peso
                    val dates = binding.timeRangeSpinner.selectedItemPosition.let { position ->
                        calculateStartAndEndDateForRange(position, dateFormatter) // Funzione helper per calcolare la data iniziale
                    }
                    updateChart(dates.first, dates.second)
                }
            }
        }
    }

    private fun visualizeWeightTracker(dateFormatter: SimpleDateFormat) {
        lifecycleScope.launch(Dispatchers.IO) {
            val today = dateFormatter.format(Date())
            val isWeightRecordedForToday = weightDao.isWeightRecorded(today) > 0

            // Aggiorna la visibilità della UI per oggi
            withContext(Dispatchers.Main) {
                if (isWeightRecordedForToday) {
                    binding.weightET.visibility = View.GONE
                    binding.saveWeightBtn.visibility = View.GONE
                } else {
                    binding.weightET.visibility = View.VISIBLE
                    binding.saveWeightBtn.visibility = View.VISIBLE
                }
            }
        }
    }

    private fun setupChart(data: List<Pair<String, Number>>, chart: LineChart, isWeightChart: Boolean = false) {
        chart.clear()

        val entries = data.mapIndexed { index, pair ->
            Entry(index.toFloat(), pair.second.toFloat())
        }

        val dataSet = LineDataSet(entries, "")
        dataSet.color = Color.parseColor("#BFA4A7")
        dataSet.setDrawCircles(false)
        dataSet.lineWidth = 2f
        dataSet.setDrawValues(false)
        dataSet.setDrawFilled(true)
        dataSet.fillDrawable = ContextCompat.getDrawable(requireContext(), R.drawable.chart_gradient)

        val xAxis = chart.xAxis
        xAxis.valueFormatter = IndexAxisValueFormatter(data.map { it.first })
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.textSize = 10f
        xAxis.textColor = ContextCompat.getColor(requireContext(), R.color.text_secondary)
        xAxis.labelRotationAngle = -45f
        xAxis.setDrawGridLines(false)
        xAxis.typeface = ResourcesCompat.getFont(requireContext(), R.font.lexend)

        val leftAxis = chart.axisLeft
        leftAxis.textColor = ContextCompat.getColor(requireContext(), R.color.text_secondary)
        leftAxis.setDrawGridLines(false)
        leftAxis.typeface = ResourcesCompat.getFont(requireContext(), R.font.lexend)
        chart.axisRight.isEnabled = false

        // Personalizza l'asse Y per il grafico del peso
        if (isWeightChart) {
            leftAxis.granularity = 1f
            leftAxis.axisMinimum = data.minOfOrNull { it.second.toFloat() }?.let { it - 1f } ?: 0f
            leftAxis.axisMaximum = data.maxOfOrNull { it.second.toFloat() }?.let { it + 1f } ?: 10f
        } else {
            leftAxis.granularity = 10f
            leftAxis.axisMinimum = 0f
            leftAxis.axisMaximum = data.maxOfOrNull { it.second.toFloat() }?.let { it + 10f } ?: 100f
        }

        chart.description.isEnabled = false
        chart.legend.isEnabled = false

        val lineData = LineData(dataSet)
        chart.data = lineData
        chart.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.background_dark))
        chart.invalidate()
    }

    // Funzione per aggiornare il grafico
    private fun updateChart(startDate: String, endDate: String) {
        lifecycleScope.launch {
            val caloriesData = getCaloriesByDateRange(startDate, endDate)
            val weightsData = getWeightsByDateRange(startDate, endDate)
            withContext(Dispatchers.Main) {
                setupChart(caloriesData, binding.caloriesLineChart)
                setupChart(weightsData, binding.weightLineChart, isWeightChart = true)
            }
        }
    }

    private suspend fun getCaloriesByDateRange(startDate: String, endDate: String): List<Pair<String, Int>> {
        val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val calendar = Calendar.getInstance()
        val caloriesData = mutableListOf<Pair<String, Int>>()

        // Converte le stringhe di startDate e endDate in oggetti Date
        val start = dateFormatter.parse(startDate)
        val end = dateFormatter.parse(endDate)

        if (start != null && end != null) {
            calendar.time = start

            while (!calendar.time.after(end)) { // Itera finché non supera la data finale
                val currentDate = dateFormatter.format(calendar.time)

                val calories = withContext(Dispatchers.IO) {
                    mealDao.getMealsByDate(currentDate).sumOf { it.macros.calories?.toInt() ?: 0 }
                }

                caloriesData.add(Pair(currentDate, calories))

                // Sposta la data al giorno successivo
                calendar.add(Calendar.DAY_OF_MONTH, 1)
            }
        }

        return caloriesData
    }

    private suspend fun getWeightsByDateRange(startDate: String, endDate: String): List<Pair<String, Double>> {
        val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val calendar = Calendar.getInstance()
        val weightsData = mutableListOf<Pair<String, Double>>()

        // Converte le stringhe di startDate e endDate in oggetti Date
        val start = dateFormatter.parse(startDate)
        val end = dateFormatter.parse(endDate)

        if (start != null && end != null) {
            calendar.time = start

            while (!calendar.time.after(end)) { // Itera finché non supera la data finale
                val currentDate = dateFormatter.format(calendar.time)

                val weight = withContext(Dispatchers.IO) {
                    weightDao.getWeightByDate(currentDate).firstOrNull()?.weight ?: 0.0
                }
                weightsData.add(Pair(currentDate, weight))
                // Sposta la data al giorno successivo
                calendar.add(Calendar.DAY_OF_MONTH, 1)
            }
        }
        return weightsData
    }

    private fun calculateStartAndEndDateForRange(position: Int, dateFormatter: SimpleDateFormat): Pair<String, String> {
        val calendar = Calendar.getInstance()

        var endDate = dateFormatter.format(calendar.time)
        var startDate = ""
        // Calcola la startDate in base al range selezionato
        when (position) {
            0 -> { calendar.add(Calendar.DAY_OF_MONTH, -6); startDate = dateFormatter.format(calendar.time) } // Ultimi 7 giorni
            1 -> { calendar.add(Calendar.DAY_OF_MONTH, -29); startDate = dateFormatter.format(calendar.time) }// Ultimi 30 giorni
            2 -> { calendar.add(Calendar.DAY_OF_MONTH, -89); startDate = dateFormatter.format(calendar.time) }// Ultimi 90 giorni
            3 -> { calendar.add(Calendar.DAY_OF_MONTH, -179); startDate = dateFormatter.format(calendar.time) }// Ultimi 180 giorni
            4 -> { calendar.add(Calendar.DAY_OF_MONTH, -364); startDate = dateFormatter.format(calendar.time) }// Ultimi 365 giorni
            5 -> { startDate = customStartDate!!; endDate = customEndDate!! }
        }

        return Pair(startDate, endDate)
    }
}