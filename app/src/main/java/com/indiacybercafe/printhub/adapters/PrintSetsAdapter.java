package com.indiacybercafe.printhub.adapters;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.indiacybercafe.printhub.databinding.ItemPrintSetBinding;
import com.indiacybercafe.printhub.models.FileModel;
import com.indiacybercafe.printhub.models.PrintSet;
import java.util.List;
import java.util.stream.Collectors;

public class PrintSetsAdapter extends RecyclerView.Adapter<PrintSetsAdapter.PrintSetViewHolder> {

    private final List<PrintSet> printSets;
    private final OnSetActionListener listener;

    public interface OnSetActionListener {
        void onDelete(int position);
    }

    public PrintSetsAdapter(List<PrintSet> printSets, OnSetActionListener listener) {
        this.printSets = printSets;
        this.listener = listener;
    }

    @NonNull
    @Override
    public PrintSetViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemPrintSetBinding binding = ItemPrintSetBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new PrintSetViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull PrintSetViewHolder holder, int position) {
        PrintSet set = printSets.get(position);
        holder.binding.tvSetName.setText("Print Set " + (position + 1));
        
        String fileNames = set.getFiles().stream()
                .map(FileModel::getFileName)
                .collect(Collectors.joining(", "));
        holder.binding.tvFilesSummary.setText(set.getFiles().size() + " Files: " + fileNames);

        String settings = String.format("%s, %s, %d Copies, %s",
                set.getSettings().getColorMode(),
                set.getSettings().getPaperQuality(),
                set.getSettings().getCopies(),
                set.getSettings().getBinding());
        holder.binding.tvSettingsSummary.setText(settings);
        
        holder.binding.tvSetPrice.setText("₹" + String.format("%.2f", set.getTotalPrice()));
        
        holder.binding.btnDeleteSet.setOnClickListener(v -> listener.onDelete(position));
    }

    @Override
    public int getItemCount() {
        return printSets.size();
    }

    static class PrintSetViewHolder extends RecyclerView.ViewHolder {
        ItemPrintSetBinding binding;
        PrintSetViewHolder(ItemPrintSetBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
