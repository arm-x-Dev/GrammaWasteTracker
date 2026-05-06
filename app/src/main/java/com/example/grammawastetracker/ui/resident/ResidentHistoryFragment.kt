package com.example.grammawastetracker.ui.resident

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.grammawastetracker.databinding.FragmentResidentHistoryBinding
import com.example.grammawastetracker.ui.driver.ReportsAdapter

class ResidentHistoryFragment : Fragment() {

    private var _binding: FragmentResidentHistoryBinding? = null
    private val binding get() = _binding ?: throw IllegalStateException("Binding accessed after onDestroyView")
    
    private fun getBindingSafe(): FragmentResidentHistoryBinding? = _binding
    
    private val viewModel: ResidentDashboardViewModel by activityViewModels()
    private lateinit var adapter: ReportsAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentResidentHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupRecyclerView()
        
        viewModel.residentHistory.observe(viewLifecycleOwner) { reports ->
            if (reports.isEmpty()) {
                binding.emptyText.visibility = View.VISIBLE
                binding.recyclerView.visibility = View.GONE
            } else {
                binding.emptyText.visibility = View.GONE
                binding.recyclerView.visibility = View.VISIBLE
                adapter.submitList(reports)
            }
        }
    }

    private fun setupRecyclerView() {
        adapter = ReportsAdapter { report ->
            if (isAdded) {
                val fragment = ResidentReportDetailFragment.newInstance(report.imageUrl, report.lat, report.lng, report.status, report.proofImageUrl)
                parentFragmentManager.beginTransaction()
                    .replace(com.example.grammawastetracker.R.id.nav_host_fragment, fragment)
                    .addToBackStack(null)
                    .commit()
            }
        }
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
