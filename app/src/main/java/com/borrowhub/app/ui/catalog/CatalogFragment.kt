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

    /**
     * Fungsi buat bikin tampilan fragment katalog.
     */
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCatalogBinding.inflate(inflater, container, false)
        return binding.root
    }

    /**
     * Fungsi yang jalan pas view udah siap.
     * 
     * Langkah-langkahnya:
     * 1. Setup recycler view buat nampilin katalog barang.
     * 2. Ambil data katalog dari Firestore.
     * 3. Pasang listener di tombol tambah barang.
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        fetchCatalogData()

        binding.fabAddItem.setOnClickListener {
            val intent = Intent(requireContext(), AddItemActivity::class.java)
            startActivity(intent)
        }
    }

    /**
     * Fungsi buat nyiapin daftar katalog (RecyclerView).
     * 
     * Langkah-langkahnya:
     * 1. Inisialisasi adapter buat katalog.
     * 2. Atur apa yang terjadi pas barang diklik atau diklik lama (hapus).
     * 3. Set layout jadi grid (2 kolom) dan pasang adapternya.
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
     * Fungsi buat nampilin detail barang di pop-up.
     * 
     * Langkah-langkahnya:
     * 1. Bikin layout pop-up secara manual.
     * 2. Isi teks detail barang (nama, status, deskripsi).
     * 3. Tampilin foto barang kalo ada.
     * 4. Munculin AlertDialog dengan tombol Tutup sama Edit.
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

        android.app.AlertDialog.Builder(requireContext())
            .setTitle("Catalog Detail")
            .setView(layout)
            .setPositiveButton("Close", null)
            .setNeutralButton("Edit") { _, _ ->
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
     * Fungsi buat nunjukin dialog konfirmasi hapus barang.
     */
    private fun showDeleteDialog(item: Items) {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Item")
            .setMessage("You sure you wanna delete '${item.name}' from the catalog?")
            .setPositiveButton("Delete") { _, _ ->
                deleteItemFromFirestore(item)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    /**
     * Fungsi buat hapus barang dari Firestore.
     * 
     * Langkah-langkahnya:
     * 1. Hapus dokumen barang di koleksi 'items'.
     * 2. Kalo sukses, kasih tau user dan refresh data katalog.
     * 3. Kalo gagal, kasih tau error-nya.
     */
    private fun deleteItemFromFirestore(item: Items) {
        db.collection("items").document(item.id)
            .delete()
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Boom, item deleted!", Toast.LENGTH_SHORT).show()
                fetchCatalogData()
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Snap, couldn't delete: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    /**
     * Fungsi buat ambil data katalog dan ngecek ketersediaan barang.
     * 
     * Langkah-langkahnya:
     * 1. Ambil semua list barang dari Firestore.
     * 2. Ambil data pinjaman yang lagi aktif.
     * 3. Bandingin: kalo barang ada di daftar pinjaman aktif, set statusnya jadi 'Borrowed'.
     * 4. Update tampilan katalog.
     */
    private fun fetchCatalogData() {
        db.collection("items").get().addOnSuccessListener { itemResult ->
            val tempItems = mutableListOf<Items>()
            for (document in itemResult) {
                val item = document.toObject(Items::class.java)
                item.id = document.id
                tempItems.add(item)
            }

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
                        val isCurrentlyBorrowed = activeBorrowedItemNames.contains(item.name)
                        val updatedItem = item.copy(isAvailable = !isCurrentlyBorrowed)

                        itemList.add(updatedItem)
                    }

                    catalogAdapter.notifyDataSetChanged()
                }
                .addOnFailureListener { e ->
                    Log.e("CatalogFragment", "Ugh, failed to load cross-loans", e)
                }
        }
            .addOnFailureListener { e ->
                Log.e("CatalogFragment", "Man, failed to grab the catalog", e)
            }
    }

    /**
     * Fungsi buat beresin view binding.
     */
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}