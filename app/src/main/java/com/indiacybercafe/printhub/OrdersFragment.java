package com.indiacybercafe.printhub;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.indiacybercafe.printhub.adapters.OrdersAdapter;
import com.indiacybercafe.printhub.databinding.FragmentOrdersBinding;
import com.indiacybercafe.printhub.models.OrderModel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class OrdersFragment extends Fragment {

    private FragmentOrdersBinding binding;
    private DatabaseReference database;
    private String uid;
    private List<OrderModel> orderList = new ArrayList<>();
    private OrdersAdapter adapter;
    private ValueEventListener ordersListener;
    private com.google.firebase.database.Query ordersQuery;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentOrdersBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Handle Safe Area Insets for Toolbar
        ViewCompat.setOnApplyWindowInsetsListener(binding.appBarLayout, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(0, systemBars.top, 0, 0);
            return insets;
        });

        database = FirebaseDatabase.getInstance().getReference();
        uid = FirebaseAuth.getInstance().getUid();

        setupRecyclerView();
        loadOrders();
    }

    private void setupRecyclerView() {
        adapter = new OrdersAdapter(orderList, new OrdersAdapter.OnOrderClickListener() {
            @Override
            public void onTrackOrder(OrderModel order) {
                // Track order or view details logic
                Toast.makeText(getContext(), "Tracking for " + order.getOrderId(), Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onViewFiles(OrderModel order) {
                if ("Completed".equalsIgnoreCase(order.getStatus())) {
                    Toast.makeText(getContext(), "Your order has been completed. Your uploaded files have been permanently deleted from our server for privacy and security.", Toast.LENGTH_LONG).show();
                } else {
                    Intent intent = new Intent(getContext(), OrderFilesActivity.class);
                    intent.putExtra("ORDER_ID", order.getOrderId());
                    intent.putExtra("ORDER_OBJ", order);
                    startActivity(intent);
                }
            }
        });
        binding.rvOrders.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.rvOrders.setAdapter(adapter);
    }

    private void loadOrders() {
        if (uid == null) return;

        binding.progressBar.setVisibility(View.VISIBLE);
        ordersQuery = database.child("orders").orderByChild("uid").equalTo(uid);
        ordersListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (_bindingIsNull() || !isAdded() || getView() == null) return;
                
                binding.progressBar.setVisibility(View.GONE);
                orderList.clear();
                for (DataSnapshot data : snapshot.getChildren()) {
                    OrderModel order = data.getValue(OrderModel.class);
                    if (order != null) {
                        orderList.add(order);
                    }
                }
                
                // Sort by date descending
                Collections.sort(orderList, (o1, o2) -> Long.compare(o2.getCreatedAt(), o1.getCreatedAt()));
                
                adapter.notifyDataSetChanged();
                
                if (orderList.isEmpty()) {
                    binding.llEmpty.setVisibility(View.VISIBLE);
                } else {
                    binding.llEmpty.setVisibility(View.GONE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (!_bindingIsNull() && isAdded()) {
                    binding.progressBar.setVisibility(View.GONE);
                    Toast.makeText(getContext(), "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        };
        ordersQuery.addValueEventListener(ordersListener);
    }

    private boolean _bindingIsNull() {
        return binding == null;
    }

    @Override
    public void onDestroyView() {
        if (ordersQuery != null && ordersListener != null) {
            ordersQuery.removeEventListener(ordersListener);
        }
        super.onDestroyView();
        binding = null;
    }
}
