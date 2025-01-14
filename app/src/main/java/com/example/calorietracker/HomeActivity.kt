package com.example.calorietracker

import android.app.AlarmManager
import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.icu.util.Calendar
import android.icu.util.TimeZone
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.navigation.NavOptions
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.NavigationUI
import androidx.navigation.ui.setupWithNavController
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.calorietracker.utils.NetworkMonitor
import com.example.calorietracker.utils.SynchronizationWorker
import com.example.calorietracker.utils.WeightReminderReceiver
import com.example.calorietracker.utils.WeightTrackerWorker
import com.google.android.material.bottomnavigation.BottomNavigationView
import java.util.concurrent.TimeUnit

class HomeActivity : AppCompatActivity() {

    private lateinit var networkMonitor: NetworkMonitor

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_home)
        scheduleWeightWorker()
        setupSyncWorker()

        networkMonitor = NetworkMonitor(this)
        networkMonitor.startMonitoring()
        networkMonitor.isConnected.observe(this) { isConnected ->
            val offlineMessage = findViewById<TextView>(R.id.offlineMessage)
            offlineMessage.visibility = if (isConnected) View.GONE else View.VISIBLE
        }

        // Controlla e richiedi i permessi per le notifiche
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    101
                )
            } else {
                // Pianifica il promemoria solo se il permesso è già concesso
                scheduleWeightReminder(this, 11, 0) // Pianifica per le 11:00 AM
            }
        } else {
            // Pianifica il promemoria per versioni precedenti ad Android 13
            scheduleWeightReminder(this, 11, 0)
        }

        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottomNavigationView)

        // Imposta il NavController per la BottomNavigationView
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_home) as NavHostFragment
        val navController = navHostFragment.navController
        bottomNavigationView.setupWithNavController(navController)
        bottomNavigationView.setOnItemSelectedListener { menuItem ->
            if (menuItem.itemId == R.id.homeFragment) {
                navController.navigate(R.id.homeFragment, null, NavOptions.Builder()
                    .setPopUpTo(R.id.homeFragment, true) // Pulisce lo stack fino a homeFragment
                    .build())
                true
            } else {
                // Usa il comportamento predefinito per gli altri elementi
                NavigationUI.onNavDestinationSelected(menuItem, navController)
                true
            }
        }

        // Personalizza il comportamento del pulsante back
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (navController.currentDestination?.id == R.id.homeFragment) {
                    finish() // Esce dall'app se sei già nella Home
                } else {
                    navController.navigateUp() // Naviga indietro
                }
            }
        })

        if (intent.hasExtra("targetFragment") && intent.getStringExtra("targetFragment") == "progress") {
            navController.navigate(R.id.progressFragment)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == 101 && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                scheduleWeightReminder(this, 11, 0)
            } else {
                Toast.makeText(
                    this,
                    "Permission for notifications was denied. Reminders won't work.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Ferma il monitoraggio per evitare perdite di memoria
        networkMonitor.stopMonitoring()
    }

    private fun scheduleWeightReminder(context: Context, hour: Int, minute: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, WeightReminderReceiver::class.java).apply {
            action = "com.example.calorietracker.WEIGHT_REMINDER"
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val calendar = Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        alarmManager.setRepeating(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            AlarmManager.INTERVAL_DAY,
            pendingIntent
        )
    }

    private fun scheduleWeightWorker() {
        val workRequest = PeriodicWorkRequestBuilder<WeightTrackerWorker>(1, TimeUnit.DAYS, 5, TimeUnit.SECONDS)
            .setInitialDelay(calculateInitialDelay(0), TimeUnit.MILLISECONDS)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "WeightTrackerWork",
            ExistingPeriodicWorkPolicy.UPDATE,
            workRequest
        )
    }

    private fun calculateInitialDelay(minute: Int): Long {
        val calendar = Calendar.getInstance().apply {
            timeZone = TimeZone.GMT_ZONE
            set(Calendar.HOUR_OF_DAY, 0) // Imposta l'orario in cui vuoi eseguire il lavoro
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
        }

        val now = System.currentTimeMillis()
        val targetTime = calendar.timeInMillis

        return if (targetTime > now) {
            targetTime - now
        } else {
            targetTime + TimeUnit.DAYS.toMillis(1) - now
        }
    }

    private fun setupSyncWorker() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val initialDelay = calculateInitialDelay(0) // Calcola il ritardo fino alle 00:01

        val synchronizationWorkRequest = PeriodicWorkRequestBuilder<SynchronizationWorker>(
            1, TimeUnit.DAYS, // Ripeti ogni giorno
            5, TimeUnit.MINUTES // Retry ogni 5 minuti
        )
            .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(this)
            .enqueueUniquePeriodicWork(
                "synchronization_work",
                ExistingPeriodicWorkPolicy.UPDATE,
                synchronizationWorkRequest
            )
    }
}