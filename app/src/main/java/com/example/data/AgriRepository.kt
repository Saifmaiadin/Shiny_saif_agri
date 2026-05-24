package com.example.data

import android.content.Context
import androidx.room.Room
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow

class AgriRepository(private val context: Context) {

    private val db: AgriDatabase by lazy {
        Room.databaseBuilder(
            context.applicationContext,
            AgriDatabase::class.java,
            "agristore_database.db"
        )
        .fallbackToDestructiveMigration(true)
        .build()
    }

    val dao get() = db.dao

    // Expose flows to represent the UI state
    val allProducts: Flow<List<Product>> = dao.getAllProductsFlow()
    val allSales: Flow<List<Sale>> = dao.getAllSalesFlow()
    val allUsers: Flow<List<User>> = dao.getAllUsersFlow()
    val allAlerts: Flow<List<SMSAlert>> = dao.getAllAlertsFlow()
    val allCustomers: Flow<List<Customer>> = dao.getAllCustomersFlow()
    val allExpenses: Flow<List<Expense>> = dao.getAllExpensesFlow()

    suspend fun getUserByUsername(username: String): User? = dao.getUserByUsername(username)
    suspend fun getProductBySku(sku: String): Product? = dao.getProductBySku(sku)
    suspend fun getProductById(id: Int): Product? = dao.getProductById(id)

    suspend fun insertProduct(product: Product) = dao.insertProduct(product)
    suspend fun updateProductStock(id: Int, newStock: Int) = dao.updateProductStock(id, newStock)
    suspend fun deleteProductById(id: Int) = dao.deleteProductById(id)

    suspend fun insertUser(user: User) = dao.insertUser(user)
    suspend fun deleteUser(username: String) = dao.deleteUser(username)

    suspend fun insertAlert(alert: SMSAlert) = dao.insertAlert(alert)
    suspend fun insertCustomer(customer: Customer) = dao.insertCustomer(customer)
    suspend fun deleteCustomerById(id: Int) = dao.deleteCustomerById(id)
    suspend fun insertExpense(expense: Expense) = dao.insertExpense(expense)
    suspend fun deleteExpenseById(id: Int) = dao.deleteExpenseById(id)

    suspend fun processSale(sale: Sale, items: List<SaleItem>): Long {
        return dao.processSaleEntry(sale, items)
    }

    suspend fun getSaleItemsForSale(saleId: Int): List<SaleItem> {
        return dao.getSaleItemsForSale(saleId)
    }

    // Preloads mock/seed data if DB starts empty to give users an immediate playground
    suspend fun preloadDataIfEmpty() = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        // 1. Check if users are empty
        val existingUsers = allUsers.first()
        if (existingUsers.isEmpty()) {
            // Seed a default admin, manager, cashier
            dao.insertUser(User("admin", "1111", "Alex Vance (Admin)", UserRole.ADMIN))
            dao.insertUser(User("manager", "2222", "Sophia Miller (Manager)", UserRole.MANAGER))
            dao.insertUser(User("cashier", "3333", "Tariq Ali (Cashier)", UserRole.CASHIER))
        }

