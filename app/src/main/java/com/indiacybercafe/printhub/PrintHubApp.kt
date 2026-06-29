package com.indiacybercafe.printhub

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import com.google.firebase.database.FirebaseDatabase
import com.indiacybercafe.printhub.utils.UploadManager

class PrintHubApp : Application() {

    companion object {
        const val CHANNEL_ID = "file_upload_channel"
        const val CHANNEL_NAME = "File Uploads"
        const val ORDERS_CHANNEL_ID = "orders_channel"
        const val ORDERS_CHANNEL_NAME = "Orders & Payments"
    }

    override fun onCreate() {
        super.onCreate()

        UploadManager.init(this)
        createNotificationChannels()
        FirebaseApp.initializeApp(this)

        try {
            val database = FirebaseDatabase.getInstance()
            database.setPersistenceEnabled(true)
            
            // Enable offline syncing for key paths
            database.getReference("services").keepSynced(true)
            database.getReference("settings").keepSynced(true)
            database.getReference("users").keepSynced(true)
            database.getReference("orders").keepSynced(true)
            
        } catch (e: Exception) {
            e.printStackTrace()
        }

        FirebaseAppCheck.getInstance()
            .installAppCheckProviderFactory(
                PlayIntegrityAppCheckProviderFactory.getInstance()
            )
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows progress for file uploads"
            }

            val ordersChannel = NotificationChannel(
                ORDERS_CHANNEL_ID,
                ORDERS_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications for orders and payments"
            }

            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(serviceChannel)
            manager?.createNotificationChannel(ordersChannel)
        }
    }
}
