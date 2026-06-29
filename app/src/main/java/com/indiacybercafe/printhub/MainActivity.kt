package com.indiacybercafe.printhub

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.indiacybercafe.printhub.databinding.ActivityMainBinding
import com.indiacybercafe.printhub.utils.GlobalUploadObserver

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var uploadObserver: GlobalUploadObserver
    private val homeFragment = HomeFragment()
    private val ordersFragment = OrdersFragment()
    private val supportFragment = SupportFragment()
    private val profileFragment = ProfileFragment()
    private var activeFragment: Fragment = homeFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        uploadObserver = GlobalUploadObserver(this)
        uploadObserver.startObserving()

        setupNavigation()
    }

    private fun setupNavigation() {
        // Initialize all fragments but only show Home
        supportFragmentManager.beginTransaction().apply {
            add(R.id.fragment_container, profileFragment, "4").hide(profileFragment)
            add(R.id.fragment_container, supportFragment, "3").hide(supportFragment)
            add(R.id.fragment_container, ordersFragment, "2").hide(ordersFragment)
            add(R.id.fragment_container, homeFragment, "1")
        }.commit()

        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    switchFragment(homeFragment)
                    true
                }
                R.id.nav_orders -> {
                    switchFragment(ordersFragment)
                    true
                }
                R.id.nav_support -> {
                    switchFragment(supportFragment)
                    true
                }
                R.id.nav_profile -> {
                    switchFragment(profileFragment)
                    true
                }
                else -> false
            }
        }
    }

    private fun switchFragment(fragment: Fragment) {
        if (fragment != activeFragment) {
            supportFragmentManager.beginTransaction()
                .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
                .hide(activeFragment)
                .show(fragment)
                .commit()
            activeFragment = fragment
        }
    }

    fun selectTab(index: Int) {
        val itemId = when (index) {
            0 -> R.id.nav_home
            1 -> R.id.nav_orders
            2 -> R.id.nav_support
            3 -> R.id.nav_profile
            else -> R.id.nav_home
        }
        binding.bottomNavigation.selectedItemId = itemId
    }
}
