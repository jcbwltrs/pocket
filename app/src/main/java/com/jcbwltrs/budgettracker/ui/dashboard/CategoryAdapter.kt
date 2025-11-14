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

// 1. Add the new click listener for the "Add" button to the constructor
class CategoryAdapter(
    private val onCategoryClick: (Triple<Category, Double, Double>) -> Unit,
    private val onCategoryLongClick: (Triple<Category, Double, Double>) -> Boolean,
    private val onAddCategoryClick: () -> Unit, // <-- NEW
    private val calculateDailyBudget: (Category, Double) -> Double,
    private val calculateBusRides: (Double) -> Int
) : ListAdapter<Triple<Category, Double, Double>, RecyclerView.ViewHolder>(CategoryDiffCallback()) { // 2. Changed to generic RecyclerView.ViewHolder

    // 3. Define constants for our two view types
    companion object {
        private const val VIEW_TYPE_CATEGORY = 0
        private const val VIEW_TYPE_ADD = 1
    }

    // 4. Create a simple ViewHolder for the "Add" card
    inner class AddCategoryViewHolder(view: View) : RecyclerView.ViewHolder(view)

    // 5. Override getItemCount to add 1 for our "Add" button
    override fun getItemCount(): Int {
        return currentList.size + 1 // List size + 1 for the "Add" button
    }

    // 6. Override getItemViewType to tell the adapter which layout to use
    override fun getItemViewType(position: Int): Int {
        return if (position == currentList.size) {
            VIEW_TYPE_ADD
        } else {
            VIEW_TYPE_CATEGORY
        }
    }

    // 7. Update onCreateViewHolder to inflate the correct layout
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == VIEW_TYPE_CATEGORY) {
            // This is the same as your old code
            val binding = ItemCategoryBinding.inflate(inflater, parent, false)
            CategoryViewHolder(binding)
        } else {
            // This inflates our new item_add_category.xml layout
            val view = inflater.inflate(R.layout.item_add_category, parent, false)
            AddCategoryViewHolder(view)
        }
    }

    // 8. Update onBindViewHolder to bind the correct data or click listener
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder.itemViewType == VIEW_TYPE_CATEGORY) {
            // This is a normal category, bind it as before
            val categoryData = getItem(position)
            (holder as CategoryViewHolder).bind(categoryData)
        } else {
            // This is our "Add" button, just set its click listener
            (holder as AddCategoryViewHolder).itemView.setOnClickListener {
                onAddCategoryClick()
            }
        }
    }

    // This is your original ViewHolder
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
            // NOTE: We will update these drawables in the next step
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
                val context = itemView.context
                when {
                    isOverspent -> {
                        visibility = View.VISIBLE
                        setImageResource(R.drawable.ic_close)
                        // UPDATED: Use new theme accent color
                        setColorFilter(ContextCompat.getColor(context, R.color.accent_red))
                    }
                    isExact -> {
                        visibility = View.VISIBLE
                        setImageResource(R.drawable.ic_check)
                        // UPDATED: Use new theme accent color
                        setColorFilter(ContextCompat.getColor(context, R.color.accent_green))
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