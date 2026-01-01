package com.wangninghao.a202305100111.endtest02_accountbook.ui.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.wangninghao.a202305100111.endtest02_accountbook.R
import com.wangninghao.a202305100111.endtest02_accountbook.data.entity.Record
import com.wangninghao.a202305100111.endtest02_accountbook.data.entity.RecordType
import com.wangninghao.a202305100111.endtest02_accountbook.databinding.ItemRecordBinding
import com.wangninghao.a202305100111.endtest02_accountbook.databinding.ItemRecordDayBinding
import com.wangninghao.a202305100111.endtest02_accountbook.util.CurrencyFormatter
import com.wangninghao.a202305100111.endtest02_accountbook.util.DateUtils

/**
 * 按天分组的记录列表适配器
 */
class DayRecordsAdapter(
    private val onRecordClick: (Record) -> Unit
) : ListAdapter<DayRecords, DayRecordsAdapter.DayViewHolder>(DayDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DayViewHolder {
        val binding = ItemRecordDayBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return DayViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DayViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class DayViewHolder(
        private val binding: ItemRecordDayBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        private val recordAdapter = RecordAdapter { record ->
            onRecordClick(record)
        }

        init {
            binding.rvRecords.adapter = recordAdapter
        }

        fun bind(dayRecords: DayRecords) {
            // 日期显示
            binding.tvDate.text = "${dayRecords.displayDate} ${dayRecords.weekDay}"

            // 当日支出
            if (dayRecords.expense > 0) {
                binding.tvDayExpense.text = "支出 ¥${CurrencyFormatter.format(dayRecords.expense)}"
                binding.tvDayExpense.visibility = View.VISIBLE
            } else {
                binding.tvDayExpense.visibility = View.GONE
            }

            // 当日收入
            if (dayRecords.income > 0) {
                binding.tvDayIncome.text = "收入 ¥${CurrencyFormatter.format(dayRecords.income)}"
                binding.tvDayIncome.visibility = View.VISIBLE
            } else {
                binding.tvDayIncome.visibility = View.GONE
            }

            // 设置记录列表
            recordAdapter.submitList(dayRecords.records)
        }
    }

    class DayDiffCallback : DiffUtil.ItemCallback<DayRecords>() {
        override fun areItemsTheSame(oldItem: DayRecords, newItem: DayRecords): Boolean {
            return oldItem.date == newItem.date
        }

        override fun areContentsTheSame(oldItem: DayRecords, newItem: DayRecords): Boolean {
            return oldItem == newItem
        }
    }
}

/**
 * 单条记录适配器
 */
class RecordAdapter(
    private val onItemClick: (Record) -> Unit
) : ListAdapter<Record, RecordAdapter.RecordViewHolder>(RecordDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecordViewHolder {
        val binding = ItemRecordBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return RecordViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RecordViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class RecordViewHolder(
        private val binding: ItemRecordBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onItemClick(getItem(position))
                }
            }
        }

        fun bind(record: Record) {
            val context = binding.root.context

            // 分类名称
            binding.tvCategory.text = record.category

            // 备注
            if (record.note.isNotBlank()) {
                binding.tvNote.text = record.note
                binding.tvNote.visibility = View.VISIBLE
            } else {
                binding.tvNote.visibility = View.GONE
            }

            // 金额和颜色
            val isExpense = record.type == RecordType.EXPENSE
            val amountText = CurrencyFormatter.formatWithCurrencyAndSign(record.amount, isExpense)
            binding.tvAmount.text = amountText
            binding.tvAmount.setTextColor(
                context.getColor(if (isExpense) R.color.expense_red else R.color.income_green)
            )

            // 时间
            binding.tvTime.text = DateUtils.formatTime(record.timestamp)

            // 设置分类图标（使用默认图标）
            binding.ivCategory.setImageResource(getCategoryIcon(record.category))
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
                else -> R.drawable.ic_category_other
            }
        }
    }

    class RecordDiffCallback : DiffUtil.ItemCallback<Record>() {
        override fun areItemsTheSame(oldItem: Record, newItem: Record): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Record, newItem: Record): Boolean {
            return oldItem == newItem
        }
    }
}
