package com.example.calorietracker

import android.app.AlertDialog
import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import com.example.calorietracker.auth.LoginActivity
import com.example.calorietracker.databinding.FragmentProfileBinding
import com.example.calorietracker.models.DailyGoals
import com.google.firebase.auth.FirebaseAuth

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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

        // Listener per salvare i nuovi valori
        binding.saveGoalsBtn.setOnClickListener {
            val sharedPref = requireContext().getSharedPreferences("daily_goals", Context.MODE_PRIVATE)
            with(sharedPref.edit()) {
                putInt("calorie_goal", binding.dailyCalorieGoalEN.text.toString().toInt())
                putInt("protein_goal", binding.dailyProteinGoalEN.text.toString().toInt())
                putInt("carb_goal", binding.dailyCarbsGoalEN.text.toString().toInt())
                putInt("fat_goal", binding.dailyFatsGoalEN.text.toString().toInt())
                putFloat("water_goal", binding.dailyWaterGoalEN.text.toString().toFloat())
                apply()
            }
            Toast.makeText(requireContext(), "Goals saved", Toast.LENGTH_SHORT).show()

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

    // Funzione per recuperare i valori dei goal giornalieri
    private fun getDailyGoals(): DailyGoals {
        val sharedPref = requireContext().getSharedPreferences("daily_goals", Context.MODE_PRIVATE)

        val calorieGoal = sharedPref.getInt("calorie_goal", 0)
        val proteinGoal = sharedPref.getInt("protein_goal", 0)
        val carbGoal = sharedPref.getInt("carb_goal", 0)
        val fatGoal = sharedPref.getInt("fat_goal", 0)
        val waterGoal = sharedPref.getFloat("water_goal", 0f)

        return DailyGoals(
            calorieGoal = calorieGoal,
            proteinGoal = proteinGoal,
            carbGoal = carbGoal,
            fatGoal = fatGoal,
            waterGoal = waterGoal
        )
    }

    // Funzione per verificare se i valori sono cambiati
    private fun checkGoalsAndSetButtonState() {
        val currentGoals = getDailyGoals()
        val enteredGoals = DailyGoals(
            binding.dailyCalorieGoalEN.text.toString().toIntOrNull() ?: 0,
            binding.dailyProteinGoalEN.text.toString().toIntOrNull() ?: 0,
            binding.dailyCarbsGoalEN.text.toString().toIntOrNull() ?: 0,
            binding.dailyFatsGoalEN.text.toString().toIntOrNull() ?: 0,
            binding.dailyWaterGoalEN.text.toString().toFloatOrNull() ?: 0f
        )
        binding.saveGoalsBtn.isEnabled = currentGoals != enteredGoals
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
        builder.setTitle("Conferm Sign Out")
        builder.setMessage("Are you sure you want to sign out?")

        // Pulsante per confermare il logout
        builder.setPositiveButton("Yes") { _, _ ->
            try {
                FirebaseAuth.getInstance().signOut()
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