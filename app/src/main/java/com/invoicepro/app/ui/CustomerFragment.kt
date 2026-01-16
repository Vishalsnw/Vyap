package com.invoicepro.app.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.invoicepro.app.data.AppDatabase
import com.invoicepro.app.databinding.FragmentCustomerBinding
import com.invoicepro.app.model.Customer
import kotlinx.coroutines.launch

class CustomerFragment : Fragment() {
    private var _binding: FragmentCustomerBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentCustomerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        binding.btnSave.setOnClickListener {
            val name = binding.editName.text.toString()
            val phone = binding.editPhone.text.toString()
            val address = binding.editAddress.text.toString()
            val gstin = binding.editGstin.text.toString()

            if (name.isNotEmpty() && phone.isNotEmpty()) {
                val customer = Customer(name = name, phone = phone, address = address, gstin = gstin.ifEmpty { null })
                lifecycleScope.launch {
                    val db = AppDatabase.getDatabase(requireContext())
                    db.customerDao().insertCustomer(customer)
                    
                    // Clear inputs and show success
                    binding.editName.setText("")
                    binding.editPhone.setText("")
                    binding.editAddress.setText("")
                    binding.editGstin.setText("")
                    android.widget.Toast.makeText(requireContext(), "Customer Saved!", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
