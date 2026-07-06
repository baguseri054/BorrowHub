package com.borrowhub.app

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.borrowhub.app.databinding.ActivityMainBinding
import com.borrowhub.app.ui.catalog.CatalogFragment
import com.borrowhub.app.ui.home.HomeFragment
import com.borrowhub.app.ui.profile.ProfileFragment
import com.borrowhub.app.ui.transaction.TransactionsFragment

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Halaman default pertama kali dibuka: Home
        loadFragment(HomeFragment())

        // Listener klik menu navigasi bawah
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
}