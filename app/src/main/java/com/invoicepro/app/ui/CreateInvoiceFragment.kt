package com.invoicepro.app.ui

import android.app.Dialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.invoicepro.app.R
import com.invoicepro.app.data.AppDatabase
import com.invoicepro.app.databinding.DialogAddProductBinding
import com.invoicepro.app.databinding.FragmentCreateInvoiceBinding
import com.invoicepro.app.databinding.ItemInvoiceProductBinding
import com.invoicepro.app.model.Customer
import com.invoicepro.app.model.Invoice
import com.invoicepro.app.model.InvoiceItem
import com.invoicepro.app.model.Product
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Date

import com.invoicepro.app.util.PreferenceManager

import androidx.core.content.FileProvider
import android.content.Intent
import com.invoicepro.app.util.PdfGenerator
import com.invoicepro.app.model.BusinessProfile

class CreateInvoiceFragment : Fragment() {
    private var _binding: FragmentCreateInvoiceBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var preferenceManager: PreferenceManager
    private var selectedCustomer: Customer? = null
    private val selectedItems = mutableListOf<InvoiceItem>()
    private lateinit var itemAdapter: SelectedItemAdapter
    private var lastGeneratedInvoice: Invoice? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentCreateInvoiceBinding.inflate(inflater, container, false)
        preferenceManager = PreferenceManager(requireContext())
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupCustomerSelector()
        setupRecyclerView()
        
        binding.btnAddItem.setOnClickListener {
            showProductSelectionDialog()
        }
        
        binding.btnGenerateInvoice.setOnClickListener {
            saveInvoice()
        }

        binding.btnDownloadPdf.setOnClickListener {
            shareInvoice(false)
        }