        // 2. Check if products are empty
        val existingProducts = allProducts.first()
        if (existingProducts.isEmpty()) {
            val seedProducts = listOf(
                Product(
                    name = "NPK 15-15-15 Max Fertilizer",
                    category = "Fertilizers",
                    price = 45.99,
                    stockQuantity = 50,
                    minStockThreshold = 15,
                    sku = "SKU-NPK-101",
                    description = "Balanced nitrogen, phosphorus, and potassium fertilizer for robust vegetable and flower growth.",
                    supplierName = "Evergreen Chemical Co.",
                    locationRack = "Aisle A-2"
                ),
                Product(
                    name = "Organic Clover Sweet Cattle Feed",
                    category = "Animal Feeds",
                    price = 24.50,
                    stockQuantity = 8,  // Starts below threshold (10) for testing alerts!
                    minStockThreshold = 10,
                    sku = "SKU-FEED-202",
                    description = "Nutritious clover and sweet grass formula enriched with minerals for premium dairy livestock.",
                    supplierName = "Dairy Nutrition Corp.",
                    locationRack = "Aisle B-4"
                ),
                Product(
                    name = "Hybrid Golden Sweet Corn Seeds (500g)",
                    category = "Seeds",
                    price = 18.25,
                    stockQuantity = 120,
                    minStockThreshold = 20,
                    sku = "SKU-SEED-303",
                    description = "High-yield hybrid corn seeds featuring extreme disease resistance and delightful sweetness.",
                    supplierName = "Pioneer Flora Agrisciences",
                    locationRack = "Aisle C-1"
                ),
                Product(
                    name = "Broad-Spectrum Bio-Pesticide (1L)",
                    category = "Pesticides",
                    price = 32.00,
                    stockQuantity = 4,   // Starts super low (Threshold 12) to test Stock alert immediately!
                    minStockThreshold = 12,
                    sku = "SKU-PEST-404",
                    description = "Ecofriendly organic neem extract compound that targets aphids, mites, and whiteflies effectively.",
                    supplierName = "NatureShield Solutions",
                    locationRack = "Aisle A-5"
                ),
                Product(
                    name = "Durable Micro-Drip Irrigation Hose (50m)",
                    category = "Equipment",
                    price = 89.90,
                    stockQuantity = 22,
                    minStockThreshold = 5,
                    sku = "SKU-HOSE-505",
                    description = "UV-resistant, professional-grade flexible drip lines designed for targeted smart watering systems.",
                    supplierName = "AgriTech Flow Inc.",
                    locationRack = "Aisle E-3"
                ),
                Product(
                    name = "Premium Wheat Growing Seed Pack (20kg)",
                    category = "Seeds",
                    price = 65.00,
                    stockQuantity = 35,
                    minStockThreshold = 8,
                    sku = "SKU-SEED-309",
                    description = "Spring wheat seed grain, high protein content, superb germination rates.",
                    supplierName = "Pioneer Flora Agrisciences",
                    locationRack = "Aisle C-2"
                )
            )

            for (p in seedProducts) {
                dao.insertProduct(p)
            }

            // Seed a few initial historical sale records so dashboards have charts loaded immediately
            val historicalSales = listOf(
                Sale(timestamp = System.currentTimeMillis() - 86400000 * 3, cashierName = "Tariq Ali (Cashier)", totalAmount = 135.90, paymentMethod = "Cash"),
                Sale(timestamp = System.currentTimeMillis() - 86400000 * 2, cashierName = "Sophia Miller (Manager)", totalAmount = 452.00, paymentMethod = "Card"),
                Sale(timestamp = System.currentTimeMillis() - 86400000 * 1, cashierName = "Tariq Ali (Cashier)", totalAmount = 289.45, paymentMethod = "Mobile Money"),
                Sale(timestamp = System.currentTimeMillis() - 3600000 * 2, cashierName = "Tariq Ali (Cashier)", totalAmount = 91.98, paymentMethod = "Cash")
            )

            for (s in historicalSales) {
                val sId = dao.insertSale(s).toInt()
                // Seed some corresponding items
                dao.insertSaleItem(SaleItem(saleId = sId, productId = 1, productName = "NPK 15-15-15 Max Fertilizer", quantity = 2, unitPrice = 45.99, subTotal = 91.98))
            }

            // 3. Seed some initial customers
            val seedCustomers = listOf(
                Customer(name = "Ahmed Mansour", phone = "+20123456789", email = "ahmed@example.com", address = "Cairo, Egypt", totalPurchases = 1500.0),
                Customer(name = "Fatima Al-Zahra", phone = "+20198765432", email = "fatima@example.com", address = "Giza, Egypt", totalPurchases = 2400.0, creditBalance = 200.0),
                Customer(name = "John Doe Agri", phone = "+15550101", email = "john@farm.com", address = "Rural Route 1", totalPurchases = 50.0)
            )
            for (c in seedCustomers) dao.insertCustomer(c)

            // 4. Seed some initial expenses
            val seedExpenses = listOf(
                Expense(title = "Monthly Electricity", category = "Utilities", amount = 120.0, timestamp = System.currentTimeMillis() - 86400000 * 5, note = "Main store meter"),
                Expense(title = "Staff Salaries - May", category = "Salaries", amount = 2500.0, timestamp = System.currentTimeMillis() - 86400000 * 2, note = "Paid to all staff"),
                Expense(title = "Seed Stock Purchase", category = "Inventory", amount = 800.0, timestamp = System.currentTimeMillis() - 3600000 * 5, note = "Bulk wheat seeds")
            )
            for (e in seedExpenses) dao.insertExpense(e)
            
            // Seed sample active alerts for already low stock items
            dao.insertAlert(SMSAlert(
                message = "CRITICAL: Stock for Broad-Spectrum Bio-Pesticide (1L) has fallen to 4 units (Threshold: 12). Restock immediately!",
                timestamp = System.currentTimeMillis() - 3600000,
                sentTo = "+1-555-AGRI-ADMIN",
                status = "DELIVERED"
            ))
            dao.insertAlert(SMSAlert(
                message = "CRITICAL: Stock for Organic Clover Sweet Cattle Feed has fallen to 8 units (Threshold: 10). Restock immediately!",
                timestamp = System.currentTimeMillis() - 1800000,
                sentTo = "+1-555-AGRI-ADMIN",
                status = "DELIVERED"
            ))
        }
    }
}
