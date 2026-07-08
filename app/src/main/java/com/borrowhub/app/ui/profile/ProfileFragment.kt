package com.borrowhub.app.ui.profile

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.fragment.app.Fragment
import com.borrowhub.app.databinding.FragmentProfileBinding
import com.borrowhub.app.ui.auth.LoginActivity
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth

/**
 * Fragment Profile digunakan untuk mengelola akun pengguna dan pengaturan notifikasi.
 * Di sini kita menggunakan SharedPreferences untuk menyimpan preferensi lokal secara persisten.
 */
class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private lateinit var sharedPrefs: SharedPreferences

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Memuat avatar profil menggunakan Glide.
        Glide.with(this)
            .load("https://images.unsplash.com/photo-1578306789121-6571b7b4d1b8?q=80&w=200&auto=format&fit=crop")
            .circleCrop()
            .into(binding.ivAvatar)

        // Fitur Test Notifikasi untuk mendemonstrasikan kapabilitas push notification aplikasi.
        binding.btnEditProfile.setOnClickListener {
            triggerTestNotification()
        }

        // Inisialisasi SharedPreferences untuk menyimpan konfigurasi reminder.
        sharedPrefs = requireContext().getSharedPreferences("BorrowHubPrefs", Context.MODE_PRIVATE)

        // Sinkronisasi status switch dengan data yang tersimpan di memori internal.
        binding.switchH1.isChecked = sharedPrefs.getBoolean("PREF_H1_REMINDER", true)
        binding.switchHariH.isChecked = sharedPrefs.getBoolean("PREF_HARI_H_REMINDER", true)

        binding.switchH1.setOnCheckedChangeListener { _, isChecked ->
            sharedPrefs.edit().putBoolean("PREF_H1_REMINDER", isChecked).apply()
        }

        binding.switchHariH.setOnCheckedChangeListener { _, isChecked ->
            sharedPrefs.edit().putBoolean("PREF_HARI_H_REMINDER", isChecked).apply()
        }

        // Proses Logout: Menghapus sesi Firebase dan mengarahkan kembali ke halaman Login.
        binding.btnLogout.setOnClickListener {
            FirebaseAuth.getInstance().signOut()

            val intent = Intent(requireContext(), LoginActivity::class.java)
            // Menghapus tumpukan activity agar pengguna tidak bisa kembali ke profil setelah logout.
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }
    }

    /**
     * Mengirimkan notifikasi lokal untuk keperluan testing.
     * Penting: Sejak Android Oreo (API 26), pembuatan Notification Channel adalah syarat wajib
     * agar notifikasi bisa muncul di layar pengguna.
     */
    private fun triggerTestNotification() {
        val channelId = "borrowhub_test_channel"
        val notificationManager = requireContext().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Membuat Channel khusus untuk Android versi Oreo ke atas.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "System Test",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        val builder = NotificationCompat.Builder(requireContext(), channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("BorrowHub Test Notification")
            .setContentText("Success! Your notification system is working perfectly.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        notificationManager.notify(999, builder.build())

        Toast.makeText(requireContext(), "Test notification sent! Check your device.", Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
