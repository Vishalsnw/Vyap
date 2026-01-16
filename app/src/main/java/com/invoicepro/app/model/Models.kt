package com.invoicepro.app.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "customers")
data class Customer(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val phone: String,
    val address: String,
    val gstin: String? = null
)

@Entity(tableName = "products")
data class Product(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val sellingPrice: Double,
    val gstPercentage: Int, // 0, 5, 12, 18, 28
    val stockQuantity: Double = 0.0,
    val unit: String = "pcs" // pcs, kg, mtr, etc.
)

@Entity(tableName = "invoice_items")
data class InvoiceItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val invoiceId: Long,
    val productId: Long,
    val productName: String,
    val quantity: Double,
    val rate: Double,
    val gstPercentage: Int,
    val amount: Double
)

@Entity(tableName = "invoices")
data class Invoice(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val invoiceNumber: String,
    val customerId: Long,
    val date: Long,
    val subtotal: Double,
    val cgst: Double,
    val sgst: Double,
    val igst: Double,
    val total: Double
)
