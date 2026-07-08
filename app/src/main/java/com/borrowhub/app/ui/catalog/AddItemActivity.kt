package com.borrowhub.app.ui.catalog

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.borrowhub.app.databinding.ActivityAddItemBinding
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore

class AddItemActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddItemBinding
    private val db = FirebaseFirestore.getInstance()

    // Variabel untuk mendeteksi mode
    private var isEditMode = false
    private var documentId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddItemBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener { finish() }

        // 1. Pengecekan Mode Edit
        documentId = intent.getStringExtra("EXTRA_ID")
        isEditMode = documentId != null

        if (isEditMode) {
            // Mengubah tampilan antarmuka untuk Mode Edit
            binding.btnSimpanBarang.text = "Update Barang"

            // Mengisi otomatis kolom input dengan data lama dari Firestore
            binding.etItemName.setText(intent.getStringExtra("EXTRA_NAME"))
            binding.etItemDesc.setText(intent.getStringExtra("EXTRA_DESC"))
            binding.etImageUrl.setText(intent.getStringExtra("EXTRA_IMAGE_URL"))
        }

        // 2. Eksekusi Tombol Simpan
        binding.btnSimpanBarang.setOnClickListener {
            if (isEditMode) {
                updateItem() // Jalankan fungsi update jika sedang di mode edit
            } else {
                saveNewItem() // Jalankan fungsi tambah baru jika mode normal
            }
        }
    }

    private fun saveNewItem() {
        val imageUrl = binding.etImageUrl.text.toString().trim()
        val itemName = binding.etItemName.text.toString().trim()
        val itemDesc = binding.etItemDesc.text.toString().trim()

        if (itemName.isEmpty()) {
            binding.etItemName.error = "Nama barang wajib diisi"
            return
        }

        binding.btnSimpanBarang.isEnabled = false
        binding.btnSimpanBarang.text = "Menyimpan..."

        val newItem = hashMapOf(
            "name" to itemName,
            "description" to itemDesc,
            "condition" to "Good",
            "imageUrl" to imageUrl,
            "isAvailable" to true,
            "createdAt" to Timestamp.now()
        )

        db.collection("items").add(newItem)
            .addOnSuccessListener {
                Toast.makeText(this, "Barang berhasil ditambahkan!", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Gagal menyimpan data: ${e.message}", Toast.LENGTH_SHORT).show()
                binding.btnSimpanBarang.isEnabled = true
                binding.btnSimpanBarang.text = "Simpan Barang"
            }
    }

    // Fungsi Baru: Memperbarui data yang sudah ada di Firestore
    private fun updateItem() {
        val imageUrl = binding.etImageUrl.text.toString().trim()
        val itemName = binding.etItemName.text.toString().trim()
        val itemDesc = binding.etItemDesc.text.toString().trim()

        if (itemName.isEmpty()) {
            binding.etItemName.error = "Nama barang wajib diisi"
            return
        }

        binding.btnSimpanBarang.isEnabled = false
        binding.btnSimpanBarang.text = "Memperbarui..."

        // Hanya memperbarui field spesifik, tidak menimpa status isAvailable
        val updatedData = mapOf(
            "name" to itemName,
            "description" to itemDesc,
            "imageUrl" to imageUrl
        )

        // Menggunakan perintah .update() yang menyasar ID dokumen spesifik
        db.collection("items").document(documentId!!)
            .update(updatedData)
            .addOnSuccessListener {
                Toast.makeText(this, "Barang berhasil diperbarui!", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Gagal memperbarui: ${e.message}", Toast.LENGTH_SHORT).show()
                binding.btnSimpanBarang.isEnabled = true
                binding.btnSimpanBarang.text = "Update Barang"
            }
    }
}