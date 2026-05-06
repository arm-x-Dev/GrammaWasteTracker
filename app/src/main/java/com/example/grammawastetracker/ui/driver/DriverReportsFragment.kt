package com.example.grammawastetracker.ui.driver

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.grammawastetracker.R
import com.example.grammawastetracker.data.model.GarbageReport
import com.example.grammawastetracker.databinding.FragmentDriverReportsBinding
import com.example.grammawastetracker.databinding.ItemGarbageReportBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DriverReportsFragment : Fragment() {

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
        
        viewModel.pendingReports.observe(viewLifecycleOwner) { reports ->
            getBindingSafe()?.let { safeBinding ->
                safeBinding.shimmerViewContainer.stopShimmer()
                safeBinding.shimmerViewContainer.visibility = View.GONE
                
                if (reports.isEmpty()) {
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

class ReportsAdapter(private val onItemClick: (GarbageReport) -> Unit) : RecyclerView.Adapter<ReportsAdapter.ReportViewHolder>() {

    private var items: List<GarbageReport> = emptyList()

    fun submitList(newItems: List<GarbageReport>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReportViewHolder {
        val binding = ItemGarbageReportBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ReportViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ReportViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount() = items.size

    inner class ReportViewHolder(private val binding: ItemGarbageReportBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(report: GarbageReport) {
            val displayImage = if (report.status == "COLLECTED" && report.proofImageUrl.isNotEmpty()) {
                report.proofImageUrl
            } else {
                report.imageUrl
            }

            Glide.with(binding.root)
                .load(displayImage)
                .placeholder(com.example.grammawastetracker.R.drawable.ic_trash_marker)
                .error(com.example.grammawastetracker.R.drawable.ic_trash_marker)
                .into(binding.reportImage)
                
            val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
            binding.timeText.text = sdf.format(Date(report.timestamp))
            
            // Handle Status Chip
            if (report.status == "PENDING") {
                binding.statusChip.text = binding.root.context.getString(R.string.status_pending)
                binding.statusChip.setChipBackgroundColorResource(R.color.status_pending)
            } else {
                binding.statusChip.text = binding.root.context.getString(R.string.status_collected)
                binding.statusChip.setChipBackgroundColorResource(R.color.status_collected)
            }

            binding.distanceText.text = binding.root.context.getString(R.string.report_details) 
            binding.coordinatesCardText.text = String.format("Lat: %.4f, Lng: %.4f", report.lat, report.lng)
            
            binding.root.setOnClickListener { onItemClick(report) }
            binding.viewDetailsButton.setOnClickListener { onItemClick(report) }
        }
    }
}
