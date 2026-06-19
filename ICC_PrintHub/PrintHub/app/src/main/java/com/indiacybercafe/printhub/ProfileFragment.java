package com.indiacybercafe.printhub;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.indiacybercafe.printhub.databinding.FragmentProfileBinding;
import com.indiacybercafe.printhub.models.UserProfile;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ProfileFragment extends Fragment {

    private FragmentProfileBinding binding;
    private DatabaseReference database;
    private String uid;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentProfileBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Handle Safe Area Insets for Profile Content
        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(0, systemBars.top, 0, 0);
            return insets;
        });

        database = FirebaseDatabase.getInstance().getReference();
        uid = FirebaseAuth.getInstance().getUid();

        setupMenu();
        loadProfileData();

        binding.btnLogout.setOnClickListener(v -> showLogoutDialog());
        binding.btnUpdateCheck.setOnClickListener(v -> 
            Toast.makeText(getContext(), "You are on the latest version", Toast.LENGTH_SHORT).show()
        );
    }

    private void setupMenu() {
        // Manage Account
        binding.menuManageAccount.ivOptionIcon.setImageResource(R.drawable.ic_account);
        binding.menuManageAccount.tvOptionTitle.setText("Manage Account");
        binding.menuManageAccount.cardOption.setOnClickListener(v -> 
            startActivity(new Intent(getActivity(), ManageAccountActivity.class))
        );

        // My Orders
        binding.menuMyOrders.ivOptionIcon.setImageResource(R.drawable.orders);
        binding.menuMyOrders.tvOptionTitle.setText("My Orders");
        binding.menuMyOrders.cardOption.setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).selectTab(1); // Switch to Orders tab
            }
        });

        // Saved Addresses
        binding.menuSavedAddresses.ivOptionIcon.setImageResource(R.drawable.ic_location);
        binding.menuSavedAddresses.tvOptionTitle.setText("Saved Addresses");
        binding.menuSavedAddresses.cardOption.setOnClickListener(v -> 
            startActivity(new Intent(getActivity(), AddressActivity.class))
        );

        // Vouchers
        binding.menuVouchers.ivOptionIcon.setImageResource(R.drawable.ic_voucher);
        binding.menuVouchers.tvOptionTitle.setText("Vouchers");
        binding.menuVouchers.cardOption.setOnClickListener(v -> 
            startActivity(new Intent(getActivity(), ComingSoonActivity.class))
        );

        // Ratings
        binding.menuRatings.ivOptionIcon.setImageResource(R.drawable.ic_star);
        binding.menuRatings.tvOptionTitle.setText("Ratings");
        binding.menuRatings.cardOption.setOnClickListener(v -> 
            Toast.makeText(getContext(), "Rating feature coming soon", Toast.LENGTH_SHORT).show()
        );

        // Legal
        binding.menuLegal.ivOptionIcon.setImageResource(R.drawable.ic_legal);
        binding.menuLegal.tvOptionTitle.setText("Legal Pages");
        binding.menuLegal.cardOption.setOnClickListener(v -> 
            startActivity(new Intent(getActivity(), ComingSoonActivity.class))
        );
    }

    private void loadProfileData() {
        if (uid == null) return;

        DatabaseReference profileRef = database.child("users").child(uid).child("profile");
        profileRef.keepSynced(true); // Offline support

        profileRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded() || binding == null) return;
                
                UserProfile profile = snapshot.getValue(UserProfile.class);
                if (profile != null) {
                    binding.tvFullName.setText(profile.getFullName());
                    binding.tvMobile.setText(profile.getMobile());
                    binding.tvEmail.setText(profile.getEmail());
                    
                    // Load Profile Photo with Glide
                    if (profile.getPhoto() != null && !profile.getPhoto().isEmpty()) {
                        Glide.with(ProfileFragment.this)
                                .load(profile.getPhoto())
                                .placeholder(R.drawable.profile)
                                .error(R.drawable.profile)
                                .circleCrop()
                                .diskCacheStrategy(DiskCacheStrategy.ALL)
                                .into(binding.ivProfilePhoto);
                    } else {
                        binding.ivProfilePhoto.setImageResource(R.drawable.profile);
                    }

                    if (profile.getCreatedAt() != null) {
                        SimpleDateFormat sdf = new SimpleDateFormat("MMM yyyy", Locale.getDefault());
                        String date = sdf.format(new Date(profile.getCreatedAt()));
                        binding.tvMemberSince.setText("Member Since " + date);
                    }
                } else {
                    // Fallback for new users
                    binding.tvFullName.setText("Welcome User");
                    if (FirebaseAuth.getInstance().getCurrentUser() != null) {
                        binding.tvMobile.setText(FirebaseAuth.getInstance().getCurrentUser().getPhoneNumber());
                    }
                    binding.tvEmail.setText("Update profile to add email");
                    binding.tvMemberSince.setText("New Member");
                    binding.ivProfilePhoto.setImageResource(R.drawable.profile);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (isAdded()) {
                    Toast.makeText(getContext(), "Failed to load profile", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void showLogoutDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Logout", (dialog, which) -> {
                    FirebaseAuth.getInstance().signOut();
                    Intent intent = new Intent(getActivity(), LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    getActivity().finish();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
