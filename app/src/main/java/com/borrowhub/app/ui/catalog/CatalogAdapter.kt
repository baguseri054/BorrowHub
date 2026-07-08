package com.borrowhub.app.ui.catalog

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.borrowhub.app.data.Items
import com.borrowhub.app.databinding.ItemCatalogBinding
import com.bumptech.glide.Glide

/**
 * Adapter untuk menampilkan daftar barang dalam bentuk Grid di halaman Katalog.
 * Menangani logika tampilan visual berdasarkan status ketersediaan barang.
 */
class CatalogAdapter(
    private val itemList: List<Items>,
    private val onItemClick: (Items) -> Unit,       // Callback untuk melihat detail barang
    private val onItemLongClick: (Items) -> Unit    // Callback untuk memicu dialog hapus
) : RecyclerView.Adapter<CatalogAdapter.CatalogViewHolder>() {

    inner class CatalogViewHolder(val binding: ItemCatalogBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CatalogViewHolder {
        val binding = ItemCatalogBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CatalogViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CatalogViewHolder, position: Int) {
        val item = itemList[position]
        val binding = holder.binding

        binding.tvItemName.text = item.name

        // Logika Status: Mengubah teks dan warna indikator berdasarkan ketersediaan (isAvailable)
        // Hal ini bertujuan agar pengguna bisa langsung membedakan barang yang bisa dipinjam dan yang tidak.
        if (item.isAvailable) {
            binding.tvItemStatus.text = "Available"
            binding.tvItemStatus.setTextColor(Color.parseColor("#2E7D32")) // Hijau tua untuk kesan aman/tersedia
            binding.vStatusDot.setCardBackgroundColor(Color.parseColor("#2E7D32"))
        } else {
            binding.tvItemStatus.text = "Borrowed"
            binding.tvItemStatus.setTextColor(Color.parseColor("#C62828")) // Merah untuk kesan sibuk/terpakai
            binding.vStatusDot.setCardBackgroundColor(Color.parseColor("#C62828"))
        }

        // Memuat gambar dari URL menggunakan Glide agar proses asinkron tidak menghambat performa UI
        Glide.with(binding.ivItemPhoto.context)
            .load(item.imageUrl)
            .centerCrop()
            .placeholder(android.R.drawable.ic_menu_gallery)
            .into(binding.ivItemPhoto)

        // Listener untuk klik biasa pada item
        binding.root.setOnClickListener {
            onItemClick(item)
        }

        // Listener untuk klik tahan (long press) untuk menghapus data
        binding.root.setOnLongClickListener {
            onItemLongClick(item)
            true
        }
    }

    override fun getItemCount(): Int = itemList.size
}
