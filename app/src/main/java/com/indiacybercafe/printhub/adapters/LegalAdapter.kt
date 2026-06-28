package com.indiacybercafe.printhub.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.indiacybercafe.printhub.databinding.ItemLegalBinding
import com.indiacybercafe.printhub.models.LegalItem

class LegalAdapter(
    private val items: List<LegalItem>,
    private val onItemClick: (LegalItem) -> Unit
) : RecyclerView.Adapter<LegalAdapter.LegalViewHolder>() {

    inner class LegalViewHolder(val binding: ItemLegalBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LegalViewHolder {
        val binding = ItemLegalBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return LegalViewHolder(binding)
    }

    override fun onBindViewHolder(holder: LegalViewHolder, position: Int) {
        val item = items[position]
        holder.binding.apply {
            ivIcon.setImageResource(item.icon)
            tvTitle.text = item.title
            tvSubtitle.text = item.subtitle
            
            if (item.isClickable) {
                ivArrow.visibility = View.VISIBLE
                root.setOnClickListener { onItemClick(item) }
            } else {
                ivArrow.visibility = View.GONE
                root.isClickable = false
            }
        }
    }

    override fun getItemCount() = items.size
}
