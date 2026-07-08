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

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val db = FirebaseFirestore.getInstance()

    // originalLoanList menyimpan data utuh dari Firestore untuk acuan pencarian
    private val originalLoanList = mutableListOf<Borrow>()
    // loanList adalah data yang disuplai ke layar (bisa berkurang/bertambah saat filter aktif)
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
        setupSearch() // Mengaktifkan fitur pencarian

        binding.btnNewLoan.setOnClickListener {
            val intent = Intent(requireContext(), NewTransactionActivity::class.java)
            startActivity(intent)
        }
    }

    private fun setupRecyclerView() {
        // Memasukkan fungsi logika untuk kedua tombol di adapter
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

    // Fungsi memantau ketikan di kolom pencarian dan tombol di keyboard
    private fun setupSearch() {
        // 1. Logika Pencarian Real-time (Otomatis menyaring saat Anda mengetik)
        binding.etSearchLoan.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                val query = s.toString().trim().lowercase(Locale.getDefault())
                filterLoans(query)
            }
        })

        // 2. Logika Tombol Search pada Keyboard HP (Menyembunyikan keyboard saat ditekan)
        binding.etSearchLoan.setOnEditorActionListener { v, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                // Menurunkan (hide) keyboard
                val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(v.windowToken, 0)

                // Menghilangkan fokus (kursor) dari kolom pencarian agar layar bersih
                binding.etSearchLoan.clearFocus()
                true
            } else {
                false
            }
        }
    }

    // Fungsi menyaring data
    private fun filterLoans(query: String) {
        loanList.clear()
        if (query.isEmpty()) {
            // Jika pencarian kosong, kembalikan semua data asli
            loanList.addAll(originalLoanList)
        } else {
            // Jika ada teks, saring berdasarkan nama barang atau peminjam
            for (loan in originalLoanList) {
                val matchItemName = loan.itemName.lowercase(Locale.getDefault()).contains(query)
                val matchBorrower = loan.borrowerName.lowercase(Locale.getDefault()).contains(query)

                if (matchItemName || matchBorrower) {
                    loanList.add(loan)
                }
            }
        }
        loanAdapter.notifyDataSetChanged() // Perbarui tampilan
    }

    // Fungsi 1: Menampilkan Detail (Diperbarui dengan Foto)
    private fun showLoanDetails(loan: Borrow) {
        val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        val startDate = loan.startDate?.toDate()?.let { sdf.format(it) } ?: "-"
        val endDate = loan.endDate?.toDate()?.let { sdf.format(it) } ?: "-"

        // Membuat tata letak (layout) secara terprogram untuk pop-up
        val layout = android.widget.LinearLayout(requireContext())
        layout.orientation = android.widget.LinearLayout.VERTICAL
        layout.setPadding(60, 40, 60, 0)

        // Membuat elemen teks
        val textDetails = android.widget.TextView(requireContext())
        textDetails.text = "Barang: ${loan.itemName}\nPeminjam: ${loan.borrowerName}\nStatus: ${loan.status}\n\nTanggal Pinjam: $startDate\nTenggat Waktu: $endDate\n\nFoto Kondisi:"
        textDetails.setTextColor(android.graphics.Color.BLACK)
        layout.addView(textDetails)

        // Membuat elemen gambar menggunakan Glide jika tautan foto tersedia
        if (loan.photoBefore.isNotEmpty()) {
            val imageView = android.widget.ImageView(requireContext())
            val params = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                500 // Tinggi gambar dalam piksel
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

        // Memasukkan layout tadi ke dalam AlertDialog
        AlertDialog.Builder(requireContext())
            .setTitle("Detail Peminjaman")
            .setView(layout)
            .setPositiveButton("Tutup", null)
            .show()
    }

    // Fungsi 2: Memproses Pengembalian (Update Firestore)
    private fun processReturn(loan: Borrow) {
        AlertDialog.Builder(requireContext())
            .setTitle("Konfirmasi Pengembalian")
            .setMessage("Tandai '${loan.itemName}' sebagai sudah dikembalikan?")
            .setPositiveButton("Ya, Proses") { _, _ ->
                // Mengubah field "status" menjadi "Completed"
                db.collection("borrows").document(loan.id)
                    .update("status", "Completed")
                    .addOnSuccessListener {
                        Toast.makeText(requireContext(), "Pengembalian berhasil", Toast.LENGTH_SHORT).show()
                        fetchActiveLoans() // Memanggil ulang data untuk menyegarkan daftar
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(requireContext(), "Gagal memproses: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
            .setNegativeButton("Batal", null)
            .show()
    }

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
                Log.e("HomeFragment", "Gagal memuat pinjaman", exception)
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}