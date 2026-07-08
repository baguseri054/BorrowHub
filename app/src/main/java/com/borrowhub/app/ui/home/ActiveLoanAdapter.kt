package com.borrowhub.app.ui.home

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.borrowhub.app.data.Borrow
import com.borrowhub.app.databinding.ItemActiveLoanBinding

class ActiveLoanAdapter(
    private val loanList: List<Borrow>,
    private val onViewDetailsClick: (Borrow) -> Unit,
    private val onProcessReturnClick: (Borrow) -> Unit
) : RecyclerView.Adapter<ActiveLoanAdapter.LoanViewHolder>() {

    inner class LoanViewHolder(val binding: ItemActiveLoanBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LoanViewHolder {
        val binding = ItemActiveLoanBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return LoanViewHolder(binding)
    }

    override fun onBindViewHolder(holder: LoanViewHolder, position: Int) {
        val loan = loanList[position]
        val binding = holder.binding

        binding.tvLoanItemName.text = loan.itemName
        binding.tvBorrowerName.text = "Borrowed by: ${loan.borrowerName}"

        binding.tvDueDate.text = loan.status
        if (loan.status.equals("Overdue", ignoreCase = true)) {
            binding.tvDueDate.setTextColor(Color.parseColor("#D32F2F")) // Merah
        } else {
            binding.tvDueDate.setTextColor(Color.parseColor("#B38F00")) // Kuning
        }

        // Menyambungkan tombol dengan fungsi callback yang dilempar dari Fragment
        binding.btnViewDetails.setOnClickListener {
            onViewDetailsClick(loan)
        }

        binding.btnProcessReturn.setOnClickListener {
            onProcessReturnClick(loan)
        }

        if (loan.status.equals("Completed", ignoreCase = true)) {
            binding.btnProcessReturn.visibility = android.view.View.GONE
        } else {
            binding.btnProcessReturn.visibility = android.view.View.VISIBLE
        }
    }

    override fun getItemCount(): Int = loanList.size
}