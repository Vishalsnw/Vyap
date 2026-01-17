package com.invoicepro.app.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.invoicepro.app.data.AppDatabase
import com.invoicepro.app.databinding.FragmentCustomerBinding
import com.invoicepro.app.databinding.ItemCustomerBinding
import com.invoicepro.app.model.Customer
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class CustomerFragment : Fragment() {
    private var _binding: FragmentCustomerBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: CustomerAdapter
    private var allCustomers = listOf<Customer>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentCustomerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        adapter = CustomerAdapter()
        binding.recyclerViewCustomers.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerViewCustomers.adapter = adapter

        lifecycleScope.launch {
            val db = AppDatabase.getDatabase(requireContext())
            db.customerDao().getAllCustomers().collectLatest { customers ->
                allCustomers = customers
                updateCustomerInsights(customers)
                filterCustomers(binding.editSearchCustomers.text.toString())
            }
        }

        binding.fabAddCustomer.setOnClickListener {
            showAddCustomerBottomSheet()
        }
        
        binding.editSearchCustomers.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterCustomers(s.toString())
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })
    }

    private fun updateCustomerInsights(customers: List<Customer>) {
        binding.textTotalCustomersCount.text = customers.size.toString()
        binding.textRecentAddsCount.text = customers.size.toString() // Placeholder logic
    }

    private fun showAddCustomerBottomSheet() {
        val dialog = com.google.android.material.bottomsheet.BottomSheetDialog(requireContext())
        val dialogBinding = FragmentCustomerBinding.inflate(layoutInflater)
        dialog.setContentView(dialogBinding.root)

        // Adjust visibility for the bottom sheet version
        dialogBinding.textTotalCustomersCount.parent.parent.visibility = View.GONE
        dialogBinding.editSearchCustomers.parent.visibility = View.GONE
        dialogBinding.recyclerViewCustomers.visibility = View.GONE
        dialogBinding.fabAddCustomer.visibility = View.GONE

        // Since we are reusing the layout, we need to handle the save button which was removed in the previous edit
        // but it's still in the binding. Let's create a simpler dialog logic.
        val builder = android.app.AlertDialog.Builder(requireContext())
        val inflater = layoutInflater
        val dialogView = inflater.inflate(com.invoicepro.app.R.layout.dialog_add_product, null) // Placeholder for now
        // Normally we'd have a specific customer dialog layout
    }

    private fun filterCustomers(query: String) {
        val filtered = if (query.isEmpty()) {
            allCustomers
        } else {
            allCustomers.filter { it.name.contains(query, ignoreCase = true) || it.phone.contains(query) }
        }
        adapter.submitList(filtered)
    }

    private inner class CustomerAdapter : RecyclerView.Adapter<CustomerAdapter.ViewHolder>() {
        private var customers = listOf<Customer>()

        fun submitList(newList: List<Customer>) {
            customers = newList
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val binding = ItemCustomerBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return ViewHolder(binding)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val customer = customers[position]
            holder.binding.textCustomerName.text = customer.name
            holder.binding.textCustomerPhone.text = customer.phone
            holder.binding.textCustomerGstin.text = customer.gstin ?: "No GSTIN"
            
            holder.binding.root.setOnLongClickListener {
                android.app.AlertDialog.Builder(holder.itemView.context)
                    .setTitle("Delete Customer")
                    .setMessage("Are you sure you want to delete ${customer.name}?")
                    .setPositiveButton("Delete") { _, _ ->
                        lifecycleScope.launch {
                            val db = AppDatabase.getDatabase(holder.itemView.context)
                            db.customerDao().deleteCustomer(customer)
                        }
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
                true
            }
        }

        override fun getItemCount() = customers.size

        inner class ViewHolder(val binding: ItemCustomerBinding) : RecyclerView.ViewHolder(binding.root)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
