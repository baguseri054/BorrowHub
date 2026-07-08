package com.borrowhub.app.ui.transaction

import android.app.DatePickerDialog
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.borrowhub.app.databinding.ActivityNewTransactionBinding
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * Activity ini menangani pembuatan transaksi peminjaman baru.
 * Di sini terdapat logika filter barang agar pengguna hanya bisa memilih barang yang sedang tidak dipinjam.
 */
class NewTransactionActivity : AppCompatActivity() {

    private lateinit var binding: ActivityNewTransactionBinding
    private val calendar = Calendar.getInstance()
    private val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.US)
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNewTransactionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener { finish() }

        setupDatePickers()
        loadAvailableItems()

        binding.btnSubmitTransaction.setOnClickListener {
            saveTransaction()
        }
    }

    /**
     * Logika Filter Barang Tersedia:
     * 1. Mengambil semua data barang dari koleksi 'items'.
     * 2. Mengambil data transaksi 'Active'/'Overdue' dari koleksi 'borrows'.
     * 3. Menyaring barang yang tidak ada dalam daftar pinjaman aktif.
     * Tujuannya untuk mencegah peminjaman ganda pada barang yang sama.
     */
    private fun loadAvailableItems() {
        binding.etSelectedItem.isEnabled = false

        db.collection("items").get().addOnSuccessListener { itemResult ->
            val allItems = mutableListOf<String>()
            for (doc in itemResult) {
                val name = doc.getString("name") ?: ""
                if (name.isNotEmpty()) {
                    allItems.add(name)
                }
            }

            db.collection("borrows")
                .whereIn("status", listOf("Active", "Overdue"))
                .get()
                .addOnSuccessListener { borrowResult ->
                    val borrowedItems = mutableListOf<String>()
                    for (doc in borrowResult) {
                        val name = doc.getString("itemName") ?: ""
                        borrowedItems.add(name)
                    }

                    // Hanya menampilkan barang yang tidak sedang dipinjam
                    val availableItems = allItems.filter { !borrowedItems.contains(it) }

                    val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, availableItems)
                    binding.etSelectedItem.setAdapter(adapter)

                    binding.etSelectedItem.isEnabled = true
                    binding.etSelectedItem.hint = "Pick an item"

                    if (availableItems.isEmpty()) {
                        Toast.makeText(this, "No items available for borrowing right now.", Toast.LENGTH_LONG).show()
                        binding.etSelectedItem.hint = "Empty catalog"
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Failed to check item availability.", Toast.LENGTH_SHORT).show()
                }
        }.addOnFailureListener {
            Toast.makeText(this, "Failed to load catalog.", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Inisialisasi DatePickerDialog untuk mempermudah pemilihan tanggal pinjam dan kembali.
     */
    private fun setupDatePickers() {
        binding.etStartDate.setOnClickListener {
            DatePickerDialog(
                this,
                { _, year, month, dayOfMonth ->
                    calendar.set(year, month, dayOfMonth)
                    binding.etStartDate.setText(dateFormat.format(calendar.time))
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        binding.etEndDate.setOnClickListener {
            DatePickerDialog(
                this,
                { _, year, month, dayOfMonth ->
                    calendar.set(year, month, dayOfMonth)
                    binding.etEndDate.setText(dateFormat.format(calendar.time))
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }
    }

    /**
     * Mengirim data transaksi baru ke Firestore.
     * Kita menggunakan Timestamp agar data tanggal tersimpan secara standar di Firebase.
     */
    private fun saveTransaction() {
        val itemName = binding.etSelectedItem.text.toString().trim()
        val borrowerName = binding.etBorrowerName.text.toString().trim()
        val photoUrl = binding.etPhotoUrl.text.toString().trim()
        val startDateStr = binding.etStartDate.text.toString().trim()
        val endDateStr = binding.etEndDate.text.toString().trim()

        if (itemName.isEmpty() || borrowerName.isEmpty() || startDateStr.isEmpty() || endDateStr.isEmpty()) {
            Toast.makeText(this, "Please fill in all required fields.", Toast.LENGTH_SHORT).show()
            return
        }

        binding.btnSubmitTransaction.isEnabled = false
        binding.btnSubmitTransaction.text = "Saving..."

        try {
            val startDate = dateFormat.parse(startDateStr)
            val endDate = dateFormat.parse(endDateStr)

            val startTimestamp = if (startDate != null) Timestamp(startDate) else null
            val endTimestamp = if (endDate != null) Timestamp(endDate) else null

            val newBorrow = hashMapOf(
                "itemName" to itemName,
                "borrowerName" to borrowerName,
                "photoBefore" to photoUrl,
                "startDate" to startTimestamp,
                "endDate" to endTimestamp,
                "status" to "Active",
                "createdAt" to Timestamp.now()
            )

            db.collection("borrows")
                .add(newBorrow)
                .addOnSuccessListener {
                    Toast.makeText(this, "Transaction saved successfully!", Toast.LENGTH_SHORT).show()
                    finish()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Failed to save transaction: ${e.message}", Toast.LENGTH_SHORT).show()
                    binding.btnSubmitTransaction.isEnabled = true
                    binding.btnSubmitTransaction.text = "Submit Transaction"
                }
        } catch (e: Exception) {
            Toast.makeText(this, "Invalid date format.", Toast.LENGTH_SHORT).show()
            binding.btnSubmitTransaction.isEnabled = true
            binding.btnSubmitTransaction.text = "Submit Transaction"
        }
    }
}
