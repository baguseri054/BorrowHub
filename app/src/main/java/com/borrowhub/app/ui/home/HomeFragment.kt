package com.borrowhub.app.ui.home

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.borrowhub.app.data.Borrow
import com.borrowhub.app.databinding.FragmentHomeBinding
import com.borrowhub.app.ui.transaction.NewTransactionActivity
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Fragment Home merupakan Dashboard utama aplikasi.
 * Berfungsi untuk menampilkan daftar barang yang sedang dipinjam (Active/Overdue)
 * serta menyediakan fitur pencarian dan proses pengembalian barang.
 */
class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val db = FirebaseFirestore.getInstance()

    // originalLoanList menyimpan data asli dari Firestore untuk keperluan filter/search
    private val originalLoanList = mutableListOf<Borrow>()
    // loanList adalah data yang ditampilkan di RecyclerView (hasil filter)
    private val loanList = mutableListOf<Borrow>()

    private lateinit var loanAdapter: ActiveLoanAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        fetchActiveLoans()
        setupSearch()

        binding.btnNewLoan.setOnClickListener {
            val intent = Intent(requireContext(), NewTransactionActivity::class.java)
            startActivity(intent)
        }
    }

    private fun setupRecyclerView() {
        loanAdapter = ActiveLoanAdapter(
            loanList = loanList,
            onViewDetailsClick = { loan -> showLoanDetails(loan) },
            onProcessReturnClick = { loan -> processReturn(loan) }
        )

        binding.rvActiveLoans.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = loanAdapter
        }
    }

    /**
     * Menyiapkan fitur pencarian real-time.
     * Menggunakan TextWatcher untuk mendeteksi setiap perubahan karakter di kolom search.
     */
    private fun setupSearch() {
        binding.etSearchLoan.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                val query = s.toString().trim().lowercase(Locale.getDefault())
                filterLoans(query)
            }
        })

        // Menutup keyboard secara otomatis saat tombol 'Search' di keyboard ditekan
        binding.etSearchLoan.setOnEditorActionListener { v, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(v.windowToken, 0)
                binding.etSearchLoan.clearFocus()
                true
            } else {
                false
            }
        }
    }

    /**
     * Logika Filter: Menyaring daftar pinjaman berdasarkan Nama Barang atau Nama Peminjam.
     * Tujuannya memudahkan staf mencari data spesifik tanpa harus scroll manual.
     */
    private fun filterLoans(query: String) {
        loanList.clear()
        if (query.isEmpty()) {
            loanList.addAll(originalLoanList)
        } else {
            for (loan in originalLoanList) {
                val matchItemName = loan.itemName.lowercase(Locale.getDefault()).contains(query)
                val matchBorrower = loan.borrowerName.lowercase(Locale.getDefault()).contains(query)

                if (matchItemName || matchBorrower) {
                    loanList.add(loan)
                }
            }
        }
        loanAdapter.notifyDataSetChanged()
    }

    /**
     * Menampilkan detail lengkap pinjaman termasuk foto kondisi barang saat dipinjam (Photo Before).
     */
    private fun showLoanDetails(loan: Borrow) {
        val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        val startDate = loan.startDate?.toDate()?.let { sdf.format(it) } ?: "-"
        val endDate = loan.endDate?.toDate()?.let { sdf.format(it) } ?: "-"

        val layout = android.widget.LinearLayout(requireContext())
        layout.orientation = android.widget.LinearLayout.VERTICAL
        layout.setPadding(60, 40, 60, 0)

        val textDetails = android.widget.TextView(requireContext())
        textDetails.text = "Item: ${loan.itemName}\nBorrower: ${loan.borrowerName}\nStatus: ${loan.status}\n\nBorrowed on: $startDate\nDue by: $endDate\n\nCondition Photo:"
        textDetails.setTextColor(android.graphics.Color.BLACK)
        layout.addView(textDetails)

        if (loan.photoBefore.isNotEmpty()) {
            val imageView = android.widget.ImageView(requireContext())
            val params = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                500
            )
            params.setMargins(0, 20, 0, 0)
            imageView.layoutParams = params
            imageView.scaleType = android.widget.ImageView.ScaleType.CENTER_CROP

            com.bumptech.glide.Glide.with(requireContext())
                .load(loan.photoBefore)
                .placeholder(android.R.drawable.ic_menu_gallery)
                .into(imageView)

            layout.addView(imageView)
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Loan Details")
            .setView(layout)
            .setPositiveButton("Close", null)
            .show()
    }

    /**
     * Fungsi untuk memproses pengembalian barang.
     * Mengubah status transaksi di Firestore menjadi 'Completed'. 
     * Setelah status berubah, barang tersebut otomatis akan muncul kembali sebagai 'Available' di Katalog.
     */
    private fun processReturn(loan: Borrow) {
        AlertDialog.Builder(requireContext())
            .setTitle("Process Return")
            .setMessage("Are you sure you want to mark '${loan.itemName}' as returned?")
            .setPositiveButton("Confirm") { _, _ ->
                db.collection("borrows").document(loan.id)
                    .update("status", "Completed")
                    .addOnSuccessListener {
                        Toast.makeText(requireContext(), "Item returned successfully!", Toast.LENGTH_SHORT).show()
                        fetchActiveLoans()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(requireContext(), "Failed to process return: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    /**
     * Mengambil data pinjaman yang belum selesai (Active/Overdue) dari Firestore.
     */
    private fun fetchActiveLoans() {
        db.collection("borrows")
            .whereIn("status", listOf("Active", "Overdue"))
            .get()
            .addOnSuccessListener { result ->
                originalLoanList.clear()
                loanList.clear()

                for (document in result) {
                    val loan = document.toObject(Borrow::class.java)
                    loan.id = document.id

                    originalLoanList.add(loan)
                    loanList.add(loan)
                }
                loanAdapter.notifyDataSetChanged()
            }
            .addOnFailureListener { exception ->
                Log.e("HomeFragment", "Error loading active loans", exception)
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
