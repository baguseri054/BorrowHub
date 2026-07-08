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

        // Merender gambar estetika ke dalam Avatar Profil menggunakan Glide
        Glide.with(this)
            .load("https://images.unsplash.com/photo-1578306789121-6571b7b4d1b8?q=80&w=200&auto=format&fit=crop")
            .into(binding.ivAvatar)

        // Tombol Edit Profile untuk Simulasi Test Notifikasi
        binding.btnEditProfile.setOnClickListener {
            // Fungsi notifikasi tetap sama, hanya memanggilnya dari tombol baru
            triggerTestNotification()
        }

        // Pengaturan Preferensi
        sharedPrefs = requireContext().getSharedPreferences("BorrowHubPrefs", Context.MODE_PRIVATE)

        binding.switchH1.isChecked = sharedPrefs.getBoolean("PREF_H1_REMINDER", true)
        binding.switchHariH.isChecked = sharedPrefs.getBoolean("PREF_HARI_H_REMINDER", true)

        binding.switchH1.setOnCheckedChangeListener { _, isChecked ->
            sharedPrefs.edit().putBoolean("PREF_H1_REMINDER", isChecked).apply()
        }

        binding.switchHariH.setOnCheckedChangeListener { _, isChecked ->
            sharedPrefs.edit().putBoolean("PREF_HARI_H_REMINDER", isChecked).apply()
        }

        binding.btnLogout.setOnClickListener {
            // End the Firebase session
            FirebaseAuth.getInstance().signOut()

            val intent = Intent(requireContext(), LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }
    }

    // Fungsi Pembantu untuk Menembakkan Notifikasi Uji Coba
    private fun triggerTestNotification() {
        val channelId = "borrowhub_test_channel"
        val notificationManager = requireContext().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Membuat Channel khusus (Wajib untuk Android versi Oreo ke atas)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Uji Coba Sistem",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        // Merakit tampilan notifikasi
        val builder = NotificationCompat.Builder(requireContext(), channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Test Notifikasi BorrowHub")
            .setContentText("Halo! Sistem notifikasi profil Anda berjalan dengan sangat baik.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true) // Notifikasi otomatis hilang saat digeser/diklik

        // Menembakkan notifikasi dengan ID unik (contoh: 999)
        notificationManager.notify(999, builder.build())

        Toast.makeText(requireContext(), "Notifikasi dikirim! Cek bar atas HP Anda.", Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}