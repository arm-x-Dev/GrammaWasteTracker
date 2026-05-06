package com.example.grammawastetracker.ui.resident

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.example.grammawastetracker.R
import com.example.grammawastetracker.databinding.FragmentResidentReportDetailBinding
import com.example.grammawastetracker.utils.showToast

class ResidentReportDetailFragment : Fragment() {

    private var _binding: FragmentResidentReportDetailBinding? = null
    private val binding get() = _binding ?: throw IllegalStateException("Binding accessed after onDestroyView")
    
    private fun getBindingSafe(): FragmentResidentReportDetailBinding? = _binding

    private var imageUrl: String? = null
    private var lat: Double = 0.0
    private var lng: Double = 0.0

    companion object {
        fun newInstance(imageUrl: String, lat: Double, lng: Double, status: String, proofImageUrl: String? = null) = ResidentReportDetailFragment().apply {
            arguments = Bundle().apply {
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
            imageUrl = it.getString("imageUrl")
            lat = it.getDouble("lat")
            lng = it.getDouble("lng")
            status = it.getString("status")
            proofImageUrl = it.getString("proofImageUrl")
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentResidentReportDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.coordinatesText.text = String.format("Lat: %.5f, Lng: %.5f", lat, lng)

        imageUrl?.let {
            Glide.with(this)
                .load(it)
                .placeholder(R.drawable.ic_trash_marker)
                .error(R.drawable.ic_trash_marker)
                .into(binding.reportImageFull)
        }

        // Handle COMPLETED state
        if (status == "COLLECTED") {
            // Show proof image if available
            proofImageUrl?.let {
                binding.proofPreviewCard.visibility = View.VISIBLE
                Glide.with(this).load(it).into(binding.proofPreviewImage)
            }
        }

        binding.openInMapsButton.setOnClickListener {
            try {
                val uri = "geo:$lat,$lng?q=$lat,$lng(Reported Garbage)"
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uri))
                intent.setPackage("com.google.android.apps.maps")
                
                // Try to open Google Maps specifically first
                if (intent.resolveActivity(requireActivity().packageManager) != null) {
                    startActivity(intent)
                } else {
                    // Fallback to any app that handles geo (like a browser or other map)
                    val fallbackIntent = Intent(Intent.ACTION_VIEW, Uri.parse(uri))
                    startActivity(fallbackIntent)
                }
            } catch (e: Exception) {
                requireContext().showToast("Could not open Maps: ${e.message}")
            }
        }

        binding.backButton.setOnClickListener {
            requireActivity().supportFragmentManager.popBackStack()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
