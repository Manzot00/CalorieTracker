package com.example.calorietracker.utils

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

    private val userId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    override suspend fun doWork(): Result {
        val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val calendar = Calendar.getInstance()

        // Controlla se il peso di ieri è registrato
        calendar.add(Calendar.DAY_OF_MONTH, -1)
        var currentDate = dateFormatter.format(calendar.time)

        // Ottieni l'ultimo peso registrato (non importa la data)
        val latestWeight = weightDao.getLatestWeight(userId)
            ?: return Result.success() // Nessun peso disponibile, nulla da fare

        // Scorri indietro nel tempo per registrare i pesi mancanti
        while (weightDao.isWeightRecorded(currentDate, userId) == 0) {
            val missingWeight = Weight(
                weightId = UUID.randomUUID().toString(),
                weight = latestWeight.weight, // Usa l'ultimo peso registrato
                date = currentDate, // Data mancante
                userId = userId
            )
            weightDao.insertWeight(missingWeight)

            // Passa alla data precedente
            calendar.add(Calendar.DAY_OF_MONTH, -1)
            currentDate = dateFormatter.format(calendar.time)
        }

        return Result.success()
    }
}