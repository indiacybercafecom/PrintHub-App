package com.indiacybercafe.printhub;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.indiacybercafe.printhub.databinding.ActivityAddAddressBinding;
import com.indiacybercafe.printhub.models.AddressModel;
import com.indiacybercafe.printhub.models.UserProfile;

import java.util.HashMap;
import java.util.Map;

public class AddAddressActivity extends AppCompatActivity {

    private ActivityAddAddressBinding binding;
    private DatabaseReference database;
    private String uid;
    private String editAddressId = null;
    private boolean isFirstAddress = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        binding = ActivityAddAddressBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Handle Safe Area Insets
        ViewCompat.setOnApplyWindowInsetsListener(binding.appBarLayout, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(0, systemBars.top, 0, 0);
            return insets;
        });

        ViewCompat.setOnApplyWindowInsetsListener(binding.btnSaveAddress, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            Insets ime = insets.getInsets(WindowInsetsCompat.Type.ime());
            int bottomPadding = Math.max(systemBars.bottom, ime.bottom) + 16; // 16dp extra padding
            v.setPadding(v.getPaddingLeft(), v.getPaddingTop(), v.getPaddingRight(), bottomPadding);
            return insets;
        });

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        uid = user.getUid();
        database = FirebaseDatabase.getInstance().getReference();
        editAddressId = getIntent().getStringExtra("addressId");

        setupToolbar();

        if (editAddressId != null) {
            loadAddressForEdit();
        } else {
            checkIfFirstAddress();
            autoFillDetails(user);
        }

        binding.btnSaveAddress.setOnClickListener(v -> saveAddress());
    }

    private void setupToolbar() {
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(editAddressId != null ? "Edit Address" : "Add New Address");
        }
    }

    private void checkIfFirstAddress() {
        database.child("users").child(uid).child("addresses").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                isFirstAddress = !snapshot.exists();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void autoFillDetails(FirebaseUser user) {
        // Fallback to Auth phone if profile is missing
        if (user.getPhoneNumber() != null) {
            binding.etMobile.setText(user.getPhoneNumber().replace("+91", ""));
        }

        database.child("users").child(uid).child("profile").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    UserProfile profile = snapshot.getValue(UserProfile.class);
                    if (profile != null) {
                        if (!TextUtils.isEmpty(profile.getFullName())) {
                            binding.etFullName.setText(profile.getFullName());
                        }
                        if (!TextUtils.isEmpty(profile.getMobile())) {
                            binding.etMobile.setText(profile.getMobile());
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void loadAddressForEdit() {
        binding.progressBar.setVisibility(View.VISIBLE);
        database.child("users").child(uid).child("addresses").child(editAddressId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        binding.progressBar.setVisibility(View.GONE);
                        AddressModel address = snapshot.getValue(AddressModel.class);
                        if (address != null) {
                            binding.etFullName.setText(address.getFullName());
                            binding.etMobile.setText(address.getMobile());
                            binding.etAddressLine1.setText(address.getAddressLine1());
                            binding.etAddressLine2.setText(address.getAddressLine2());
                            binding.etCity.setText(address.getCity());
                            binding.etState.setText(address.getState());
                            binding.etPincode.setText(address.getPincode());
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        binding.progressBar.setVisibility(View.GONE);
                    }
                });
    }

    private void saveAddress() {
        String name = binding.etFullName.getText().toString().trim();
        String mobile = binding.etMobile.getText().toString().trim();
        String line1 = binding.etAddressLine1.getText().toString().trim();
        String line2 = binding.etAddressLine2.getText().toString().trim();
        String city = binding.etCity.getText().toString().trim();
        String state = binding.etState.getText().toString().trim();
        String pincode = binding.etPincode.getText().toString().trim();

        if (TextUtils.isEmpty(name) || name.length() < 3) {
            binding.etFullName.setError("Enter valid name (min 3 chars)");
            return;
        }

        if (TextUtils.isEmpty(mobile) || mobile.length() != 10) {
            binding.etMobile.setError("Enter 10-digit mobile number");
            return;
        }

        if (TextUtils.isEmpty(line1)) {
            binding.etAddressLine1.setError("Address required");
            return;
        }

        if (TextUtils.isEmpty(city)) {
            binding.etCity.setError("City required");
            return;
        }

        if (TextUtils.isEmpty(state)) {
            binding.etState.setError("State required");
            return;
        }

        if (TextUtils.isEmpty(pincode) || pincode.length() != 6) {
            binding.etPincode.setError("Enter 6-digit pincode");
            return;
        }

        binding.btnSaveAddress.setEnabled(false);
        binding.progressBar.setVisibility(View.VISIBLE);

        DatabaseReference addressRef;
        String addressId;

        if (editAddressId != null) {
            addressId = editAddressId;
            addressRef = database.child("users").child(uid).child("addresses").child(addressId);
        } else {
            addressRef = database.child("users").child(uid).child("addresses").push();
            addressId = addressRef.getKey();
        }

        if (addressId == null) {
            binding.btnSaveAddress.setEnabled(true);
            binding.progressBar.setVisibility(View.GONE);
            return;
        }

        AddressModel address = new AddressModel();
        address.setId(addressId);
        address.setFullName(name);
        address.setMobile(mobile);
        address.setAddressLine1(line1);
        address.setAddressLine2(line2);
        address.setCity(city);
        address.setState(state);
        address.setPincode(pincode);
        address.setUpdatedAt(System.currentTimeMillis());
        if (editAddressId == null) {
            address.setCreatedAt(System.currentTimeMillis());
            address.setDefault(isFirstAddress);
        }

        addressRef.setValue(address)
                .addOnSuccessListener(aVoid -> {
                    syncProfile(name, mobile);
                    Toast.makeText(this, "Address saved", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    binding.btnSaveAddress.setEnabled(true);
                    binding.progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void syncProfile(String name, String mobile) {
        database.child("users").child(uid).child("profile").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Map<String, Object> updates = new HashMap<>();
                if (!snapshot.exists()) {
                    updates.put("fullName", name);
                    updates.put("mobile", mobile);
                    updates.put("createdAt", System.currentTimeMillis());
                } else {
                    String existingName = snapshot.child("fullName").getValue(String.class);
                    String existingMobile = snapshot.child("mobile").getValue(String.class);
                    if (TextUtils.isEmpty(existingName)) updates.put("fullName", name);
                    if (TextUtils.isEmpty(existingMobile)) updates.put("mobile", mobile);
                }
                if (!updates.isEmpty()) {
                    database.child("users").child(uid).child("profile").updateChildren(updates);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
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
