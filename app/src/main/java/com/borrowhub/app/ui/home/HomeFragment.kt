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

    private val originalLoanList = mutableListOf<Borrow>()
    private val loanList = mutableListOf<Borrow>()

    private lateinit var loanAdapter: ActiveLoanAdapter

    /**
     * Fungsi buat bikin tampilan fragment.
     * 
     * Langkah-langkahnya:
     * 1. Inflate layout fragment home pake view binding.
     * 2. Balikin root view-nya biar ditampilin.
     */
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    /**
     * Fungsi yang dipanggil pas view-nya udah jadi.
     * 
     * Langkah-langkahnya:
     * 1. Setup recycler view buat daftar barang yang dipinjem.
     * 2. Ambil data pinjaman yang aktif dari Firestore.
     * 3. Aktifin fitur search.
     * 4. Pasang klik listener buat tombol nambah pinjaman baru.
     */
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

    /**
     * Fungsi buat nyiapin daftar (RecyclerView).
     * 
     * Langkah-langkahnya:
     * 1. Inisialisasi adapter buat daftar pinjaman.
     * 2. Kasih tau apa yang harus dilakuin pas tombol detail atau balik dipencet.
     * 3. Set layout manager sama adapter ke RecyclerView.
     */
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
     * Fungsi buat nyiapin fitur pencarian.
     * 
     * Langkah-langkahnya:
     * 1. Pasang text watcher biar pencarian jalan otomatis pas ngetik.
     * 2. Pasang editor action biar keyboard sembunyi pas teken tombol cari.
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
     * Fungsi buat nyaring data pinjaman sesuai yang diketik user.
     * 
     * Langkah-langkahnya:
     * 1. Kosongin list tampilan sekarang.
     * 2. Kalo query kosong, tampilin semua data asli.
     * 3. Kalo ada isinya, cari data yang nama barang atau peminjamnya cocok.
     * 4. Update tampilan daftarnya.
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
     * Fungsi buat nunjukin detail pinjaman lewat pop-up.
     * 
     * Langkah-langkahnya:
     * 1. Format tanggal biar enak dibaca.
     * 2. Bikin layout pop-up secara manual.
     * 3. Isi teks detail barang sama fotonya kalo ada.
     * 4. Tampilin pop-up-nya pake AlertDialog.
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
     * Fungsi buat proses pengembalian barang.
     * 
     * Langkah-langkahnya:
     * 1. Munculin konfirmasi apakah beneran mau dikembaliin.
     * 2. Kalo user yakin, update status di Firestore jadi 'Completed'.
     * 3. Kasih tau hasilnya sukses atau gagal lewat Toast.
     * 4. Refresh daftar pinjaman kalo sukses.
     */
    private fun processReturn(loan: Borrow) {
        AlertDialog.Builder(requireContext())
            .setTitle("Ready to return?")
            .setMessage("Should we mark '${loan.itemName}' as returned?")
            .setPositiveButton("Yup, done!", { _, _ ->
                db.collection("borrows").document(loan.id)
                    .update("status", "Completed")
                    .addOnSuccessListener {
                        Toast.makeText(requireContext(), "Awesome, item returned!", Toast.LENGTH_SHORT).show()
                        fetchActiveLoans()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(requireContext(), "Ouch, failed to process: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            })
            .setNegativeButton("Not yet", null)
            .show()
    }

    /**
     * Fungsi buat ambil data pinjaman dari Firestore.
     * 
     * Langkah-langkahnya:
     * 1. Kueri koleksi 'borrows' nyari yang statusnya Active atau Overdue.
     * 2. Kalo dapet datanya, masukin ke list.
     * 3. Update adapter biar datanya muncul di layar.
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
                Log.e("HomeFragment", "Man, failed to load loans", exception)
            }
    }

    /**
     * Fungsi buat ngebersihin view binding pas fragment ancur.
     */
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}