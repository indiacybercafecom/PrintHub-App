package com.indiacybercafe.printhub.utils

import android.util.Log
import com.google.firebase.database.FirebaseDatabase
import com.indiacybercafe.printhub.NotificationModel

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

    fun sendOrderCreatedNotification(uid: String, orderId: String, isCod: Boolean) {
        val title = if (isCod) "COD Order Created" else "Order Created"
        val message = "Your order #$orderId has been successfully created. We will start processing it soon."
        sendNotification(uid, title, message, if (isCod) "general" else "success", orderId)
    }

    fun sendPaymentSuccessNotification(uid: String, orderId: String) {
        sendNotification(uid, "Payment Successful", "Payment for order #$orderId was successful.", "payment", orderId)
    }

    fun sendPaymentFailedNotification(uid: String, orderId: String) {
        sendNotification(uid, "Payment Failed", "Payment for order #$orderId failed. Please try again.", "error", orderId)
    }
}
