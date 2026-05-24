package com.example.data

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface AgriDao {
    // --- USER QUERIES ---
    @Query("SELECT * FROM users WHERE username = :username LIMIT 1")
    suspend fun getUserByUsername(username: String): User?

    @Query("SELECT * FROM users")
    fun getAllUsersFlow(): Flow<List<User>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: User)

    @Query("DELETE FROM users WHERE username = :username")
    suspend fun deleteUser(username: String)

    // --- PRODUCT QUERIES ---
    @Query("SELECT * FROM products ORDER BY name ASC")
    fun getAllProductsFlow(): Flow<List<Product>>

    @Query("SELECT * FROM products WHERE id = :id LIMIT 1")
    suspend fun getProductById(id: Int): Product?

    @Query("SELECT * FROM products WHERE sku = :sku LIMIT 1")
    suspend fun getProductBySku(sku: String): Product?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: Product): Long

    @Query("UPDATE products SET stockQuantity = :newStock WHERE id = :id")
    suspend fun updateProductStock(id: Int, newStock: Int)

    @Query("DELETE FROM products WHERE id = :id")
    suspend fun deleteProductById(id: Int)

    // --- SALES QUERIES ---
    @Query("SELECT * FROM sales ORDER BY timestamp DESC")
    fun getAllSalesFlow(): Flow<List<Sale>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSale(sale: Sale): Long

    // --- SALE ITEMS QUERIES ---
    @Query("SELECT * FROM sale_items WHERE saleId = :saleId")
    fun getSaleItemsForSaleFlow(saleId: Int): Flow<List<SaleItem>>

    @Query("SELECT * FROM sale_items WHERE saleId = :saleId")
    suspend fun getSaleItemsForSale(saleId: Int): List<SaleItem>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSaleItem(saleItem: SaleItem): Long

    // --- SMS ALERTS QUERIES ---
    @Query("SELECT * FROM sms_alerts ORDER BY timestamp DESC")
    fun getAllAlertsFlow(): Flow<List<SMSAlert>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlert(alert: SMSAlert): Long

    // --- CUSTOMER QUERIES ---
    @Query("SELECT * FROM customers ORDER BY name ASC")
    fun getAllCustomersFlow(): Flow<List<Customer>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomer(customer: Customer): Long

    @Query("DELETE FROM customers WHERE id = :id")
    suspend fun deleteCustomerById(id: Int)

    // --- EXPENSE QUERIES ---
    @Query("SELECT * FROM expenses ORDER BY timestamp DESC")
    fun getAllExpensesFlow(): Flow<List<Expense>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: Expense): Long

    @Query("DELETE FROM expenses WHERE id = :id")
    suspend fun deleteExpenseById(id: Int)

    // --- TRANSACTION FOR SALE ENTRY ---
    @Transaction
    suspend fun processSaleEntry(sale: Sale, items: List<SaleItem>): Long {
        val saleId = insertSale(sale).toInt()
        for (item in items) {
            val dbItem = item.copy(saleId = saleId)
            insertSaleItem(dbItem)
            
            // Subtract stock
            val product = getProductById(dbItem.productId)
            if (product != null) {
                val newStock = (product.stockQuantity - dbItem.quantity).coerceAtLeast(0)
                updateProductStock(product.id, newStock)
                
                // Trigger SMS alert if stock falls below threshold
                if (newStock <= product.minStockThreshold) {
                    val alertMessage = "CRITICAL: Stock for ${product.name} (SKU: ${product.sku}) has fallen to $newStock units (Threshold: ${product.minStockThreshold}). Please restock!"
                    insertAlert(SMSAlert(
                        message = alertMessage,
                        timestamp = System.currentTimeMillis(),
                        sentTo = "+1-555-AGRI-ADMIN",
                        status = "DELIVERED"
                    ))
                }
            }
        }
        return saleId.toLong()
    }
}

@Database(
    entities = [User::class, Product::class, Sale::class, SaleItem::class, SMSAlert::class, Customer::class, Expense::class],
    version = 3,
    exportSchema = false
)
abstract class AgriDatabase : RoomDatabase() {
    abstract val dao: AgriDao
}
