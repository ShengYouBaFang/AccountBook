package com.wangninghao.a202305100111.endtest02_accountbook.ui.statistics

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.wangninghao.a202305100111.endtest02_accountbook.R
import com.wangninghao.a202305100111.endtest02_accountbook.data.dao.CategoryStatWithCount
import com.wangninghao.a202305100111.endtest02_accountbook.data.entity.RecordType
import com.wangninghao.a202305100111.endtest02_accountbook.databinding.ItemRankingBinding
import com.wangninghao.a202305100111.endtest02_accountbook.util.CurrencyFormatter

/**
 * 排行榜适配器
 */
class RankingAdapter(
    private val recordType: RecordType = RecordType.EXPENSE
) : ListAdapter<CategoryStatWithCount, RankingAdapter.ViewHolder>(DiffCallback()) {

    private var currentRecordType = recordType

    fun setRecordType(type: RecordType) {
        currentRecordType = type
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemRankingBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position), position + 1)
    }

    inner class ViewHolder(
        private val binding: ItemRankingBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: CategoryStatWithCount, rank: Int) {
            val context = binding.root.context

            // 排名背景
            val rankBg = when (rank) {
                1 -> R.drawable.bg_rank_1
                2 -> R.drawable.bg_rank_2
                3 -> R.drawable.bg_rank_3
                else -> R.drawable.bg_rank_other
            }
            binding.tvRank.setBackgroundResource(rankBg)
            binding.tvRank.text = rank.toString()
            binding.tvRank.setTextColor(
                if (rank <= 3) context.getColor(R.color.white)
                else context.getColor(R.color.text_secondary)
            )

            // 分类图标
            val iconRes = getCategoryIcon(item.category)
            binding.ivCategory.setImageResource(iconRes)

            // 分类名称
            binding.tvCategory.text = item.category

            // 笔数
            binding.tvCount.text = "${item.count}笔"

            // 金额
            val amountText = "¥${CurrencyFormatter.format(item.total)}"
            binding.tvAmount.text = amountText
            binding.tvAmount.setTextColor(
                if (currentRecordType == RecordType.EXPENSE) {
                    context.getColor(R.color.expense_red)
                } else {
                    context.getColor(R.color.income_green)
                }
            )
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

    class DiffCallback : DiffUtil.ItemCallback<CategoryStatWithCount>() {
        override fun areItemsTheSame(
            oldItem: CategoryStatWithCount,
            newItem: CategoryStatWithCount
        ) = oldItem.category == newItem.category

        override fun areContentsTheSame(
            oldItem: CategoryStatWithCount,
            newItem: CategoryStatWithCount
        ) = oldItem == newItem
    }
}
