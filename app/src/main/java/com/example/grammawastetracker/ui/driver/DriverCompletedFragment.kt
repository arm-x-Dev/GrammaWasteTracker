package com.example.grammawastetracker.ui.driver

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.grammawastetracker.R
import com.example.grammawastetracker.databinding.FragmentDriverReportsBinding

class DriverCompletedFragment : Fragment() {

    private var _binding: FragmentDriverReportsBinding? = null
    private val binding get() = _binding ?: throw IllegalStateException("Binding accessed after onDestroyView")
    
    private fun getBindingSafe(): FragmentDriverReportsBinding? = _binding
    
    private val viewModel: DriverDashboardViewModel by activityViewModels()
    private lateinit var adapter: ReportsAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentDriverReportsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupRecyclerView()
        
        // Hide shimmer for completed tab for simplicity
        getBindingSafe()?.shimmerViewContainer?.visibility = View.GONE
        
        viewModel.completedReports.observe(viewLifecycleOwner) { reports ->
            getBindingSafe()?.let { safeBinding ->
                if (reports.isEmpty()) {
                    safeBinding.emptyText.text = "No completed tasks yet"
                    safeBinding.emptyText.visibility = View.VISIBLE
                    safeBinding.recyclerView.visibility = View.GONE
                } else {
                    safeBinding.emptyText.visibility = View.GONE
                    safeBinding.recyclerView.visibility = View.VISIBLE
                    adapter.submitList(reports)
                }
            }
        }
    }

    private fun setupRecyclerView() {
        adapter = ReportsAdapter { report ->
            // Clicking a completed report shows details (and the proof if we want)
            val fragment = ReportDetailFragment.newInstance(report.id, report.imageUrl, report.lat, report.lng, report.status, report.proofImageUrl)
            requireActivity().supportFragmentManager.beginTransaction()
                .replace(R.id.nav_host_fragment, fragment)
                .addToBackStack(null)
                .commit()
        }
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
