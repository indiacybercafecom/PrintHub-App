package com.indiacybercafe.printhub

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.indiacybercafe.printhub.databinding.ItemServiceBinding
import com.indiacybercafe.printhub.utils.ServiceIconMapper

class ServiceAdapter(
    private var services: List<ServiceModel>,
    private val onServiceClick: (ServiceModel) -> Unit
) : RecyclerView.Adapter<ServiceAdapter.ServiceViewHolder>() {

    class ServiceViewHolder(val binding: ItemServiceBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ServiceViewHolder {
        val binding = ItemServiceBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ServiceViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ServiceViewHolder, position: Int) {
        val service = services[position]
        holder.binding.txtServiceName.text = service.title
        
        // Ensure no tint or filter is applied to the icon
        holder.binding.imgServiceIcon.clearColorFilter()
        holder.binding.imgServiceIcon.imageTintList = null
        
        // Load icon from local drawable resources instead of Firebase/Glide
        holder.binding.imgServiceIcon.setImageResource(
            ServiceIconMapper.getIcon(service.action)
        )

        // Set dynamic background color from Firebase
        try {
            if (service.backgroundColor.isNotEmpty()) {
                holder.binding.root.setCardBackgroundColor(Color.parseColor(service.backgroundColor))
            }
        } catch (e: Exception) {
            // Default to white if parsing fails
            holder.binding.root.setCardBackgroundColor(Color.WHITE)
        }
        
        holder.itemView.setOnClickListener {
            onServiceClick(service)
        }
    }

    override fun getItemCount(): Int = services.size

    fun updateData(newServices: List<ServiceModel>) {
        this.services = newServices
        notifyDataSetChanged()
    }
}
