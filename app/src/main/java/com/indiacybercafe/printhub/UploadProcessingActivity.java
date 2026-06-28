package com.indiacybercafe.printhub;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.indiacybercafe.printhub.databinding.ActivityUploadProcessingBinding;
import com.indiacybercafe.printhub.models.FileModel;
import com.indiacybercafe.printhub.models.OrderDraft;
import com.indiacybercafe.printhub.models.OrderModel;
import com.indiacybercafe.printhub.models.PrintSet;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class UploadProcessingActivity extends AppCompatActivity {
    private static final String TAG = "UploadProcessing";
    private ActivityUploadProcessingBinding binding;
    private OrderDraft orderDraft;
    private List<FileModel> allFilesToUpload = new ArrayList<>();
    private int currentFileIndex = 0;
    private long totalBytes = 0;
    private long totalBytesTransferred = 0;
    private long currentFileBytesTransferred = 0;
    private DatabaseReference database;
    private String orderId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        binding = ActivityUploadProcessingBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Handle Safe Area Insets
        ViewCompat.setOnApplyWindowInsetsListener(binding.main, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        orderDraft = (OrderDraft) getIntent().getSerializableExtra("orderDraft");
        if (orderDraft == null) {
            finish();
            return;
        }

        database = FirebaseDatabase.getInstance().getReference();
        prepareUploadList();
        startSequentialUpload();
    }

    private void prepareUploadList() {
        for (PrintSet set : orderDraft.getPrintSets()) {
            for (FileModel file : set.getFiles()) {
                if (file.getDownloadUrl() != null && file.getDownloadUrl().startsWith("content://")) {
                    allFilesToUpload.add(file);
                    totalBytes += file.getFileSize();
                }
            }
        }
        binding.tvFileCount.setText("Total Files: " + allFilesToUpload.size());
        binding.tvUploaded.setText("Uploaded: 0 / " + allFilesToUpload.size());
        binding.tvRemaining.setText("Remaining: " + allFilesToUpload.size());
    }

    private void startSequentialUpload() {
        if (currentFileIndex >= allFilesToUpload.size()) {
            generateUniqueOrderIdAndCreateOrder();
            return;
        }

        FileModel currentFile = allFilesToUpload.get(currentFileIndex);
        binding.tvCurrentFile.setText("Uploading: " + currentFile.getFileName());
        binding.tvUploaded.setText("Uploaded: " + currentFileIndex + " / " + allFilesToUpload.size());
        binding.tvRemaining.setText("Remaining: " + (allFilesToUpload.size() - currentFileIndex));

        uploadFile(currentFile);
    }

    private void uploadFile(FileModel file) {
        String serviceName = file.getCategory().toLowerCase();
        
        if (orderId == null) {
            orderId = generateOrderId();
        }

        StorageReference storageRef = FirebaseStorage.getInstance().getReference()
                .child("orders")
                .child(orderDraft.getUid())
                .child(orderId)
                .child(serviceName)
                .child(file.getFileName());

        Uri fileUri = Uri.parse(file.getDownloadUrl());
        UploadTask uploadTask = storageRef.putFile(fileUri);

        uploadTask.addOnProgressListener(taskSnapshot -> {
            currentFileBytesTransferred = taskSnapshot.getBytesTransferred();
            updateOverallProgress();
        }).continueWithTask(task -> {
            if (!task.isSuccessful()) {
                throw task.getException();
            }
            return storageRef.getDownloadUrl();
        }).addOnSuccessListener(downloadUri -> {
            file.setDownloadUrl(downloadUri.toString());
            file.setStoragePath(storageRef.getPath());
            file.setUploadStatus("Success");
            file.setUploadedAt(System.currentTimeMillis());

            totalBytesTransferred += file.getFileSize();
            currentFileBytesTransferred = 0;
            currentFileIndex++;
            startSequentialUpload();
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Upload failed for " + file.getFileName(), e);
            binding.tvStatus.setText("Upload Failed: " + file.getFileName());
            Toast.makeText(this, "Upload failed. Please check internet.", Toast.LENGTH_LONG).show();
        });
    }

    private void updateOverallProgress() {
        long overallTransferred = totalBytesTransferred + currentFileBytesTransferred;
        int progress = (int) ((100.0 * overallTransferred) / totalBytes);
        
        binding.pbUpload.setProgress(progress);
        binding.tvPercentage.setText(progress + "%");
    }

    private void generateUniqueOrderIdAndCreateOrder() {
        binding.tvCurrentFile.setText("Finalizing Order...");
        binding.tvUploadTitle.setText("Creating Your Order");
        
        database.child("orders").child(orderId).get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                if (task.getResult().exists()) {
                    orderId = generateOrderId();
                    generateUniqueOrderIdAndCreateOrder();
                } else {
                    saveOrderToDb();
                }
            } else {
                saveOrderToDb(); // Fallback
            }
        });
    }

    private void saveOrderToDb() {
        Intent intent = new Intent(this, PaymentActivity.class);
        intent.putExtra("orderDraft", orderDraft);
        intent.putExtra("orderId", orderId);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private String generateOrderId() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder sb = new StringBuilder();
        Random random = new Random();
        for (int i = 0; i < 5; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }
}
