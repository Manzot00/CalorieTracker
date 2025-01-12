package com.example.calorietracker.auth

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.calorietracker.HomeActivity
import com.example.calorietracker.NetworkMonitor
import com.example.calorietracker.R
import com.google.firebase.auth.FirebaseAuth

class LoginActivity : AppCompatActivity() {

    lateinit var auth: FirebaseAuth
    lateinit var networkMonitor: NetworkMonitor

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_login)

        networkMonitor = NetworkMonitor(this)
        networkMonitor.startMonitoring()
        networkMonitor.isConnected.observe(this) { isConnected ->
            val offlineMessage = findViewById<TextView>(R.id.offlineMessage)
            offlineMessage.visibility = if (isConnected) View.GONE else View.VISIBLE
        }

        // Inizializza FirebaseAuth
        auth = FirebaseAuth.getInstance()

        // Controlla se l'utente è già autenticato
        if (auth.currentUser != null) {
            goToHomeActivity()
        }
    }

    private fun goToHomeActivity() {
        startActivity(Intent(this, HomeActivity::class.java))
        finish()
    }
}