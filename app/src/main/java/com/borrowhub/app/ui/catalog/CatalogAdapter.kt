package com.borrowhub.app.ui.catalog

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.borrowhub.app.data.Items
import com.borrowhub.app.databinding.ItemCatalogBinding
import com.bumptech.glide.Glide

class CatalogAdapter(
    private val itemList: List<Items>,
    private val onItemClick: (Items) -> Unit,       // Callback untuk Klik Biasa (Detail)
    private val onItemLongClick: (Items) -> Unit    // Callback untuk Klik Tahan (Hapus)
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

        if (item.isAvailable) {
            binding.tvItemStatus.text = "Available"
            binding.tvItemStatus.setTextColor(Color.parseColor("#2E7D32")) // Warna Hijau
            binding.vStatusDot.setCardBackgroundColor(Color.parseColor("#2E7D32"))
        } else {
            binding.tvItemStatus.text = "Borrowed"
            binding.tvItemStatus.setTextColor(Color.parseColor("#C62828")) // Warna Merah
            binding.vStatusDot.setCardBackgroundColor(Color.parseColor("#C62828"))
        }

        Glide.with(binding.ivItemPhoto.context)
            .load(item.imageUrl)
            .centerCrop()
            .placeholder(android.R.drawable.ic_menu_gallery)
            .into(binding.ivItemPhoto)

        // Memicu aksi saat kartu diklik biasa
        binding.root.setOnClickListener {
            onItemClick(item)
        }

        // Memicu aksi saat kartu ditekan tahan
        binding.root.setOnLongClickListener {
            onItemLongClick(item)
            true
        }
    }

    override fun getItemCount(): Int = itemList.size
}