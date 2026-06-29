package com.indiacybercafe.printhub;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.RadioButton;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import androidx.core.content.ContextCompat;

import com.indiacybercafe.printhub.databinding.ActivityPrintSettingsBinding;
import com.indiacybercafe.printhub.models.FileModel;
import com.indiacybercafe.printhub.models.PrintSet;
import com.indiacybercafe.printhub.models.PrintSettings;
import com.indiacybercafe.printhub.utils.PageCounter;

import java.util.List;
import java.util.Locale;

public class PrintSettingsActivity extends AppCompatActivity {

    private ActivityPrintSettingsBinding binding;
    private List<FileModel> selectedFiles;
    private int totalPages = 0;
    private int copies = 1;
    private double currentPrice = 0.0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        binding = ActivityPrintSettingsBinding.inflate(getLayoutInflater());
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

        selectedFiles = (List<FileModel>) getIntent().getSerializableExtra("files");
        if (selectedFiles == null) {
            finish();
            return;
        }

        setupToolbar();
        calculatePages();
        setupListeners();
        updatePrice();
    }

    private void setupToolbar() {
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    private void calculatePages() {
        totalPages = 0;
        boolean detectionFailed = false;

        if (selectedFiles == null || selectedFiles.isEmpty()) return;

        // Display details for the first file or a summary
        FileModel firstFile = selectedFiles.get(0);
        binding.tvFileName.setText("📄 File: " + firstFile.getFileName());
        binding.tvFileType.setText("📁 Type: " + firstFile.getCategory());
        binding.tvFileSize.setText("📏 Size: " + formatFileSize(firstFile.getFileSize()));

        for (FileModel file : selectedFiles) {
            Uri fileUri = Uri.parse(file.getDownloadUrl());
            int pages = PageCounter.getPageCount(this, fileUri, file.getFileName(), file.getCategory());

            if (pages == -1) {
                detectionFailed = true;
                file.setPageCount(1); // Default to 1 for pricing fallback
            } else {
                file.setPageCount(pages);
                totalPages += pages;
            }
        }

        if (detectionFailed) {
            binding.tvDetectedPages.setText("📑 Actual Pages: Unable to detect pages");
            binding.tvDetectedPages.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_dark));
        } else {
            binding.tvDetectedPages.setText("📑 Actual Pages: " + totalPages);
            binding.tvDetectedPages.setTextColor(ContextCompat.getColor(this, R.color.primary));
        }
    }

    private String formatFileSize(long size) {
        if (size <= 0) return "0 B";
        final String[] units = new String[]{"B", "KB", "MB", "GB", "TB"};
        int digitGroups = (int) (Math.log10(size) / Math.log10(1024));
        return new java.text.DecimalFormat("#,##0.#").format(size / Math.pow(1024, digitGroups)) + " " + units[digitGroups];
    }

    private void setupListeners() {
        binding.rgColorMode.setOnCheckedChangeListener((group, checkedId) -> {
            updatePaperOptions(checkedId == R.id.rbColor);
            updatePrice();
        });

        binding.rgPaperQuality.setOnCheckedChangeListener((group, checkedId) -> updatePrice());
        binding.rgBinding.setOnCheckedChangeListener((group, checkedId) -> updatePrice());

        binding.btnPlus.setOnClickListener(v -> {
            copies++;
            binding.tvCopies.setText(String.valueOf(copies));
            updatePrice();
        });

        binding.btnMinus.setOnClickListener(v -> {
            if (copies > 1) {
                copies--;
                binding.tvCopies.setText(String.valueOf(copies));
                updatePrice();
            }
        });

        binding.btnSave.setOnClickListener(v -> savePrintSet());
    }

    private void updatePaperOptions(boolean isColor) {
        if (isColor) {
            binding.rbLightBW.setVisibility(View.GONE);
            binding.rbMediumBW.setText("Medium Color (₹3/page)");
            binding.rbHighBW.setText("High Color (₹4/page)");
        } else {
            binding.rbLightBW.setVisibility(View.VISIBLE);
            binding.rbMediumBW.setText("Medium BW (₹1/page)");
            binding.rbHighBW.setText("High BW (₹1.5/page)");
        }
    }

    private void updatePrice() {
        double perPagePrice = 0.0;
        boolean isColor = binding.rbColor.isChecked();

        if (isColor) {
            if (binding.rbMediumBW.isChecked()) perPagePrice = 3.0;
            else if (binding.rbHighBW.isChecked()) perPagePrice = 4.0;
        } else {
            if (binding.rbLightBW.isChecked()) perPagePrice = 0.7;
            else if (binding.rbMediumBW.isChecked()) perPagePrice = 1.0;
            else if (binding.rbHighBW.isChecked()) perPagePrice = 1.5;
        }

        double bindingPrice = 0.0;
        if (binding.rbStapled.isChecked()) bindingPrice = 15.0;
        else if (binding.rbSpiral.isChecked()) bindingPrice = 40.0;

        currentPrice = (perPagePrice * totalPages * copies) + bindingPrice;
        binding.tvPrice.setText("₹" + String.format(Locale.getDefault(), "%.2f", currentPrice));

        // Update Summary Card
        binding.tvPrintType.setText("🖨 Print Type: " + (isColor ? "Color" : "B&W"));
        binding.tvCopiesSummary.setText("📋 Copies: " + copies);
        binding.tvPricePerPage.setText(String.format(Locale.getDefault(), "💰 Price/Page: ₹%.2f", perPagePrice));
        binding.tvTotalPrice.setText(String.format(Locale.getDefault(), "💵 Total: ₹%.2f", currentPrice));
    }

    private void savePrintSet() {
        PrintSettings settings = new PrintSettings();
        settings.setColorMode(binding.rbColor.isChecked() ? "Color" : "Black & White");
        
        RadioButton selectedPaper = findViewById(binding.rgPaperQuality.getCheckedRadioButtonId());
        settings.setPaperQuality(selectedPaper.getText().toString());
        
        settings.setCopies(copies);
        
        RadioButton selectedBinding = findViewById(binding.rgBinding.getCheckedRadioButtonId());
        settings.setBinding(selectedBinding.getText().toString());
        
        settings.setPrice(currentPrice);

        PrintSet printSet = new PrintSet("Print Set");
        printSet.setFiles(selectedFiles);
        printSet.setSettings(settings);
        printSet.setTotalPrice(currentPrice);

        Intent intent = new Intent(this, SummaryActivity.class);
        intent.putExtra("printSet", printSet);
        startActivity(intent);
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
