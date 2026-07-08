package com.borrowhub.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.borrowhub.app.databinding.ActivityMainBinding
import com.borrowhub.app.ui.catalog.CatalogFragment
import com.borrowhub.app.ui.home.HomeFragment
import com.borrowhub.app.ui.profile.ProfileFragment
import com.borrowhub.app.ui.transaction.TransactionsFragment
import com.borrowhub.app.worker.ReminderWorker
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    /**
     * Fungsi utama pas activity ini dibuat.
     * 
     * Langkah-langkahnya:
     * 1. Siapin view binding buat nampilin layout.
     * 2. Minta izin notifikasi (penting buat Android versi baru).
     * 3. Atur jadwal reminder harian biar jalan di background.
     * 4. Tampilin fragment Home sebagai tampilan awal.
     * 5. Atur navigasi bawah biar bisa pindah-pindah halaman.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Izin notif sama jadwalin worker
        requestNotificationPermission()
        scheduleDailyReminders()

        // Buka Home pas pertama kali masuk
        loadFragment(HomeFragment())

        // Atur apa yang terjadi pas menu bawah diklik
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            val fragment: Fragment = when (item.itemId) {
                R.id.nav_home -> HomeFragment()
                R.id.nav_catalog -> CatalogFragment()
                R.id.nav_transactions -> TransactionsFragment()
                R.id.nav_profile -> ProfileFragment()
                else -> HomeFragment()
            }
            loadFragment(fragment)
            true
        }
    }

    /**
     * Fungsi buat ganti-ganti fragment di layar.
     * 
     * Langkah-langkahnya:
     * 1. Mulai transaksi fragment manager.
     * 2. Ganti isi container fragment sama fragment yang baru.
     * 3. Commit transaksinya biar perubahannya keliatan.
     */
    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }

    /**
     * Fungsi buat minta izin notifikasi ke user.
     * 
     * Langkah-langkahnya:
     * 1. Cek kalo Android-nya versi Tiramisu (13) atau lebih baru.
     * 2. Kalo belum dapet izin POST_NOTIFICATIONS, minta izin ke user.
     */
    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 101)
            }
        }
    }

    /**
     * Fungsi buat bikin jadwal reminder harian.
     * 
     * Langkah-langkahnya:
     * 1. Bikin request buat jalanin ReminderWorker tiap 24 jam.
     * 2. Daftarin request-nya ke WorkManager biar dijadwalin sama sistem.
     * 3. Pake policy KEEP biar jadwal yang lama nggak keganggu kalo udah ada.
     */
    private fun scheduleDailyReminders() {
        val workRequest = PeriodicWorkRequestBuilder<ReminderWorker>(24, TimeUnit.HOURS)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "BorrowHubDailyReminder",
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }
}