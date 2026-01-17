package com.invoicepro.app.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.invoicepro.app.data.AppDatabase
import com.invoicepro.app.databinding.FragmentInvoiceHistoryBinding
import com.invoicepro.app.databinding.ItemInvoiceBinding
import com.invoicepro.app.model.Invoice
import com.invoicepro.app.model.BusinessProfile
import com.invoicepro.app.util.PdfGenerator
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class InvoiceHistoryFragment : Fragment() {
    private var _binding: FragmentInvoiceHistoryBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: InvoiceAdapter
    private var allInvoices = listOf<Invoice>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentInvoiceHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        adapter = InvoiceAdapter()
        binding.recyclerViewInvoices.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerViewInvoices.adapter = adapter

        binding.buttonExportCsv.setOnClickListener {
            exportToCsv()
        }

        lifecycleScope.launch {
            val db = AppDatabase.getDatabase(requireContext())
            db.invoiceDao().getAllInvoices().collectLatest { invoices ->
                allInvoices = invoices
                updateHistorySummary(invoices)
                filterInvoices(binding.editSearchInvoices.text.toString())
            }
        }

        binding.editSearchInvoices.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterInvoices(s.toString())
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun updateHistorySummary(invoices: List<Invoice>) {
        binding.textTotalInvoicesCount.text = invoices.size.toString()
        val totalRevenue = invoices.sumOf { it.total }
        binding.textTotalRevenue.text = "₹%.2f".format(totalRevenue)
    }

    private fun exportToCsv() {
        lifecycleScope.launch {
            try {
                val db = AppDatabase.getDatabase(requireContext())
                val invoices = db.invoiceDao().getAllInvoices().first()
                if (invoices.isEmpty()) {
                    Toast.makeText(requireContext(), "No invoices to export", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                val csvBuilder = StringBuilder("Invoice Number,Date,Customer,Subtotal,GST,Total\n")
                val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                
                val customers = db.customerDao().getAllCustomers().first()
                
                invoices.forEach { invoice ->
                    val customer = customers.find { it.id == invoice.customerId }
                    val totalGst = invoice.cgst + invoice.sgst + invoice.igst
                    csvBuilder.append("${invoice.invoiceNumber},${dateFormat.format(Date(invoice.date))},${customer?.name ?: "Unknown"},${invoice.subtotal},${totalGst},${invoice.total}\n")
                }
                
                val fileName = "InvoicePro_Export_${System.currentTimeMillis()}.csv"
                val file = File(requireContext().getExternalFilesDir(null), fileName)
                file.writeText(csvBuilder.toString())
                
                val uri = FileProvider.getUriForFile(requireContext(), "${requireContext().packageName}.provider", file)
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/csv"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                startActivity(Intent.createChooser(intent, "Export Invoices CSV"))
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Export failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun filterInvoices(query: String) {
        val filtered = if (query.isEmpty()) {
            allInvoices
        } else {
            allInvoices.filter { it.invoiceNumber.contains(query, ignoreCase = true) }
        }
        adapter.submitList(filtered)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    class InvoiceAdapter : RecyclerView.Adapter<InvoiceAdapter.ViewHolder>() {
        private var invoices = listOf<Invoice>()
        private val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

        fun submitList(newList: List<Invoice>) {
            invoices = newList
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val binding = ItemInvoiceBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return ViewHolder(binding)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val invoice = invoices[position]
            holder.binding.textInvoiceNum.text = invoice.invoiceNumber
            holder.binding.textInvoiceTotal.text = "₹%.2f".format(invoice.total)
            holder.binding.textInvoiceDate.text = dateFormat.format(invoice.date)
            
            holder.binding.root.setOnClickListener {
                val context = holder.itemView.context
                (context as? androidx.appcompat.app.AppCompatActivity)?.let { activity ->
                    (context as? androidx.lifecycle.LifecycleOwner)?.lifecycleScope?.launch {
                        val db = AppDatabase.getDatabase(activity)
                        val business = db.businessProfileDao().getProfile() ?: BusinessProfile(name = "Business Name", address = "", phone = "", gstin = "")
                        val customer = db.customerDao().getAllCustomers().first().find { it.id == invoice.customerId } ?: return@launch
                        val items = db.invoiceDao().getItemsForInvoice(invoice.id)
                        
                        val pdfGenerator = PdfGenerator(activity)
                        val file = pdfGenerator.generateInvoicePdf(business, customer, invoice, items)
                        
                        file?.let {
                            val uri = androidx.core.content.FileProvider.getUriForFile(activity, "${activity.packageName}.provider", it)
                            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                                setDataAndType(uri, "application/pdf")
                                addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            activity.startActivity(android.content.Intent.createChooser(intent, "Open Invoice"))
                        }
                    }
                }
            }
        }

        override fun getItemCount() = invoices.size

        class ViewHolder(val binding: ItemInvoiceBinding) : RecyclerView.ViewHolder(binding.root)
    }
}