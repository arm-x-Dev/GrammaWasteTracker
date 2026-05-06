package com.example.grammawastetracker.ui.resident

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.grammawastetracker.databinding.FragmentUploadReportBinding
import com.example.grammawastetracker.utils.PermissionHelper
import com.example.grammawastetracker.utils.showToast
import com.google.android.gms.location.LocationServices
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class UploadReportFragment : Fragment() {

    private var _binding: FragmentUploadReportBinding? = null
    private val binding get() = _binding ?: throw IllegalStateException("Binding accessed after onDestroyView")
    
    private fun getBindingSafe(): FragmentUploadReportBinding? = _binding
    
    private val viewModel: ResidentDashboardViewModel by activityViewModels()
    private var imageCapture: ImageCapture? = null
    private lateinit var cameraExecutor: ExecutorService
    
    private var photoUri: Uri? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentUploadReportBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (PermissionHelper.hasCameraPermission(requireContext()) && PermissionHelper.hasLocationPermissions(requireContext())) {
            startCamera()
        } else {
            requestPermissions(
                arrayOf(Manifest.permission.CAMERA, Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                10
            )
        }

        binding.captureButton.setOnClickListener { takePhoto() }
        binding.submitButton.setOnClickListener { submitPhoto() }

        cameraExecutor = Executors.newSingleThreadExecutor()
        
        viewModel.uploadResult.observe(viewLifecycleOwner) { success ->
            if (success == true) {
                binding.uploadProgress.visibility = View.GONE
                requireContext().showToast("Report submitted!")
                binding.submitButton.visibility = View.GONE
                binding.captureButton.visibility = View.VISIBLE
                photoUri = null
                
                // Reset result so it doesn't fire again when coming back to this tab
                viewModel.resetUploadResult()
                
                val activity = activity
                if (activity is ResidentDashboardActivity && !activity.isFinishing) {
                    activity.switchToHistoryTab()
                }
            } else if (success == false) {
                // We only handle true/false, null is ignored or reset
                if (binding.uploadProgress.visibility == View.VISIBLE) {
                    binding.uploadProgress.visibility = View.GONE
                    binding.submitButton.visibility = View.VISIBLE
                    // requirement says "failed" but we only show if it was actually trying
                }
            }
        }
    }

    private fun startCamera() {
        val context = context ?: return
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        
        cameraProviderFuture.addListener({
            if (_binding == null) return@addListener
            
            try {
                val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(getBindingSafe()?.viewFinder?.surfaceProvider ?: return@addListener)
                }
                imageCapture = ImageCapture.Builder().build()
                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(viewLifecycleOwner, cameraSelector, preview, imageCapture)
            } catch(exc: Exception) {
                exc.printStackTrace()
            }
        }, ContextCompat.getMainExecutor(context))
    }

    private fun takePhoto() {
        val imageCapture = imageCapture ?: return
        val photoFile = File(requireContext().externalMediaDirs.firstOrNull(), SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.US).format(System.currentTimeMillis()) + ".jpg")
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        imageCapture.takePicture(
            outputOptions, ContextCompat.getMainExecutor(requireContext()), object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    requireContext().showToast("Photo capture failed: ${exc.message}")
                }
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    photoUri = Uri.fromFile(photoFile)
                    getBindingSafe()?.let { safeBinding ->
                        safeBinding.captureButton.visibility = View.GONE
                        safeBinding.submitButton.visibility = View.VISIBLE
                    }
                    requireContext().showToast("Photo captured successfully")
                }
            })
    }

    private fun submitPhoto() {
        val uri = photoUri ?: return
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
        
        binding.submitButton.visibility = View.INVISIBLE
        binding.uploadProgress.visibility = View.VISIBLE
        
        try {
            // Start a safety timer in case the location request hangs
            val locationTimeoutHandler = android.os.Handler(android.os.Looper.getMainLooper())
            val timeoutRunnable = Runnable {
                if (binding.uploadProgress.visibility == View.VISIBLE) {
                    binding.submitButton.visibility = View.VISIBLE
                    binding.uploadProgress.visibility = View.GONE
                    requireContext().showToast("Location request timed out. Try again or check GPS.")
                }
            }
            locationTimeoutHandler.postDelayed(timeoutRunnable, 8000) // 8 second timeout

            fusedLocationClient.getCurrentLocation(com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener { location ->
                    locationTimeoutHandler.removeCallbacks(timeoutRunnable)
                    if (location != null) {
                        viewModel.submitReport(uri, location.latitude, location.longitude)
                    } else {
                        fusedLocationClient.lastLocation.addOnSuccessListener { lastLoc ->
                            if (lastLoc != null) {
                                viewModel.submitReport(uri, lastLoc.latitude, lastLoc.longitude)
                            } else {
                                binding.submitButton.visibility = View.VISIBLE
                                binding.uploadProgress.visibility = View.GONE
                                requireContext().showToast("Could not get location. Ensure GPS is on.")
                            }
                        }
                    }
                }
                .addOnFailureListener { e ->
                    locationTimeoutHandler.removeCallbacks(timeoutRunnable)
                    binding.submitButton.visibility = View.VISIBLE
                    binding.uploadProgress.visibility = View.GONE
                    requireContext().showToast("Location error: ${e.message}")
                }
        } catch (e: SecurityException) {
            binding.submitButton.visibility = View.VISIBLE
            binding.uploadProgress.visibility = View.GONE
            requireContext().showToast("Location permission missing")
            e.printStackTrace()
        } catch (e: Exception) {
            binding.submitButton.visibility = View.VISIBLE
            binding.uploadProgress.visibility = View.GONE
            requireContext().showToast("Error: ${e.message}")
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        cameraExecutor.shutdown()
        _binding = null
    }
}
