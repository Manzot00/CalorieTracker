package com.example.calorietracker

import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.OptIn
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.calorietracker.api.RetrofitClient
import com.example.calorietracker.databinding.FragmentBarcodeScannerBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.concurrent.Executors


class BarcodeScannerFragment : Fragment() {

    private val cameraPermission = android.Manifest.permission.CAMERA
    private val cameraRequestCode = 100
    private var _binding: FragmentBarcodeScannerBinding? = null
    private val binding get() = _binding!!
    private val args: BarcodeScannerFragmentArgs by navArgs()
    private var isProcessingBarcode = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        _binding = FragmentBarcodeScannerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        requestCameraPermission()
    }

    private fun requestCameraPermission() {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                cameraPermission
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(cameraPermission),
                cameraRequestCode
            )
        } else {
            startCamera()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == cameraRequestCode && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startCamera()
        }
    }


    @OptIn(ExperimentalGetImage::class)
    private fun startCamera(){
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build()
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            val barcodeScanner = BarcodeScanning.getClient()

            val analysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()

            analysis.setAnalyzer(Executors.newSingleThreadExecutor()) { imageProxy ->
                val mediaImage = imageProxy.image
                if (mediaImage != null) {
                    val inputImage = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                    barcodeScanner.process(inputImage)
                        .addOnSuccessListener { barcodes ->
                            for (barcode in barcodes) {
                                val rawValue = barcode.rawValue
                                if (!isProcessingBarcode) { // Verifica ancora prima di elaborare
                                    isProcessingBarcode = true // Imposta lo stato come "in corso"
                                    val gtin13Code = convertToGTIN13(rawValue!!)
                                    Log.d("BarcodeScanner", "Barcode detected: $gtin13Code")
                                    getFoodId(gtin13Code) // Chiama l'API
                                }
                            }
                        }
                        .addOnFailureListener { e ->
                            Log.e("BarcodeScanner", "Barcode scanning failed", e)
                        }
                        .addOnCompleteListener {
                            imageProxy.close() // Assicurati di chiudere il frame per evitare memory leaks
                        }
                }
            }

            preview.surfaceProvider = binding.cameraPreview.surfaceProvider

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, analysis)
            } catch (e: Exception) {
                Log.e("BarcodeScanner", "Camera binding failed", e)
            }
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun convertToGTIN13(code: String): String {
        return when (code.length) {
            12 -> "0$code" // UPC-A: aggiungi uno zero
            13 -> code     // EAN-13: già valido
            8 -> code.padStart(13, '0') // EAN-8: aggiungi zeri fino a 13 cifre
            6 -> {
                // UPC-E: Converti in UPC-A, poi in GTIN-13
                val upcA = convertUpcEToUpcA(code)
                "0$upcA"
            }
            else -> throw IllegalArgumentException("Invalid barcode length: ${code.length}. Supported lengths are 6, 8, 12, or 13.")
        }
    }

    private fun convertUpcEToUpcA(upcE: String): String {
        if (upcE.length != 6) throw IllegalArgumentException("Invalid UPC-E code length: ${upcE.length}")

        val manufacturer = upcE.substring(0, 3)
        val product = upcE.substring(3, 5)

        return when (val checkDigit = upcE.last()) {
            '0', '1', '2' -> "${manufacturer}${product[1]}0000${checkDigit}"
            '3' -> "${manufacturer}${product[0]}00000${checkDigit}"
            '4' -> "${manufacturer}00000${product}${checkDigit}"
            in '5'..'9' -> "${manufacturer}${product}0000${checkDigit}"
            else -> throw IllegalArgumentException("Invalid check digit in UPC-E: $checkDigit")
        }
    }

    private fun getFoodId(barcode: String) {
        lifecycleScope.launch(Dispatchers.IO){
            try {
                val user = FirebaseAuth.getInstance().currentUser
                val token = user?.getIdToken(false)?.await()?.token

                val response = RetrofitClient.myAPIService.getFoodByBarcode(barcode, "Bearer $token")
                if (response.isSuccessful) {
                    val foodId = response.body()?.food_id
                    Log.d("BarcodeScanner", "Food ID: $foodId")
                    withContext(Dispatchers.Main) {
                        val navController = findNavController()
                        val action = BarcodeScannerFragmentDirections.actionBarcodeScannerFragmentToAddFoodFragment(foodId!!, args.mealCategory)
                        navController.navigate(action)
                    }
                } else {
                    Log.e("BarcodeScanner", "Failed to get food ID")
                }
            } catch (e: Exception) {
                Log.e("BarcodeScanner", "Exception occurred", e)
            }finally {
                isProcessingBarcode = false // Ripristina lo stato
            }
        }

    }

}