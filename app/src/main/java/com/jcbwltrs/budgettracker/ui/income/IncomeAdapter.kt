package com.jcbwltrs.budgettracker.ui.income

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.jcbwltrs.budgettracker.data.model.Income
import com.jcbwltrs.budgettracker.databinding.ItemIncomeBinding
import java.text.SimpleDateFormat
import java.util.Locale

class IncomeAdapter(
    private val onItemClick: (Income) -> Unit,
    private val onEditClick: (Income) -> Unit,
    private val onDeleteClick: (Income) -> Unit
) : ListAdapter<Income, IncomeAdapter.IncomeViewHolder>(IncomeDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): IncomeViewHolder {
        val binding = ItemIncomeBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return IncomeViewHolder(binding)
    }

    override fun onBindViewHolder(holder: IncomeViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class IncomeViewHolder(
        private val binding: ItemIncomeBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        private val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.US)

        fun bind(income: Income) {
            binding.apply {
                tvAmount.text = String.format("$%.2f", income.amount)
                tvSource.text = income.source
                tvDescription.text = income.description
                tvDate.text = dateFormat.format(income.date)
                root.setOnClickListener { onItemClick(income) }
                root.setOnLongClickListener { 
                    showPopupMenu(it, income)
                    true
                }
            }
        }

        private fun showPopupMenu(view: View, income: Income) {
            PopupMenu(view.context, view).apply {
                menu.add("Edit").setOnMenuItemClickListener {
                    onEditClick(income)
                    true
                }
                menu.add("Delete").setOnMenuItemClickListener {
                    onDeleteClick(income)
                    true
                }
                show()
            }
        }
    }

    private class IncomeDiffCallback : DiffUtil.ItemCallback<Income>() {
        override fun areItemsTheSame(oldItem: Income, newItem: Income): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Income, newItem: Income): Boolean {
            return oldItem == newItem
        }
    }
}
