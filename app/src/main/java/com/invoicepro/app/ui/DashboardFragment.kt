package com.invoicepro.app.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.invoicepro.app.data.AppDatabase
import com.invoicepro.app.databinding.FragmentDashboardBinding
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class DashboardFragment : Fragment() {
    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadStats()
    }

    private fun loadStats() {
        lifecycleScope.launch {
            val db = AppDatabase.getDatabase(requireContext())
            val invoices = db.invoiceDao().getAllInvoices().first()
            val totalSales = invoices.sumOf { it.total }
            
            binding.textTotalSales.text = "₹%.2f".format(totalSales)
            binding.textReceivables.text = "₹%.2f".format(totalSales * 0.15) // Mock for now
            binding.textStockValue.text = "₹0.00"
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}