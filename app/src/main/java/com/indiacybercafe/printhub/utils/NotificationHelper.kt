package com.indiacybercafe.printhub.utils

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.database.FirebaseDatabase
import com.indiacybercafe.printhub.MainActivity
import com.indiacybercafe.printhub.NotificationModel
import com.indiacybercafe.printhub.PrintHubApp
import com.indiacybercafe.printhub.R

object NotificationHelper {
    
    fun sendNotification(uid: String, title: String, message: String, type: String, orderId: String) {
        try {
            if (uid.isEmpty()) return
            val database = FirebaseDatabase.getInstance().reference
            val notificationId = database.child("notifications").child(uid).push().key ?: return
            
            val notification = NotificationModel(
                id = notificationId,
                title = title,
                message = message,
                type = type,
                timestamp = System.currentTimeMillis(),
                isRead = false,
                orderId = orderId
            )
            
            database.child("notifications").child(uid).child(notificationId).setValue(notification)
                .addOnFailureListener { e ->
                    Log.e("NOTIFICATION_ERROR", "Failed to save: ${e.message}")
                }
        } catch (e: Exception) {
            Log.e("NOTIFICATION_CRASH", e.stackTraceToString())
        }
    }

    private fun showSystemNotification(context: Context, title: String, message: String) {
        try {
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            val pendingIntent = PendingIntent.getActivity(
                context, 0, intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

            val largeIcon = BitmapFactory.decodeResource(context.resources, R.mipmap.ic_launcher)
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val notification = NotificationCompat.Builder(context, PrintHubApp.ORDERS_CHANNEL_ID)
                .setSmallIcon(R.drawable.icon_notification)
                .setLargeIcon(largeIcon)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .build()

            notificationManager.notify(System.currentTimeMillis().toInt(), notification)
        } catch (e: Exception) {
            Log.e("SYSTEM_NOTIFICATION", "Failed to show: ${e.message}")
        }
    }

    fun sendOrderCreatedNotification(context: Context, uid: String, orderId: String, isCod: Boolean) {
        val title = if (isCod) "COD Order Created" else "Order Created"
        val message = "Your order #$orderId has been successfully created. We will start processing it soon."
        sendNotification(uid, title, message, if (isCod) "general" else "success", orderId)
        showSystemNotification(context, title, message)
    }

    fun sendPaymentSuccessNotification(context: Context, uid: String, orderId: String) {
        val title = "Payment Successful"
        val message = "Payment for order #$orderId was successful."
        sendNotification(uid, title, message, "payment", orderId)
        showSystemNotification(context, title, message)
    }

    fun sendPaymentFailedNotification(context: Context, uid: String, orderId: String) {
        val title = "Payment Failed"
        val message = "Payment for order #$orderId failed. Please try again."
        sendNotification(uid, title, message, "error", orderId)
        showSystemNotification(context, title, message)
    }
}
