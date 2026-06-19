package com.indiacybercafe.printhub;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.indiacybercafe.printhub.adapters.PrintSetsAdapter;
import com.indiacybercafe.printhub.databinding.ActivitySummaryBinding;
import com.indiacybercafe.printhub.models.PrintSet;

import java.util.ArrayList;
import java.util.List;

public class SummaryActivity extends AppCompatActivity {

    private ActivitySummaryBinding binding;
    private static List<PrintSet> masterPrintSets = new ArrayList<>();
    private PrintSetsAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        binding = ActivitySummaryBinding.inflate(getLayoutInflater());
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

        PrintSet newSet = (PrintSet) getIntent().getSerializableExtra("printSet");
        if (newSet != null) {
            masterPrintSets.add(newSet);
        }

        setupToolbar();
        setupRecyclerView();
        updateTotal();

        binding.btnAddMore.setOnClickListener(v -> {
            Intent intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
        });

        binding.btnNext.setOnClickListener(v -> {
            if (masterPrintSets.isEmpty()) {
                Toast.makeText(this, "Please add at least one print set", Toast.LENGTH_SHORT).show();
                return;
            }
            Intent intent = new Intent(this, AddressActivity.class);
            startActivity(intent);
        });
    }

    private void setupToolbar() {
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    private void setupRecyclerView() {
        adapter = new PrintSetsAdapter(masterPrintSets, position -> {
            masterPrintSets.remove(position);
            adapter.notifyItemRemoved(position);
            updateTotal();
        });
        binding.rvPrintSets.setLayoutManager(new LinearLayoutManager(this));
        binding.rvPrintSets.setAdapter(adapter);
    }

    private void updateTotal() {
        double subtotal = 0;
        for (PrintSet set : masterPrintSets) {
            subtotal += set.getTotalPrice();
        }
        binding.tvSubtotal.setText("₹" + String.format("%.2f", subtotal));
        binding.btnNext.setEnabled(!masterPrintSets.isEmpty());
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            getOnBackPressedDispatcher().onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public static List<PrintSet> getMasterPrintSets() {
        return masterPrintSets;
    }
    
    public static void clearMasterSets() {
        masterPrintSets.clear();
    }
}
