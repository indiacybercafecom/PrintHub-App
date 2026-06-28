package com.indiacybercafe.printhub;

import android.app.DownloadManager;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.indiacybercafe.printhub.adapters.OrderFilesAdapter;
import com.indiacybercafe.printhub.databinding.ActivityOrderFilesBinding;
import com.indiacybercafe.printhub.models.FileModel;
import com.indiacybercafe.printhub.models.OrderModel;
import com.indiacybercafe.printhub.models.PrintSet;

import java.util.ArrayList;
import java.util.List;

public class OrderFilesActivity extends AppCompatActivity {

    private ActivityOrderFilesBinding binding;
    private List<FileModel> fileList = new ArrayList<>();
    private OrderFilesAdapter adapter;
    private String orderId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        binding = ActivityOrderFilesBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ViewCompat.setOnApplyWindowInsetsListener(binding.appBarLayout, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(0, systemBars.top, 0, 0);
            return insets;
        });

        orderId = getIntent().getStringExtra("ORDER_ID");
        OrderModel order = (OrderModel) getIntent().getSerializableExtra("ORDER_OBJ");

        setupToolbar();
        setupRecyclerView();

        if (order != null) {
            if ("Completed".equalsIgnoreCase(order.getStatus())) {
                binding.llEmpty.setVisibility(View.GONE);
                binding.rvFiles.setVisibility(View.GONE);
                binding.llCompleted.setVisibility(View.VISIBLE);
                Toast.makeText(this, "Your files have already been deleted from our server.", Toast.LENGTH_SHORT).show();
            } else {
                extractFiles(order);
            }
        } else {
            binding.llEmpty.setVisibility(View.VISIBLE);
        }
    }

    private void setupToolbar() {
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            if (orderId != null) {
                getSupportActionBar().setTitle("Files: " + orderId);
            }
        }
    }

    private void setupRecyclerView() {
        adapter = new OrderFilesAdapter(fileList, orderId, this::downloadFile);
        binding.rvFiles.setLayoutManager(new LinearLayoutManager(this));
        binding.rvFiles.setAdapter(adapter);
    }

    private void extractFiles(OrderModel order) {
        fileList.clear();
        if (order.getPrintSets() != null) {
            for (PrintSet set : order.getPrintSets()) {
                if (set.getFiles() != null) {
                    fileList.addAll(set.getFiles());
                }
            }
        }
        
        adapter.notifyDataSetChanged();
        
        if (fileList.isEmpty()) {
            binding.llEmpty.setVisibility(View.VISIBLE);
        } else {
            binding.llEmpty.setVisibility(View.GONE);
        }
    }

    private void downloadFile(FileModel file) {
        if (file.getDownloadUrl() == null || file.getDownloadUrl().isEmpty()) {
            Toast.makeText(this, "Download URL not available", Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(this, "Downloading " + file.getFileName(), Toast.LENGTH_SHORT).show();

        try {
            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(file.getDownloadUrl()));
            request.setTitle(file.getFileName());
            request.setDescription("Downloading file from PrintHub");
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, file.getFileName());

            DownloadManager manager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
            if (manager != null) {
                manager.enqueue(request);
            }
        } catch (Exception e) {
            Toast.makeText(this, "Failed to download: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
