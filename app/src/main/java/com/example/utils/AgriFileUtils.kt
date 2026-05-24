package com.example.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.data.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

object AgriFileUtils {

    fun exportSalesToCSV(context: Context, sales: List<Sale>) {
        val fileName = "Sales_Report_${System.currentTimeMillis()}.csv"
        val file = File(context.cacheDir, fileName)
        
        val header = "Sale ID,Date,Amount,Payment Method,Status\n"
        val csvData = StringBuilder(header)
        
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
        
        sales.forEach { sale ->
            csvData.append("${sale.id},")
            csvData.append("${dateFormat.format(Date(sale.timestamp))},")
            csvData.append("${sale.totalAmount},")
            csvData.append("${sale.paymentMethod},")
            csvData.append("${if (sale.isPendingSync) "Pending" else "Synced"}\n")
        }
        
        file.writeText(csvData.toString())
        shareFile(context, file, "text/csv")
    }

    fun exportInventoryToCSV(context: Context, products: List<Product>) {
        val fileName = "Inventory_Report_${System.currentTimeMillis()}.csv"
        val file = File(context.cacheDir, fileName)
        
        val header = "SKU,Product Name,Category,Price,Quantity,Threshold\n"
        val csvData = StringBuilder(header)
        
        products.forEach { p ->
            csvData.append("${p.sku},")
            csvData.append("${p.name},")
            csvData.append("${p.category},")
            csvData.append("${p.price},")
            csvData.append("${p.stockQuantity},")
            csvData.append("${p.minStockThreshold}\n")
        }
        
        file.writeText(csvData.toString())
        shareFile(context, file, "text/csv")
    }

    fun createBackup(context: Context, sales: List<Sale>, products: List<Product>, customers: List<Customer>, expenses: List<Expense>) {
        // We use a simple comma-separated multi-section file as a "Backup" package
        // This is easier for users to save as a file
        val fileName = "AgriStore_Backup_${System.currentTimeMillis()}.txt"
        val file = File(context.cacheDir, fileName)
        
        val backupData = StringBuilder()
        backupData.append("--- BACKUP DATE: ${Date()} ---\n\n")
        
        backupData.append("[PRODUCTS]\n")
        products.forEach { backupData.append("${it.sku}|${it.name}|${it.category}|${it.price}|${it.stockQuantity}\n") }
        
        backupData.append("\n[CUSTOMERS]\n")
        customers.forEach { backupData.append("${it.name}|${it.phone}|${it.email}|${it.address}|${it.totalPurchases}\n") }

        backupData.append("\n[EXPENSES]\n")
        expenses.forEach { backupData.append("${it.title}|${it.category}|${it.amount}|${it.timestamp}\n") }

        file.writeText(backupData.toString())
        shareFile(context, file, "text/plain")
    }

    private fun shareFile(context: Context, file: File, mimeType: String) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share Document"))
    }
}
