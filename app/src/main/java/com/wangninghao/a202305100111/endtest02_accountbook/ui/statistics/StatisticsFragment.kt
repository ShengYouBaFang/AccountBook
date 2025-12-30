package com.wangninghao.a202305100111.endtest02_accountbook.ui.statistics

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
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

        setupCharts()
        setupClickListeners()
        observeViewModel()
    }

    private fun setupCharts() {
        // 配置饼图
        binding.pieChart.apply {
            setUsePercentValues(true)
            description.isEnabled = false
            isDrawHoleEnabled = true
            holeRadius = 50f
            setDrawEntryLabels(false)
            legend.isEnabled = true
        }

        // 配置分类柱形图
        binding.barChartCategory.apply {
            description.isEnabled = false
            setDrawGridBackground(false)
            xAxis.position = XAxis.XAxisPosition.BOTTOM
            xAxis.granularity = 1f
            axisLeft.axisMinimum = 0f
            axisRight.isEnabled = false
            legend.isEnabled = false
        }

        // 配置每日柱形图
        binding.barChartDaily.apply {
            description.isEnabled = false
            setDrawGridBackground(false)
            xAxis.position = XAxis.XAxisPosition.BOTTOM
            xAxis.granularity = 1f
            axisLeft.axisMinimum = 0f
            axisRight.isEnabled = false
            legend.isEnabled = false
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
        }

        // 观察分类统计
        viewModel.categoryStats.observe(viewLifecycleOwner) { stats ->
            if (stats.isEmpty()) {
                binding.layoutEmpty.visibility = View.VISIBLE
            } else {
                binding.layoutEmpty.visibility = View.GONE
                updatePieChart(stats)
                updateCategoryBarChart(stats)
            }
        }

        // 观察每日统计
        viewModel.dailyStats.observe(viewLifecycleOwner) { stats ->
            updateDailyBarChart(stats)
        }
    }

    private fun updatePieChart(stats: List<Pair<String, Double>>) {
        val entries = stats.map { PieEntry(it.second.toFloat(), it.first) }
        val dataSet = PieDataSet(entries, "").apply {
            colors = ColorTemplate.MATERIAL_COLORS.toList()
            valueTextSize = 12f
            valueTextColor = Color.WHITE
        }

        binding.pieChart.data = PieData(dataSet).apply {
            setValueFormatter(PercentFormatter(binding.pieChart))
        }
        binding.pieChart.invalidate()
    }

    private fun updateCategoryBarChart(stats: List<Pair<String, Double>>) {
        val entries = stats.mapIndexed { index, stat ->
            BarEntry(index.toFloat(), stat.second.toFloat())
        }
        val dataSet = BarDataSet(entries, "").apply {
            colors = ColorTemplate.MATERIAL_COLORS.toList()
            valueTextSize = 10f
        }

        binding.barChartCategory.apply {
            data = BarData(dataSet)
            xAxis.valueFormatter = IndexAxisValueFormatter(stats.map { it.first })
            invalidate()
        }
    }

    private fun updateDailyBarChart(stats: List<Pair<String, Double>>) {
        val entries = stats.mapIndexed { index, stat ->
            BarEntry(index.toFloat(), stat.second.toFloat())
        }
        val dataSet = BarDataSet(entries, "").apply {
            color = requireContext().getColor(R.color.primary)
            valueTextSize = 8f
        }

        binding.barChartDaily.apply {
            data = BarData(dataSet)
            xAxis.valueFormatter = IndexAxisValueFormatter(stats.map { it.first })
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
