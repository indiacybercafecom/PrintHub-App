package com.indiacybercafe.printhub.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.indiacybercafe.printhub.databinding.ItemFaqBinding
import com.indiacybercafe.printhub.models.FaqModel

class FaqAdapter(private val faqList: List<FaqModel>) : RecyclerView.Adapter<FaqAdapter.FaqViewHolder>() {

    class FaqViewHolder(val binding: ItemFaqBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FaqViewHolder {
        val binding = ItemFaqBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return FaqViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FaqViewHolder, position: Int) {
        val faq = faqList[position]
        holder.binding.apply {
            tvQuestion.text = faq.question
            tvAnswer.text = faq.answer
            
            val isExpanded = faq.isExpanded
            tvAnswer.visibility = if (isExpanded) View.VISIBLE else View.GONE
            ivExpand.rotation = if (isExpanded) 180f else 0f

            root.setOnClickListener {
                faq.isExpanded = !faq.isExpanded
                notifyItemChanged(position)
            }
        }
    }

    override fun getItemCount(): Int = faqList.size
}
