package com.indiacybercafe.printhub.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.indiacybercafe.printhub.databinding.ItemAddressBinding;
import com.indiacybercafe.printhub.models.AddressModel;
import java.util.List;

public class AddressAdapter extends RecyclerView.Adapter<AddressAdapter.AddressViewHolder> {

    private final List<AddressModel> addressList;
    private int selectedPosition = -1;
    private final OnAddressActionListener listener;

    public interface OnAddressActionListener {
        void onSelected(AddressModel address);
        void onDelete(AddressModel address, int position);
        void onEdit(AddressModel address);
    }

    public AddressAdapter(List<AddressModel> addressList, OnAddressActionListener listener) {
        this.addressList = addressList;
        this.listener = listener;
        
        // Set default selected position
        for (int i = 0; i < addressList.size(); i++) {
            if (addressList.get(i).isDefault()) {
                selectedPosition = i;
                break;
            }
        }
    }

    @NonNull
    @Override
    public AddressViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemAddressBinding binding = ItemAddressBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new AddressViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull AddressViewHolder holder, int position) {
        AddressModel address = addressList.get(position);
        holder.binding.tvName.setText(address.getFullName());
        holder.binding.tvPhone.setText(address.getMobile());
        holder.binding.tvAddress.setText(String.format("%s, %s, %s, %s - %s",
                address.getAddressLine1(), address.getAddressLine2(),
                address.getCity(), address.getState(), address.getPincode()));

        holder.binding.rbSelected.setChecked(position == selectedPosition);
        holder.binding.badgeDefault.setVisibility(address.isDefault() ? View.VISIBLE : View.GONE);

        holder.itemView.setOnClickListener(v -> {
            int pos = holder.getBindingAdapterPosition();
            if (pos != RecyclerView.NO_POSITION) {
                updateSelection(pos, address);
            }
        });

        holder.binding.rbSelected.setOnClickListener(v -> {
            int pos = holder.getBindingAdapterPosition();
            if (pos != RecyclerView.NO_POSITION) {
                updateSelection(pos, address);
            }
        });

        holder.binding.btnDelete.setOnClickListener(v -> listener.onDelete(address, holder.getBindingAdapterPosition()));
        holder.binding.btnEdit.setOnClickListener(v -> listener.onEdit(address));
    }

    private void updateSelection(int position, AddressModel address) {
        int previousSelected = selectedPosition;
        selectedPosition = position;
        notifyItemChanged(previousSelected);
        notifyItemChanged(selectedPosition);
        listener.onSelected(address);
    }

    @Override
    public int getItemCount() {
        return addressList.size();
    }

    public void setAddressList(List<AddressModel> list) {
        this.addressList.clear();
        this.addressList.addAll(list);
        
        // Find default again
        selectedPosition = -1;
        for (int i = 0; i < addressList.size(); i++) {
            if (addressList.get(i).isDefault()) {
                selectedPosition = i;
                break;
            }
        }
        notifyDataSetChanged();
    }

    public static class AddressViewHolder extends RecyclerView.ViewHolder {
        public ItemAddressBinding binding;
        public AddressViewHolder(ItemAddressBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
