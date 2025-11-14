package com.jcbwltrs.budgettracker.ui.dashboard

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
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
    private val onAddCategoryClick: () -> Unit,
    private val calculateDailyBudget: (Category, Double) -> Double,
    private val calculateBusRides: (Double) -> Int
) : ListAdapter<Triple<Category, Double, Double>, RecyclerView.ViewHolder>(CategoryDiffCallback()) {

    companion object {
        private const val VIEW_TYPE_CATEGORY = 0
        private const val VIEW_TYPE_ADD = 1
    }

    // NEW: Create a list of our "normal" pastel colors
    private val pastelColors = listOf(
        R.color.pastel_orange,
        R.color.pastel_orange,
        R.color.pastel_orange,
        R.color.pastel_pink,
        R.color.pastel_pink,
        R.color.pastel_pink,
        R.color.pastel_yellow,
        R.color.pastel_yellow,
        R.color.pastel_yellow,
        R.color.pastel_purple,
        R.color.pastel_purple,
        R.color.pastel_purple,
    )

    inner class AddCategoryViewHolder(view: View) : RecyclerView.ViewHolder(view)

    override fun getItemCount(): Int {
        return currentList.size + 1
    }

    override fun getItemViewType(position: Int): Int {
        return if (position == currentList.size) {
            VIEW_TYPE_ADD
        } else {
            VIEW_TYPE_CATEGORY
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == VIEW_TYPE_CATEGORY) {
            val binding = ItemCategoryBinding.inflate(inflater, parent, false)
            CategoryViewHolder(binding)
        } else {
            val view = inflater.inflate(R.layout.item_add_category, parent, false)
            AddCategoryViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder.itemViewType == VIEW_TYPE_CATEGORY) {
            val categoryData = getItem(position)
            // Pass the position to bind, so we can cycle colors
            (holder as CategoryViewHolder).bind(categoryData, position)
        } else {
            (holder as AddCategoryViewHolder).itemView.setOnClickListener {
                onAddCategoryClick()
            }
        }
    }

    inner class CategoryViewHolder(
        private val binding: ItemCategoryBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        // Add position to the bind function parameters
        fun bind(categoryData: Triple<Category, Double, Double>, position: Int) {
            val (category, spent, budget) = categoryData
            val remaining = budget - spent
            val isOverspent = remaining < 0
            val isExact = remaining == 0.0
            val currencyFormat = NumberFormat.getCurrencyInstance(Locale.US)

            with(binding.root) {
                setOnClickListener { onCategoryClick(categoryData) }
                setOnLongClickListener {
                    onCategoryLongClick(categoryData)
                }
            }
            binding.tvCategoryName.text = category.name
            binding.tvAmount.text = when {
                isOverspent -> "-$" + currencyFormat.format(remaining.absoluteValue).substring(1)
                else -> "+$" + currencyFormat.format(remaining.absoluteValue).substring(1)
            }

            // --- THIS IS THE NEW LOGIC ---
            // Replace the old background drawable logic with this:
            val colorRes = when {
                isOverspent -> R.color.accent_red
                isExact -> R.color.pastel_purple // Our "completed" state
                else -> pastelColors[position % pastelColors.size] // Cycle through pastels
            }
            binding.root.setCardBackgroundColor(ContextCompat.getColor(itemView.context, colorRes))
            // --- END OF NEW LOGIC ---


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
                val context = itemView.context
                when {
                    isOverspent -> {
                        visibility = View.VISIBLE
                        setImageResource(R.drawable.ic_close)
                        setColorFilter(ContextCompat.getColor(context, R.color.text_dark_primary)) // Use dark text
                    }
                    isExact -> {
                        visibility = View.VISIBLE
                        setImageResource(R.drawable.ic_check)
                        setColorFilter(ContextCompat.getColor(context, R.color.text_dark_primary)) // Use dark text
                    }
                    else -> {
                        visibility = View.GONE
                    }
                }
            }
        }
    }
}

// DiffCallback is unchanged
private class CategoryDiffCallback : DiffUtil.ItemCallback<Triple<Category, Double, Double>>() {
    override fun areItemsTheSame(oldItem: Triple<Category, Double, Double>, newItem: Triple<Category, Double, Double>): Boolean {
        return oldItem.first.id == newItem.first.id
    }

    override fun areContentsTheSame(oldItem: Triple<Category, Double, Double>, newItem: Triple<Category, Double, Double>): Boolean {
        return oldItem == newItem
    }
}