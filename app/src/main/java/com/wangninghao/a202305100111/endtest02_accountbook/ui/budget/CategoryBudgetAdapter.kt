package com.wangninghao.a202305100111.endtest02_accountbook.ui.budget

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.wangninghao.a202305100111.endtest02_accountbook.R
import com.wangninghao.a202305100111.endtest02_accountbook.databinding.ItemCategoryBudgetBinding
import com.wangninghao.a202305100111.endtest02_accountbook.util.CurrencyFormatter

/**
 * 分类预算列表适配器
 */
class CategoryBudgetAdapter(
    private val onDeleteClick: (CategoryBudgetItem) -> Unit
) : ListAdapter<CategoryBudgetItem, CategoryBudgetAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemCategoryBudgetBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(
        private val binding: ItemCategoryBudgetBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.btnDelete.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onDeleteClick(getItem(position))
                }
            }
        }

        fun bind(item: CategoryBudgetItem) {
            val context = binding.root.context

            // 分类名称
            binding.tvCategory.text = item.category

            // 预算金额
            binding.tvBudget.text = "预算 ¥${CurrencyFormatter.format(item.budgetAmount)}"

            // 已使用
            binding.tvUsed.text = "已使用 ¥${CurrencyFormatter.format(item.used)}"

            // 剩余/超支
            if (item.isOverBudget) {
                val overAmount = item.used - item.budgetAmount
                binding.tvRemaining.text = "超支 ¥${CurrencyFormatter.format(overAmount)}"
                binding.tvRemaining.setTextColor(context.getColor(R.color.expense_red))
                binding.progressBar.setIndicatorColor(context.getColor(R.color.expense_red))
            } else {
                binding.tvRemaining.text = "剩余 ¥${CurrencyFormatter.format(item.remaining)}"
                binding.tvRemaining.setTextColor(context.getColor(R.color.income_green))
                binding.progressBar.setIndicatorColor(context.getColor(R.color.primary))
            }

            // 进度条
            binding.progressBar.progress = item.progress.toInt()
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<CategoryBudgetItem>() {
        override fun areItemsTheSame(oldItem: CategoryBudgetItem, newItem: CategoryBudgetItem): Boolean {
            return oldItem.budget.id == newItem.budget.id
        }

        override fun areContentsTheSame(oldItem: CategoryBudgetItem, newItem: CategoryBudgetItem): Boolean {
            return oldItem == newItem
        }
    }
}
