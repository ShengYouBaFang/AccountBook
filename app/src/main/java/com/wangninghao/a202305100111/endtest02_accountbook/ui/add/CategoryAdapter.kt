package com.wangninghao.a202305100111.endtest02_accountbook.ui.add

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.wangninghao.a202305100111.endtest02_accountbook.R
import com.wangninghao.a202305100111.endtest02_accountbook.data.entity.Category
import com.wangninghao.a202305100111.endtest02_accountbook.databinding.ItemCategoryBinding

/**
 * 分类选择适配器
 * 支持在列表末尾显示"添加"按钮
 */
class CategoryAdapter(
    private val onCategoryClick: (Category) -> Unit,
    private val onAddClick: (() -> Unit)? = null
) : ListAdapter<Category, CategoryAdapter.CategoryViewHolder>(CategoryDiffCallback()) {

    companion object {
        private const val VIEW_TYPE_CATEGORY = 0
        private const val VIEW_TYPE_ADD = 1
    }

    private var selectedCategoryId: Long? = null

    override fun getItemCount(): Int {
        val count = super.getItemCount()
        // 如果有添加回调，额外显示一个"添加"项
        return if (onAddClick != null) count + 1 else count
    }

    override fun getItemViewType(position: Int): Int {
        return if (onAddClick != null && position == super.getItemCount()) {
            VIEW_TYPE_ADD
        } else {
            VIEW_TYPE_CATEGORY
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val binding = ItemCategoryBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return CategoryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        if (getItemViewType(position) == VIEW_TYPE_ADD) {
            holder.bindAdd()
        } else {
            holder.bind(getItem(position))
        }
    }

    fun setSelectedCategory(categoryId: Long?) {
        val oldId = selectedCategoryId
        selectedCategoryId = categoryId

        // 刷新旧的和新的选中项
        currentList.forEachIndexed { index, category ->
            if (category.id == oldId || category.id == categoryId) {
                notifyItemChanged(index)
            }
        }
    }

    inner class CategoryViewHolder(
        private val binding: ItemCategoryBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    if (getItemViewType(position) == VIEW_TYPE_ADD) {
                        onAddClick?.invoke()
                    } else {
                        onCategoryClick(getItem(position))
                    }
                }
            }
        }

        fun bind(category: Category) {
            val context = binding.root.context
            val isSelected = category.id == selectedCategoryId

            // 分类名称
            binding.tvName.text = category.name

            // 图标
            binding.ivIcon.setImageResource(getCategoryIcon(category.name))

            // 选中状态
            if (isSelected) {
                binding.layoutIcon.setBackgroundResource(R.drawable.bg_category_selected)
                binding.ivIcon.setColorFilter(context.getColor(R.color.white))
                binding.tvName.setTextColor(context.getColor(R.color.primary))
            } else {
                binding.layoutIcon.setBackgroundResource(R.drawable.bg_category_unselected)
                binding.ivIcon.setColorFilter(context.getColor(R.color.text_secondary))
                binding.tvName.setTextColor(context.getColor(R.color.text_secondary))
            }
        }

        /**
         * 绑定"添加分类"项
         */
        fun bindAdd() {
            val context = binding.root.context

            binding.tvName.text = "添加"
            binding.ivIcon.setImageResource(R.drawable.ic_add)

            // 使用虚线边框样式
            binding.layoutIcon.setBackgroundResource(R.drawable.bg_category_add)
            binding.ivIcon.setColorFilter(context.getColor(R.color.primary))
            binding.tvName.setTextColor(context.getColor(R.color.primary))
        }

        private fun getCategoryIcon(category: String): Int {
            return when (category) {
                "餐饮" -> R.drawable.ic_category_food
                "交通" -> R.drawable.ic_category_transport
                "购物" -> R.drawable.ic_category_shopping
                "娱乐" -> R.drawable.ic_category_entertainment
                "教育" -> R.drawable.ic_category_education
                "医疗" -> R.drawable.ic_category_medical
                "住房" -> R.drawable.ic_category_housing
                "通讯" -> R.drawable.ic_category_communication
                "工资" -> R.drawable.ic_category_salary
                "奖金" -> R.drawable.ic_category_bonus
                "兼职" -> R.drawable.ic_category_parttime
                "投资" -> R.drawable.ic_category_investment
                "红包" -> R.drawable.ic_category_gift
                else -> R.drawable.ic_category_other
            }
        }
    }

    class CategoryDiffCallback : DiffUtil.ItemCallback<Category>() {
        override fun areItemsTheSame(oldItem: Category, newItem: Category): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Category, newItem: Category): Boolean {
            return oldItem == newItem
        }
    }
}
