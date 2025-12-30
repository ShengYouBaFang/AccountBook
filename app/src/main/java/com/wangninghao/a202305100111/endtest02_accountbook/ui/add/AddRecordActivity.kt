package com.wangninghao.a202305100111.endtest02_accountbook.ui.add

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.doAfterTextChanged
import com.google.android.material.datepicker.MaterialDatePicker
import com.wangninghao.a202305100111.endtest02_accountbook.AccountBookApplication
import com.wangninghao.a202305100111.endtest02_accountbook.R
import com.wangninghao.a202305100111.endtest02_accountbook.data.entity.RecordType
import com.wangninghao.a202305100111.endtest02_accountbook.data.repository.CategoryRepository
import com.wangninghao.a202305100111.endtest02_accountbook.data.repository.RecordRepository
import com.wangninghao.a202305100111.endtest02_accountbook.databinding.ActivityAddRecordBinding
import com.wangninghao.a202305100111.endtest02_accountbook.util.DateUtils
import com.wangninghao.a202305100111.endtest02_accountbook.util.SessionManager
import java.util.Date

/**
 * 记账页面
 */
class AddRecordActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddRecordBinding
    private lateinit var categoryAdapter: CategoryAdapter

    private val viewModel: AddRecordViewModel by viewModels {
        val database = AccountBookApplication.instance.database
        val recordRepository = RecordRepository(database.recordDao())
        val categoryRepository = CategoryRepository(database.categoryDao())
        val userId = SessionManager(this).getCurrentUserPhone() ?: ""
        AddRecordViewModelFactory(recordRepository, categoryRepository, userId)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityAddRecordBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 处理系统栏
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupToolbar()
        setupViews()
        setupCategoryList()
        observeViewModel()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun setupViews() {
        // 支出/收入切换
        binding.toggleGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                when (checkedId) {
                    R.id.btnExpense -> viewModel.setRecordType(RecordType.EXPENSE)
                    R.id.btnIncome -> viewModel.setRecordType(RecordType.INCOME)
                }
            }
        }

        // 金额输入监听
        binding.etAmount.doAfterTextChanged {
            updateSaveButtonState()
        }

        // 日期选择
        binding.cardDate.setOnClickListener {
            showDatePicker()
        }

        // 保存按钮
        binding.btnSave.setOnClickListener {
            saveRecord()
        }

        // OCR按钮
        binding.btnOcr.setOnClickListener {
            Toast.makeText(this, "OCR功能将在后续版本中实现", Toast.LENGTH_SHORT).show()
        }

        // 初始化日期显示
        updateDateDisplay(System.currentTimeMillis())
    }

    private fun setupCategoryList() {
        categoryAdapter = CategoryAdapter { category ->
            viewModel.selectCategory(category)
        }
        binding.rvCategories.adapter = categoryAdapter
    }

    private fun observeViewModel() {
        // 观察记录类型
        viewModel.recordType.observe(this) { type ->
            // 更新按钮选中状态
            when (type) {
                RecordType.EXPENSE -> binding.toggleGroup.check(R.id.btnExpense)
                RecordType.INCOME -> binding.toggleGroup.check(R.id.btnIncome)
            }
        }

        // 观察分类列表
        viewModel.categories.observe(this) { categories ->
            categoryAdapter.submitList(categories)
        }

        // 观察选中的分类
        viewModel.selectedCategory.observe(this) { category ->
            categoryAdapter.setSelectedCategory(category?.id)
            updateSaveButtonState()
        }

        // 观察日期
        viewModel.selectedDate.observe(this) { timestamp ->
            updateDateDisplay(timestamp)
        }

        // 观察保存状态
        viewModel.saveState.observe(this) { state ->
            when (state) {
                is SaveState.Success -> {
                    Toast.makeText(this, R.string.save_success, Toast.LENGTH_SHORT).show()
                    finish()
                }
                is SaveState.Error -> {
                    Toast.makeText(this, state.message, Toast.LENGTH_SHORT).show()
                }
                is SaveState.Idle -> {
                    // 空闲状态
                }
            }
        }

        // 观察加载状态
        viewModel.isLoading.observe(this) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            binding.btnSave.isEnabled = !isLoading && canSave()
        }
    }

    private fun updateDateDisplay(timestamp: Long) {
        val friendlyDate = DateUtils.getFriendlyDate(timestamp)
        val fullDate = DateUtils.formatDate(timestamp)
        binding.tvDate.text = if (friendlyDate == fullDate.substring(5)) {
            friendlyDate
        } else {
            "$friendlyDate ${fullDate.substring(5)}"
        }
    }

    private fun showDatePicker() {
        val datePicker = MaterialDatePicker.Builder.datePicker()
            .setTitleText(R.string.select_date)
            .setSelection(viewModel.selectedDate.value ?: MaterialDatePicker.todayInUtcMilliseconds())
            .build()

        datePicker.addOnPositiveButtonClickListener { selection ->
            viewModel.setDate(selection)
        }

        datePicker.show(supportFragmentManager, "date_picker")
    }

    private fun saveRecord() {
        val amountStr = binding.etAmount.text?.toString()?.trim() ?: ""
        val amount = amountStr.toDoubleOrNull() ?: 0.0
        val note = binding.etNote.text?.toString()?.trim() ?: ""

        if (amount <= 0) {
            Toast.makeText(this, R.string.error_amount_invalid, Toast.LENGTH_SHORT).show()
            return
        }

        viewModel.saveRecord(amount, note)
    }

    private fun canSave(): Boolean {
        val amountStr = binding.etAmount.text?.toString()?.trim() ?: ""
        val amount = amountStr.toDoubleOrNull() ?: 0.0
        return amount > 0 && viewModel.selectedCategory.value != null
    }

    private fun updateSaveButtonState() {
        binding.btnSave.isEnabled = canSave() && viewModel.isLoading.value != true
    }
}
