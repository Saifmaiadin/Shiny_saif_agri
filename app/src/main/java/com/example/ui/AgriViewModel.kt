package com.example.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.*
import com.example.utils.AgriFileUtils
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class StoreNavScreen {
    LOGIN,
    DASHBOARD,
    CASHIER_TERMINAL,
    INVENTORY_MANAGER,
    SALES_REPORTS,
    ALERT_CENTER,
    ROLE_USERS,
    EXPENSES,
    CUSTOMERS
}

class AgriViewModel(private val repository: AgriRepository) : ViewModel() {
    
    // ... items ...

    // --- FILE OPERATIONS ---
    fun exportSalesReport(context: android.content.Context) {
        AgriFileUtils.exportSalesToCSV(context, allSales.value)
    }

    fun exportInventoryReport(context: android.content.Context) {
        AgriFileUtils.exportInventoryToCSV(context, allProducts.value)
    }

    fun backupAllData(context: android.content.Context) {
        AgriFileUtils.createBackup(
            context,
            allSales.value,
            allProducts.value,
            allCustomers.value,
            allExpenses.value
        )
    }

    // ... rest of class ...
    // --- NAVIGATION & ROLE SESSIONS ---
    var currentScreen by mutableStateOf(StoreNavScreen.LOGIN)
        private set

    var currentUser by mutableStateOf<User?>(null)
        private set

    var authError by mutableStateOf<String?>(null)

    // --- OFFLINE / ONLINE SIMULATION ---
    var isOfflineMode by mutableStateOf(false)
        private set

    // --- BASKET / TRANSACTIONS ---
    val basket = mutableStateMapOf<Product, Int>()

    var barcodeScanInput by mutableStateOf("")
    var scanMessage by mutableStateOf<String?>(null)
    var isScanningMode by mutableStateOf(false)

    // --- NEW PRODUCT FORM STATES ---
    var formName by mutableStateOf("")
    var formCategory by mutableStateOf("")
    var formPrice by mutableStateOf("")
    var formStock by mutableStateOf("")
    var formThreshold by mutableStateOf("")
    var formSku by mutableStateOf("")
    var formDesc by mutableStateOf("")
    var formSupplier by mutableStateOf("Global Agri Sourcing")
    var formRack by mutableStateOf("A-1")

    var editingProduct by mutableStateOf<Product?>(null)

    // --- RE-SYNC PENDING CUE ---
    var unsyncedSalesState = MutableStateFlow<List<Sale>>(emptyList())

