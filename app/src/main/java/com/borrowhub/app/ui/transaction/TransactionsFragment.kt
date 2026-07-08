package com.borrowhub.app.ui.transaction

import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.borrowhub.app.data.Borrow
import com.borrowhub.app.databinding.FragmentTransactionsBinding
import com.borrowhub.app.ui.home.ActiveLoanAdapter
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Fragment Transactions menampilkan daftar riwayat peminjaman yang sudah selesai (Completed).
 */
class TransactionsFragment : Fragment() {

    private var _binding: FragmentTransactionsBinding? = null
    private val binding get() = _binding!!

    private val db = FirebaseFirestore.getInstance()
    private val historyList = mutableListOf<Borrow>()
    private lateinit var historyAdapter: ActiveLoanAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTransactionsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        fetchTransactionHistory()
    }

    private fun setupRecyclerView() {
        // Kita menggunakan kembali ActiveLoanAdapter untuk konsistensi UI.
        historyAdapter = ActiveLoanAdapter(
            loanList = historyList,
            onViewDetailsClick = { loan -> showLoanDetails(loan) },
            onProcessReturnClick = { /* Tombol pengembalian disembunyikan otomatis untuk status Completed */ }
        )

        binding.rvTransactions.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = historyAdapter
        }
    }

    /**
     * Mengambil data transaksi yang statusnya 'Completed'.
     * Tujuannya agar pengguna bisa melihat riwayat barang apa saja yang pernah dipinjam sebelumnya.
     */
    private fun fetchTransactionHistory() {
        db.collection("borrows")
            .whereEqualTo("status", "Completed")
            .get()
            .addOnSuccessListener { result ->
                historyList.clear()
                for (document in result) {
                    val loan = document.toObject(Borrow::class.java)
                    loan.id = document.id
                    historyList.add(loan)
                }
                historyAdapter.notifyDataSetChanged()
            }
            .addOnFailureListener { exception ->
                Log.e("TransactionsFragment", "Failed to fetch transaction history", exception)
            }
    }

    /**
     * Menampilkan detail riwayat transaksi.
     */
    private fun showLoanDetails(loan: Borrow) {
        val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        val startDate = loan.startDate?.toDate()?.let { sdf.format(it) } ?: "-"
        val endDate = loan.endDate?.toDate()?.let { sdf.format(it) } ?: "-"

        AlertDialog.Builder(requireContext())
            .setTitle("Transaction History Detail")
            .setMessage("Item: ${loan.itemName}\nBorrower: ${loan.borrowerName}\nStatus: ${loan.status}\n\nBorrowed Date: $startDate\nReturned Date: $endDate")
            .setPositiveButton("Close", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
