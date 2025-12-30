package com.wangninghao.a202305100111.endtest02_accountbook.ui.statistics

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.github.mikephil.charting.animation.Easing
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.PercentFormatter
import com.github.mikephil.charting.utils.ColorTemplate
import com.google.android.material.datepicker.MaterialDatePicker
import com.wangninghao.a202305100111.endtest02_accountbook.AccountBookApplication
import com.wangninghao.a202305100111.endtest02_accountbook.R
import com.wangninghao.a202305100111.endtest02_accountbook.data.entity.RecordType
import com.wangninghao.a202305100111.endtest02_accountbook.data.repository.RecordRepository
import com.wangninghao.a202305100111.endtest02_accountbook.databinding.FragmentStatisticsBinding
import com.wangninghao.a202305100111.endtest02_accountbook.util.DateUtils
import com.wangninghao.a202305100111.endtest02_accountbook.util.SessionManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 统计页面Fragment
 */
class StatisticsFragment : Fragment() {

    private var _binding: FragmentStatisticsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: StatisticsViewModel by viewModels {
        val database = AccountBookApplication.instance.database
        val recordRepository = RecordRepository(database.recordDao())
        val userId = SessionManager(requireContext()).getCurrentUserPhone() ?: ""
        StatisticsViewModelFactory(recordRepository, userId)
    }

    private lateinit var rankingAdapter: RankingAdapter
    private lateinit var categoryBarAdapter: CategoryBarAdapter

    // 图表颜色
    private val chartColors = listOf(
        Color.parseColor("#5C6BC0"),
        Color.parseColor("#42A5F5"),
        Color.parseColor("#26A69A"),
        Color.parseColor("#66BB6A"),
        Color.parseColor("#FFCA28"),
        Color.parseColor("#FF7043"),
        Color.parseColor("#EC407A"),
        Color.parseColor("#AB47BC"),
        Color.parseColor("#78909C")
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStatisticsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupAdapters()
        setupCharts()
        setupClickListeners()
        observeViewModel()
    }

    override fun onResume() {
        super.onResume()
        // 每次返回页面时刷新数据
        viewModel.refreshData()
    }

    private fun setupAdapters() {
        // 排行榜适配器
        rankingAdapter = RankingAdapter()
        binding.rvRanking.adapter = rankingAdapter

        // 分类横向柱形条适配器
        categoryBarAdapter = CategoryBarAdapter()
        binding.rvCategoryBars.adapter = categoryBarAdapter
    }

