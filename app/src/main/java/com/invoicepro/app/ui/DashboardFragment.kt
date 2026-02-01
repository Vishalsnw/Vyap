package com.invoicepro.app.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.invoicepro.app.data.AppDatabase
import com.invoicepro.app.databinding.FragmentDashboardBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

import androidx.navigation.fragment.findNavController
import com.invoicepro.app.R

class DashboardFragment : Fragment() {
    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        observeStats()
        setupClickListeners()
    }

    private fun setupClickListeners() {
        binding.buttonSale.setOnClickListener {
            findNavController().navigate(R.id.action_dashboardFragment_to_createInvoiceFragment)
        }
        
        binding.buttonPurchase.setOnClickListener {
            findNavController().navigate(R.id.productFragment)
        }
    }

    private fun observeStats() {
        lifecycleScope.launch {
            val db = AppDatabase.getDatabase(requireContext())
            
            // Observe Invoices for real-time sales updates
            db.invoiceDao().getAllInvoices().collectLatest { invoices ->
                val totalSales = invoices.sumOf { it.total }
                binding.textTotalSales.text = "₹%.2f".format(totalSales)
                
                // Real-time dynamic insights based on actual data
                val totalReceivables = 0.0 // To be implemented with payment status in Invoice model
                val totalPurchases = 0.0 // To be implemented with a Purchase model
                val totalExpenses = 0.0 // To be implemented with an Expense model
                
                binding.textReceivables.text = "₹%.2f".format(totalReceivables)
                // binding.textPurchases.text = "₹%.2f".format(totalPurchases)
                // binding.textExpenses.text = "₹%.2f".format(totalExpenses)

                // Simple Daily Sales Chart Logic
                try {
                    val salesByDate = invoices.groupBy { 
                        java.text.SimpleDateFormat("dd/MM", java.util.Locale.getDefault()).format(java.util.Date(it.date))
                    }.mapValues { entry -> entry.value.sumOf { it.total } }
                    
                    val sortedDates = salesByDate.keys.sorted()
                    if (sortedDates.isNotEmpty()) {
                        val lastDates = sortedDates.takeLast(5)
                        binding.textChartPlaceholder.text = "Recent Sales: " + lastDates.joinToString { date -> 
                            "$date: ₹${salesByDate[date]?.toLong() ?: 0}" 
                        }
                    } else {
                        binding.textChartPlaceholder.text = "No sales data available yet"
                    }
                } catch (e: Exception) {
                    binding.textChartPlaceholder.text = "Error loading chart"
                }
            }
        }

        lifecycleScope.launch {
            val db = AppDatabase.getDatabase(requireContext())
            // Observe Products for real-time stock value updates
            db.productDao().getAllProducts().collectLatest { products ->
                val stockValue = products.sumOf { it.sellingPrice * it.stockQuantity }
                binding.textStockValue.text = "₹%.2f".format(stockValue)
                
                val lowStockCount = products.count { it.stockQuantity <= it.minStockLevel }
                if (lowStockCount > 0) {
                    binding.textLowStockAlert.visibility = View.VISIBLE
                    binding.textLowStockAlert.text = "⚠️ $lowStockCount Items Low in Stock"
                } else {
                    binding.textLowStockAlert.visibility = View.GONE
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}