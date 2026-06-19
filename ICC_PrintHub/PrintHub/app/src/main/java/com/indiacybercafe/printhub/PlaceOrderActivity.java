package com.indiacybercafe.printhub;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.RadioButton;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.indiacybercafe.printhub.adapters.PrintSetsAdapter;
import com.indiacybercafe.printhub.databinding.ActivityPlaceOrderBinding;
import com.indiacybercafe.printhub.models.AddressModel;
import com.indiacybercafe.printhub.models.OrderDraft;
import com.indiacybercafe.printhub.models.PrintSet;

import java.util.List;

public class PlaceOrderActivity extends AppCompatActivity {

    private ActivityPlaceOrderBinding binding;
    private AddressModel selectedAddress;
    private List<PrintSet> printSets;
    private DatabaseReference database;
    private String uid;
    private double deliveryCharge = 47.0;
    private double totalAmount = 0.0;
    private double printingCharges = 0.0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        binding = ActivityPlaceOrderBinding.inflate(getLayoutInflater());
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

        database = FirebaseDatabase.getInstance().getReference();
        uid = FirebaseAuth.getInstance().getUid();

        selectedAddress = (AddressModel) getIntent().getSerializableExtra("selectedAddress");
        printSets = SummaryActivity.getMasterPrintSets();

        if (selectedAddress == null || printSets == null || printSets.isEmpty()) {
            finish();
            return;
        }

        setupToolbar();
        displayDetails();
        loadSettings();
        calculateTotal();

        binding.btnPayNow.setOnClickListener(v -> startPaymentFlow());
    }

    private void setupToolbar() {
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Place Order");
        }
    }

    private void displayDetails() {
        binding.tvOrderAddressName.setText(selectedAddress.getFullName());
        binding.tvOrderAddressDetails.setText(String.format("%s, %s, %s, %s - %s",
                selectedAddress.getAddressLine1(), selectedAddress.getAddressLine2(),
                selectedAddress.getCity(), selectedAddress.getState(), selectedAddress.getPincode()));

        PrintSetsAdapter adapter = new PrintSetsAdapter(printSets, position -> {});
        binding.rvOrderSets.setLayoutManager(new LinearLayoutManager(this));
        binding.rvOrderSets.setAdapter(adapter);
    }

    private void loadSettings() {
        database.child("settings").child("deliveryCharge").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    Double val = snapshot.getValue(Double.class);
                    if (val != null) {
                        deliveryCharge = val;
                        binding.tvDeliveryCharges.setText("₹" + String.format("%.2f", deliveryCharge));
                        calculateTotal();
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void calculateTotal() {
        printingCharges = 0;
        for (PrintSet set : printSets) {
            printingCharges += set.getTotalPrice();
        }
        totalAmount = printingCharges + deliveryCharge;

        binding.tvPrintingCharges.setText("₹" + String.format("%.2f", printingCharges));
        binding.tvTotalAmount.setText("₹" + String.format("%.2f", totalAmount));
    }

    private void startPaymentFlow() {
        OrderDraft draft = new OrderDraft();
        draft.setUid(uid);
        draft.setAddress(selectedAddress);
        draft.setPrintSets(printSets);
        draft.setPaymentId("pay_simulated_" + System.currentTimeMillis());
        draft.setTotalAmount(totalAmount);
        draft.setDeliveryCharge(deliveryCharge);
        draft.setPrintingCharges(printingCharges);
        
        RadioButton selectedGst = findViewById(binding.rgGst.getCheckedRadioButtonId());
        draft.setGstType(selectedGst != null ? selectedGst.getText().toString() : "No GST");

        Intent intent = new Intent(this, UploadProcessingActivity.class);
        intent.putExtra("orderDraft", draft);
        startActivity(intent);
        
        SummaryActivity.clearMasterSets();
        finish();
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
