package com.wangninghao.a202305100111.endtest02_accountbook.ui.budget

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.wangninghao.a202305100111.endtest02_accountbook.AccountBookApplication
import com.wangninghao.a202305100111.endtest02_accountbook.R
import com.wangninghao.a202305100111.endtest02_accountbook.data.repository.BudgetRepository
import com.wangninghao.a202305100111.endtest02_accountbook.data.repository.RecordRepository
import com.wangninghao.a202305100111.endtest02_accountbook.databinding.FragmentBudgetBinding
import com.wangninghao.a202305100111.endtest02_accountbook.util.CurrencyFormatter
import com.wangninghao.a202305100111.endtest02_accountbook.util.DateUtils
import com.wangninghao.a202305100111.endtest02_accountbook.util.SessionManager

/**
 * 预算管理页面Fragment
 */
class BudgetFragment : Fragment() {

    private var _binding: FragmentBudgetBinding? = null
    private val binding get() = _binding!!

    private val viewModel: BudgetViewModel by viewModels {
        val database = AccountBookApplication.instance.database
        val budgetRepository = BudgetRepository(database.budgetDao())
        val recordRepository = RecordRepository(database.recordDao())
        val userId = SessionManager(requireContext()).getCurrentUserPhone() ?: ""
        BudgetViewModelFactory(budgetRepository, recordRepository, userId)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBudgetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupChart()
        setupClickListeners()
        observeViewModel()
    }

    private fun setupChart() {
        binding.pieChartTotal.apply {
            description.isEnabled = false
            isDrawHoleEnabled = true
            holeRadius = 70f
            setDrawEntryLabels(false)
            legend.isEnabled = false
            setTouchEnabled(false)
        }
    }

    private fun setupClickListeners() {
        binding.btnMonth.setOnClickListener {
            // 月份选择
        }

        binding.btnSetTotalBudget.setOnClickListener {
            showSetBudgetDialog()
        }

        binding.btnAddCategoryBudget.setOnClickListener {
            Toast.makeText(context, "分类预算功能开发中", Toast.LENGTH_SHORT).show()
        }
    }

    private fun observeViewModel() {
        viewModel.currentMonth.observe(viewLifecycleOwner) { month ->
            binding.btnMonth.text = DateUtils.getMonthDisplayName(month)
        }

        viewModel.totalBudget.observe(viewLifecycleOwner) { budget ->
            // 更新预算显示
        }

        viewModel.monthlyExpense.observe(viewLifecycleOwner) { expense ->
            val budget = viewModel.totalBudget.value?.amount ?: 0.0
            updateBudgetChart(expense, budget)
        }
    }

    private fun updateBudgetChart(used: Double, total: Double) {
        if (total <= 0) {
            binding.pieChartTotal.clear()
            binding.tvUsed.text = "已使用 ¥${CurrencyFormatter.format(used)}"
            binding.tvRemaining.text = "未设置预算"
            return
        }

        val remaining = (total - used).coerceAtLeast(0.0)
        val entries = listOf(
            PieEntry(used.toFloat(), "已使用"),
            PieEntry(remaining.toFloat(), "剩余")
        )

        val colors = listOf(
            requireContext().getColor(R.color.expense_red),
            requireContext().getColor(R.color.income_green)
        )

        val dataSet = PieDataSet(entries, "").apply {
            this.colors = colors
            setDrawValues(false)
        }

        binding.pieChartTotal.data = PieData(dataSet)
        binding.pieChartTotal.centerText = "¥${CurrencyFormatter.format(total)}"
        binding.pieChartTotal.invalidate()

        binding.tvUsed.text = "已使用 ¥${CurrencyFormatter.format(used)}"
        binding.tvRemaining.text = if (remaining > 0) {
            "剩余 ¥${CurrencyFormatter.format(remaining)}"
        } else {
            "已超支 ¥${CurrencyFormatter.format(-remaining)}"
        }
    }

    private fun showSetBudgetDialog() {
        val editText = EditText(requireContext()).apply {
            hint = "请输入预算金额"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
        }

        AlertDialog.Builder(requireContext())
            .setTitle("设置月度总预算")
            .setView(editText)
            .setPositiveButton("确定") { _, _ ->
                val amount = editText.text.toString().toDoubleOrNull()
                if (amount != null && amount > 0) {
                    viewModel.setTotalBudget(amount)
                } else {
                    Toast.makeText(context, "请输入正确的金额", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
