package com.indiacybercafe.printhub;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.indiacybercafe.printhub.databinding.ActivityManageAccountBinding;
import com.indiacybercafe.printhub.models.UserProfile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class ManageAccountActivity extends AppCompatActivity {

    private ActivityManageAccountBinding binding;
    private DatabaseReference database;
    private StorageReference storage;
    private String uid;
    private Uri imageUri;

    private final ActivityResultLauncher<Intent> galleryLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    imageUri = result.getData().getData();
                    Glide.with(this)
                            .load(imageUri)
                            .circleCrop()
                            .into(binding.ivProfilePhoto);
                }
            }
    );

    private final ActivityResultLauncher<Intent> cameraLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Bitmap bitmap = (Bitmap) result.getData().getExtras().get("data");
                    imageUri = getImageUri(bitmap);
                    Glide.with(this)
                            .load(bitmap)
                            .circleCrop()
                            .into(binding.ivProfilePhoto);
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        binding = ActivityManageAccountBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Handle Safe Area Insets
        ViewCompat.setOnApplyWindowInsetsListener(binding.appBarLayout, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(0, systemBars.top, 0, 0);
            return insets;
        });

        ViewCompat.setOnApplyWindowInsetsListener(binding.btnUpdateProfile, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            Insets ime = insets.getInsets(WindowInsetsCompat.Type.ime());
            int bottomPadding = Math.max(systemBars.bottom, ime.bottom) + 16;
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
        storage = FirebaseStorage.getInstance().getReference().child("profile_photos").child(uid + ".jpg");

        setupToolbar();
        setupSpinners();
        loadUserProfile();

        binding.fabEditPhoto.setOnClickListener(v -> showImageSourceDialog());
        binding.btnUpdateProfile.setOnClickListener(v -> updateProfile());
    }

    private void setupToolbar() {
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    private void setupSpinners() {
        String[] genders = {"Male", "Female", "Other", "Prefer not to say"};
        ArrayAdapter<String> genderAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, genders);
        binding.spinnerGender.setAdapter(genderAdapter);

        String[] professions = {"Student", "Employee", "Business Man", "Teacher", "Lawyer", "Other"};
        ArrayAdapter<String> professionAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, professions);
        binding.spinnerProfession.setAdapter(professionAdapter);
    }

    private void loadUserProfile() {
        showLoading(true);
        DatabaseReference profileRef = database.child("users").child(uid).child("profile");
        profileRef.keepSynced(true);

        profileRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                showLoading(false);
                UserProfile profile = snapshot.getValue(UserProfile.class);
                if (profile != null) {
                    binding.etFullName.setText(profile.getFullName());
                    binding.etMobile.setText(profile.getMobile());
                    binding.etEmail.setText(profile.getEmail());
                    binding.spinnerGender.setText(profile.getGender(), false);
                    binding.spinnerProfession.setText(profile.getProfession(), false);
                    
                    if (profile.getPhoto() != null && !profile.getPhoto().isEmpty()) {
                        Glide.with(ManageAccountActivity.this)
                                .load(profile.getPhoto())
                                .placeholder(R.drawable.profile)
                                .error(R.drawable.profile)
                                .circleCrop()
                                .diskCacheStrategy(DiskCacheStrategy.ALL)
                                .into(binding.ivProfilePhoto);
                    }
                } else {
                    FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                    if (user != null) {
                        binding.etMobile.setText(user.getPhoneNumber());
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                showLoading(false);
                Toast.makeText(ManageAccountActivity.this, "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showImageSourceDialog() {
        String[] options = {"Camera", "Gallery"};
        new MaterialAlertDialogBuilder(this)
                .setTitle("Select Image Source")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                        cameraLauncher.launch(intent);
                    } else {
                        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                        galleryLauncher.launch(intent);
                    }
                })
                .show();
    }

    private void updateProfile() {
        String name = binding.etFullName.getText().toString().trim();
        String email = binding.etEmail.getText().toString().trim();
        String gender = binding.spinnerGender.getText().toString();
        String profession = binding.spinnerProfession.getText().toString();

        if (TextUtils.isEmpty(name)) {
            binding.etFullName.setError("Name required");
            return;
        }

        if (TextUtils.isEmpty(email) || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.etEmail.setError("Valid email required");
            return;
        }

        if (TextUtils.isEmpty(gender)) {
            Toast.makeText(this, "Gender required", Toast.LENGTH_SHORT).show();
            return;
        }

        if (TextUtils.isEmpty(profession)) {
            Toast.makeText(this, "Profession required", Toast.LENGTH_SHORT).show();
            return;
        }

        showLoading(true);
        binding.btnUpdateProfile.setEnabled(false);

        if (imageUri != null) {
            uploadPhoto(name, email, gender, profession);
        } else {
            saveProfileToDatabase(name, email, gender, profession, null);
        }
    }

    private void uploadPhoto(String name, String email, String gender, String profession) {
        try {
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 50, baos);
            byte[] data = baos.toByteArray();

            storage.putBytes(data)
                    .addOnSuccessListener(taskSnapshot -> storage.getDownloadUrl().addOnSuccessListener(uri -> {
                        saveProfileToDatabase(name, email, gender, profession, uri.toString());
                    }))
                    .addOnFailureListener(e -> {
                        showLoading(false);
                        binding.btnUpdateProfile.setEnabled(true);
                        Toast.makeText(this, "Photo upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        } catch (IOException e) {
            e.printStackTrace();
            saveProfileToDatabase(name, email, gender, profession, null);
        }
    }

    private void saveProfileToDatabase(String name, String email, String gender, String profession, String photoUrl) {
        long currentTime = System.currentTimeMillis();
        Map<String, Object> profileData = new HashMap<>();
        profileData.put("fullName", name);
        profileData.put("email", email);
        profileData.put("gender", gender);
        profileData.put("profession", profession);
        profileData.put("updatedAt", currentTime);
        
        if (photoUrl != null) {
            profileData.put("photo", photoUrl);
        }

        database.child("users").child(uid).child("profile").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    profileData.put("createdAt", currentTime);
                    profileData.put("mobile", FirebaseAuth.getInstance().getCurrentUser().getPhoneNumber());
                }
                
                database.child("users").child(uid).child("profile").updateChildren(profileData)
                        .addOnSuccessListener(aVoid -> {
                            showLoading(false);
                            binding.btnUpdateProfile.setEnabled(true);
                            Toast.makeText(ManageAccountActivity.this, "Profile Updated Successfully", Toast.LENGTH_SHORT).show();
                            finish();
                        })
                        .addOnFailureListener(e -> {
                            showLoading(false);
                            binding.btnUpdateProfile.setEnabled(true);
                            Toast.makeText(ManageAccountActivity.this, "Update failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                showLoading(false);
                binding.btnUpdateProfile.setEnabled(true);
                Toast.makeText(ManageAccountActivity.this, "Database error", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showLoading(boolean loading) {
        binding.progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
    }

    private Uri getImageUri(Bitmap bitmap) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
        String path = MediaStore.Images.Media.insertImage(getContentResolver(), bitmap, "ProfilePhoto", null);
        return Uri.parse(path);
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
