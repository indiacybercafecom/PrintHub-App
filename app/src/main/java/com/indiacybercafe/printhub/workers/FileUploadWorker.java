package com.indiacybercafe.printhub.workers;

import android.content.Context;
import android.net.Uri;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.DatabaseReference;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

public class FileUploadWorker extends Worker {
    private static final String TAG = "FileUploadWorker";

    public FileUploadWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        String uriString = getInputData().getString("uri");
        String storagePath = getInputData().getString("storagePath");
        String dbPath = getInputData().getString("dbPath");

        if (uriString == null || storagePath == null || dbPath == null) {
            Log.e(TAG, "Missing required parameters");
            return Result.failure();
        }

        Log.d(TAG, "Starting upload for: " + storagePath);
        
        Uri fileUri = Uri.parse(uriString);
        StorageReference storageRef = FirebaseStorage.getInstance().getReference().child(storagePath);
        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference().child(dbPath);

        try {
            // Update status to Uploading
            Tasks.await(dbRef.child("uploadStatus").setValue("Uploading"));

            // Upload file
            Tasks.await(storageRef.putFile(fileUri));
            Log.d(TAG, "File uploaded successfully: " + storagePath);
            
            // Get download URL
            Uri downloadUri = Tasks.await(storageRef.getDownloadUrl());
            String downloadUrl = downloadUri.toString();
            Log.d(TAG, "Download URL obtained: " + downloadUrl);
            
            // Update database with metadata
            Map<String, Object> updates = new HashMap<>();
            updates.put("downloadUrl", downloadUrl);
            updates.put("storagePath", storagePath);
            updates.put("uploadStatus", "Success");
            updates.put("uploadedAt", System.currentTimeMillis());
            
            Tasks.await(dbRef.updateChildren(updates));
            Log.d(TAG, "Database updated for: " + dbPath);

            return Result.success();
        } catch (ExecutionException | InterruptedException e) {
            Log.e(TAG, "Upload failed for " + storagePath, e);
            
            // Mark as failed in DB on final retry or just let it be retried
            if (getRunAttemptCount() >= 3) {
                try {
                    Tasks.await(dbRef.child("uploadStatus").setValue("Failed"));
                } catch (Exception ex) {
                    Log.e(TAG, "Failed to update status to Failed", ex);
                }
                return Result.failure();
            }
            return Result.retry();
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error during upload", e);
            try {
                Tasks.await(dbRef.child("uploadStatus").setValue("Failed"));
            } catch (Exception ex) { /* Ignore */ }
            return Result.failure();
        }
    }
}
