package com.indiacybercafe.printhub

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.viewpager2.widget.ViewPager2
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.indiacybercafe.printhub.databinding.FragmentHomeBinding

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private lateinit var database: DatabaseReference
    private var auth: FirebaseAuth = FirebaseAuth.getInstance()
    private lateinit var serviceAdapter: ServiceAdapter

    private val sliderHandler = Handler(Looper.getMainLooper())
    private val sliderRunnable = Runnable {
        val currentItem = binding.heroViewPager.currentItem
        val nextItem = if (currentItem == heroImages.size - 1) 0 else currentItem + 1
        binding.heroViewPager.setCurrentItem(nextItem, true)
    }

    private val heroImages = listOf(
        R.drawable.hero_banner_1,
        R.drawable.hero_banner_2
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Apply Safe Area Insets for Home Header
        ViewCompat.setOnApplyWindowInsetsListener(binding.headerLayout) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.updatePadding(top = systemBars.top)
            insets
        }

        database = FirebaseDatabase.getInstance().reference

        setupNotificationBadge()
        setupHeroSection()
        setupServicesGrid()

        binding.btnNotifications.setOnClickListener {
            startActivity(Intent(requireContext(), NotificationsActivity::class.java))
        }
    }

    private fun setupHeroSection() {
        val heroAdapter = BannerAdapter(heroImages)
        binding.heroViewPager.adapter = heroAdapter

        binding.heroViewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                sliderHandler.removeCallbacks(sliderRunnable)
                sliderHandler.postDelayed(sliderRunnable, 5000)
            }
        })
    }

    private fun setupServicesGrid() {
        val localServices = ServiceRepository.getLocalServices()
        serviceAdapter = ServiceAdapter(localServices) { service ->
            handleServiceClick(service)
        }
        
        binding.rvServices.apply {
            layoutManager = GridLayoutManager(requireContext(), 4)
            setHasFixedSize(true)
            adapter = serviceAdapter
        }
    }

    private fun handleServiceClick(service: ServiceModel) {
        when (service.action) {
            "all_files" -> {
                startActivity(Intent(requireContext(), AllFilesActivity::class.java))
            }
            "camera", "pdf", "gallery", "doc", "xls", "ppt", "id_card" -> {
                val intent = Intent(requireContext(), ComingSoonActivity::class.java)
                intent.putExtra("title", service.title)
                startActivity(intent)
            }
            else -> {}
        }
    }

    private fun setupNotificationBadge() {
        val uid = auth.currentUser?.uid ?: return
        val notificationsRef = database.child("notifications").child(uid)

        notificationsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                var unreadCount = 0
                for (data in snapshot.children) {
                    val isRead = data.child("isRead").getValue(Boolean::class.java) ?: true
                    if (!isRead) {
                        unreadCount++
                    }
                }
                binding.unreadDot.visibility = if (unreadCount > 0) View.VISIBLE else View.GONE
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    override fun onPause() {
        super.onPause()
        sliderHandler.removeCallbacks(sliderRunnable)
    }

    override fun onResume() {
        super.onResume()
        sliderHandler.postDelayed(sliderRunnable, 5000)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
