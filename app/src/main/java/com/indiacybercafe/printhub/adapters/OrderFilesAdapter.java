package com.indiacybercafe.printhub.adapters;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.indiacybercafe.printhub.R;
import com.indiacybercafe.printhub.databinding.ItemOrderFileBinding;
import com.indiacybercafe.printhub.models.FileModel;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class OrderFilesAdapter extends RecyclerView.Adapter<OrderFilesAdapter.FileViewHolder> {

    private final List<FileModel> files;
    private final String orderId;
    private final OnFileClickListener listener;

    public interface OnFileClickListener {
        void onDownloadFile(FileModel file);
    }

    public OrderFilesAdapter(List<FileModel> files, String orderId, OnFileClickListener listener) {
        this.files = files;
        this.orderId = orderId;
        this.listener = listener;
    }

    @NonNull
    @Override
    public FileViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemOrderFileBinding binding = ItemOrderFileBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new FileViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull FileViewHolder holder, int position) {
        FileModel file = files.get(position);
        
        holder.binding.tvFileName.setText(file.getFileName());
        
        String size = formatFileSize(file.getFileSize());
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
        String date = sdf.format(new Date(file.getUploadedAt()));
        
        holder.binding.tvFileDetails.setText(size + " • " + date);
        holder.binding.tvOrderId.setText("Order #" + orderId);
        
        setFileIcon(holder, file.getFileType());

        holder.binding.btnDownload.setOnClickListener(v -> listener.onDownloadFile(file));
    }

    private void setFileIcon(FileViewHolder holder, String mimeType) {
        int icon = R.drawable.ic_service_placeholder;
        if (mimeType != null) {
            if (mimeType.contains("pdf")) icon = R.drawable.pdf;
            else if (mimeType.contains("image")) icon = R.drawable.gallery;
            else if (mimeType.contains("word") || mimeType.contains("msword")) icon = R.drawable.doc;
            else if (mimeType.contains("excel") || mimeType.contains("spreadsheet")) icon = R.drawable.xls;
            else if (mimeType.contains("powerpoint") || mimeType.contains("presentation")) icon = R.drawable.ppt;
        }
        holder.binding.ivFileIcon.setImageResource(icon);
    }

    private String formatFileSize(long size) {
        if (size <= 0) return "0 B";
        final String[] units = new String[]{"B", "KB", "MB", "GB", "TB"};
        int digitGroups = (int) (Math.log10(size) / Math.log10(1024));
        return new java.text.DecimalFormat("#,##0.#").format(size / Math.pow(1024, digitGroups)) + " " + units[digitGroups];
    }

    @Override
    public int getItemCount() {
        return files.size();
    }

    static class FileViewHolder extends RecyclerView.ViewHolder {
        ItemOrderFileBinding binding;
        FileViewHolder(ItemOrderFileBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
