package com.wangninghao.a202305100111.endtest02_accountbook.ui.home

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.OvershootInterpolator
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.google.android.material.datepicker.MaterialDatePicker
import com.wangninghao.a202305100111.endtest02_accountbook.AccountBookApplication
import com.wangninghao.a202305100111.endtest02_accountbook.data.entity.Record
import com.wangninghao.a202305100111.endtest02_accountbook.data.repository.RecordRepository
import com.wangninghao.a202305100111.endtest02_accountbook.databinding.FragmentHomeBinding
import com.wangninghao.a202305100111.endtest02_accountbook.ui.add.AddRecordActivity
import com.wangninghao.a202305100111.endtest02_accountbook.util.CurrencyFormatter
import com.wangninghao.a202305100111.endtest02_accountbook.util.DateUtils
import com.wangninghao.a202305100111.endtest02_accountbook.util.SessionManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 记账本主页Fragment
 */
class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var sessionManager: SessionManager
    private lateinit var adapter: DayRecordsAdapter

    private val viewModel: HomeViewModel by viewModels {
        val database = AccountBookApplication.instance.database
        val recordRepository = RecordRepository(database.recordDao())
        val userId = SessionManager(requireContext()).getCurrentUserPhone() ?: ""
        HomeViewModelFactory(recordRepository, userId)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sessionManager = SessionManager(requireContext())

        setupRecyclerView()
        setupClickListeners()
        observeViewModel()
        animateFab()
    }

    override fun onResume() {
        super.onResume()
        // 每次返回页面时刷新数据
        viewModel.loadRecords()
        // 重新播放列表动画
        binding.recyclerView.scheduleLayoutAnimation()
    }

    private fun animateFab() {
        binding.fabAdd.scaleX = 0f
        binding.fabAdd.scaleY = 0f

        binding.fabAdd.postDelayed({
            AnimatorSet().apply {
                playTogether(
                    ObjectAnimator.ofFloat(binding.fabAdd, View.SCALE_X, 0f, 1f),
                    ObjectAnimator.ofFloat(binding.fabAdd, View.SCALE_Y, 0f, 1f)
                )
                duration = 400
                interpolator = OvershootInterpolator(2f)
                start()
            }
        }, 300)
    }

    private fun setupRecyclerView() {
        adapter = DayRecordsAdapter { record ->
            // 点击记录，弹出操作对话框
            showRecordActionDialog(record)
        }
        binding.recyclerView.adapter = adapter
    }

    /**
     * 显示记录操作对话框
     */
    private fun showRecordActionDialog(record: Record) {
        val options = arrayOf("编辑", "删除")

        AlertDialog.Builder(requireContext())
            .setTitle("${record.category} ¥${CurrencyFormatter.format(record.amount)}")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> {
                        // 编辑
                        val intent = Intent(requireContext(), AddRecordActivity::class.java)
                        intent.putExtra(AddRecordActivity.EXTRA_RECORD_ID, record.id)
                        startActivity(intent)
                    }
                    1 -> {
                        // 删除
                        showDeleteConfirmDialog(record)
                    }
                }
            }
            .show()
    }

    /**
     * 显示删除确认对话框
     */
    private fun showDeleteConfirmDialog(record: Record) {
        AlertDialog.Builder(requireContext())
            .setTitle("删除记录")
            .setMessage("确定要删除这条记录吗？\n${record.category} ¥${CurrencyFormatter.format(record.amount)}")
            .setPositiveButton("删除") { _, _ ->
                viewModel.deleteRecord(record)
                Toast.makeText(context, "已删除", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun setupClickListeners() {
        // 悬浮按钮 - 添加记录
        binding.fabAdd.setOnClickListener {
            startActivity(Intent(requireContext(), AddRecordActivity::class.java))
        }

        // 月份选择
        binding.btnMonth.setOnClickListener {
            showMonthPicker()
        }

        // 类型筛选
        binding.btnFilter.setOnClickListener {
            showFilterDialog()
        }
    }

    private fun observeViewModel() {
        // 观察月份变化
        viewModel.currentMonth.observe(viewLifecycleOwner) { month ->
            binding.btnMonth.text = DateUtils.getMonthDisplayName(month)
        }

        // 观察记录列表
        viewModel.groupedRecords.observe(viewLifecycleOwner) { records ->
            adapter.submitList(records)

            // 显示/隐藏空状态
            if (records.isEmpty()) {
                binding.layoutEmpty.visibility = View.VISIBLE
                binding.recyclerView.visibility = View.GONE
            } else {
                binding.layoutEmpty.visibility = View.GONE
                binding.recyclerView.visibility = View.VISIBLE
            }
        }

        // 观察月度支出
        viewModel.monthlyExpense.observe(viewLifecycleOwner) { expense ->
            binding.tvTotalExpense.text = "¥${CurrencyFormatter.format(expense)}"
        }

        // 观察月度收入
        viewModel.monthlyIncome.observe(viewLifecycleOwner) { income ->
            binding.tvTotalIncome.text = "¥${CurrencyFormatter.format(income)}"
        }

        // 观察加载状态
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        // 观察筛选状态
        viewModel.filterCategory.observe(viewLifecycleOwner) { category ->
            updateFilterButton()
        }
        viewModel.filterType.observe(viewLifecycleOwner) { type ->
            updateFilterButton()
        }
    }

    private fun updateFilterButton() {
        val type = viewModel.filterType.value
        val category = viewModel.filterCategory.value

        val text = when {
            category != null -> category
            type != null -> if (type == com.wangninghao.a202305100111.endtest02_accountbook.data.entity.RecordType.EXPENSE) "支出" else "收入"
            else -> getString(com.wangninghao.a202305100111.endtest02_accountbook.R.string.filter_all)
        }
        binding.btnFilter.text = text
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

    private fun showFilterDialog() {
        val dialogView = layoutInflater.inflate(
            com.wangninghao.a202305100111.endtest02_accountbook.R.layout.dialog_filter,
            null
        )

        val dialog = com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
            .setView(dialogView)
            .setPositiveButton("确定", null)
            .setNegativeButton("取消", null)
            .create()

        // 获取Chip引用
        val chipAll = dialogView.findViewById<com.google.android.material.chip.Chip>(
            com.wangninghao.a202305100111.endtest02_accountbook.R.id.chipAll
        )
        val chipExpense = dialogView.findViewById<com.google.android.material.chip.Chip>(
            com.wangninghao.a202305100111.endtest02_accountbook.R.id.chipExpense
        )
        val chipIncome = dialogView.findViewById<com.google.android.material.chip.Chip>(
            com.wangninghao.a202305100111.endtest02_accountbook.R.id.chipIncome
        )

        // 支出分类Chips
        val chipFood = dialogView.findViewById<com.google.android.material.chip.Chip>(
            com.wangninghao.a202305100111.endtest02_accountbook.R.id.chipFood
        )
        val chipTransport = dialogView.findViewById<com.google.android.material.chip.Chip>(
            com.wangninghao.a202305100111.endtest02_accountbook.R.id.chipTransport
        )
        val chipShopping = dialogView.findViewById<com.google.android.material.chip.Chip>(
            com.wangninghao.a202305100111.endtest02_accountbook.R.id.chipShopping
        )
        val chipEntertain = dialogView.findViewById<com.google.android.material.chip.Chip>(
            com.wangninghao.a202305100111.endtest02_accountbook.R.id.chipEntertain
        )
        val chipEducation = dialogView.findViewById<com.google.android.material.chip.Chip>(
            com.wangninghao.a202305100111.endtest02_accountbook.R.id.chipEducation
        )
        val chipOther = dialogView.findViewById<com.google.android.material.chip.Chip>(
            com.wangninghao.a202305100111.endtest02_accountbook.R.id.chipOther
        )

        // 收入分类Chips
        val chipSalary = dialogView.findViewById<com.google.android.material.chip.Chip>(
            com.wangninghao.a202305100111.endtest02_accountbook.R.id.chipSalary
        )
        val chipBonus = dialogView.findViewById<com.google.android.material.chip.Chip>(
            com.wangninghao.a202305100111.endtest02_accountbook.R.id.chipBonus
        )
        val chipIncomeOther = dialogView.findViewById<com.google.android.material.chip.Chip>(
            com.wangninghao.a202305100111.endtest02_accountbook.R.id.chipIncomeOther
        )

        // 所有Chips列表（用于清除选择）
        val allChips = listOf(
            chipAll, chipExpense, chipIncome,
            chipFood, chipTransport, chipShopping, chipEntertain, chipEducation, chipOther,
            chipSalary, chipBonus, chipIncomeOther
        )

        // 清除其他选择的函数
        fun clearOtherChips(selected: com.google.android.material.chip.Chip) {
            allChips.forEach { chip ->
                if (chip != selected) chip.isChecked = false
            }
        }

        // 设置点击监听
        chipAll.setOnClickListener { clearOtherChips(chipAll) }
        chipExpense.setOnClickListener { clearOtherChips(chipExpense) }
        chipIncome.setOnClickListener { clearOtherChips(chipIncome) }
        chipFood.setOnClickListener { clearOtherChips(chipFood) }
        chipTransport.setOnClickListener { clearOtherChips(chipTransport) }
        chipShopping.setOnClickListener { clearOtherChips(chipShopping) }
        chipEntertain.setOnClickListener { clearOtherChips(chipEntertain) }
        chipEducation.setOnClickListener { clearOtherChips(chipEducation) }
        chipOther.setOnClickListener { clearOtherChips(chipOther) }
        chipSalary.setOnClickListener { clearOtherChips(chipSalary) }
        chipBonus.setOnClickListener { clearOtherChips(chipBonus) }
        chipIncomeOther.setOnClickListener { clearOtherChips(chipIncomeOther) }

        // 设置当前选择状态
        chipAll.isChecked = true

        dialog.setOnShowListener {
            dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                when {
                    chipAll.isChecked -> viewModel.clearFilter()
                    chipExpense.isChecked -> viewModel.setFilterType(com.wangninghao.a202305100111.endtest02_accountbook.data.entity.RecordType.EXPENSE)
                    chipIncome.isChecked -> viewModel.setFilterType(com.wangninghao.a202305100111.endtest02_accountbook.data.entity.RecordType.INCOME)
                    chipFood.isChecked -> { viewModel.setFilterType(null); viewModel.setFilterCategory("餐饮") }
                    chipTransport.isChecked -> { viewModel.setFilterType(null); viewModel.setFilterCategory("交通") }
                    chipShopping.isChecked -> { viewModel.setFilterType(null); viewModel.setFilterCategory("购物") }
                    chipEntertain.isChecked -> { viewModel.setFilterType(null); viewModel.setFilterCategory("娱乐") }
                    chipEducation.isChecked -> { viewModel.setFilterType(null); viewModel.setFilterCategory("教育") }
                    chipOther.isChecked -> { viewModel.setFilterType(null); viewModel.setFilterCategory("其他") }
                    chipSalary.isChecked -> { viewModel.setFilterType(null); viewModel.setFilterCategory("工资") }
                    chipBonus.isChecked -> { viewModel.setFilterType(null); viewModel.setFilterCategory("奖金") }
                    chipIncomeOther.isChecked -> { viewModel.setFilterType(null); viewModel.setFilterCategory("其他") }
                    else -> viewModel.clearFilter()
                }
                dialog.dismiss()
            }
        }

        dialog.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
