package com.indiacybercafe.printhub;

import android.content.Intent;
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

import com.indiacybercafe.printhub.databinding.ActivityPrintSettingsBinding;
import com.indiacybercafe.printhub.models.FileModel;
import com.indiacybercafe.printhub.models.PrintSet;
import com.indiacybercafe.printhub.models.PrintSettings;

import java.util.List;

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
        for (FileModel file : selectedFiles) {
            if (file.getCategory().equals("IMAGE")) {
                totalPages += 1;
            } else if (file.getCategory().equals("PDF")) {
                totalPages += 1; // Placeholder
            } else {
                totalPages += 1; // Default
            }
        }
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
        binding.tvPrice.setText("₹" + String.format("%.2f", currentPrice));
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
