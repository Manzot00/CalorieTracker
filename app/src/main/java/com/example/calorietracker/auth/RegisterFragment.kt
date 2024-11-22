package com.example.calorietracker.auth

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
import com.example.calorietracker.R
import com.example.calorietracker.api.RetrofitClient
import com.example.calorietracker.databinding.FragmentRegisterBinding
import com.example.calorietracker.models.User
import kotlinx.coroutines.launch
import org.json.JSONObject

class RegisterFragment : Fragment() {

    private var _binding: FragmentRegisterBinding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        _binding = FragmentRegisterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.loadingBar.visibility = View.INVISIBLE

        binding.registerBtn.setOnClickListener {
            if(!validateInput(binding.emailRegister, binding.pwRegister, binding.usernameRegister))
                return@setOnClickListener

            binding.loadingBar.visibility = View.VISIBLE

            val emailText = binding.emailRegister.text.toString()
            val passwordText = binding.pwRegister.text.toString()
            val usernameText = binding.usernameRegister.text.toString()

            lifecycleScope.launch {
                try{
                    val newUser = User(usernameText, emailText, passwordText)
                    val response = RetrofitClient.myAPIService.registerUser(newUser)
                    when (response.code()) {
                        201 -> {
                            Log.d("RegistrationSuccess", "User registered successfully")
                            // get the response message
                            val responseMessage = response.body()?.message
                            Log.d("RegistrationSuccess", "Response message: $responseMessage")
                            Toast.makeText(requireContext(), "$responseMessage", Toast.LENGTH_SHORT).show()
                            // Navigate to LoginFragment
                            findNavController().navigate(R.id.action_registerFragment_to_loginFragment)
                        }
                        400 -> {
                            val errorBody = response.errorBody()?.string()
                            val errorMessage = extractErrorMessage(errorBody)
                            Log.e("RegistrationError", "Error registering user: $errorBody")
                            Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_LONG).show()
                            binding.loadingBar.visibility = View.INVISIBLE
                        }
                        500 -> {
                            val errorBody = response.errorBody()?.string()
                            Log.e("RegistrationError", "Error registering user: $errorBody")
                            Toast.makeText(requireContext(), "Registration failed", Toast.LENGTH_SHORT).show()
                            binding.loadingBar.visibility = View.INVISIBLE
                        }
                    }

                } catch (e: Exception) {
                    Log.e("RegistrationException", "Error registering user: ", e)
                    // Show error message to the user
                    Toast.makeText(requireContext(), "Registration failed", Toast.LENGTH_SHORT).show()
                    binding.loadingBar.visibility = View.INVISIBLE
                }
            }
        }

        binding.toLoginTV.setOnClickListener {
            findNavController().navigate(R.id.action_registerFragment_to_loginFragment)
        }
    }

    private fun extractErrorMessage(errorBody: String?): String {
        if(errorBody != null) {
            val errorJson = JSONObject(errorBody)
            return errorJson.getString("error")
        } else {
            return "Registration failed"
        }
    }

    private fun validateInput(email: TextView, password: TextView, username: TextView): Boolean {
        when {
            email.text.toString().isEmpty() -> { email.error = "Email is required"; email.requestFocus(); return false }
            password.text.toString().isEmpty() -> { password.error = "Password is required"; password.requestFocus(); return false }
            username.text.toString().isEmpty() -> { username.error = "Username is required"; username.requestFocus(); return false }
            password.text.toString().length < 6 -> { password.error = "Password must be at least 6 characters long"; password.requestFocus(); return false }
            !Patterns.EMAIL_ADDRESS.matcher(email.text.toString()).matches() -> { email.error = "Email is invalid"; email.requestFocus(); return false }
            else -> return true
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}