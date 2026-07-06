package com.borrowhub.app.ui.catalog

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import com.borrowhub.app.data.Items
import com.borrowhub.app.databinding.FragmentCatalogBinding
import com.google.firebase.firestore.FirebaseFirestore

class CatalogFragment : Fragment() {

    private var _binding: FragmentCatalogBinding? = null
    private val binding get() = _binding!!

    private lateinit var firestore: FirebaseFirestore
    private lateinit var catalogAdapter: CatalogAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Menggunakan ViewBinding untuk inflasi layout fragment_catalog.xml
        _binding = FragmentCatalogBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Inisialisasi Firebase Instance
        firestore = FirebaseFirestore.getInstance()

        setupRecyclerView()
        fetchCatalogData()

        // Menangani aksi klik FAB untuk membuka halaman form input tambah barang baru
        binding.fabAddItem.setOnClickListener {
            startActivity(Intent(requireContext(), AddItemActivity::class.java))
        }
    }

    private fun setupRecyclerView() {
        catalogAdapter = CatalogAdapter(emptyList())
        binding.rvCatalog.apply {
            // Mengatur tampilan Grid menjadi 2 kolom sesuai dengan visual mockup catalog
            layoutManager = GridLayoutManager(context, 2)
            adapter = catalogAdapter
        }
    }

    private fun fetchCatalogData() {
        // Melakukan Realtime Listening ke Koleksi "Items" di Firestore (DFD Proses 2.2)
        firestore.collection("Items")
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    Toast.makeText(context, "Gagal memuat katalog: ${error.message}", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                val itemsList = mutableListOf<Items>()
                if (snapshots != null) {
                    for (document in snapshots) {
                        // Mapping dokumen Firestore menjadi objek data class Items
                        val item = document.toObject(Items::class.java)
                        item.id = document.id
                        itemsList.add(item)
                    }
                }
                // Memperbarui data pada adapter secara real-time
                catalogAdapter.updateData(itemsList)
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Menghindari memory leak dengan menetapkan backing property binding ke null
        _binding = null
    }
}