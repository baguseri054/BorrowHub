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

    /**
     * Fungsi pas activity ini dibuat.
     * 
     * Langkah-langkahnya:
     * 1. Setup view binding.
     * 2. Pasang listener tombol balik.
     * 3. Setup pemilih tanggal buat mulai & selesai pinjam.
     * 4. Ambil barang-barang yang bisa dipinjem.
     * 5. Pasang listener buat tombol submit.
     */
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
     * Fungsi buat ambil barang yang lagi nggak dipinjem.
     * 
     * Langkah-langkahnya:
     * 1. Kunci dropdown-nya biar nggak error pas loading.
     * 2. Ambil semua list barang dari Firestore.
     * 3. Ambil data pinjaman yang statusnya masih aktif.
     * 4. Saring barang mana aja yang beneran free.
     * 5. Masukin hasil saringannya ke dropdown menu.
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

                    val availableItems = allItems.filter { !borrowedItems.contains(it) }

                    val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, availableItems)
                    binding.etSelectedItem.setAdapter(adapter)

                    binding.etSelectedItem.isEnabled = true
                    binding.etSelectedItem.hint = "Pick an item"

                    if (availableItems.isEmpty()) {
                        Toast.makeText(this, "Bummer, everything's out!", Toast.LENGTH_LONG).show()
                        binding.etSelectedItem.hint = "Empty catalog"
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Oops, couldn't load loan status", Toast.LENGTH_SHORT).show()
                }
        }.addOnFailureListener {
            Toast.makeText(this, "My bad, couldn't load the catalog", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Fungsi buat nampilin kalender pas input tanggal diklik.
     * 
     * Langkah-langkahnya:
     * 1. Pasang klik listener di input tanggal mulai & selesai.
     * 2. Munculin DatePickerDialog pas diklik.
     * 3. Update teks input-nya sesuai tanggal yang dipilih.
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
     * Fungsi buat nyimpen transaksi pinjaman baru ke Firestore.
     * 
     * Langkah-langkahnya:
     * 1. Ambil semua input dari layar.
     * 2. Pastiin semuanya diisi (kecuali foto opsional).
     * 3. Ubah teks tanggal jadi format Timestamp Firestore.
     * 4. Bungkus datanya jadi HashMap.
     * 5. Kirim datanya ke Firestore koleksi 'borrows'.
     * 6. Tutup halaman kalo sukses, atau kasih tau error-nya.
     */
    private fun saveTransaction() {
        val itemName = binding.etSelectedItem.text.toString().trim()
        val borrowerName = binding.etBorrowerName.text.toString().trim()
        val photoUrl = binding.etPhotoUrl.text.toString().trim()
        val startDateStr = binding.etStartDate.text.toString().trim()
        val endDateStr = binding.etEndDate.text.toString().trim()

        if (itemName.isEmpty() || borrowerName.isEmpty() || startDateStr.isEmpty() || endDateStr.isEmpty()) {
            Toast.makeText(this, "Hey, fill everything in please!", Toast.LENGTH_SHORT).show()
            return
        }

        binding.btnSubmitTransaction.isEnabled = false
        binding.btnSubmitTransaction.text = "Saving it for ya..."

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
                    Toast.makeText(this, "Sweet, it's saved!", Toast.LENGTH_SHORT).show()
                    finish()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Darn, couldn't save: ${e.message}", Toast.LENGTH_SHORT).show()
                    binding.btnSubmitTransaction.isEnabled = true
                    binding.btnSubmitTransaction.text = "Submit Transaction"
                }
        } catch (e: Exception) {
            Toast.makeText(this, "Date format is wonky!", Toast.LENGTH_SHORT).show()
            binding.btnSubmitTransaction.isEnabled = true
            binding.btnSubmitTransaction.text = "Submit Transaction"
        }
    }
}