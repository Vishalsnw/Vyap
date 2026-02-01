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
    private var allProducts = listOf<Product>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentProductBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        adapter = ProductAdapter()
        binding.recyclerViewProducts.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerViewProducts.adapter = adapter

        viewLifecycleOwner.lifecycleScope.launch {
            val db = AppDatabase.getDatabase(requireContext())
            db.productDao().getAllProducts().collectLatest { products ->
                if (!isAdded || view == null) return@collectLatest
                allProducts = products
                updateStockSummary(products)
                filterProducts(binding.editSearchProducts.text.toString())
            }
        }
        
        binding.fabAddProduct.setOnClickListener {
            showAddProductBottomSheet()
        }

        binding.editSearchProducts.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterProducts(s.toString())
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })
    }

    private fun updateStockSummary(products: List<Product>) {
        binding.textTotalItemsCount.text = products.size.toString()
        binding.textLowStockCount.text = products.count { it.stockQuantity <= it.minStockLevel }.toString()
    }

    private fun showAddProductBottomSheet() {
        val dialog = com.google.android.material.bottomsheet.BottomSheetDialog(requireContext())
        val dialogBinding = com.invoicepro.app.databinding.DialogNewProductBinding.inflate(layoutInflater)
        dialog.setContentView(dialogBinding.root)

        dialogBinding.btnSaveProduct.setOnClickListener {
            val name = dialogBinding.editProductName.text.toString().trim()
            val priceStr = dialogBinding.editProductPrice.text.toString().trim()
            val stockStr = dialogBinding.editProductStock.text.toString().trim()
            val gstStr = dialogBinding.editProductGst.text.toString().trim()
            val minStockStr = dialogBinding.editProductMinStock.text.toString().trim()

            if (name.isNotEmpty() && priceStr.isNotEmpty()) {
                try {
                    val price = priceStr.toDoubleOrNull() ?: 0.0
                    val stock = stockStr.toDoubleOrNull() ?: 0.0
                    val gst = gstStr.toIntOrNull() ?: 0
                    val minStock = minStockStr.toDoubleOrNull() ?: 5.0

                    val product = Product(
                        name = name,
                        sellingPrice = price,
                        gstPercentage = gst,
                        stockQuantity = stock,
                        minStockLevel = minStock,
                        unit = "pcs"
                    )
                    
                    viewLifecycleOwner.lifecycleScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                        try {
                            val db = AppDatabase.getDatabase(requireContext().applicationContext)
                            db.productDao().insertProduct(product)
                            
                            launch(kotlinx.coroutines.Dispatchers.Main) {
                                if (!isAdded || view == null) return@launch
                                android.widget.Toast.makeText(requireContext(), "Product Saved!", android.widget.Toast.LENGTH_SHORT).show()
                                dialog.dismiss()
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("PRODUCT_SAVE_ERROR", "Failed to save product", e)
                            launch(kotlinx.coroutines.Dispatchers.Main) {
                                if (!isAdded || view == null) return@launch
                                android.widget.Toast.makeText(requireContext(), "Database Error: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.e("PRODUCT_SAVE_ERROR", "Input parsing error", e)
                    android.widget.Toast.makeText(requireContext(), "Input Error: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                }
            } else {
                android.widget.Toast.makeText(requireContext(), "Name and Price are required", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
        dialog.show()
    }

    private fun filterProducts(query: String) {
        val filtered = if (query.isEmpty()) {
            allProducts
        } else {
            allProducts.filter { it.name.contains(query, ignoreCase = true) }
        }
        adapter.submitList(filtered)
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
            
            // Low stock alert (using custom threshold)
            if (product.stockQuantity <= product.minStockLevel) {
                holder.binding.textStockAlert.visibility = View.VISIBLE
            } else {
                holder.binding.textStockAlert.visibility = View.GONE
            }
            
            holder.binding.root.setOnLongClickListener {
                android.app.AlertDialog.Builder(holder.itemView.context)
                    .setTitle("Delete Product")
                    .setMessage("Are you sure you want to delete ${product.name}?")
                    .setPositiveButton("Delete") { _, _ ->
                        viewLifecycleOwner.lifecycleScope.launch {
                            try {
                                val db = AppDatabase.getDatabase(holder.itemView.context)
                                db.productDao().deleteProduct(product)
                            } catch (e: Exception) {
                                android.util.Log.e("PRODUCT_DELETE_ERROR", "Failed to delete product", e)
                            }
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
