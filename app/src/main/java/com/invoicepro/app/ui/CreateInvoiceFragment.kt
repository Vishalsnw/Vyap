package com.invoicepro.app.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.invoicepro.app.R
import com.invoicepro.app.data.AppDatabase
import com.invoicepro.app.databinding.FragmentCreateInvoiceBinding
import com.invoicepro.app.model.Customer
import com.invoicepro.app.model.Invoice
import com.invoicepro.app.model.InvoiceItem
import com.invoicepro.app.model.Product
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Date

class CreateInvoiceFragment : Fragment() {
    private var _binding: FragmentCreateInvoiceBinding? = null
    private val binding get() = _binding!!
    
    private var selectedCustomer: Customer? = null
    private val selectedItems = mutableListOf<InvoiceItem>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentCreateInvoiceBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupCustomerSelector()
        
        binding.btnAddItem.setOnClickListener {
            // In a real app, this would open a dialog to pick a product
            // For now, we'll pick a random saved product for demonstration
            lifecycleScope.launch {
                val db = AppDatabase.getDatabase(requireContext())
                val products = db.productDao().getAllProducts().first()
                if (products.isNotEmpty()) {
                    val product = products.random()
                    val item = InvoiceItem(
                        invoiceId = 0,
                        productId = product.id,
                        productName = product.name,
                        quantity = 1.0,
                        rate = product.price,
                        gstPercentage = product.gstPercentage,
                        amount = product.price * (1 + product.gstPercentage / 100.0)
                    )
                    selectedItems.add(item)
                    updateUI()
                } else {
                    Toast.makeText(requireContext(), "Add products first!", Toast.LENGTH_SHORT).show()
                }
            }
        }
        
        binding.btnGenerateInvoice.setOnClickListener {
            saveInvoice()
        }
    }

    private fun updateUI() {
        val total = selectedItems.sumOf { it.amount }
        binding.textTotalAmount.text = "₹%.2f".format(total)
        // In a full implementation, you'd show the list in the RecyclerView
    }

    private fun setupCustomerSelector() {
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
        val customer = selectedCustomer ?: run {
            Toast.makeText(requireContext(), "Select a customer", Toast.LENGTH_SHORT).show()
            return
        }
        
        if (selectedItems.isEmpty()) {
            Toast.makeText(requireContext(), "Add at least one item", Toast.LENGTH_SHORT).show()
            return
        }
        
        val subtotal = selectedItems.sumOf { it.rate * it.quantity }
        val total = selectedItems.sumOf { it.amount }
        val totalGst = total - subtotal
        
        val invoice = Invoice(
            invoiceNumber = "INV-${System.currentTimeMillis()}",
            customerId = customer.id,
            date = Date().time,
            subtotal = subtotal,
            cgst = totalGst / 2,
            sgst = totalGst / 2,
            igst = 0.0,
            total = total
        )

        lifecycleScope.launch {
            val db = AppDatabase.getDatabase(requireContext())
            val invoiceId = db.invoiceDao().insertInvoice(invoice)
            
            val finalItems = selectedItems.map { it.copy(invoiceId = invoiceId) }
            db.invoiceDao().insertInvoiceItems(finalItems)
            
            Toast.makeText(requireContext(), "Invoice Saved Successfully!", Toast.LENGTH_SHORT).show()
            selectedItems.clear()
            updateUI()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}