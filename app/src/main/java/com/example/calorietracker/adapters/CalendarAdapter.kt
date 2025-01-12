package com.example.calorietracker.adapters

import android.icu.util.Calendar
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.calorietracker.R
import com.example.calorietracker.database.MealDao
import com.example.calorietracker.models.SelectedDay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CalendarAdapter(private var days: List<String>, private val mealDao: MealDao) : RecyclerView.Adapter<CalendarAdapter.CalendarViewHolder>() {

    class CalendarViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val dayTV: TextView = itemView.findViewById(R.id.cellDayText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CalendarViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.day_week_item, parent, false)
        val holder = CalendarViewHolder(view)

        return holder
    }

    override fun onBindViewHolder(holder: CalendarViewHolder, position: Int) {
        val selectedDay = extractDayFromDate(days[position])

        holder.dayTV.text = selectedDay.toString()

        if (days[position] == SelectedDay.selectedDate.value) {
            holder.dayTV.setBackgroundResource(R.drawable.selected_day_background)
        }
        else {
            holder.dayTV.setBackgroundResource(android.R.color.transparent)
        }

        holder.itemView.setOnClickListener {
            if(days[position] == SelectedDay.selectedDate.value) {
                return@setOnClickListener
            }
            SelectedDay.updateSelectedDate(days[position])
            notifyDataSetChanged()
        }
    }

    override fun getItemCount(): Int {
        return days.size
    }

    private fun extractDayFromDate(dateString: String): Int {
        // Specifica il formato della stringa
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        // Converte la stringa in un oggetto Date
        val date = dateFormat.parse(dateString)

        // Usa Calendar per estrarre il giorno del mese
        val calendar = Calendar.getInstance()
        calendar.time = date ?: Date() // Imposta il tempo, usa una data di fallback se `date` è null
        return calendar.get(Calendar.DAY_OF_MONTH)
    }

    // Metodo per aggiornare i giorni nell'adapter
    fun updateDays(newDays: List<String>) {
        days = newDays
        notifyDataSetChanged() // Notifica i cambiamenti all'adapter
    }
}