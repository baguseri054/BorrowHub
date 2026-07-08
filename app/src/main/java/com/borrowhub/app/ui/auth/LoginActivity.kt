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

    /**
     * Fungsi yang dipanggil pas activity pertama kali dibuat.
     * 
     * Langkah-langkahnya:
     * 1. Setup view binding buat akses UI.
     * 2. Inisialisasi Firebase Auth biar bisa login.
     * 3. Cek kalo user udah login, langsung lempar ke home.
     * 4. Pasang klik listener buat tombol login sama reset password.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Siapin Firebase Auth-nya
        auth = FirebaseAuth.getInstance()

        // Kalo emang udah login, gas langsung ke MainActivity
        if (auth.currentUser != null) {
            navigateToMain()
        }

        // Kalo tombol login diklik
        binding.btnLogin.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

            // Pastiin email sama password nggak kosong
            if (email.isNotEmpty() && password.isNotEmpty()) {
                loginUser(email, password)
            } else {
                // Kasih tau user kalo ada yang kosong
                Toast.makeText(this, "Hey, don't leave your email and password empty!", Toast.LENGTH_SHORT).show()
            }
        }

        // Kalo tombol lupa password diklik
        binding.btnForgotPassword.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()

            // Validasi: pastiin email diisi dulu sebelum minta reset
            if (email.isEmpty()) {
                binding.etEmail.error = "Put your email here first, buddy!"
                binding.etEmail.requestFocus() // Fokusin kursor ke input email
                return@setOnClickListener
            }

            resetPassword(email)
        }
    }

    /**
     * Fungsi buat proses login user pake Firebase.
     * 
     * Langkah-langkahnya:
     * 1. Panggil fungsi Firebase buat sign in pake email & password.
     * 2. Kalo sukses, pindah ke MainActivity.
     * 3. Kalo gagal, tampilin pesan error-nya.
     */
    private fun loginUser(email: String, pass: String) {
        auth.signInWithEmailAndPassword(email, pass)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    navigateToMain()
                } else {
                    // Kasih tau kalo login-nya gagal
                    Toast.makeText(this, "Oops, login failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }

    /**
     * Fungsi buat ngirim email reset password.
     * 
     * Langkah-langkahnya:
     * 1. Panggil Firebase buat kirim email reset password.
     * 2. Kalo sukses, kasih tau user link-nya udah dikirim.
     * 3. Kalo gagal, tampilin error-nya kenapa.
     */
    private fun resetPassword(email: String) {
        auth.sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // Berhasil kirim email reset
                    Toast.makeText(this, "Sent! Check your email at $email for the reset link.", Toast.LENGTH_LONG).show()
                } else {
                    // Gagal kirim email reset
                    Toast.makeText(this, "My bad, couldn't send the email: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }

    /**
     * Fungsi buat pindah halaman ke MainActivity.
     * 
     * Langkah-langkahnya:
     * 1. Buat intent buat buka MainActivity.
     * 2. Jalanin intent-nya.
     * 3. Tutup activity login biar nggak bisa balik lagi kalo dipencet back.
     */
    private fun navigateToMain() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish() // Matiin activity ini
    }
}