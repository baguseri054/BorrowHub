package com.borrowhub.app.ui.catalog

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.borrowhub.app.databinding.ActivityAddItemBinding
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore

/**
 * Activity ini berfungsi untuk mengelola penambahan barang baru ke katalog
 * serta melakukan pembaruan (update) data barang yang sudah ada di Firestore.
 */
class AddItemActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddItemBinding
    private val db = FirebaseFirestore.getInstance()

    // Variabel state untuk menentukan apakah kita sedang menambah barang baru atau mengedit yang lama
    private var isEditMode = false
    private var documentId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddItemBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener { finish() }

        // Mendeteksi mode berdasarkan ada tidaknya ID dokumen yang dikirim lewat Intent
        documentId = intent.getStringExtra("EXTRA_ID")
        isEditMode = documentId != null

        if (isEditMode) {
            // Jika dalam mode edit, kita ubah teks tombol dan isi field input dengan data lama
            binding.btnSimpanBarang.text = "Update Item"

            binding.etItemName.setText(intent.getStringExtra("EXTRA_NAME"))
            binding.etItemDesc.setText(intent.getStringExtra("EXTRA_DESC"))
            binding.etImageUrl.setText(intent.getStringExtra("EXTRA_IMAGE_URL"))
        }

        // Logika tombol simpan: bercabang tergantung mode yang aktif
        binding.btnSimpanBarang.setOnClickListener {
            if (isEditMode) {
                updateItem()
            } else {
                saveNewItem()
            }
        }
    }

    /**
     * Fungsi ini menyimpan data barang baru ke dalam koleksi 'items' di Firestore.
     * Status ketersediaan secara default diset ke 'true' (Available).
     */
    private fun saveNewItem() {
        val imageUrl = binding.etImageUrl.text.toString().trim()
        val itemName = binding.etItemName.text.toString().trim()
        val itemDesc = binding.etItemDesc.text.toString().trim()

        if (itemName.isEmpty()) {
            binding.etItemName.error = "Item name is required"
            return
        }

        // Mencegah klik ganda saat proses upload data berlangsung
        binding.btnSimpanBarang.isEnabled = false
        binding.btnSimpanBarang.text = "Saving..."

        val newItem = hashMapOf(
            "name" to itemName,
            "description" to itemDesc,
            "condition" to "Good",
            "imageUrl" to imageUrl,
            "isAvailable" to true, // Default barang baru adalah tersedia
            "createdAt" to Timestamp.now()
        )

        db.collection("items").add(newItem)
            .addOnSuccessListener {
                Toast.makeText(this, "Item successfully added to catalog!", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error saving data: ${e.message}", Toast.LENGTH_SHORT).show()
                binding.btnSimpanBarang.isEnabled = true
                binding.btnSimpanBarang.text = "Save Item"
            }
    }

    /**
     * Fungsi ini memperbarui dokumen yang sudah ada di Firestore berdasarkan documentId.
     * Menggunakan metode update() agar hanya field tertentu yang berubah (tidak menimpa seluruh dokumen).
     */
    private fun updateItem() {
        val imageUrl = binding.etImageUrl.text.toString().trim()
        val itemName = binding.etItemName.text.toString().trim()
        val itemDesc = binding.etItemDesc.text.toString().trim()

        if (itemName.isEmpty()) {
            binding.etItemName.error = "Item name is required"
            return
        }

        binding.btnSimpanBarang.isEnabled = false
        binding.btnSimpanBarang.text = "Updating..."

        // Menyusun data yang ingin diubah saja
        val updatedData = mapOf(
            "name" to itemName,
            "description" to itemDesc,
            "imageUrl" to imageUrl
        )

        // Menargetkan dokumen spesifik menggunakan ID-nya untuk dilakukan pembaruan
        db.collection("items").document(documentId!!)
            .update(updatedData)
            .addOnSuccessListener {
                Toast.makeText(this, "Item details updated successfully!", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to update item: ${e.message}", Toast.LENGTH_SHORT).show()
                binding.btnSimpanBarang.isEnabled = true
                binding.btnSimpanBarang.text = "Update Item"
            }
    }
}
