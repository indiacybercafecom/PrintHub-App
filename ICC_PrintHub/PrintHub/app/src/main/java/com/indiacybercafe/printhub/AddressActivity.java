package com.indiacybercafe.printhub;

import android.content.Intent;
import android.os.Bundle;
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

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.indiacybercafe.printhub.adapters.AddressAdapter;
import com.indiacybercafe.printhub.databinding.ActivityAddressBinding;
import com.indiacybercafe.printhub.models.AddressModel;

import java.util.ArrayList;
import java.util.List;

public class AddressActivity extends AppCompatActivity {

    private ActivityAddressBinding binding;
    private DatabaseReference database;
    private String uid;
    private AddressAdapter adapter;
    private List<AddressModel> addressList = new ArrayList<>();
    private AddressModel selectedAddress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        binding = ActivityAddressBinding.inflate(getLayoutInflater());
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

        setupToolbar();
        setupRecyclerView();
        loadAddresses();

        binding.btnAddAddress.setOnClickListener(v -> {
            startActivity(new Intent(this, AddAddressActivity.class));
        });

        binding.btnDeliverHere.setOnClickListener(v -> {
            if (selectedAddress != null) {
                Intent intent = new Intent(this, PlaceOrderActivity.class);
                intent.putExtra("selectedAddress", selectedAddress);
                startActivity(intent);
            } else {
                Toast.makeText(this, "Please select an address", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupToolbar() {
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Select Address");
        }
    }

    private void setupRecyclerView() {
        adapter = new AddressAdapter(addressList, new AddressAdapter.OnAddressActionListener() {
            @Override
            public void onSelected(AddressModel address) {
                selectedAddress = address;
                binding.btnDeliverHere.setEnabled(true);
            }

            @Override
            public void onDelete(AddressModel address, int position) {
                database.child("users").child(uid).child("addresses").child(address.getId()).removeValue();
            }

            @Override
            public void onEdit(AddressModel address) {
                Intent intent = new Intent(AddressActivity.this, AddAddressActivity.class);
                intent.putExtra("addressId", address.getId());
                startActivity(intent);
            }
        });
        binding.rvAddresses.setLayoutManager(new LinearLayoutManager(this));
        binding.rvAddresses.setAdapter(adapter);
    }

    private void loadAddresses() {
        binding.progressBar.setVisibility(View.VISIBLE);
        database.child("users").child(uid).child("addresses").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                binding.progressBar.setVisibility(View.GONE);
                addressList.clear();
                for (DataSnapshot data : snapshot.getChildren()) {
                    AddressModel address = data.getValue(AddressModel.class);
                    if (address != null) {
                        addressList.add(address);
                        if (address.isDefault() && selectedAddress == null) {
                            selectedAddress = address;
                            binding.btnDeliverHere.setEnabled(true);
                        }
                    }
                }
                adapter.notifyDataSetChanged();
                
                if (addressList.isEmpty()) {
                    binding.tvEmpty.setVisibility(View.VISIBLE);
                } else {
                    binding.tvEmpty.setVisibility(View.GONE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                binding.progressBar.setVisibility(View.GONE);
                Toast.makeText(AddressActivity.this, "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
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
