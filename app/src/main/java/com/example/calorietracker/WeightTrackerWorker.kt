package com.example.calorietracker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.calorietracker.database.LocalDatabase
import com.example.calorietracker.database.WeightDao
import com.example.calorietracker.models.Weight
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID

class WeightTrackerWorker(appContext: Context, workerParams: WorkerParameters) : CoroutineWorker(appContext, workerParams) {

    private val weightDao: WeightDao by lazy {
        LocalDatabase.getInstance(appContext).getWeightDao()
    }

    override suspend fun doWork(): Result {
        val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val today = dateFormatter.format(Date())
        val calendar = Calendar.getInstance()

        // Controlla se il peso di ieri è registrato
        calendar.add(Calendar.DAY_OF_MONTH, -1)
        var currentDate = dateFormatter.format(calendar.time)

        while (weightDao.isWeightRecorded(currentDate) == 0) {
            val latestWeight = weightDao.getLatestWeight()
            if (latestWeight != null) {
                // Registra il peso mancante
                val missingWeight = Weight(
                    weightId = UUID.randomUUID().toString(),
                    weight = latestWeight.weight,
                    date = currentDate,
                    userId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
                )
                weightDao.insertWeight(missingWeight)
            } else {
                break // Esci se non ci sono dati di peso precedenti
            }

            // Passa alla data precedente
            calendar.add(Calendar.DAY_OF_MONTH, -1)
            currentDate = dateFormatter.format(calendar.time)
        }

        return Result.success()
    }
}