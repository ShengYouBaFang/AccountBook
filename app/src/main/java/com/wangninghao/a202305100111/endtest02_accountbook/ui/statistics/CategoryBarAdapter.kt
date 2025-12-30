package com.wangninghao.a202305100111.endtest02_accountbook.ui.statistics

import android.animation.ObjectAnimator
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.wangninghao.a202305100111.endtest02_accountbook.databinding.ItemCategoryBarBinding
import com.wangninghao.a202305100111.endtest02_accountbook.util.CurrencyFormatter

/**
 * 分类横向柱形条数据
 * @param percent 进度条长度百分比（按最大值为100%）
 * @param displayPercent 显示的百分比文字（按总和计算）
 */
data class CategoryBarItem(
    val category: String,
    val amount: Double,
    val percent: Float,
    val displayPercent: Float = percent,
    val color: Int
)

/**
 * 分类横向柱形条适配器
 */
class CategoryBarAdapter : ListAdapter<CategoryBarItem, CategoryBarAdapter.ViewHolder>(DiffCallback()) {

    // 记录是否需要播放动画（仅在首次加载或数据变更时）
    private var shouldAnimate = true

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemCategoryBarBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position), shouldAnimate)
    }

    override fun submitList(list: List<CategoryBarItem>?) {
        shouldAnimate = true
        super.submitList(list) {
            // 动画播放完成后标记为不需要动画
            shouldAnimate = false
        }
    }

    inner class ViewHolder(
        private val binding: ItemCategoryBarBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        private var currentAnimator: ObjectAnimator? = null

        fun bind(item: CategoryBarItem, animate: Boolean) {
            binding.tvCategory.text = item.category
            binding.tvAmount.text = "¥${CurrencyFormatter.format(item.amount)}"
            // 显示的百分比使用 displayPercent（按总和计算的占比）
            binding.tvPercent.text = String.format("%.1f%%", item.displayPercent)

            // 设置进度条颜色
            binding.progressBar.progressDrawable.setTint(item.color)

            // 取消之前的动画
            currentAnimator?.cancel()

            // 进度条长度使用 percent（按最大值为100%）
            val targetProgress = item.percent.toInt().coerceIn(0, 100)

            if (animate) {
                // 先重置进度为0
                binding.progressBar.progress = 0

                // 动画设置进度
                currentAnimator = ObjectAnimator.ofInt(
                    binding.progressBar,
                    "progress",
                    0,
                    targetProgress
                ).apply {
                    duration = 800
                    interpolator = DecelerateInterpolator()
                    startDelay = (bindingAdapterPosition * 80).toLong() // 错开动画时间
                    start()
                }
            } else {
                // 不播放动画，直接设置进度
                binding.progressBar.progress = targetProgress
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<CategoryBarItem>() {
        override fun areItemsTheSame(oldItem: CategoryBarItem, newItem: CategoryBarItem) =
            oldItem.category == newItem.category

        override fun areContentsTheSame(oldItem: CategoryBarItem, newItem: CategoryBarItem) =
            oldItem == newItem
    }
}
