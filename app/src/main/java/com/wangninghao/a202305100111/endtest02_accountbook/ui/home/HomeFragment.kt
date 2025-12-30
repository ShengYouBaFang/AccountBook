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
        val options = arrayOf("全部", "支出", "收入", "餐饮", "交通", "购物", "娱乐", "教育", "工资", "奖金", "其他")

        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("选择筛选条件")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> viewModel.clearFilter()
                    1 -> viewModel.setFilterType(com.wangninghao.a202305100111.endtest02_accountbook.data.entity.RecordType.EXPENSE)
                    2 -> viewModel.setFilterType(com.wangninghao.a202305100111.endtest02_accountbook.data.entity.RecordType.INCOME)
                    else -> {
                        viewModel.setFilterType(null)
                        viewModel.setFilterCategory(options[which])
                    }
                }
            }
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
