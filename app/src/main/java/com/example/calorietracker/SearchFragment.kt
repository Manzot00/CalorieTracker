package com.example.calorietracker

import android.content.ContentValues.TAG
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SearchView
import android.widget.Toast
import androidx.activity.addCallback
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.calorietracker.api.RetrofitClient
import com.example.calorietracker.databinding.FragmentSearchBinding
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class SearchFragment : Fragment() {

    private var _binding: FragmentSearchBinding? = null
    private val binding get() = _binding!!
    private val args: SearchFragmentArgs by navArgs()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        _binding = FragmentSearchBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.searchRV.layoutManager = LinearLayoutManager(requireContext())

        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                if (query != null) {
                    searchFoods(query)
                }
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                return true
            }
        })

        binding.barcodeBtn.setOnClickListener {
            val action = SearchFragmentDirections.actionSearchFragmentToBarcodeScannerFragment(args.mealCategory)
            findNavController().navigate(action)
        }
    }

    private fun searchFoods(query: String) {
        lifecycleScope.launch {
            try {
                binding.fetchingProgress.visibility = View.VISIBLE

                val user = FirebaseAuth.getInstance().currentUser
                val token = user?.getIdToken(false)?.await()?.token

                val response = RetrofitClient.myAPIService.searchFoods("Bearer $token", query)
                when (response.code()) {
                    200 -> {
                        val data = response.body()
                        if (data != null) {
                            binding.searchRV.adapter = SearchFoodAdapter(data, args.mealCategory)
                        }
                    }
                    400 -> {
                        // Bad request
                        Log.e(TAG, "Bad request: ${response.errorBody()?.string()}")
                    }
                    401 -> {
                        // Unauthorized
                        Log.e(TAG, "Unauthorized: ${response.errorBody()?.string()}")
                    }
                    403 -> {
                        // Forbidden
                        Log.e(TAG, "Forbidden: ${response.errorBody()?.string()}")
                    }
                    500 -> {
                        // Internal server error
                        Toast.makeText(requireContext(), "Internal server error", Toast.LENGTH_SHORT).show()
                        Log.e(TAG, "Internal server error: ${response.errorBody()?.string()}")
                    }
                    else -> {
                        Log.e(TAG, "Unknown error: ${response.errorBody()?.string()}")
                    }
                }

            }catch (e: Exception) {
                Toast.makeText(requireContext(), "Error retrieving data", Toast.LENGTH_SHORT).show()
                Log.e(TAG, e.toString())
            }finally {
                // Nascondi il ProgressBar dopo aver ricevuto la risposta
                binding.fetchingProgress.visibility = View.GONE
            }
        }
    }
}