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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 1. Meminta izin notifikasi dan mengatur jadwal pengecekan latar belakang
        requestNotificationPermission()
        scheduleDailyReminders()

        // 2. Halaman default pertama kali dibuka: Home
        loadFragment(HomeFragment())

        // 3. Listener klik menu navigasi bawah
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

    // Fungsi pembantu untuk menukar fragment di FrameLayout
    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }

    // Fungsi untuk meminta izin memunculkan notifikasi (Wajib untuk Android 13+)
    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 101)
            }
        }
    }

    // Fungsi untuk menginstruksikan Worker mengecek Firestore setiap 24 jam sekali
    private fun scheduleDailyReminders() {
        val workRequest = PeriodicWorkRequestBuilder<ReminderWorker>(24, TimeUnit.HOURS)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "BorrowHubDailyReminder",
            ExistingPeriodicWorkPolicy.KEEP, // KEEP = Jangan tindih jadwal yang sudah ada
            workRequest
        )
    }
}