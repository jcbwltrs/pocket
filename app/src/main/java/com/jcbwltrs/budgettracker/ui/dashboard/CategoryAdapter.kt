package com.jcbwltrs.budgettracker.ui.dashboard

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.jcbwltrs.budgettracker.R
import com.jcbwltrs.budgettracker.data.model.Category
import com.jcbwltrs.budgettracker.databinding.ItemCategoryBinding
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.absoluteValue

class CategoryAdapter(
    private val onCategoryClick: (Triple<Category, Double, Double>) -> Unit,
    private val onCategoryLongClick: (Triple<Category, Double, Double>) -> Boolean,
    private val calculateDailyBudget: (Category, Double) -> Double,
    private val calculateBusRides: (Double) -> Int
) : ListAdapter<Triple<Category, Double, Double>, CategoryAdapter.CategoryViewHolder>(CategoryDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val binding = ItemCategoryBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return CategoryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class CategoryViewHolder(
        private val binding: ItemCategoryBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(categoryData: Triple<Category, Double, Double>) {
            val (category, spent, budget) = categoryData
            val remaining = budget - spent
            val isOverspent = remaining < 0
            val isExact = remaining == 0.0
            val currencyFormat = NumberFormat.getCurrencyInstance(Locale.US)

            with(binding.root) {
                setOnClickListener { onCategoryClick(categoryData) }
                setOnLongClickListener {
                    // Return true to indicate we've handled the long click and prevent the normal click
                    onCategoryLongClick(categoryData)
                }
            }
            binding.tvCategoryName.text = category.name

            // Set amount with +/- sign
            binding.tvAmount.text = when {
                isOverspent -> "-$" + currencyFormat.format(remaining.absoluteValue).substring(1)
                else -> "+$" + currencyFormat.format(remaining.absoluteValue).substring(1)
            }

            // Update the appearance based on status
            binding.root.background = when {
                isOverspent -> itemView.context.getDrawable(R.drawable.bg_category_overspent)
                isExact -> itemView.context.getDrawable(R.drawable.bg_category_completed)
                else -> itemView.context.getDrawable(R.drawable.bg_category_normal)
            }

            // Handle progress bar visibility
            binding.progressCategory.visibility = when {
                isOverspent || isExact -> View.GONE
                else -> View.VISIBLE
            }

            // Update progress bar if visible
            if (!isOverspent && !isExact) {
                binding.progressCategory.max = budget.toInt()
                binding.progressCategory.progress = spent.toInt()
            }

            // Handle special category information
            binding.tvAdditionalInfo.apply {
                when (category.name) {
                    "Discretionary" -> {
                        visibility = View.VISIBLE
                        text = "Daily: ${currencyFormat.format(calculateDailyBudget(category, budget))}"
                    }
                    "Transportation" -> {
                        visibility = View.VISIBLE
                        text = "${calculateBusRides(remaining)} rides left"
                    }
                    else -> {
                        visibility = View.GONE
                    }
                }
            }

            // Handle status icons
            binding.ivStatus.apply {
                when {
                    isOverspent -> {
                        visibility = View.VISIBLE
                        setImageResource(R.drawable.ic_close)
                        setColorFilter(itemView.context.getColor(android.R.color.holo_red_light))
                    }
                    isExact -> {
                        visibility = View.VISIBLE
                        setImageResource(R.drawable.ic_check)
                        setColorFilter(itemView.context.getColor(android.R.color.holo_green_light))
                    }
                    else -> {
                        visibility = View.GONE
                    }
                }
            }
        }
    }
}

private class CategoryDiffCallback : DiffUtil.ItemCallback<Triple<Category, Double, Double>>() {
    override fun areItemsTheSame(oldItem: Triple<Category, Double, Double>, newItem: Triple<Category, Double, Double>): Boolean {
        return oldItem.first.id == newItem.first.id
    }

    override fun areContentsTheSame(oldItem: Triple<Category, Double, Double>, newItem: Triple<Category, Double, Double>): Boolean {
        return oldItem == newItem
    }
}
