package com.borrowhub.app.ui.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.borrowhub.app.MainActivity
import com.borrowhub.app.databinding.ActivityLoginBinding
import com.google.firebase.auth.FirebaseAuth

/**
 * Activity ini menangani proses autentikasi pengguna (Login dan Reset Password).
 * Menggunakan Firebase Authentication sebagai layanan backend untuk verifikasi kredensial.
 */
class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Menggunakan View Binding untuk akses elemen UI yang lebih aman dan ringkas
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Inisialisasi instance Firebase Auth untuk mengelola sesi pengguna
        auth = FirebaseAuth.getInstance()

        // Cek sesi: Jika pengguna sudah login sebelumnya, langsung arahkan ke Dashboard Utama
        if (auth.currentUser != null) {
            navigateToMain()
        }

        // Listener untuk tombol Login
        binding.btnLogin.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

            // Validasi input dasar: memastikan kolom tidak kosong sebelum memproses ke Firebase
            if (email.isNotEmpty() && password.isNotEmpty()) {
                loginUser(email, password)
            } else {
                Toast.makeText(this, "Please enter your email and password.", Toast.LENGTH_SHORT).show()
            }
        }

        // Listener untuk fitur Lupa Password
        binding.btnForgotPassword.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()

            // Validasi: Pengguna harus memasukkan email terlebih dahulu sebelum meminta reset link
            if (email.isEmpty()) {
                binding.etEmail.error = "Please enter your email address first."
                binding.etEmail.requestFocus()
                return@setOnClickListener
            }

            resetPassword(email)
        }
    }

    /**
     * Fungsi ini memproses autentikasi ke Firebase menggunakan email dan password.
     * Tujuannya agar kita bisa memvalidasi apakah pengguna terdaftar secara resmi di database.
     */
    private fun loginUser(email: String, pass: String) {
        auth.signInWithEmailAndPassword(email, pass)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Login berhasil, arahkan ke halaman utama
                    navigateToMain()
                } else {
                    // Login gagal, tampilkan pesan error dari Firebase (misalnya: salah password atau user tidak ditemukan)
                    Toast.makeText(this, "Login failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }

    /**
     * Fungsi ini mengirimkan email pemulihan kata sandi melalui Firebase.
     * Metodenya adalah memicu server Firebase untuk mengirimkan link unik ke email pengguna.
     */
    private fun resetPassword(email: String) {
        auth.sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // Berhasil memicu pengiriman email
                    Toast.makeText(this, "Reset link sent! Please check your email inbox.", Toast.LENGTH_LONG).show()
                } else {
                    // Gagal mengirim email, biasanya karena email tidak terdaftar
                    Toast.makeText(this, "Failed to send reset email: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }

    /**
     * Fungsi navigasi untuk berpindah ke MainActivity.
     * Kita menggunakan finish() agar Activity login dihapus dari backstack, 
     * sehingga pengguna tidak bisa kembali ke halaman login dengan menekan tombol Back setelah masuk.
     */
    private fun navigateToMain() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }
}
