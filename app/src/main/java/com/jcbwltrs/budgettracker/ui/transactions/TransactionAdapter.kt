package com.jcbwltrs.budgettracker.ui.transactions

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.jcbwltrs.budgettracker.data.model.Transaction
import com.jcbwltrs.budgettracker.data.model.TransactionWithCategoryName
import com.jcbwltrs.budgettracker.databinding.ItemTransactionBinding
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale

class TransactionAdapter(
    private val onTransactionClick: (Transaction) -> Unit
) : ListAdapter<TransactionWithCategoryName, TransactionAdapter.TransactionViewHolder>(TransactionDiffCallback()) {

    private val dateFormat = SimpleDateFormat("MMM dd", Locale.getDefault())
    private val currencyFormat = NumberFormat.getCurrencyInstance(Locale.US)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder {
        val binding = ItemTransactionBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return TransactionViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class TransactionViewHolder(
        private val binding: ItemTransactionBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(transactionWithCategory: TransactionWithCategoryName) {
            binding.apply {
                root.setOnClickListener { onTransactionClick(transactionWithCategory.transaction) }
                tvMerchant.text = transactionWithCategory.transaction.merchant
                tvDescription.text = transactionWithCategory.transaction.description
                tvAmount.text = currencyFormat.format(
                    if (transactionWithCategory.transaction.categoryId == TransactionType.INCOME.categoryId) {
                        transactionWithCategory.transaction.amount
                    } else {
                        -transactionWithCategory.transaction.amount
                    }
                )
                tvDate.text = dateFormat.format(transactionWithCategory.transaction.date)
                tvCategory.text = transactionWithCategory.categoryName
            }
        }
    }

    private class TransactionDiffCallback : DiffUtil.ItemCallback<TransactionWithCategoryName>() {
        override fun areItemsTheSame(
            oldItem: TransactionWithCategoryName,
            newItem: TransactionWithCategoryName
        ): Boolean {
            return oldItem.transaction.id == newItem.transaction.id
        }

        override fun areContentsTheSame(
            oldItem: TransactionWithCategoryName,
            newItem: TransactionWithCategoryName
        ): Boolean {
            return oldItem == newItem
        }
    }
}