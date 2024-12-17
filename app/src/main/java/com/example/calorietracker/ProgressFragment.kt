package com.example.calorietracker

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.lifecycle.lifecycleScope
import com.example.calorietracker.database.LocalDatabase
import com.example.calorietracker.database.MealDao
import com.example.calorietracker.databinding.FragmentProgressBinding
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class ProgressFragment : Fragment() {

    private var _binding: FragmentProgressBinding? = null
    private val binding get() = _binding!!
    private lateinit var mealDao : MealDao

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
        mealDao = LocalDatabase.getInstance(requireContext()).getMealDao()
        val calendar = Calendar.getInstance()
        val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

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
                        // Apri il date picker qui o gestisci il range personalizzato
                    }
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {
                // Non fa nulla
            }
        }


    }

    private fun setupChart(data: List<Pair<String, Int>>) {
        val lineChart = binding.caloriesLineChart

        // Pulisce i dati vecchi
        lineChart.clear()

        // Crea una lista di Entry per il grafico
        val entries = data.mapIndexed { index, pair ->
            Entry(index.toFloat(), pair.second.toFloat())
        }

        // Crea un dataset
        val dataSet = LineDataSet(entries, "")
        dataSet.color = Color.parseColor("#BFA4A7") // Colore tenue per la linea
        dataSet.setDrawCircles(false) // Rimuove i cerchi sui punti
        dataSet.lineWidth = 2f
        dataSet.setDrawValues(false) // Nasconde i valori sui punti
        //dataSet.mode = LineDataSet.Mode.CUBIC_BEZIER // Rende la linea curva
        dataSet.setDrawFilled(true) // Abilita il riempimento sotto la linea
        dataSet.fillDrawable = ContextCompat.getDrawable(requireContext(), R.drawable.chart_gradient) // Gradient

        // Configura l'asse X
        val xAxis = lineChart.xAxis
        xAxis.valueFormatter = IndexAxisValueFormatter(data.map { it.first })
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.textSize = 10f
        xAxis.textColor = ContextCompat.getColor(requireContext(), R.color.text_secondary)
        xAxis.labelRotationAngle = -45f // Ruota le etichette per evitare sovrapposizioni
        xAxis.setDrawGridLines(false)
        xAxis.typeface = ResourcesCompat.getFont(requireContext(), R.font.lexend)

        // Configura l'asse Y
        val leftAxis = lineChart.axisLeft
        leftAxis.textColor = ContextCompat.getColor(requireContext(), R.color.text_secondary)
        leftAxis.setDrawGridLines(false)
        leftAxis.granularity = 10f // Step asse Y
        leftAxis.typeface = ResourcesCompat.getFont(requireContext(), R.font.lexend)
        lineChart.axisRight.isEnabled = false
        leftAxis.axisMinimum = 0f
        leftAxis.axisMaximum = data.maxOfOrNull { it.second.toFloat() }?.plus(10) ?: 100f

        // Rimuovi descrizione e legende
        lineChart.description.isEnabled = false
        lineChart.legend.isEnabled = false

        // Aggiungi i dati al grafico
        val lineData = LineData(dataSet)
        lineChart.data = lineData
        lineChart.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.background_dark)) // Sfondo scuro
        lineChart.invalidate()
    }

    // Funzione per aggiornare il grafico
    private fun updateChart(startDate: String, endDate: String) {
        lifecycleScope.launch {
            val caloriesData = getCaloriesByDateRange(startDate, endDate)
            withContext(Dispatchers.Main) {
                setupChart(caloriesData)
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
}