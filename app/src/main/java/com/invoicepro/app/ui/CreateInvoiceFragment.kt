package com.invoicepro.app.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.invoicepro.app.data.AppDatabase
import com.invoicepro.app.databinding.FragmentCreateInvoiceBinding
import com.invoicepro.app.model.Customer
import com.invoicepro.app.model.Invoice
import com.invoicepro.app.model.Product
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Date

class CreateInvoiceFragment : Fragment() {
    private var _binding: FragmentCreateInvoiceBinding? = null
    private val binding get() = _binding!!
    
    private var selectedCustomer: Customer? = null
    private val selectedProducts = mutableListOf<Product>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentCreateInvoiceBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupSelectors()
        
        binding.btnGenerateInvoice.setOnClickListener {
            saveInvoice()
        }
    }

    private fun setupSelectors() {
        lifecycleScope.launch {
            val db = AppDatabase.getDatabase(requireContext())
            val customers = db.customerDao().getAllCustomers().first()
            val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, customers.map { it.name })
            binding.spinnerCustomer.adapter = adapter
            binding.spinnerCustomer.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    selectedCustomer = customers[position]
                }
                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }
        }
    }

    private fun saveInvoice() {
        val customer = selectedCustomer ?: return
        
        // Simplified calculation for demonstration
        val subtotal = selectedProducts.sumOf { it.price }
        val totalGst = selectedProducts.sumOf { it.price * it.gstPercentage / 100.0 }
        
        val invoice = Invoice(
            invoiceNumber = "INV-${System.currentTimeMillis()}",
            customerId = customer.id,
            date = Date().time,
            subtotal = subtotal,
            cgst = totalGst / 2,
            sgst = totalGst / 2,
            igst = 0.0,
            total = subtotal + totalGst
        )

        lifecycleScope.launch {
            val db = AppDatabase.getDatabase(requireContext())
            db.invoiceDao().insertInvoice(invoice)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