        binding.btnShareWhatsapp.setOnClickListener {
            shareInvoice(true)
        }
    }

    private fun shareInvoice(isWhatsapp: Boolean) {
        val customer = selectedCustomer ?: run {
            Toast.makeText(requireContext(), "Please select a customer first", Toast.LENGTH_SHORT).show()
            return
        }
        if (selectedItems.isEmpty()) {
            Toast.makeText(requireContext(), "Please add at least one item", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            try {
                val db = AppDatabase.getDatabase(requireContext())
                val business = db.businessProfileDao().getProfile() ?: BusinessProfile(
                    name = "My Business",
                    address = "Business Address",
                    phone = "1234567890",
                    gstin = ""
                )

                val invoice = lastGeneratedInvoice ?: Invoice(
                    invoiceNumber = "DRAFT-${System.currentTimeMillis()}",
                    customerId = customer.id,
                    date = System.currentTimeMillis(),
                    subtotal = selectedItems.sumOf { it.rate * it.quantity },
                    cgst = 0.0,
                    sgst = 0.0,
                    igst = 0.0,
                    total = selectedItems.sumOf { it.amount }
                )

                val pdfGenerator = PdfGenerator(requireContext())
                val file = pdfGenerator.generateInvoicePdf(business, customer, invoice, selectedItems)

                if (file != null && file.exists()) {
                    val uri = FileProvider.getUriForFile(requireContext(), "${requireContext().packageName}.provider", file)
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "application/pdf"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        if (isWhatsapp) {
                            setPackage("com.whatsapp")
                        }
                    }
                    startActivity(Intent.createChooser(intent, "Share Invoice"))
                } else {
                    Toast.makeText(requireContext(), "Failed to generate PDF", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupRecyclerView() {
        itemAdapter = SelectedItemAdapter(selectedItems) {
            updateUI()
        }
        binding.recyclerItems.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerItems.adapter = itemAdapter
    }

    private fun showProductSelectionDialog() {
        lifecycleScope.launch {
            val db = AppDatabase.getDatabase(requireContext())
            val products = db.productDao().getAllProducts().first()
            
            if (products.isEmpty()) {
                Toast.makeText(requireContext(), "Please add products first", Toast.LENGTH_SHORT).show()
                return@launch
            }

            val dialogBinding = DialogAddProductBinding.inflate(layoutInflater)
            val dialog = Dialog(requireContext())
            dialog.setContentView(dialogBinding.root)
            
            val productNames = products.map { "${it.name} (₹${it.sellingPrice})" }
            val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, productNames)
            dialogBinding.spinnerProductSelect.adapter = adapter
            
            var selectedProduct: Product = products[0]
            dialogBinding.spinnerProductSelect.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    selectedProduct = products[position]
                    dialogBinding.editItemRate.setText(selectedProduct.sellingPrice.toString())
                }
                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }

            dialogBinding.btnConfirmAdd.setOnClickListener {
                try {
                    val qtyStr = dialogBinding.editItemQty.text.toString()
                    val rateStr = dialogBinding.editItemRate.text.toString()
                    
                    val qty = qtyStr.toDoubleOrNull() ?: 1.0
                    val rate = rateStr.toDoubleOrNull() ?: selectedProduct.sellingPrice
                    
                    val amount = qty * rate * (1 + selectedProduct.gstPercentage / 100.0)
                    
                    val item = InvoiceItem(
                        invoiceId = 0,
                        productId = selectedProduct.id,
                        productName = selectedProduct.name,
                        quantity = qty,
                        rate = rate,
                        gstPercentage = selectedProduct.gstPercentage,
                        amount = amount
                    )
                    
                    selectedItems.add(item)
                    itemAdapter.notifyItemInserted(selectedItems.size - 1)
                    updateUI()
                    dialog.dismiss()
                } catch (e: Exception) {
                    Toast.makeText(requireContext(), "Error adding item: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }

            dialog.window?.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT)
            dialog.show()
        }
    }

    private fun updateUI() {
        val total = selectedItems.sumOf { it.amount }
        binding.textTotalAmount.text = "₹%.2f".format(total)
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
        try {
            if (!preferenceManager.isProVersion() && preferenceManager.getInvoiceCount() >= 5) {
                Toast.makeText(requireContext(), "Free limit reached (5 invoices). Upgrade to Pro for unlimited!", Toast.LENGTH_LONG).show()
                return
            }

            val customer = selectedCustomer ?: run {
                Toast.makeText(requireContext(), "Please select a customer first", Toast.LENGTH_LONG).show()
                return
            }
            
            if (selectedItems.isEmpty()) {
                Toast.makeText(requireContext(), "Please add at least one item to the invoice", Toast.LENGTH_LONG).show()
                return
            }
            
            val subtotal = selectedItems.sumOf { it.rate * it.quantity }
            val total = selectedItems.sumOf { it.amount }
            val totalGst = total - subtotal
            
            val invoice = Invoice(
                invoiceNumber = "INV-${System.currentTimeMillis()}",
                customerId = customer.id,
                date = System.currentTimeMillis(),
                subtotal = subtotal,
                cgst = totalGst / 2,
                sgst = totalGst / 2,
                igst = 0.0,
                total = total
            )

            lifecycleScope.launch {
                try {
                    val db = AppDatabase.getDatabase(requireContext())
                    val invoiceId = db.invoiceDao().insertInvoice(invoice)
                    
                    val finalItems = selectedItems.map { it.copy(invoiceId = invoiceId) }
                    if (finalItems.isNotEmpty()) {
                        db.invoiceDao().insertInvoiceItems(finalItems)
                        
                        finalItems.forEach { item ->
                            db.productDao().reduceStock(item.productId, item.quantity)
                        }
                    }
                    
                    lastGeneratedInvoice = invoice.copy(id = invoiceId)
                    preferenceManager.incrementInvoiceCount()
                    
                    Toast.makeText(requireContext(), "Invoice ${invoice.invoiceNumber} saved successfully!", Toast.LENGTH_SHORT).show()
                    selectedItems.clear()
                    itemAdapter.notifyDataSetChanged()
                    updateUI()
                    
                    // Navigate back to history or clear view
                    parentFragmentManager.popBackStack()
                } catch (e: Exception) {
                    Toast.makeText(requireContext(), "Error saving invoice: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Critical Error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    class SelectedItemAdapter(private val items: MutableList<InvoiceItem>, private val onUpdate: () -> Unit) : 
        RecyclerView.Adapter<SelectedItemAdapter.ViewHolder>() {
        
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val binding = ItemInvoiceProductBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return ViewHolder(binding)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]
            holder.binding.textItemName.text = item.productName
            holder.binding.textItemDetails.text = "${item.quantity} x ₹${item.rate} (+${item.gstPercentage}% GST)"
            holder.binding.textItemTotal.text = "₹%.2f".format(item.amount)
            holder.binding.btnRemoveItem.setOnClickListener {
                items.removeAt(holder.adapterPosition)
                notifyItemRemoved(holder.adapterPosition)
                onUpdate()
            }
        }

        override fun getItemCount() = items.size

        class ViewHolder(val binding: ItemInvoiceProductBinding) : RecyclerView.ViewHolder(binding.root)
    }
}