    private fun setupCharts() {
        // 配置饼图
        binding.pieChart.apply {
            setUsePercentValues(true)
            description.isEnabled = false
            isDrawHoleEnabled = true
            holeRadius = 45f
            transparentCircleRadius = 50f
            setDrawEntryLabels(false)
            legend.isEnabled = true
            legend.textSize = 12f
            setExtraOffsets(5f, 10f, 5f, 5f)
            rotationAngle = 0f
            isRotationEnabled = true
            isHighlightPerTapEnabled = true
        }

        // 配置每日柱形图
        binding.barChartDaily.apply {
            description.isEnabled = false
            setDrawGridBackground(false)
            setDrawBarShadow(false)
            setDrawValueAboveBar(true)
            setPinchZoom(false)
            setScaleEnabled(false)

            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                granularity = 1f
                setDrawGridLines(false)
                textSize = 10f
                textColor = context.getColor(R.color.text_secondary)
            }

            axisLeft.apply {
                axisMinimum = 0f
                setDrawGridLines(true)
                gridColor = Color.parseColor("#E0E0E0")
                textSize = 10f
                textColor = context.getColor(R.color.text_secondary)
            }

            axisRight.isEnabled = false
            legend.isEnabled = false

            // 设置最小条宽和最大条宽
            setFitBars(true)
        }
    }

    private fun setupClickListeners() {
        // 月份选择
        binding.btnMonth.setOnClickListener {
            showMonthPicker()
        }

        // 支出/收入切换
        binding.toggleGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                when (checkedId) {
                    R.id.btnExpense -> viewModel.setRecordType(RecordType.EXPENSE)
                    R.id.btnIncome -> viewModel.setRecordType(RecordType.INCOME)
                }
            }
        }
    }

    private fun observeViewModel() {
        // 观察月份
        viewModel.currentMonth.observe(viewLifecycleOwner) { month ->
            binding.btnMonth.text = DateUtils.getMonthDisplayName(month)
        }

        // 观察类型
        viewModel.recordType.observe(viewLifecycleOwner) { type ->
            binding.tvPieTitle.text = if (type == RecordType.EXPENSE) {
                getString(R.string.expense_composition)
            } else {
                getString(R.string.income_composition)
            }
            rankingAdapter.setRecordType(type)
        }

        // 观察分类统计
        viewModel.categoryStats.observe(viewLifecycleOwner) { stats ->
            if (stats.isEmpty()) {
                binding.layoutEmpty.visibility = View.VISIBLE
            } else {
                binding.layoutEmpty.visibility = View.GONE
                updatePieChart(stats)
                updateCategoryBars(stats)
            }
        }

        // 观察排行榜数据
        viewModel.rankingStats.observe(viewLifecycleOwner) { stats ->
            rankingAdapter.submitList(stats)
        }

        // 观察每日统计
        viewModel.dailyStats.observe(viewLifecycleOwner) { stats ->
            updateDailyBarChart(stats)
        }

        // 观察月度总额（用于计算百分比）
        viewModel.monthTotal.observe(viewLifecycleOwner) { /* Used in updateCategoryBars */ }
    }

    private fun updatePieChart(stats: List<Pair<String, Double>>) {
        val entries = stats.map { PieEntry(it.second.toFloat(), it.first) }
        val dataSet = PieDataSet(entries, "").apply {
            colors = chartColors
            valueTextSize = 11f
            valueTextColor = Color.WHITE
            sliceSpace = 2f
            selectionShift = 5f
        }

        binding.pieChart.apply {
            data = PieData(dataSet).apply {
                setValueFormatter(PercentFormatter(binding.pieChart))
            }
            animateY(800, Easing.EaseInOutQuad)
            invalidate()
        }
    }

    private fun updateCategoryBars(stats: List<Pair<String, Double>>) {
        if (stats.isEmpty()) return

        // 找出最大值，作为100%基准
        val maxAmount = stats.maxOfOrNull { it.second } ?: 0.0
        if (maxAmount == 0.0) return

        // 计算总和用于显示百分比文字
        val total = stats.sumOf { it.second }

        val items = stats.mapIndexed { index, stat ->
            CategoryBarItem(
                category = stat.first,
                amount = stat.second,
                // 进度条长度按最大值为100%绘制
                percent = ((stat.second / maxAmount) * 100).toFloat(),
                // 显示的百分比文字按总和计算
                displayPercent = if (total > 0) ((stat.second / total) * 100).toFloat() else 0f,
                color = chartColors[index % chartColors.size]
            )
        }
        categoryBarAdapter.submitList(items)
    }

    private fun updateDailyBarChart(stats: List<Pair<String, Double>>) {
        if (stats.isEmpty()) {
            binding.barChartDaily.clear()
            return
        }

        val entries = stats.mapIndexed { index, stat ->
            BarEntry(index.toFloat(), stat.second.toFloat())
        }

        val dataSet = BarDataSet(entries, "").apply {
            color = requireContext().getColor(R.color.primary)
            valueTextSize = 8f
            valueTextColor = requireContext().getColor(R.color.text_secondary)
            setDrawValues(stats.size <= 15) // 数据多时不显示值
        }

        binding.barChartDaily.apply {
            data = BarData(dataSet).apply {
                // 根据数据量动态调整柱形宽度
                barWidth = when {
                    stats.size <= 5 -> 0.4f
                    stats.size <= 10 -> 0.5f
                    stats.size <= 20 -> 0.6f
                    else -> 0.7f
                }
            }

            xAxis.apply {
                valueFormatter = IndexAxisValueFormatter(stats.map { "${it.first.toInt()}日" })
                labelCount = minOf(stats.size, 10)
                labelRotationAngle = if (stats.size > 15) -45f else 0f
            }

            // 设置可见范围，数据少时居中显示
            if (stats.size < 10) {
                setVisibleXRangeMaximum(10f)
                moveViewToX(0f)
            }

            animateY(800)
            invalidate()
        }
    }

    private fun showMonthPicker() {
        val datePicker = MaterialDatePicker.Builder.datePicker()
            .setTitleText("选择月份")
            .build()

        datePicker.addOnPositiveButtonClickListener { selection ->
            val sdf = SimpleDateFormat("yyyy-MM", Locale.CHINA)
            viewModel.setMonth(sdf.format(Date(selection)))
        }

        datePicker.show(parentFragmentManager, "month_picker")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
