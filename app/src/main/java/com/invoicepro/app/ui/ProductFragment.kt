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
import com.invoicepro.app.databinding.FragmentProductBinding
import com.invoicepro.app.databinding.ItemProductBinding
import com.invoicepro.app.model.Product
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class ProductFragment : Fragment() {
    private var _binding: FragmentProductBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: ProductAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentProductBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        adapter = ProductAdapter()
        binding.recyclerViewProducts.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerViewProducts.adapter = adapter

        lifecycleScope.launch {
            val db = AppDatabase.getDatabase(requireContext())
            db.productDao().getAllProducts().collectLatest {
                adapter.submitList(it)
            }
        }
        
        binding.btnSaveProduct.setOnClickListener {
            val name = binding.editProductName.text.toString()
            val priceStr = binding.editProductPrice.text.toString()
            val stockStr = binding.editProductStock.text.toString()
            val unit = binding.editProductUnit.text.toString()
            val gstStr = binding.spinnerGst.selectedItem.toString().replace("%", "")

            if (name.isNotEmpty() && priceStr.isNotEmpty()) {
                val product = Product(
                    name = name,
                    sellingPrice = priceStr.toDoubleOrNull() ?: 0.0,
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

    private inner class ProductAdapter : RecyclerView.Adapter<ProductAdapter.ViewHolder>() {
        private var products = listOf<Product>()

        fun submitList(newList: List<Product>) {
            products = newList
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val binding = ItemProductBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return ViewHolder(binding)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val product = products[position]
            holder.binding.textProductName.text = product.name
            holder.binding.textProductStock.text = "Stock: ${product.stockQuantity} ${product.unit}"
            holder.binding.textProductPrice.text = "₹%.2f".format(product.sellingPrice)
            
            holder.binding.root.setOnLongClickListener {
                android.app.AlertDialog.Builder(holder.itemView.context)
                    .setTitle("Delete Product")
                    .setMessage("Are you sure you want to delete ${product.name}?")
                    .setPositiveButton("Delete") { _, _ ->
                        lifecycleScope.launch {
                            val db = AppDatabase.getDatabase(holder.itemView.context)
                            db.productDao().deleteProduct(product)
                        }
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
                true
            }
        }

        override fun getItemCount() = products.size

        inner class ViewHolder(val binding: ItemProductBinding) : RecyclerView.ViewHolder(binding.root)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
