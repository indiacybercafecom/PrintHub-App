package com.indiacybercafe.printhub.adapters;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.indiacybercafe.printhub.R;
import com.indiacybercafe.printhub.databinding.ItemSelectedFileBinding;
import com.indiacybercafe.printhub.models.FileModel;
import java.util.List;

public class SelectedFilesAdapter extends RecyclerView.Adapter<SelectedFilesAdapter.FileViewHolder> {

    private final List<FileModel> fileList;
    private final OnFileRemoveListener removeListener;

    public interface OnFileRemoveListener {
        void onRemove(int position);
    }

    public SelectedFilesAdapter(List<FileModel> fileList, OnFileRemoveListener removeListener) {
        this.fileList = fileList;
        this.removeListener = removeListener;
    }

    @NonNull
    @Override
    public FileViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemSelectedFileBinding binding = ItemSelectedFileBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new FileViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull FileViewHolder holder, int position) {
        FileModel file = fileList.get(position);
        holder.binding.tvFileName.setText(file.getFileName());
        holder.binding.tvFileSize.setText(formatFileSize(file.getFileSize()));
        
        // Set icon based on file type
        int iconRes = R.drawable.bg_otp_box; // Default
        String type = file.getFileType().toLowerCase();
        if (type.contains("pdf")) {
            // holder.binding.ivFileIcon.setImageResource(R.drawable.ic_pdf); // Need to add icons
        }
        
        holder.binding.btnRemove.setOnClickListener(v -> removeListener.onRemove(position));
    }

    @Override
    public int getItemCount() {
        return fileList.size();
    }

    private String formatFileSize(long size) {
        if (size <= 0) return "0 B";
        final String[] units = new String[]{"B", "KB", "MB", "GB", "TB"};
        int digitGroups = (int) (Math.log10(size) / Math.log10(1024));
        return new java.text.DecimalFormat("#,##0.#").format(size / Math.pow(1024, digitGroups)) + " " + units[digitGroups];
    }

    static class FileViewHolder extends RecyclerView.ViewHolder {
        ItemSelectedFileBinding binding;
        FileViewHolder(ItemSelectedFileBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
