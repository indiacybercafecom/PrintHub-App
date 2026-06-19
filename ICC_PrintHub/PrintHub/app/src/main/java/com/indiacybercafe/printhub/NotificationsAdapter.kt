package com.indiacybercafe.printhub

import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.indiacybercafe.printhub.databinding.ItemNotificationBinding
import java.text.SimpleDateFormat
import java.util.*

class NotificationsAdapter(
    private var notifications: MutableList<NotificationModel>,
    private val onNotificationClick: (NotificationModel) -> Unit,
    private val onClearClick: (NotificationModel) -> Unit
) : RecyclerView.Adapter<NotificationsAdapter.NotificationViewHolder>() {

    inner class NotificationViewHolder(val binding: ItemNotificationBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationViewHolder {
        val binding = ItemNotificationBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return NotificationViewHolder(binding)
    }

    override fun onBindViewHolder(holder: NotificationViewHolder, position: Int) {
        val notification = notifications[position]
        val binding = holder.binding

        binding.txtTitle.text = notification.title
        binding.txtMessage.text = notification.message
        binding.txtDate.text = formatDate(notification.timestamp)

        // Set icon color based on type
        val iconColor = when (notification.type) {
            "success" -> ContextCompat.getColor(binding.root.context, R.color.success_green)
            "payment" -> ContextCompat.getColor(binding.root.context, R.color.primary_brown)
            "welcome" -> ContextCompat.getColor(binding.root.context, R.color.accent_gold)
            else -> ContextCompat.getColor(binding.root.context, R.color.primary_brown)
        }
        binding.imgType.setColorFilter(iconColor)

        // Unread System
        if (!notification.isRead) {
            binding.txtTitle.setTypeface(null, Typeface.BOLD)
            binding.unreadDot.visibility = View.VISIBLE
        } else {
            binding.txtTitle.setTypeface(null, Typeface.NORMAL)
            binding.unreadDot.visibility = View.GONE
        }

        // Handle expansion state
        binding.expandedLayout.visibility = if (notification.isExpanded) View.VISIBLE else View.GONE
        binding.imgExpand.rotation = if (notification.isExpanded) 180f else 0f

        binding.collapsedLayout.setOnClickListener {
            notification.isExpanded = !notification.isExpanded
            notifyItemChanged(position)
            if (!notification.isRead) {
                onNotificationClick(notification)
            }
        }

        binding.btnClear.setOnClickListener {
            onClearClick(notification)
        }
    }

    override fun getItemCount(): Int = notifications.size

    private fun formatDate(timestamp: Long): String {
        return try {
            val sdf = SimpleDateFormat("MMMM dd", Locale.getDefault())
            val netDate = Date(timestamp)
            sdf.format(netDate)
        } catch (e: Exception) {
            ""
        }
    }

    fun updateList(newList: List<NotificationModel>) {
        notifications.clear()
        notifications.addAll(newList)
        notifyDataSetChanged()
    }
}
