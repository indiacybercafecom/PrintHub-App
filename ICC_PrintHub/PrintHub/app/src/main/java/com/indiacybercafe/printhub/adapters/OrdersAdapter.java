package com.indiacybercafe.printhub.adapters;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.indiacybercafe.printhub.R;
import com.indiacybercafe.printhub.databinding.ItemOrderBinding;
import com.indiacybercafe.printhub.models.OrderModel;
import com.indiacybercafe.printhub.models.PrintSet;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class OrdersAdapter extends RecyclerView.Adapter<OrdersAdapter.OrderViewHolder> {

    private final List<OrderModel> orders;
    private final OnOrderClickListener listener;

    public interface OnOrderClickListener {
        void onTrackOrder(OrderModel order);
    }

    public OrdersAdapter(List<OrderModel> orders, OnOrderClickListener listener) {
        this.orders = orders;
        this.listener = listener;
    }

    @NonNull
    @Override
    public OrderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemOrderBinding binding = ItemOrderBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new OrderViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull OrderViewHolder holder, int position) {
        OrderModel order = orders.get(position);
        
        // Use the ID directly as it now contains the # prefix
        String orderId = order.getOrderId();
        holder.binding.tvOrderId.setText("Order ID: " + (orderId != null ? orderId : "N/A"));
        
        holder.binding.tvTotalAmount.setText(String.format("₹%.2f", order.getTotalAmount()));
        
        int fileCount = 0;
        if (order.getPrintSets() != null) {
            for (PrintSet set : order.getPrintSets()) {
                if (set.getFiles() != null) fileCount += set.getFiles().size();
            }
        }
        holder.binding.tvFileCount.setText(fileCount + " Files");

        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault());
        holder.binding.tvOrderDate.setText(sdf.format(new Date(order.getCreatedAt())));

        setStatusBadge(holder, order.getStatus());

        holder.binding.btnTrackOrder.setOnClickListener(v -> listener.onTrackOrder(order));
    }

    private void setStatusBadge(OrderViewHolder holder, String status) {
        if (status == null) status = "Pending";
        holder.binding.tvOrderStatus.setText(status);
        int background;

        switch (status.toLowerCase()) {
            case "pending":
                background = R.drawable.bg_status_pending;
                break;
            case "printing":
            case "accepted":
                background = R.drawable.bg_status_printing;
                break;
            case "delivered":
                background = R.drawable.bg_status_delivered;
                break;
            case "cancelled":
                background = R.drawable.bg_status_cancelled;
                break;
            default:
                background = R.drawable.bg_status_pending;
                break;
        }
        holder.binding.tvOrderStatus.setBackgroundResource(background);
    }

    @Override
    public int getItemCount() {
        return orders.size();
    }

    static class OrderViewHolder extends RecyclerView.ViewHolder {
        ItemOrderBinding binding;
        OrderViewHolder(ItemOrderBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
