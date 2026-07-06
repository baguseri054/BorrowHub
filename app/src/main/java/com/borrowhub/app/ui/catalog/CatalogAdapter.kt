package com.borrowhub.app.ui.catalog

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.borrowhub.app.data.Items
import com.borrowhub.app.databinding.ItemCatalogBinding
import com.bumptech.glide.Glide

class CatalogAdapter(private var itemList: List<Items>) :
    RecyclerView.Adapter<CatalogAdapter.CatalogViewHolder>() {

    class CatalogViewHolder(val binding: ItemCatalogBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CatalogViewHolder {
        val binding = ItemCatalogBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CatalogViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CatalogViewHolder, position: Int) {
        val item = itemList[position]
        holder.binding.tvItemName.text = item.name

        // Set Text Status dan Warna Indikator Bulat (isAvailable)
        val dotDrawable = holder.binding.vStatusDot.background as GradientDrawable
        if (item.isAvailable) {
            holder.binding.tvItemStatus.text = "Available"
            holder.binding.tvItemStatus.setTextColor(Color.parseColor("#4CAF50"))
            dotDrawable.setColor(Color.parseColor("#4CAF50"))
        } else {
            holder.binding.tvItemStatus.text = "Borrowed"
            holder.binding.tvItemStatus.setTextColor(Color.parseColor("#D32F2F"))
            dotDrawable.setColor(Color.parseColor("#D32F2F"))
        }

        // Load Gambar Menggunakan Glide
        if (item.imageUrl.isNotEmpty()) {
            Glide.with(holder.itemView.context)
                .load(item.imageUrl)
                .into(holder.binding.ivItemPhoto)
        }
    }

    override fun getItemCount(): Int = itemList.size

    // Fungsi pembantu jika ada pembaruan data secara dinamis
    fun updateData(newItems: List<Items>) {
        this.itemList = newItems
        notifyDataSetChanged()
    }
}