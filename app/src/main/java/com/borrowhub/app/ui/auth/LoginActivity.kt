package com.borrowhub.app.ui.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.borrowhub.app.MainActivity
import com.borrowhub.app.databinding.ActivityLoginBinding
import com.google.firebase.auth.FirebaseAuth

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Inisialisasi Firebase Auth
        auth = FirebaseAuth.getInstance()

        // Cek jika sudah login, langsung lempar ke MainActivity
        if (auth.currentUser != null) {
            navigateToMain()
        }

        // Eksekusi Tombol Login
        binding.btnLogin.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

            if (email.isNotEmpty() && password.isNotEmpty()) {
                loginUser(email, password)
            } else {
                Toast.makeText(this, "Email dan password tidak boleh kosong", Toast.LENGTH_SHORT).show()
            }
        }

        // Eksekusi Tombol Forgot Password
        binding.btnForgotPassword.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()

            // Validasi: Pastikan kolom email tidak kosong sebelum reset sandi
            if (email.isEmpty()) {
                binding.etEmail.error = "Masukkan email Anda di sini terlebih dahulu"
                binding.etEmail.requestFocus() // Mengarahkan kursor otomatis ke kolom email
                return@setOnClickListener
            }

            resetPassword(email)
        }
    }

    private fun loginUser(email: String, pass: String) {
        auth.signInWithEmailAndPassword(email, pass)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    navigateToMain()
                } else {
                    Toast.makeText(this, "Login gagal: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }

    // Fungsi komunikasi dengan Firebase untuk mengirim email reset sandi
    private fun resetPassword(email: String) {
        auth.sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, "Tautan pemulihan sandi telah dikirim ke $email", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(this, "Gagal mengirim email: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                }
            }
    }

    private fun navigateToMain() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish() // Menutup halaman login agar pengguna tidak bisa kembali menggunakan tombol 'Back'
    }
}