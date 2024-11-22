package com.example.calorietracker.auth

import android.content.ContentValues.TAG
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import com.example.calorietracker.HomeActivity
import com.example.calorietracker.R
import com.example.calorietracker.databinding.FragmentLoginBinding
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await


class LoginFragment : Fragment() {

    private lateinit var auth: FirebaseAuth
    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = FirebaseAuth.getInstance()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.loadingBar.visibility = View.INVISIBLE

        binding.loginBtn.setOnClickListener {
            if(!validateInput(binding.emailLogin, binding.pwLogin))
                return@setOnClickListener

            binding.loadingBar.visibility = View.VISIBLE

            lifecycleScope.launch {
                try {
                    auth.signInWithEmailAndPassword(binding.emailLogin.text.toString(), binding.pwLogin.text.toString()).await()
                    Log.d(TAG, "User logged in successfully")

                    auth.currentUser?.let { user -> saveUserData(user) }
                    navigateToHome()

                } catch (e: Exception) {
                    when (e) {
                        is FirebaseAuthInvalidCredentialsException -> { Log.e(TAG, "Wrong email or password", e); Toast.makeText(requireContext(), "Wrong email or password", Toast.LENGTH_LONG).show() }
                        is FirebaseAuthInvalidUserException -> { Log.e(TAG, "Invalid user", e); Toast.makeText(requireContext(), "Invalid user", Toast.LENGTH_LONG).show() }
                        is FirebaseNetworkException -> { Log.e(TAG, "Network error", e); Toast.makeText(requireContext(), "Network error", Toast.LENGTH_LONG).show() }
                        else -> { Log.e(TAG, "Error logging in", e); Toast.makeText(requireContext(), "Error logging in", Toast.LENGTH_LONG).show() }
                    }
                    binding.loadingBar.visibility = View.INVISIBLE
                }
            }
        }

        binding.toRegisterTV.setOnClickListener {
            findNavController().navigate(R.id.action_loginFragment_to_registerFragment)
        }
    }

    private fun validateInput(email: TextView, password: TextView): Boolean {
        when {
            email.text.isEmpty() -> {
                email.error = "Email is required"
                email.requestFocus()
                return false
            }
            password.text.isEmpty() -> {
                password.error = "Password is required"
                password.requestFocus()
                return false
            }
            password.text.length < 6 -> {
                password.error = "Password must be at least 6 characters long"
                password.requestFocus()
                return false
            }
            !Patterns.EMAIL_ADDRESS.matcher(email.text).matches() -> {
                email.error = "Email is invalid"
                email.requestFocus()
                return false
            }
            else -> return true
        }
    }

    private fun navigateToHome() {
        val intent = Intent(requireContext(), HomeActivity::class.java)
        startActivity(intent)
        requireActivity().finish()
    }

    private suspend fun saveUserData(user: FirebaseUser) {
        // Genero il token di autenticazione, ma non lo salvo. Lo genero per averlo in cache e poterlo usare in seguito
        val token = user.getIdToken(true).await()?.token
        val username = user.displayName
        val email = user.email
        val userId = user.uid

        val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)

        val sharedPreferences = EncryptedSharedPreferences.create(
            "user_data",
            masterKeyAlias,
            requireContext(),
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )

        with(sharedPreferences.edit()) {
            putString("email", email)
            putString("username", username)
            putString("userId", userId)
            apply()
        }
        Log.d(TAG, "Username: $username, userId: $userId, token: $token")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}