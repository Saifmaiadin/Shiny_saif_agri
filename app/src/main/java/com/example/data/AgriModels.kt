package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class UserRole {
    ADMIN,
    MANAGER,
    CASHIER
}

@Entity(tableName = "users")
data class User(
    @PrimaryKey val username: String,
    val passwordHash: String, // Simply stored for auth demonstration
    val fullName: String,
    val role: UserRole
)

@Entity(tableName = "products")
data class Product(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val category: String,
    val price: Double,
    val stockQuantity: Int,
    val minStockThreshold: Int,
    val sku: String, // Barcode/SKU code matching scan simulation
    val description: String,
    val supplierName: String = "Global Agri Sourcing",
    val locationRack: String = "A-1"
)

@Entity(tableName = "sales")
data class Sale(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val timestamp: Long,
    val cashierName: String,
    val totalAmount: Double,
    val paymentMethod: String, // Cash, Card, Mobile
    val isPendingSync: Boolean = false // Simulate offline queue status
)

@Entity(tableName = "sale_items")
data class SaleItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val saleId: Int,
    val productId: Int,
    val productName: String,
    val quantity: Int,
    val unitPrice: Double,
    val subTotal: Double
)

@Entity(tableName = "sms_alerts")
data class SMSAlert(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val message: String,
    val timestamp: Long,
    val sentTo: String,
    val status: String // "DELIVERED", "QUEUED"
)

@Entity(tableName = "customers")
data class Customer(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val phone: String,
    val email: String,
    val address: String,
    val totalPurchases: Double = 0.0,
    val creditBalance: Double = 0.0
)

@Entity(tableName = "expenses")
data class Expense(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val category: String,
    val amount: Double,
    val timestamp: Long,
    val note: String
)
