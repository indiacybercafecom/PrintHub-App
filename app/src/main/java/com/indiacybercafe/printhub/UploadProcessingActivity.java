package com.indiacybercafe.printhub;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.indiacybercafe.printhub.databinding.ActivityUploadProcessingBinding;
import com.indiacybercafe.printhub.models.OrderDraft;
import com.indiacybercafe.printhub.services.FileUploadService;
import com.indiacybercafe.printhub.utils.UploadManager;
import com.indiacybercafe.printhub.utils.UploadState;
import com.indiacybercafe.printhub.utils.UploadStatus;

import java.util.Random;

public class UploadProcessingActivity extends AppCompatActivity implements UploadManager.UploadListener {
    private ActivityUploadProcessingBinding binding;
    private OrderDraft orderDraft;
    private String orderId;
    private DatabaseReference database;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        binding = ActivityUploadProcessingBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ViewCompat.setOnApplyWindowInsetsListener(binding.main, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        orderDraft = (OrderDraft) getIntent().getSerializableExtra("orderDraft");
        orderId = getIntent().getStringExtra("orderId");
        database = FirebaseDatabase.getInstance().getReference();

        Log.d("UPLOAD_DEBUG", "UploadActivity started");
        int totalFiles = 0;
        if (orderDraft != null && orderDraft.getPrintSets() != null) {
            for (com.indiacybercafe.printhub.models.PrintSet set : orderDraft.getPrintSets()) {
                if (set.getFiles() != null) {
                    totalFiles += set.getFiles().size();
                }
            }
        }
        Log.d("UPLOAD_DEBUG", "Selected files=" + totalFiles);
        Log.d("UPLOAD_DEBUG", "Upload session=" + (orderId != null ? orderId : "new"));

        binding.btnCancel.setOnClickListener(v -> cancelUpload());

        UploadState currentState = UploadManager.getCurrentState();
        Log.d("UPLOAD_DEBUG", "Current UploadManager status=" + currentState.getStatus());

        if (orderDraft != null) {
            // If previous upload was completed, we must reset for the new one
            if (currentState.getStatus() == UploadStatus.COMPLETED || currentState.getStatus() == UploadStatus.FAILED) {
                Log.d("UPLOAD_DEBUG", "Resetting UploadManager for new session");
                UploadManager.reset(this);
                currentState = UploadManager.getCurrentState();
            }

            if (currentState.getStatus() == UploadStatus.IDLE) {
                Log.d("UPLOAD_DEBUG", "Starting upload process");
                checkPermissionAndStartService();
            } else {
                Log.d("UPLOAD_DEBUG", "Upload already in progress or state mismatch: " + currentState.getStatus());
            }
        } else {
            if (currentState.getStatus() == UploadStatus.IDLE) {
                Log.d("UPLOAD_DEBUG", "No orderDraft and state is IDLE, finishing activity");
                finish();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        UploadManager.addListener(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        UploadManager.removeListener(this);
    }

    @Override
    public void onStateChanged(@NonNull UploadState state) {
        runOnUiThread(() -> {
            binding.pbUpload.setProgress(state.getProgressPercentage());
            binding.tvPercentage.setText(state.getProgressPercentage() + "%");
            binding.tvCurrentFile.setText("Uploading: " + state.getCurrentFileName());
            binding.tvFileCount.setText("Total Files: " + state.getTotalFiles());
            binding.tvUploaded.setText("Uploaded: " + state.getUploadedFiles() + " / " + state.getTotalFiles());
            binding.tvRemaining.setText("Remaining: " + (state.getTotalFiles() - state.getUploadedFiles()));
            binding.tvSpeed.setText("Speed: " + state.getUploadSpeed());
            binding.tvTimeRemaining.setText("ETA: " + state.getEstimatedTime());

            if (state.getStatus() == UploadStatus.COMPLETED) {
                orderDraft = state.getOrderDraft();
                orderId = state.getUploadId();
                generateUniqueOrderIdAndCreateOrder();
            } else if (state.getStatus() == UploadStatus.FAILED) {
                binding.tvStatus.setText("Upload Failed");
                Toast.makeText(this, "Upload failed. Please try again.", Toast.LENGTH_LONG).show();
            }
        });
    }

    private void checkPermissionAndStartService() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, 101);
            } else {
                startUploadService();
            }
        } else {
            startUploadService();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 101) {
            startUploadService();
        }
    }

    private void startUploadService() {
        if (orderId == null) {
            orderId = generateOrderId();
        }
        
        Intent serviceIntent = new Intent(this, FileUploadService.class);
        serviceIntent.putExtra("orderDraft", orderDraft);
        serviceIntent.putExtra("orderId", orderId);
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }
    }

    private void generateUniqueOrderIdAndCreateOrder() {
        binding.btnCancel.setEnabled(false);
        binding.tvCurrentFile.setText("Finalizing Order...");
        binding.tvUploadTitle.setText("Creating Your Order");
        
        database.child("orders").child(orderId).get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult().exists()) {
                orderId = generateOrderId();
                generateUniqueOrderIdAndCreateOrder();
            } else {
                saveOrderToDb();
            }
        });
    }

    private void cancelUpload() {
        new AlertDialog.Builder(this)
            .setTitle("Cancel Upload")
            .setMessage("Are you sure you want to cancel the upload? This will stop the ordering process.")
            .setPositiveButton("Yes, Cancel", (dialog, which) -> {
                Intent intent = new Intent(this, FileUploadService.class);
                intent.setAction("CANCEL_UPLOAD");
                startService(intent);
                finish();
            })
            .setNegativeButton("No", null)
            .show()
            .getButton(AlertDialog.BUTTON_POSITIVE)
            .setTextColor(ContextCompat.getColor(this, R.color.error_red));
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
