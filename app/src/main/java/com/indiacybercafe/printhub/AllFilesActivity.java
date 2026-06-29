package com.indiacybercafe.printhub;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.util.Log;
import android.view.MenuItem;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.indiacybercafe.printhub.adapters.SelectedFilesAdapter;
import com.indiacybercafe.printhub.databinding.ActivityAllFilesBinding;
import com.indiacybercafe.printhub.models.FileModel;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class AllFilesActivity extends AppCompatActivity {

    private ActivityAllFilesBinding binding;
    private List<FileModel> selectedFiles = new ArrayList<>();
    private SelectedFilesAdapter adapter;
    private String[] currentMimeTypes;

    private final ActivityResultLauncher<String[]> filePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.OpenMultipleDocuments(),
            uris -> {
                if (uris != null && !uris.isEmpty()) {
                    for (Uri uri : uris) {
                        try {
                            // Take persistable URI permission for WorkManager access
                            getContentResolver().takePersistableUriPermission(uri, 
                                    Intent.FLAG_GRANT_READ_URI_PERMISSION);
                            addFileFromUri(uri);
                        } catch (SecurityException e) {
                            Log.e("AllFilesActivity", "Failed to take persistable permission: " + e.getMessage());
                            // Fallback: still add the file, but it might fail in background
                            addFileFromUri(uri);
                        }
                    }
                    updateUI();
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        binding = ActivityAllFilesBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Handle Safe Area Insets
        ViewCompat.setOnApplyWindowInsetsListener(binding.appBarLayout, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(0, systemBars.top, 0, 0);
            return insets;
        });

        ViewCompat.setOnApplyWindowInsetsListener(binding.bottomCard, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(0, 0, 0, systemBars.bottom);
            return insets;
        });

        setupToolbar();
        setupRecyclerView();
        setupMimeTypes();

        binding.btnUpload.setOnClickListener(v -> {
            filePickerLauncher.launch(currentMimeTypes);
        });

        binding.btnContinue.setOnClickListener(v -> {
            Intent intent = new Intent(this, PrintSettingsActivity.class);
            intent.putExtra("files", (Serializable) selectedFiles);
            startActivity(intent);
        });

        // Auto-launch picker if opened from a specific category
        if (getIntent().hasExtra("action") && !"all_files".equals(getIntent().getStringExtra("action"))) {
            filePickerLauncher.launch(currentMimeTypes);
        }
    }

    private void setupMimeTypes() {
        String action = getIntent().getStringExtra("action");
        if (action == null) action = "all_files";

        switch (action) {
            case "pdf":
                currentMimeTypes = new String[]{"application/pdf"};
                binding.toolbar.setTitle("Select PDF Files");
                break;
            case "camera":
            case "gallery":
                currentMimeTypes = new String[]{"image/*"};
                binding.toolbar.setTitle("Select Images");
                break;
            case "id_card":
                currentMimeTypes = new String[]{"application/pdf", "image/*"};
                binding.toolbar.setTitle("Select ID Card (PDF/Image)");
                break;
            case "doc":
                currentMimeTypes = new String[]{
                        "application/msword",
                        "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                };
                binding.toolbar.setTitle("Select Word Docs");
                break;
            case "xls":
                currentMimeTypes = new String[]{
                        "application/vnd.ms-excel",
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                };
                binding.toolbar.setTitle("Select Excel Sheets");
                break;
            case "ppt":
                currentMimeTypes = new String[]{
                        "application/vnd.ms-powerpoint",
                        "application/vnd.openxmlformats-officedocument.presentationml.presentation"
                };
                binding.toolbar.setTitle("Select PPT Slides");
                break;
            default:
                currentMimeTypes = new String[]{
                        "application/pdf",
                        "application/msword",
                        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                        "application/vnd.ms-excel",
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                        "application/vnd.ms-powerpoint",
                        "application/vnd.openxmlformats-officedocument.presentationml.presentation",
                        "image/*",
                        "text/plain",
                        "application/zip"
                };
                binding.toolbar.setTitle("All Files");
                break;
        }
    }

    private void setupToolbar() {
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    private void setupRecyclerView() {
        adapter = new SelectedFilesAdapter(selectedFiles, position -> {
            selectedFiles.remove(position);
            updateUI();
        });
        binding.rvSelectedFiles.setLayoutManager(new LinearLayoutManager(this));
        binding.rvSelectedFiles.setAdapter(adapter);
    }

    private void addFileFromUri(Uri uri) {
        String fileName = "Unknown";
        long fileSize = 0;
        
        try (android.database.Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                int sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE);
                if (nameIndex != -1) fileName = cursor.getString(nameIndex);
                if (sizeIndex != -1) fileSize = cursor.getLong(sizeIndex);
            }
        }

        // Sanitize file name
        String safeFileName = fileName.replaceAll("[^a-zA-Z0-9.*-]", "_");

        String mimeType = getContentResolver().getType(uri);
        if (mimeType == null) mimeType = "application/octet-stream";

        FileModel file = new FileModel(safeFileName, mimeType, fileSize, getCategoryFromMime(mimeType));
        file.setDownloadUrl(uri.toString()); // Temporarily store local URI
        selectedFiles.add(file);
    }

    private String getCategoryFromMime(String mimeType) {
        if (mimeType.contains("pdf")) return "PDF";
        if (mimeType.contains("word") || mimeType.contains("msword")) return "DOC";
        if (mimeType.contains("excel") || mimeType.contains("spreadsheet")) return "XLS";
        if (mimeType.contains("presentation") || mimeType.contains("powerpoint")) return "PPT";
        if (mimeType.contains("image")) return "IMAGE";
        if (mimeType.contains("text/plain")) return "TXT";
        return "OTHER";
    }

    private void updateUI() {
        adapter.notifyDataSetChanged();
        int count = selectedFiles.size();
        binding.tvFileCount.setText(count + " Files Selected");
        binding.btnContinue.setEnabled(count > 0);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            getOnBackPressedDispatcher().onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
