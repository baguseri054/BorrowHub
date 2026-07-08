package com.borrowhub.app.ui.catalog

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import com.borrowhub.app.data.Borrow
import com.borrowhub.app.data.Items
import com.borrowhub.app.databinding.FragmentCatalogBinding
import com.google.firebase.firestore.FirebaseFirestore

class CatalogFragment : Fragment() {

    private var _binding: FragmentCatalogBinding? = null
    private val binding get() = _binding!!

    private val db = FirebaseFirestore.getInstance()
    private val itemList = mutableListOf<Items>()
    private lateinit var catalogAdapter: CatalogAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCatalogBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        fetchCatalogData()

        binding.fabAddItem.setOnClickListener {
            val intent = Intent(requireContext(), AddItemActivity::class.java)
            startActivity(intent)
        }
    }

    private fun setupRecyclerView() {
        catalogAdapter = CatalogAdapter(
            itemList = itemList,
            onItemClick = { item -> showItemDetails(item) }, // Mengaktifkan klik biasa
            onItemLongClick = { itemToDelete -> showDeleteDialog(itemToDelete) }
        )
        binding.rvCatalog.apply {
            layoutManager = GridLayoutManager(requireContext(), 2)
            adapter = catalogAdapter
        }
    }

    // Fungsi Baru: Menampilkan detail barang beserta fotonya
    private fun showItemDetails(item: Items) {
        val layout = android.widget.LinearLayout(requireContext())
        layout.orientation = android.widget.LinearLayout.VERTICAL
        layout.setPadding(60, 40, 60, 0)

        val textDetails = android.widget.TextView(requireContext())
        val statusText = if (item.isAvailable) "Tersedia" else "Sedang Dipinjam"
        textDetails.text = "Nama Barang: ${item.name}\nStatus: $statusText\nDeskripsi: ${item.description}\n\nFoto Barang:"
        textDetails.setTextColor(android.graphics.Color.BLACK)
        layout.addView(textDetails)

        if (item.imageUrl.isNotEmpty()) {
            val imageView = android.widget.ImageView(requireContext())
            val params = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                500
            )
            params.setMargins(0, 20, 0, 0)
            imageView.layoutParams = params
            imageView.scaleType = android.widget.ImageView.ScaleType.CENTER_CROP

            com.bumptech.glide.Glide.with(requireContext())
                .load(item.imageUrl)
                .placeholder(android.R.drawable.ic_menu_gallery)
                .into(imageView)

            layout.addView(imageView)
        }

// Memasukkan layout tadi ke dalam AlertDialog dan menambahkan tombol Edit
        android.app.AlertDialog.Builder(requireContext())
            .setTitle("Detail Katalog")
            .setView(layout)
            .setPositiveButton("Tutup", null)
            .setNeutralButton("Edit") { _, _ ->
                // Melempar data lama ke AddItemActivity menggunakan Intent
                val intent = Intent(requireContext(), AddItemActivity::class.java).apply {
                    putExtra("EXTRA_ID", item.id)
                    putExtra("EXTRA_NAME", item.name)
                    putExtra("EXTRA_DESC", item.description)
                    putExtra("EXTRA_IMAGE_URL", item.imageUrl)
                }
                startActivity(intent)
            }
            .show()
    }

    private fun showDeleteDialog(item: Items) {
        AlertDialog.Builder(requireContext())
            .setTitle("Hapus Barang")
            .setMessage("Apakah Anda yakin ingin menghapus '${item.name}' dari katalog?")
            .setPositiveButton("Hapus") { _, _ ->
                deleteItemFromFirestore(item)
            }
            .setNegativeButton("Batal", null) // Tutup dialog jika batal
            .show()
    }

    private fun deleteItemFromFirestore(item: Items) {
        db.collection("items").document(item.id)
            .delete()
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Barang berhasil dihapus", Toast.LENGTH_SHORT).show()
                fetchCatalogData() // Panggil ulang data untuk menyegarkan tampilan RecyclerView
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Gagal menghapus: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun fetchCatalogData() {
        // 1. Ambil semua data barang dari Firestore
        db.collection("items").get().addOnSuccessListener { itemResult ->
            val tempItems = mutableListOf<Items>()
            for (document in itemResult) {
                val item = document.toObject(Items::class.java)
                item.id = document.id
                tempItems.add(item)
            }

            // 2. Ambil data peminjaman yang berstatus "Active" atau "Overdue"
            db.collection("borrows")
                .whereIn("status", listOf("Active", "Overdue"))
                .get()
                .addOnSuccessListener { borrowResult ->
                    // Kumpulkan semua nama barang yang sedang dipinjam
                    val activeBorrowedItemNames = mutableListOf<String>()
                    for (doc in borrowResult) {
                        val borrow = doc.toObject(Borrow::class.java)
                        activeBorrowedItemNames.add(borrow.itemName)
                    }

                    // 3. Cocokkan data. Jika barang sedang dipinjam, ubah statusnya menjadi Borrowed
                    itemList.clear()
                    for (item in tempItems) {
                        // Salin objek item dan paksa isAvailable menjadi false jika namanya ada di daftar pinjaman aktif
                        val isCurrentlyBorrowed = activeBorrowedItemNames.contains(item.name)
                        val updatedItem = item.copy(isAvailable = !isCurrentlyBorrowed)

                        itemList.add(updatedItem)
                    }

                    catalogAdapter.notifyDataSetChanged()
                }
                .addOnFailureListener { e ->
                    Log.e("CatalogFragment", "Gagal memuat pinjaman silang", e)
                }
        }
            .addOnFailureListener { e ->
                Log.e("CatalogFragment", "Gagal mengambil katalog", e)
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}