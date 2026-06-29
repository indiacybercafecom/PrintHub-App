package com.indiacybercafe.printhub.services

import android.app.*
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.storage.FirebaseStorage
import com.indiacybercafe.printhub.PrintHubApp
import com.indiacybercafe.printhub.R
import com.indiacybercafe.printhub.UploadProcessingActivity
import com.indiacybercafe.printhub.models.FileModel
import com.indiacybercafe.printhub.models.OrderDraft
import com.indiacybercafe.printhub.utils.UploadManager
import com.indiacybercafe.printhub.utils.UploadStatus
import java.util.ArrayList

class FileUploadService : Service() {

    private val TAG = "FileUploadService"
    private val NOTIFICATION_ID = 1001
    
    private var orderDraft: OrderDraft? = null
    private var orderId: String? = null
    private var allFilesToUpload = ArrayList<FileModel>()
    private var totalBytes: Long = 0
    private var totalBytesTransferredAcrossFiles: Long = 0
    private var currentFileBytesTransferred: Long = 0
    private var isDestroyed = false
    private var isUploading = false
    
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent != null) {
            val action = intent.action
            if (action == "CANCEL_UPLOAD") {
                cancelUpload()
                return START_NOT_STICKY
            }

            val newOrderDraft = intent.getSerializableExtra("orderDraft") as? OrderDraft
            val newOrderId = intent.getStringExtra("orderId")
            
            if (newOrderDraft != null && newOrderId != null) {
                if (isUploading && newOrderId == orderId) {
                    Log.d(TAG, "Already uploading this order")
                    return START_NOT_STICKY
                }
                
                orderDraft = newOrderDraft
                orderId = newOrderId
                prepareUploadList()
                
                UploadManager.startUpload(this, orderId!!, allFilesToUpload.size, totalBytes, orderDraft!!)
                
                startForeground(NOTIFICATION_ID, createNotification())
                startSequentialUpload()
            } else {
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    private fun prepareUploadList() {
        allFilesToUpload.clear()
        totalBytes = 0
        orderDraft?.printSets?.forEach { set ->
            set.files?.forEach { file ->
                if (file.downloadUrl != null && file.downloadUrl.startsWith("content://")) {
                    allFilesToUpload.add(file)
                    totalBytes += file.fileSize
                }
            }
        }
    }

    private fun createNotification(): Notification {
        val state = UploadManager.uploadState.value
        
        val notificationIntent = Intent(this, UploadProcessingActivity::class.java)
        notificationIntent.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val cancelIntent = Intent(this, FileUploadService::class.java).apply {
            action = "CANCEL_UPLOAD"
        }
        val cancelPendingIntent = PendingIntent.getService(
            this, 0, cancelIntent, PendingIntent.FLAG_IMMUTABLE
        )

        val contentText = "${state.currentFileName} - ${state.progressPercentage}%\n" +
                "Speed: ${state.uploadSpeed} | ETA: ${state.estimatedTime}"

        val largeIcon = BitmapFactory.decodeResource(resources, R.mipmap.ic_launcher)

        return NotificationCompat.Builder(this, PrintHubApp.CHANNEL_ID)
            .setContentTitle("Uploading your print files")
            .setContentText(contentText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(contentText))
            .setSmallIcon(R.drawable.icon_notification)
            .setLargeIcon(largeIcon)
            .setProgress(100, state.progressPercentage, false)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setContentIntent(pendingIntent)
            .addAction(R.drawable.ic_back, "View", pendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Cancel", cancelPendingIntent)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
    }

    private fun updateNotification() {
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(NOTIFICATION_ID, createNotification())
    }

    private fun startSequentialUpload() {
        if (allFilesToUpload.isEmpty()) {
            onUploadComplete()
            return
        }
        isUploading = true
        uploadNextFile(0)
    }

    private fun uploadNextFile(index: Int) {
        if (isDestroyed || !isUploading) return
        
        if (index >= allFilesToUpload.size) {
            onUploadComplete()
            return
        }

        val file = allFilesToUpload[index]
        val serviceName = file.category?.lowercase() ?: "general"
        val storageRef = FirebaseStorage.getInstance().reference
            .child("orders")
            .child(orderDraft!!.uid)
            .child(orderId!!)
            .child(serviceName)
            .child(file.fileName)

        val fileUri = Uri.parse(file.downloadUrl)
        val uploadTask = storageRef.putFile(fileUri)

        uploadTask.addOnProgressListener { taskSnapshot ->
            currentFileBytesTransferred = taskSnapshot.bytesTransferred
            val totalTransferred = totalBytesTransferredAcrossFiles + currentFileBytesTransferred
            
            UploadManager.updateProgress(this, file.fileName, index, totalTransferred)
            updateNotification()
        }.addOnSuccessListener {
            storageRef.downloadUrl.addOnSuccessListener { downloadUri ->
                file.downloadUrl = downloadUri.toString()
                file.storagePath = storageRef.path
                file.uploadStatus = "Success"
                file.uploadedAt = System.currentTimeMillis()

                totalBytesTransferredAcrossFiles += file.fileSize
                currentFileBytesTransferred = 0
                uploadNextFile(index + 1)
            }.addOnFailureListener {
                Log.e(TAG, "Failed to get download URL")
                onUploadFailed()
            }
        }.addOnFailureListener { e ->
            Log.e(TAG, "Upload failed for ${file.fileName}", e)
            onUploadFailed()
        }
    }

    private fun onUploadComplete() {
        isUploading = false
        UploadManager.completeUpload(this)
        
        val intent = Intent("com.indiacybercafe.printhub.UPLOAD_COMPLETE")
        intent.putExtra("orderDraft", orderDraft)
        intent.putExtra("orderId", orderId)
        sendBroadcast(intent)
        
        showCompletionNotification()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun onUploadFailed() {
        isUploading = false
        UploadManager.setFailed(this)
        
        val intent = Intent("com.indiacybercafe.printhub.UPLOAD_FAILED")
        sendBroadcast(intent)
        
        showErrorNotification()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun cancelUpload() {
        isUploading = false
        UploadManager.reset(this)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun showCompletionNotification() {
        val notificationIntent = Intent(this, UploadProcessingActivity::class.java)
        notificationIntent.putExtra("orderDraft", orderDraft)
        notificationIntent.putExtra("orderId", orderId)
        notificationIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        
        val pendingIntent = PendingIntent.getActivity(
            this, 1, notificationIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val largeIcon = BitmapFactory.decodeResource(resources, R.mipmap.ic_launcher)
        val notificationManager = getSystemService(NotificationManager::class.java)
        val notification = NotificationCompat.Builder(this, PrintHubApp.CHANNEL_ID)
            .setContentTitle("Upload Complete")
            .setContentText("Your files have been uploaded successfully. Tap to proceed.")
            .setSmallIcon(R.drawable.icon_notification)
            .setLargeIcon(largeIcon)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()
        notificationManager.notify(1002, notification)
    }

    private fun showErrorNotification() {
        val notificationIntent = Intent(this, UploadProcessingActivity::class.java)
        notificationIntent.putExtra("orderDraft", orderDraft)
        notificationIntent.putExtra("orderId", orderId)
        
        val pendingIntent = PendingIntent.getActivity(
            this, 2, notificationIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val largeIcon = BitmapFactory.decodeResource(resources, R.mipmap.ic_launcher)
        val notificationManager = getSystemService(NotificationManager::class.java)
        val notification = NotificationCompat.Builder(this, PrintHubApp.CHANNEL_ID)
            .setContentTitle("Upload Failed")
            .setContentText("There was an error uploading your files. Tap to retry.")
            .setSmallIcon(R.drawable.icon_notification)
            .setLargeIcon(largeIcon)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()
        notificationManager.notify(1003, notification)
    }

    override fun onDestroy() {
        isDestroyed = true
        super.onDestroy()
    }
}
