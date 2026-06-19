package com.indiacybercafe.printhub

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.indiacybercafe.printhub.databinding.ActivityNotificationsBinding

class NotificationsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityNotificationsBinding
    private lateinit var adapter: NotificationsAdapter
    private val notificationList = mutableListOf<NotificationModel>()
    private lateinit var database: DatabaseReference
    private var auth: FirebaseAuth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        binding = ActivityNotificationsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Handle Safe Area Insets for AppBarLayout
        ViewCompat.setOnApplyWindowInsetsListener(binding.appBarLayout) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.updatePadding(top = systemBars.top)
            insets
        }

        // Handle Bottom Navigation Insets for RecyclerView
        ViewCompat.setOnApplyWindowInsetsListener(binding.rvNotifications) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.updatePadding(bottom = systemBars.bottom)
            insets
        }

        database = FirebaseDatabase.getInstance().reference
        setupRecyclerView()
        fetchNotifications()

        // Setup Toolbar back navigation
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun setupRecyclerView() {
        adapter = NotificationsAdapter(notificationList, 
            onNotificationClick = { notification ->
                markAsRead(notification.id)
            },
            onClearClick = { notification ->
                showDeleteConfirmation(notification.id)
            }
        )
        binding.rvNotifications.layoutManager = LinearLayoutManager(this)
        binding.rvNotifications.adapter = adapter
    }

    private fun fetchNotifications() {
        val uid = auth.currentUser?.uid ?: return
        val notificationsRef = database.child("notifications").child(uid)

        notificationsRef.orderByChild("timestamp").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                notificationList.clear()
                for (data in snapshot.children) {
                    val notification = data.getValue(NotificationModel::class.java)
                    notification?.let { 
                        notificationList.add(0, it) 
                    }
                }
                adapter.notifyDataSetChanged()
                updateEmptyState()
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@NotificationsActivity, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun markAsRead(notificationId: String) {
        val uid = auth.currentUser?.uid ?: return
        database.child("notifications").child(uid).child(notificationId).child("isRead").setValue(true)
    }

    private fun showDeleteConfirmation(notificationId: String) {
        AlertDialog.Builder(this)
            .setTitle("Clear Notification")
            .setMessage("Are you sure you want to remove this notification?")
            .setPositiveButton("Clear") { _, _ ->
                deleteNotification(notificationId)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteNotification(notificationId: String) {
        val uid = auth.currentUser?.uid ?: return
        database.child("notifications").child(uid).child(notificationId).removeValue()
            .addOnSuccessListener {
                Toast.makeText(this, "Notification cleared", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateEmptyState() {
        if (notificationList.isEmpty()) {
            binding.emptyState.visibility = View.VISIBLE
            binding.rvNotifications.visibility = View.GONE
        } else {
            binding.emptyState.visibility = View.GONE
            binding.rvNotifications.visibility = View.VISIBLE
        }
    }
}
