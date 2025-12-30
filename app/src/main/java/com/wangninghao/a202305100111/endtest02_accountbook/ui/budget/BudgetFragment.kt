package com.wangninghao.a202305100111.endtest02_accountbook.ui.budget

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.google.android.material.datepicker.MaterialDatePicker
import com.wangninghao.a202305100111.endtest02_accountbook.AccountBookApplication
import com.wangninghao.a202305100111.endtest02_accountbook.R
import com.wangninghao.a202305100111.endtest02_accountbook.data.repository.BudgetRepository
import com.wangninghao.a202305100111.endtest02_accountbook.data.repository.RecordRepository
import com.wangninghao.a202305100111.endtest02_accountbook.databinding.FragmentBudgetBinding
import com.wangninghao.a202305100111.endtest02_accountbook.util.CurrencyFormatter
import com.wangninghao.a202305100111.endtest02_accountbook.util.DateUtils
import com.wangninghao.a202305100111.endtest02_accountbook.util.SessionManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 预算管理页面Fragment
 */
class BudgetFragment : Fragment() {

    private var _binding: FragmentBudgetBinding? = null
    private val binding get() = _binding!!

    private lateinit var categoryBudgetAdapter: CategoryBudgetAdapter

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
        setupRecyclerView()
        setupClickListeners()
        observeViewModel()
    }

    override fun onResume() {
        super.onResume()
        // 每次返回页面时刷新数据
        viewModel.refreshData()
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

    private fun setupRecyclerView() {
        categoryBudgetAdapter = CategoryBudgetAdapter { item ->
            showDeleteBudgetDialog(item)
        }
        binding.rvCategoryBudgets.adapter = categoryBudgetAdapter
    }

    private fun setupClickListeners() {
        binding.btnMonth.setOnClickListener {
            showMonthPicker()
        }

        binding.btnSetTotalBudget.setOnClickListener {
            showSetBudgetDialog()
        }

        binding.btnAddCategoryBudget.setOnClickListener {
            showAddCategoryBudgetDialog()
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

        viewModel.categoryBudgets.observe(viewLifecycleOwner) { budgets ->
            categoryBudgetAdapter.submitList(budgets)

            // 显示/隐藏空状态
            if (budgets.isEmpty()) {
                binding.tvEmpty.visibility = View.VISIBLE
                binding.rvCategoryBudgets.visibility = View.GONE
            } else {
                binding.tvEmpty.visibility = View.GONE
                binding.rvCategoryBudgets.visibility = View.VISIBLE
            }
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

    private fun showMonthPicker() {
        val datePicker = MaterialDatePicker.Builder.datePicker()
            .setTitleText("选择月份")
            .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
            .build()

        datePicker.addOnPositiveButtonClickListener { selection ->
            val sdf = SimpleDateFormat("yyyy-MM", Locale.CHINA)
            val month = sdf.format(Date(selection))
            viewModel.setMonth(month)
        }

        datePicker.show(parentFragmentManager, "month_picker")
    }

    private fun showSetBudgetDialog() {
        val editText = EditText(requireContext()).apply {
            hint = "请输入预算金额"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
            setPadding(48, 32, 48, 32)
        }

        // 如果已有预算，显示当前值
        viewModel.totalBudget.value?.let {
            editText.setText(String.format("%.2f", it.amount))
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

    /**
     * 显示添加分类预算对话框
     */
    private fun showAddCategoryBudgetDialog() {
        val availableCategories = viewModel.getAvailableCategories()

        if (availableCategories.isEmpty()) {
            Toast.makeText(context, "所有分类已设置预算", Toast.LENGTH_SHORT).show()
            return
        }

        // 创建对话框布局
        val layout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 32, 48, 16)
        }

        // 分类选择下拉框
        val spinner = Spinner(requireContext())
        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            availableCategories
        )
        spinner.adapter = adapter
        layout.addView(spinner)

        // 金额输入框
        val editText = EditText(requireContext()).apply {
            hint = "请输入预算金额"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
        }
        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        params.topMargin = 32
        editText.layoutParams = params
        layout.addView(editText)

        AlertDialog.Builder(requireContext())
            .setTitle("添加分类预算")
            .setView(layout)
            .setPositiveButton("添加") { _, _ ->
                val category = spinner.selectedItem as? String
                val amount = editText.text.toString().toDoubleOrNull()

                if (category != null && amount != null && amount > 0) {
                    viewModel.setCategoryBudget(category, amount)
                    Toast.makeText(context, "添加成功", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "请输入正确的金额", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    /**
     * 显示删除预算确认对话框
     */
    private fun showDeleteBudgetDialog(item: CategoryBudgetItem) {
        AlertDialog.Builder(requireContext())
            .setTitle("删除预算")
            .setMessage("确定要删除「${item.category}」的预算吗？")
            .setPositiveButton("删除") { _, _ ->
                viewModel.deleteCategoryBudget(item.category)
                Toast.makeText(context, "已删除", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
