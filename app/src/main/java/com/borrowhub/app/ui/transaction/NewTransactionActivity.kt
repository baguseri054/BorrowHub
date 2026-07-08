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

        // Panggil fungsi untuk mengisi dropdown dengan barang yang tersedia
        loadAvailableItems()

        binding.btnSubmitTransaction.setOnClickListener {
            saveTransaction()
        }
    }

    private fun loadAvailableItems() {
        // Kunci dropdown sementara selagi mengambil data dari server
        binding.etSelectedItem.isEnabled = false

        // 1. Ambil semua barang dari koleksi items
        db.collection("items").get().addOnSuccessListener { itemResult ->
            val allItems = mutableListOf<String>()
            for (doc in itemResult) {
                val name = doc.getString("name") ?: ""
                if (name.isNotEmpty()) {
                    allItems.add(name)
                }
            }

            // 2. Ambil data peminjaman yang sedang aktif
            db.collection("borrows")
                .whereIn("status", listOf("Active", "Overdue"))
                .get()
                .addOnSuccessListener { borrowResult ->
                    val borrowedItems = mutableListOf<String>()
                    for (doc in borrowResult) {
                        val name = doc.getString("itemName") ?: ""
                        borrowedItems.add(name)
                    }

                    // 3. Saring! Hanya ambil barang yang namanya TIDAK ADA di daftar pinjaman
                    val availableItems = allItems.filter { !borrowedItems.contains(it) }

                    // 4. Masukkan daftar yang sudah disaring ke dalam Dropdown Menu
                    val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, availableItems)
                    binding.etSelectedItem.setAdapter(adapter)

                    // Buka kunci dropdown
                    binding.etSelectedItem.isEnabled = true
                    binding.etSelectedItem.hint = "Pilih Barang Tersedia"

                    if (availableItems.isEmpty()) {
                        Toast.makeText(this, "Semua barang sedang dipinjam!", Toast.LENGTH_LONG).show()
                        binding.etSelectedItem.hint = "Katalog Kosong"
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Gagal memuat status pinjaman", Toast.LENGTH_SHORT).show()
                }
        }.addOnFailureListener {
            Toast.makeText(this, "Gagal memuat katalog barang", Toast.LENGTH_SHORT).show()
        }
    }

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

    private fun saveTransaction() {
        val itemName = binding.etSelectedItem.text.toString().trim()
        val borrowerName = binding.etBorrowerName.text.toString().trim()
        val photoUrl = binding.etPhotoUrl.text.toString().trim()
        val startDateStr = binding.etStartDate.text.toString().trim()
        val endDateStr = binding.etEndDate.text.toString().trim()

        if (itemName.isEmpty() || borrowerName.isEmpty() || startDateStr.isEmpty() || endDateStr.isEmpty()) {
            Toast.makeText(this, "Mohon lengkapi data (kecuali foto)", Toast.LENGTH_SHORT).show()
            return
        }

        binding.btnSubmitTransaction.isEnabled = false
        binding.btnSubmitTransaction.text = "Menyimpan..."

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
                    Toast.makeText(this, "Transaksi berhasil disimpan", Toast.LENGTH_SHORT).show()
                    finish()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Gagal menyimpan: ${e.message}", Toast.LENGTH_SHORT).show()
                    binding.btnSubmitTransaction.isEnabled = true
                    binding.btnSubmitTransaction.text = "Submit Transaction"
                }
        } catch (e: Exception) {
            Toast.makeText(this, "Format tanggal salah!", Toast.LENGTH_SHORT).show()
            binding.btnSubmitTransaction.isEnabled = true
            binding.btnSubmitTransaction.text = "Submit Transaction"
        }
    }
}