package com.invoicepro.app.data

import androidx.room.*
import com.invoicepro.app.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomerDao {
    @Query("SELECT * FROM customers")
    fun getAllCustomers(): Flow<List<Customer>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomer(customer: Customer)
    
    @Delete
    suspend fun deleteCustomer(customer: Customer)
}

@Dao
interface ProductDao {
    @Query("SELECT * FROM products")
    fun getAllProducts(): Flow<List<Product>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: Product)

    @Delete
    suspend fun deleteProduct(product: Product)

    @Query("UPDATE products SET stockQuantity = stockQuantity - :quantity WHERE id = :productId")
    suspend fun reduceStock(productId: Long, quantity: Double)
}

@Dao
interface InvoiceDao {
    @Query("SELECT * FROM invoices ORDER BY date DESC")
    fun getAllInvoices(): Flow<List<Invoice>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInvoice(invoice: Invoice): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInvoiceItems(items: List<InvoiceItem>)

    @Transaction
    @Query("SELECT * FROM invoice_items WHERE invoiceId = :invoiceId")
    suspend fun getItemsForInvoice(invoiceId: Long): List<InvoiceItem>
}

@Dao
interface BusinessProfileDao {
    @Query("SELECT * FROM business_profile WHERE id = 0")
    suspend fun getProfile(): BusinessProfile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: BusinessProfile)
}

@Database(entities = [Customer::class, Product::class, Invoice::class, InvoiceItem::class, BusinessProfile::class], version = 4, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun customerDao(): CustomerDao
    abstract fun productDao(): ProductDao
    abstract fun invoiceDao(): InvoiceDao
    abstract fun businessProfileDao(): BusinessProfileDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: android.content.Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "invoice_pro_database"
                )
                .fallbackToDestructiveMigration()
                .allowMainThreadQueries()   // TEMP DEBUG ONLY
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