    // --- FLOWS FROM REPOSITORY ---
    val allProducts: StateFlow<List<Product>> = repository.allProducts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allSales: StateFlow<List<Sale>> = repository.allSales
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allUsers: StateFlow<List<User>> = repository.allUsers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allAlerts: StateFlow<List<SMSAlert>> = repository.allAlerts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allCustomers: StateFlow<List<Customer>> = repository.allCustomers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allExpenses: StateFlow<List<Expense>> = repository.allExpenses
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            repository.preloadDataIfEmpty()
            updateOfflineLists()
        }
    }

    private fun updateOfflineLists() {
        viewModelScope.launch {
            // Filter list of offline unsynced sales locally
            repository.allSales.collect { sales ->
                unsyncedSalesState.value = sales.filter { it.isPendingSync }
            }
        }
    }

    // --- OFFLINE CONTROLS ---
    fun setOffline(enabled: Boolean) {
        isOfflineMode = enabled
    }

    fun syncOfflineSales() {
        viewModelScope.launch {
            val unsynced = unsyncedSalesState.value
            for (sale in unsynced) {
                // Update isPendingSync to false in database to simulate cloud sync
                val updatedSale = sale.copy(isPendingSync = false)
                repository.dao.insertSale(updatedSale)
            }
            updateOfflineLists()
        }
    }

    // --- AUTH ACTIONS ---
    fun login(username: String, pin: String) {
        viewModelScope.launch {
            val user = repository.getUserByUsername(username.trim().lowercase())
            if (user != null && user.passwordHash == pin.trim()) {
                currentUser = user
                authError = null
                currentScreen = StoreNavScreen.DASHBOARD
                clearBasket()
            } else {
                authError = "Invalid Username or PIN combination!"
            }
        }
    }

    fun logout() {
        currentUser = null
        currentScreen = StoreNavScreen.LOGIN
        clearBasket()
    }

    fun navigateTo(screen: StoreNavScreen) {
        currentScreen = screen
    }

    // --- CASHIER BASKET ACTIONS ---
    fun addToBasket(product: Product) {
        if (product.stockQuantity <= 0) {
            scanMessage = "Error: ${product.name} is out of stock!"
            return
        }
        val currentQty = basket[product] ?: 0
        if (currentQty >= product.stockQuantity) {
            scanMessage = "Maximum stock capacity (${product.stockQuantity}) reached in cart!"
            return
        }
        basket[product] = currentQty + 1
        scanMessage = "Added ${product.name} to basket."
    }

    fun removeOneFromBasket(product: Product) {
        val currentQty = basket[product] ?: 0
        if (currentQty <= 1) {
            basket.remove(product)
        } else {
            basket[product] = currentQty - 1
        }
    }

    fun clearBasket() {
        basket.clear()
        barcodeScanInput = ""
        scanMessage = null
    }

    fun getBasketTotal(): Double {
        return basket.entries.sumOf { it.key.price * it.value }
    }

    // --- BARCODE SCANNING SIMULATOR ---
    fun simulateBarcodeScan(skuCode: String) {
        viewModelScope.launch {
            val product = repository.getProductBySku(skuCode.trim())
            if (product != null) {
                addToBasket(product)
                scanMessage = "🎉 Beep! Scanned: ${product.name} - SKU ${product.sku} added ($${product.price})"
                barcodeScanInput = ""
            } else {
                scanMessage = "❌ Scanned SKU '$skuCode' not found in store database!"
            }
        }
    }

    // --- FAST SKU STOCK MIN-SCANNING (INVENTORY CONTROLLER) ---
    fun scanToUpdateStock(skuCode: String, qtyToAdd: Int) {
        viewModelScope.launch {
            val product = repository.getProductBySku(skuCode.trim())
            if (product != null) {
                val newStock = product.stockQuantity + qtyToAdd
                repository.updateProductStock(product.id, newStock)
                scanMessage = "Added $qtyToAdd to ${product.name}. New Stock: $newStock"
            } else {
                scanMessage = "SKU '$skuCode' not found for stock adjustment!"
            }
        }
    }

    // --- SALES CHECKOUT ---
    fun checkoutCurrentBasket(paymentMethod: String) {
        val activeUser = currentUser ?: return
        if (basket.isEmpty()) return

        viewModelScope.launch {
            val saleItems = basket.map { (product, qty) ->
                SaleItem(
                    saleId = 0, // set during db transaction
                    productId = product.id,
                    productName = product.name,
                    quantity = qty,
                    unitPrice = product.price,
                    subTotal = product.price * qty
                )
            }

            val saleRecord = Sale(
                timestamp = System.currentTimeMillis(),
                cashierName = activeUser.fullName,
                totalAmount = getBasketTotal(),
                paymentMethod = paymentMethod,
                isPendingSync = isOfflineMode // pending sync cache if offline
            )

            repository.processSale(saleRecord, saleItems)
            clearBasket()
            scanMessage = if (isOfflineMode) {
                "Sale processed OFFLINE. Cached in sync queue!"
            } else {
                "Sale processed successfully! Receipts and inventory tracking sync updated."
            }
            updateOfflineLists()
        }
    }

    // --- PRODUCT CRUD ACTIONS ---
    fun deleteProduct(id: Int) {
        viewModelScope.launch {
            repository.deleteProductById(id)
        }
    }

    fun saveProduct() {
        if (formName.isBlank() || formPrice.toDoubleOrNull() == null || formStock.toIntOrNull() == null) {
            return
        }
        viewModelScope.launch {
            val currentEditing = editingProduct
            val p = Product(
                id = currentEditing?.id ?: 0,
                name = formName,
                category = formCategory.ifBlank { "Uncategorized" },
                price = formPrice.toDoubleOrNull() ?: 1.0,
                stockQuantity = formStock.toIntOrNull() ?: 0,
                minStockThreshold = formThreshold.toIntOrNull() ?: 5,
                sku = formSku.ifBlank { "SKU-${formName.take(3).uppercase()}-${(100..999).random()}" },
                description = formDesc,
                supplierName = formSupplier,
                locationRack = formRack
            )

            repository.insertProduct(p)
            clearProductForm()
        }
    }

    fun startEditingProduct(product: Product) {
        editingProduct = product
        formName = product.name
        formCategory = product.category
        formPrice = product.price.toString()
        formStock = product.stockQuantity.toString()
        formThreshold = product.minStockThreshold.toString()
        formSku = product.sku
        formDesc = product.description
        formSupplier = product.supplierName
        formRack = product.locationRack
    }

    fun clearProductForm() {
        editingProduct = null
        formName = ""
        formCategory = "Seeds"
        formPrice = ""
        formStock = ""
        formThreshold = "10"
        formSku = ""
        formDesc = ""
        formSupplier = "Global Agri Sourcing"
        formRack = "A-1"
    }

    // --- SYSTEM SMS SIMULATOR & CLEAR ALERTS ---
    fun clearAlerts() {
        viewModelScope.launch {
            repository.allAlerts.first().forEach { alert ->
                repository.dao.insertAlert(alert.copy(status = "RESOLVED"))
            }
        }
    }

    // --- DATA REPORTS COMPILER (CSV) ---
    fun compileInventoryReportCsv(): String {
        val products = allProducts.value
        val sb = StringBuilder()
        sb.append("AGRISTORE MANAGER - STOCK INVENTORY REPORT\n")
        sb.append("Timestamp: ${System.currentTimeMillis()}\n\n")
        sb.append("ID,Name,Category,Price,Current Stock,Threshold,SKU,Supplier,Rack\n")
        for (p in products) {
            sb.append("${p.id},\"${p.name}\",\"${p.category}\",${p.price},${p.stockQuantity},${p.minStockThreshold},\"${p.sku}\",\"${p.supplierName}\",\"${p.locationRack}\"\n")
        }
        return sb.toString()
    }

    fun compileSalesReportCsv(): String {
        val sales = allSales.value
        val sb = StringBuilder()
        sb.append("AGRISTORE MANAGER - FINANCIAL SALES REPORT\n")
        sb.append("Timestamp: ${System.currentTimeMillis()}\n\n")
        sb.append("SaleID,Timestamp,Cashier,TotalAmount,PaymentMethod,SyncStatus\n")
        for (s in sales) {
            sb.append("${s.id},${s.timestamp},\"${s.cashierName}\",${s.totalAmount},\"${s.paymentMethod}\",${if (s.isPendingSync) "Offline Pending" else "Synced"}\n")
        }
        return sb.toString()
    }

    // --- SIMULATED USER CREATION ---
    fun createSystemUser(username: String, pin: String, fullName: String, role: UserRole) {
        viewModelScope.launch {
            if (username.isNotBlank() && pin.isNotBlank()) {
                repository.insertUser(User(username.trim().lowercase(), pin.trim(), fullName, role))
            }
        }
    }

    fun deleteSystemUser(username: String) {
        viewModelScope.launch {
            repository.deleteUser(username)
        }
    }

    // --- EXPENSE MANAGEMENT ---
    fun addExpense(title: String, category: String, amount: Double, note: String) {
        viewModelScope.launch {
            repository.insertExpense(Expense(
                title = title,
                category = category,
                amount = amount,
                timestamp = System.currentTimeMillis(),
                note = note
            ))
        }
    }

    fun deleteExpense(id: Int) {
        viewModelScope.launch {
            repository.deleteExpenseById(id)
        }
    }

    // --- CUSTOMER MANAGEMENT ---
    fun addCustomer(name: String, phone: String, email: String, address: String) {
        viewModelScope.launch {
            repository.insertCustomer(Customer(
                name = name,
                phone = phone,
                email = email,
                address = address
            ))
        }
    }

    fun deleteCustomer(id: Int) {
        viewModelScope.launch {
            repository.deleteCustomerById(id)
        }
    }
}

class AgriViewModelFactory(private val repository: AgriRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AgriViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AgriViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
