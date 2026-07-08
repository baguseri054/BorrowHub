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
        // Menggunakan ulang ActiveLoanAdapter
        historyAdapter = ActiveLoanAdapter(
            loanList = historyList,
            onViewDetailsClick = { loan -> showLoanDetails(loan) },
            onProcessReturnClick = { /* Tombol ini sudah disembunyikan untuk status Completed */ }
        )

        binding.rvTransactions.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = historyAdapter
        }
    }

    private fun fetchTransactionHistory() {
        // KUNCI: Hanya mengambil data yang statusnya "Completed"
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
                Log.e("TransactionsFragment", "Gagal memuat riwayat", exception)
            }
    }

    private fun showLoanDetails(loan: Borrow) {
        val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        val startDate = loan.startDate?.toDate()?.let { sdf.format(it) } ?: "-"
        val endDate = loan.endDate?.toDate()?.let { sdf.format(it) } ?: "-"

        AlertDialog.Builder(requireContext())
            .setTitle("Detail Riwayat")
            .setMessage("Barang: ${loan.itemName}\nPeminjam: ${loan.borrowerName}\nStatus: ${loan.status}\n\nTanggal Pinjam: $startDate\nTenggat Waktu: $endDate")
            .setPositiveButton("Tutup", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}