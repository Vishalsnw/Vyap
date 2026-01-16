package com.invoicepro.app.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.invoicepro.app.data.AppDatabase
import com.invoicepro.app.databinding.FragmentProductBinding
import com.invoicepro.app.model.Product
import kotlinx.coroutines.launch

class ProductFragment : Fragment() {
    private var _binding: FragmentProductBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentProductBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        binding.btnSaveProduct.setOnClickListener {
            val name = binding.editProductName.text.toString()
            val priceStr = binding.editProductPrice.text.toString()
            val stockStr = binding.editProductStock.text.toString()
            val unit = binding.editProductUnit.text.toString()
            val gstStr = binding.spinnerGst.selectedItem.toString().replace("%", "")

            if (name.isNotEmpty() && priceStr.isNotEmpty()) {
                val product = Product(
                    name = name,
                    price = priceStr.toDoubleOrNull() ?: 0.0,
                    gstPercentage = gstStr.toIntOrNull() ?: 0,
                    stockQuantity = stockStr.toDoubleOrNull() ?: 0.0,
                    unit = unit.ifEmpty { "pcs" }
                )
                lifecycleScope.launch {
                    val db = AppDatabase.getDatabase(requireContext())
                    db.productDao().insertProduct(product)
                    
                    // Clear inputs and show success
                    binding.editProductName.setText("")
                    binding.editProductPrice.setText("")
                    binding.editProductStock.setText("")
                    binding.editProductUnit.setText("pcs")
                    android.widget.Toast.makeText(requireContext(), "Product Saved!", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
