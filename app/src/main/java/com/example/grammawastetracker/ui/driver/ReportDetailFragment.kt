package com.example.grammawastetracker.ui.driver

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.bumptech.glide.Glide
import com.example.grammawastetracker.R
import com.example.grammawastetracker.databinding.FragmentReportDetailBinding
import com.example.grammawastetracker.utils.setup
import com.example.grammawastetracker.utils.showToast
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.Marker

class ReportDetailFragment : Fragment() {

    private var _binding: FragmentReportDetailBinding? = null
    private val binding get() = _binding ?: throw IllegalStateException("Binding accessed after onDestroyView")
    
    private fun getBindingSafe(): FragmentReportDetailBinding? = _binding
    
    private val viewModel: DriverDashboardViewModel by activityViewModels()

    private var reportId: String? = null
    private var imageUrl: String? = null
    private var lat: Double = 0.0
    private var lng: Double = 0.0

    companion object {
        fun newInstance(reportId: String, imageUrl: String, lat: Double, lng: Double, status: String, proofImageUrl: String? = null) = ReportDetailFragment().apply {
            arguments = Bundle().apply {
                putString("reportId", reportId)
                putString("imageUrl", imageUrl)
                putDouble("lat", lat)
                putDouble("lng", lng)
                putString("status", status)
                putString("proofImageUrl", proofImageUrl)
            }
        }
    }

    private var status: String? = null
    private var proofImageUrl: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            reportId = it.getString("reportId")
            imageUrl = it.getString("imageUrl")
            lat = it.getDouble("lat")
            lng = it.getDouble("lng")
            status = it.getString("status")
            proofImageUrl = it.getString("proofImageUrl")
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentReportDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    private var proofImageUri: Uri? = null

    private val takePhotoLauncher = registerForActivityResult(androidx.activity.result.contract.ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            binding.proofPreviewCard.visibility = View.VISIBLE
            binding.proofPreviewImage.setImageURI(proofImageUri)
            binding.collectButton.text = "Confirm Collection"
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Reset state so we don't immediately pop backstack if coming from a previous success
        viewModel.resetCollectionResult()
        
        binding.detailMapView.setup(requireContext())
        
        imageUrl?.let {
            Glide.with(this)
                .load(it)
                .placeholder(R.drawable.ic_trash_marker)
                .error(R.drawable.ic_trash_marker)
                .into(binding.reportImageFull)
        }

        // Handle COMPLETED state
        if (status == "COLLECTED") {
            binding.navigateButton.visibility = View.GONE
            binding.collectButton.visibility = View.GONE
            
            // Show proof image if we have it
            proofImageUrl?.let {
                binding.proofPreviewCard.visibility = View.VISIBLE
                Glide.with(this).load(it).into(binding.proofPreviewImage)
            }
        }

        val reportPoint = GeoPoint(lat, lng)
        val marker = Marker(binding.detailMapView).apply {
            position = reportPoint
            icon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_trash_marker)
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            title = "Garbage Location"
        }
        binding.detailMapView.overlays.add(marker)
        binding.detailMapView.controller.setCenter(reportPoint)

        binding.navigateButton.setOnClickListener {
            // Automatically accept report when driver starts navigating
            reportId?.let { id ->
                viewModel.acceptReport(id)
            }
            
            val uri = "geo:$lat,$lng?q=$lat,$lng(Garbage)"
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uri))
            startActivity(intent)
        }

        binding.collectButton.setOnClickListener {
            if (proofImageUri == null) {
                // Check camera permission first
                if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                    requestPermissions(arrayOf(Manifest.permission.CAMERA), 101)
                    return@setOnClickListener
                }

                // First step: Capture proof
                try {
                    val photoFile = java.io.File(requireContext().filesDir, "proof_${System.currentTimeMillis()}.jpg")
                    proofImageUri = androidx.core.content.FileProvider.getUriForFile(
                        requireContext(),
                        "com.example.grammawastetracker.provider",
                        photoFile
                    )
                    takePhotoLauncher.launch(proofImageUri)
                } catch (e: Exception) {
                    requireContext().showToast("Camera error: ${e.message}")
                    e.printStackTrace()
                }
            } else {
                // Second step: Confirm and Upload
                reportId?.let { id ->
                    binding.collectButton.isEnabled = false
                    binding.collectButton.text = "Uploading..."
                    viewModel.markAsCollected(id, proofImageUri)
                }
            }
        }

        viewModel.collectionResult.observe(viewLifecycleOwner) { success ->
            if (success) {
                requireContext().showToast("Collection complete! Status updated.")
                requireActivity().supportFragmentManager.popBackStack()
            } else {
                if (binding.collectButton.text == "Uploading...") {
                    binding.collectButton.isEnabled = true
                    binding.collectButton.text = "Confirm Collection"
                    requireContext().showToast("Failed to update status")
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        binding.detailMapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        binding.detailMapView.onPause()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.detailMapView.onDetach()
        _binding = null
    }
}
