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

        val (statusText, statusColor) = getStatusDisplay(loan)
        binding.tvDueDate.text = statusText
        binding.tvDueDate.setTextColor(statusColor)

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

    private fun getStatusDisplay(loan: Borrow): Pair<String, Int> {
        val calendarNow = java.util.Calendar.getInstance()
        val calendarDue = java.util.Calendar.getInstance().apply {
            time = loan.endDate?.toDate() ?: java.util.Date()
        }

        // Reset time to midnight for day comparison
        calendarNow.set(java.util.Calendar.HOUR_OF_DAY, 0)
        calendarNow.set(java.util.Calendar.MINUTE, 0)
        calendarNow.set(java.util.Calendar.SECOND, 0)
        calendarNow.set(java.util.Calendar.MILLISECOND, 0)

        val midnightDue = java.util.Calendar.getInstance().apply {
            time = calendarDue.time
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }

        val diffMillis = midnightDue.timeInMillis - calendarNow.timeInMillis
        val diffDays = (diffMillis / (1000 * 60 * 60 * 24)).toInt()

        return when {
            diffDays < 0 -> Pair("Overdue", android.graphics.Color.parseColor("#D32F2F"))
            diffDays == 0 -> Pair("Due Today", android.graphics.Color.parseColor("#B38F00"))
            diffDays == 1 -> Pair("Due Tomorrow", android.graphics.Color.parseColor("#B38F00"))
            diffDays > 1 -> Pair("Due in $diffDays days", android.graphics.Color.parseColor("#B38F00"))
            else -> Pair(loan.status, android.graphics.Color.parseColor("#B38F00"))
        }
    }
}
