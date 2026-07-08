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

/**
 * Fragment ini menampilkan seluruh koleksi barang yang ada di gudang/katalog.
 * Fitur utama: Menampilkan daftar barang, melihat detail, mengedit, menghapus,
 * dan menentukan ketersediaan barang secara otomatis berdasarkan data transaksi pinjaman.
 */
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

        // FAB (Floating Action Button) untuk navigasi ke halaman tambah barang baru
        binding.fabAddItem.setOnClickListener {
            val intent = Intent(requireContext(), AddItemActivity::class.java)
            startActivity(intent)
        }
    }

    /**
     * Inisialisasi daftar katalog menggunakan GridLayout agar tampilan terlihat lebih modern.
     */
    private fun setupRecyclerView() {
        catalogAdapter = CatalogAdapter(
            itemList = itemList,
            onItemClick = { item -> showItemDetails(item) },
            onItemLongClick = { itemToDelete -> showDeleteDialog(itemToDelete) }
        )
        binding.rvCatalog.apply {
            layoutManager = GridLayoutManager(requireContext(), 2)
            adapter = catalogAdapter
        }
    }

    /**
     * Menampilkan dialog detail barang. 
     * Selain info teks, kita juga menampilkan gambar barang menggunakan Glide di dalam dialog.
     */
    private fun showItemDetails(item: Items) {
        val layout = android.widget.LinearLayout(requireContext())
        layout.orientation = android.widget.LinearLayout.VERTICAL
        layout.setPadding(60, 40, 60, 0)

        val textDetails = android.widget.TextView(requireContext())
        val statusText = if (item.isAvailable) "Available" else "Borrowed"
        textDetails.text = "Item Name: ${item.name}\nStatus: $statusText\nDescription: ${item.description}\n\nItem Photo:"
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

        AlertDialog.Builder(requireContext())
            .setTitle("Catalog Detail")
            .setView(layout)
            .setPositiveButton("Close", null)
            .setNeutralButton("Edit") { _, _ ->
                // Navigasi ke AddItemActivity dengan membawa data yang sudah ada untuk diedit
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

    /**
     * Dialog konfirmasi sebelum menghapus data dari Firestore untuk mencegah kesalahan hapus.
     */
    private fun showDeleteDialog(item: Items) {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Item")
            .setMessage("Are you sure you want to delete '${item.name}' from the catalog?")
            .setPositiveButton("Delete") { _, _ ->
                deleteItemFromFirestore(item)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    /**
     * Proses penghapusan dokumen secara permanen di Firestore berdasarkan ID dokumen.
     */
    private fun deleteItemFromFirestore(item: Items) {
        db.collection("items").document(item.id)
            .delete()
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Item has been removed from catalog.", Toast.LENGTH_SHORT).show()
                fetchCatalogData() // Refresh data setelah berhasil dihapus
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Error deleting item: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    /**
     * Logika Inti: Sinkronisasi status barang.
     * 1. Mengambil seluruh daftar barang.
     * 2. Mengambil seluruh transaksi pinjaman yang statusnya 'Active' atau 'Overdue'.
     * 3. Jika nama barang ada di daftar pinjaman aktif, maka statusnya diset 'Borrowed'.
     * Hal ini menjamin status 'Available'/'Borrowed' selalu akurat secara real-time.
     */
    private fun fetchCatalogData() {
        db.collection("items").get().addOnSuccessListener { itemResult ->
            val tempItems = mutableListOf<Items>()
            for (document in itemResult) {
                val item = document.toObject(Items::class.java)
                item.id = document.id
                tempItems.add(item)
            }

            // Memeriksa transaksi pinjaman yang masih berjalan
            db.collection("borrows")
                .whereIn("status", listOf("Active", "Overdue"))
                .get()
                .addOnSuccessListener { borrowResult ->
                    val activeBorrowedItemNames = mutableListOf<String>()
                    for (doc in borrowResult) {
                        val borrow = doc.toObject(Borrow::class.java)
                        activeBorrowedItemNames.add(borrow.itemName)
                    }

                    itemList.clear()
                    for (item in tempItems) {
                        // Jika nama barang ditemukan di daftar pinjaman aktif, tandai sebagai tidak tersedia
                        val isCurrentlyBorrowed = activeBorrowedItemNames.contains(item.name)
                        val updatedItem = item.copy(isAvailable = !isCurrentlyBorrowed)

                        itemList.add(updatedItem)
                    }

                    catalogAdapter.notifyDataSetChanged()
                }
                .addOnFailureListener { e ->
                    Log.e("CatalogFragment", "Failed to cross-check loans", e)
                }
        }
            .addOnFailureListener { e ->
                Log.e("CatalogFragment", "Failed to fetch catalog items", e)
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
