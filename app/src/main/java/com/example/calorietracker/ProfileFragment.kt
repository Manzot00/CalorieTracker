package com.example.calorietracker

import android.app.AlertDialog
import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.lifecycle.lifecycleScope
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import com.example.calorietracker.api.RetrofitClient
import com.example.calorietracker.auth.LoginActivity
import com.example.calorietracker.databinding.FragmentProfileBinding
import com.example.calorietracker.models.DailyGoals
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private lateinit var sharedPref: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sharedPref = requireActivity().getSharedPreferences("daily_goals", Context.MODE_PRIVATE)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setSavedGoals()

        binding.saveGoalsBtn.setOnClickListener {
                updateDailyGoals()
                // Dopo il salvataggio, aggiorna i valori di riferimento e disabilita il pulsante
                checkGoalsAndSetButtonState()
        }

        // Aggiungi TextWatcher per monitorare i cambiamenti nei campi di input
        val editTexts = listOf(
            binding.dailyCalorieGoalEN,
            binding.dailyProteinGoalEN,
            binding.dailyCarbsGoalEN,
            binding.dailyFatsGoalEN,
            binding.dailyWaterGoalEN
        )
        editTexts.forEach { editText ->
            addTextWatcher(editText)
        }


        val (email, username) = getUserData()
        binding.emailTV.text = email
        binding.usernameTV.text = username

        binding.signOutBtn.setOnClickListener {
            showSignOutConfirmationDialog()
        }
    }

    override fun onResume() {
        super.onResume()
        setSavedGoals()
    }

    // Helper function to check for valid data in DailyGoals
    private fun DailyGoals.hasValidData(): Boolean {
        return calorieGoal != -1 && proteinGoal != -1 && carbGoal != -1 && fatGoal != -1 && waterGoal != -1.0
    }

    private fun setSavedGoals(){
        val dailyGoals = getDailyGoals()

        // Imposta i valori iniziali nelle EditText
        binding.dailyCalorieGoalEN.setText(dailyGoals.calorieGoal.toString())
        binding.dailyProteinGoalEN.setText(dailyGoals.proteinGoal.toString())
        binding.dailyCarbsGoalEN.setText(dailyGoals.carbGoal.toString())
        binding.dailyFatsGoalEN.setText(dailyGoals.fatGoal.toString())
        binding.dailyWaterGoalEN.setText(dailyGoals.waterGoal.toString())

        // Disabilita il pulsante inizialmente poiché i valori non sono stati modificati
        binding.saveGoalsBtn.isEnabled = false
    }

    private var isFetchingFromAPI = false

    // Funzione per recuperare i valori dei goal giornalieri
    private fun getDailyGoals(): DailyGoals {
        if (sharedPref.contains("calorie_goal") &&
            sharedPref.contains("protein_goal") &&
            sharedPref.contains("carb_goal") &&
            sharedPref.contains("fat_goal") &&
            sharedPref.contains("water_goal")
        ) {
            return DailyGoals(
                sharedPref.getInt("calorie_goal", -1),
                sharedPref.getInt("protein_goal", -1),
                sharedPref.getInt("carb_goal", -1),
                sharedPref.getInt("fat_goal", -1),
                sharedPref.getFloat("water_goal", -1.0f).toDouble()
            )
        } else {
            // Return default values immediately
            if (!isFetchingFromAPI) {
                isFetchingFromAPI = true
                getDailyGoalsFromAPI(sharedPref)
            }
            return DailyGoals() // Return default values to display the screen quickly
        }
    }

    private fun getDailyGoalsFromAPI(sharedPreferences: SharedPreferences) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                isFetchingFromAPI = true
                val user = FirebaseAuth.getInstance().currentUser
                val token = user?.getIdToken(false)?.await()?.token
                val userId = user?.uid

                val response = RetrofitClient.myAPIService.getDailyGoals(userId!!, "Bearer $token")
                when (response.code()) {
                    200, 201 -> {
                        val dailyGoals = response.body()
                        if (dailyGoals != null) {
                            sharedPreferences.edit().apply {
                                putInt("calorie_goal", dailyGoals.calorieGoal)
                                putInt("protein_goal", dailyGoals.proteinGoal)
                                putInt("carb_goal", dailyGoals.carbGoal)
                                putInt("fat_goal", dailyGoals.fatGoal)
                                putFloat("water_goal", dailyGoals.waterGoal.toFloat())
                                apply()
                            }
                            withContext(Dispatchers.Main) {
                                binding.dailyCalorieGoalEN.setText(dailyGoals.calorieGoal.toString())
                                binding.dailyProteinGoalEN.setText(dailyGoals.proteinGoal.toString())
                                binding.dailyCarbsGoalEN.setText(dailyGoals.carbGoal.toString())
                                binding.dailyFatsGoalEN.setText(dailyGoals.fatGoal.toString())
                                binding.dailyWaterGoalEN.setText(dailyGoals.waterGoal.toString())
                            }
                            isFetchingFromAPI = false
                        }
                    }
                    401 -> {
                        withContext(Dispatchers.Main) {
                            Log.e(TAG, "Unauthorized request: ${response.errorBody()?.string()}")
                            Toast.makeText(requireContext(), "Unauthorized request", Toast.LENGTH_SHORT).show()
                        }
                    }
                    403 -> {
                        withContext(Dispatchers.Main) {
                            Log.e(TAG, "Forbidden request: ${response.errorBody()?.string()}")
                            Toast.makeText(requireContext(), "Forbidden request", Toast.LENGTH_SHORT).show()
                        }
                    }
                    500 -> {
                        withContext(Dispatchers.Main) {
                            Log.e(TAG, "Internal server error: ${response.errorBody()?.string()}")
                            Toast.makeText(requireContext(), "Internal server error", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    //isFetchingFromAPI = false
                    Log.e(TAG, "Error fetching daily goals: ${e.message}")
                    Toast.makeText(requireContext(), "Error fetching daily goals", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun updateDailyGoals() {
        //Local update
        sharedPref.edit().apply {
            putInt("calorie_goal", binding.dailyCalorieGoalEN.text.toString().toInt())
            putInt("protein_goal", binding.dailyProteinGoalEN.text.toString().toInt())
            putInt("carb_goal", binding.dailyCarbsGoalEN.text.toString().toInt())
            putInt("fat_goal", binding.dailyFatsGoalEN.text.toString().toInt())
            putFloat("water_goal", binding.dailyWaterGoalEN.text.toString().toFloat())
            apply()
        }
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                //Remote update
                val user = FirebaseAuth.getInstance().currentUser
                val token = user?.getIdToken(false)?.await()?.token
                val userId = user?.uid

                val dailyGoals = DailyGoals(
                    binding.dailyCalorieGoalEN.text.toString().toInt(),
                    binding.dailyProteinGoalEN.text.toString().toInt(),
                    binding.dailyCarbsGoalEN.text.toString().toInt(),
                    binding.dailyFatsGoalEN.text.toString().toInt(),
                    binding.dailyWaterGoalEN.text.toString().toDouble()
                )

                val response = RetrofitClient.myAPIService.updateDailyGoals(
                    userId!!,
                    "Bearer $token",
                    dailyGoals
                )
                when (response.code()) {
                    200, 201 -> {
                        withContext(Dispatchers.Main) {
                            Log.d(TAG, "Daily goals updated successfully")
                            Toast.makeText(requireContext(), "Daily goals updated successfully", Toast.LENGTH_SHORT).show()
                        }
                    }
                    400 -> {
                        withContext(Dispatchers.Main) {
                            Log.e(TAG, "Bad request: ${response.errorBody()?.string()}")
                            Toast.makeText(requireContext(), "Bad request", Toast.LENGTH_SHORT).show()
                        }
                    }

                    401 -> {
                        withContext(Dispatchers.Main) {
                            Log.e(TAG, "Unauthorized request: ${response.errorBody()?.string()}")
                            Toast.makeText(requireContext(), "Unauthorized request", Toast.LENGTH_SHORT).show()
                        }
                    }

                    403 -> {
                        withContext(Dispatchers.Main) {
                            Log.e(TAG, "Forbidden request: ${response.errorBody()?.string()}")
                            Toast.makeText(
                                requireContext(),
                                "Forbidden request",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }

                    500 -> {
                        withContext(Dispatchers.Main) {
                            Log.e(TAG, "Internal server error: ${response.errorBody()?.string()}")
                            Toast.makeText(
                                requireContext(),
                                "Internal server error",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Log.e(TAG, "Error updating daily goals: ${e.message}")
                    Toast.makeText(
                        requireContext(),
                        "Error updating daily goals",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    // Funzione per verificare se i valori sono cambiati
    private fun checkGoalsAndSetButtonState() {
        val currentGoals = getDailyGoals()
        val enteredGoals = DailyGoals(
            binding.dailyCalorieGoalEN.text.toString().toIntOrNull() ?: -1,
            binding.dailyProteinGoalEN.text.toString().toIntOrNull() ?: -1,
            binding.dailyCarbsGoalEN.text.toString().toIntOrNull() ?: -1,
            binding.dailyFatsGoalEN.text.toString().toIntOrNull() ?: -1,
            binding.dailyWaterGoalEN.text.toString().toDoubleOrNull() ?: -1.0
        )
        // Check if any EditText is empty
        val isAnyEditTextEmpty = listOf(
            binding.dailyCalorieGoalEN,
            binding.dailyProteinGoalEN,
            binding.dailyCarbsGoalEN,
            binding.dailyFatsGoalEN,
            binding.dailyWaterGoalEN
        ).any { it.text.isEmpty() }

        binding.saveGoalsBtn.isEnabled = currentGoals != enteredGoals && !isAnyEditTextEmpty
    }

    // Funzione per aggiungere un TextWatcher generico
    private fun addTextWatcher(editText: EditText) {
        editText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                checkGoalsAndSetButtonState()
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    // Funzione per recuperare i dati dell'utente
    private fun getUserData(): Pair<String?, String?> {
        val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)

        val sharedPreferences = EncryptedSharedPreferences.create(
            "user_data",
            masterKeyAlias,
            requireContext(),
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )

        val email = sharedPreferences.getString("email", null) // Recupera l'email, null se non trovata
        val username = sharedPreferences.getString("username", null) // Recupera lo username, null se non trovato

        return Pair(email, username)
    }

    // Funzione per mostrare il dialog di conferma per il logout
    private fun showSignOutConfirmationDialog() {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("SIGN OUT")
        builder.setMessage("Are you sure you want to sign out?")

        // Pulsante per confermare il logout
        builder.setPositiveButton("Yes") { _, _ ->
            try {
                FirebaseAuth.getInstance().signOut()
                sharedPref.edit().clear().apply()
                clearUserData()
                goToLoginActivity()
            } catch (e: Exception) {
                Log.e(TAG, "Error signing out: ${e.message}")
                Toast.makeText(requireContext(), "Error signing out", Toast.LENGTH_SHORT).show()
                e.printStackTrace()
            }
        }

        // Pulsante per annullare
        builder.setNegativeButton("No") { dialog, _ ->
            dialog.dismiss() // Chiudi il dialog
        }

        // Mostra il dialog
        val alertDialog = builder.create()
        alertDialog.setOnShowListener {
            alertDialog.window?.setBackgroundDrawableResource(R.drawable.bottom_nav_background)

            val messageTextView = alertDialog.findViewById<TextView>(android.R.id.message)
            messageTextView?.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary))
            messageTextView?.typeface = ResourcesCompat.getFont(requireContext(), R.font.lexend)

            alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(ContextCompat.getColor(requireContext(), R.color.enabled_color))
            alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(ContextCompat.getColor(requireContext(), R.color.auth_button_background))
        }
        alertDialog.show()
    }

    // Funzione per cancellare i dati dell'utente quando si effettua il logout
    private fun clearUserData() {
        val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)

        val sharedPreferences = EncryptedSharedPreferences.create(
            "user_data",
            masterKeyAlias,
            requireContext(),
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )

        with(sharedPreferences.edit()) {
            clear() // Rimuove tutti i dati salvati
            apply() // Applica le modifiche
        }
        Log.d(TAG, "User data cleared from EncryptedSharedPreferences")
    }

    private fun goToLoginActivity() {
        startActivity(Intent(requireContext(), LoginActivity::class.java))
        requireActivity().finish()
    }
}