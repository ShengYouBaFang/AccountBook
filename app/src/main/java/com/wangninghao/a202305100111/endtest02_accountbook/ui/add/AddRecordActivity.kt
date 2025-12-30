package com.wangninghao.a202305100111.endtest02_accountbook.ui.add

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.doAfterTextChanged
import com.google.android.material.datepicker.MaterialDatePicker
import com.wangninghao.a202305100111.endtest02_accountbook.AccountBookApplication
import com.wangninghao.a202305100111.endtest02_accountbook.R
import com.wangninghao.a202305100111.endtest02_accountbook.data.entity.RecordType
import com.wangninghao.a202305100111.endtest02_accountbook.data.repository.CategoryRepository
import com.wangninghao.a202305100111.endtest02_accountbook.data.repository.OCRRepository
import com.wangninghao.a202305100111.endtest02_accountbook.data.repository.RecordRepository
import com.wangninghao.a202305100111.endtest02_accountbook.databinding.ActivityAddRecordBinding
import com.wangninghao.a202305100111.endtest02_accountbook.util.DateUtils
import com.wangninghao.a202305100111.endtest02_accountbook.util.SessionManager
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 记账页面
 */
class AddRecordActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddRecordBinding
    private lateinit var categoryAdapter: CategoryAdapter

    // 用于拍照的临时文件Uri
    private var photoUri: Uri? = null

    private val viewModel: AddRecordViewModel by viewModels {
        val database = AccountBookApplication.instance.database
        val recordRepository = RecordRepository(database.recordDao())
        val categoryRepository = CategoryRepository(database.categoryDao())
        val ocrRepository = OCRRepository(this)
        val userId = SessionManager(this).getCurrentUserPhone() ?: ""
        AddRecordViewModelFactory(recordRepository, categoryRepository, ocrRepository, userId)
    }

    // 相机权限请求
    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            launchCamera()
        } else {
            Toast.makeText(this, "需要相机权限才能拍照", Toast.LENGTH_SHORT).show()
        }
    }

    // 拍照结果
    private val takePictureLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            photoUri?.let { uri ->
                viewModel.recognizeReceipt(this, uri)
            }
        }
    }

    // 选择图片结果
    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            viewModel.recognizeReceipt(this, it)
        }
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
            showImageSourceDialog()
        }

        // 初始化日期显示
        updateDateDisplay(System.currentTimeMillis())
    }

    /**
     * 显示图片来源选择对话框
     */
    private fun showImageSourceDialog() {
        // 检查设备是否有相机
        val hasCamera = packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)

        if (hasCamera) {
            val options = arrayOf("拍照", "从相册选择")
            AlertDialog.Builder(this)
                .setTitle("选择小票图片")
                .setItems(options) { _, which ->
                    when (which) {
                        0 -> checkCameraPermissionAndLaunch()
                        1 -> pickImageLauncher.launch("image/*")
                    }
                }
                .show()
        } else {
            // 没有相机，直接打开相册
            pickImageLauncher.launch("image/*")
        }
    }

    /**
     * 检查相机权限并启动相机
     */
    private fun checkCameraPermissionAndLaunch() {
        // 再次检查相机是否可用
        if (!packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)) {
            Toast.makeText(this, "设备没有可用的相机", Toast.LENGTH_SHORT).show()
            return
        }

        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                launchCamera()
            }
            else -> {
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    /**
     * 启动相机拍照
     */
    private fun launchCamera() {
        try {
            val photoFile = createImageFile()
            photoUri = FileProvider.getUriForFile(
                this,
                "${packageName}.fileprovider",
                photoFile
            )
            photoUri?.let { uri ->
                takePictureLauncher.launch(uri)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "无法启动相机: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * 创建用于保存拍照图片的临时文件
     */
    private fun createImageFile(): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val imageFileName = "RECEIPT_${timeStamp}"
        val storageDir = cacheDir
        return File.createTempFile(imageFileName, ".jpg", storageDir)
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

        // 观察OCR状态
        viewModel.ocrState.observe(this) { state ->
            when (state) {
                is OCRState.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                    binding.btnOcr.isEnabled = false
                    Toast.makeText(this, "正在识别小票...", Toast.LENGTH_SHORT).show()
                }
                is OCRState.Success -> {
                    binding.progressBar.visibility = View.GONE
                    binding.btnOcr.isEnabled = true
                    handleOcrSuccess(state.result)
                }
                is OCRState.Error -> {
                    binding.progressBar.visibility = View.GONE
                    binding.btnOcr.isEnabled = true
                    Toast.makeText(this, state.message, Toast.LENGTH_SHORT).show()
                }
                is OCRState.Idle -> {
                    binding.btnOcr.isEnabled = true
                }
            }
        }
    }

    /**
     * 处理OCR识别成功
     */
    private fun handleOcrSuccess(result: com.wangninghao.a202305100111.endtest02_accountbook.network.OCRParsedResult) {
        val amount = result.totalAmount
        val shopName = result.shopName
        val items = result.items

        // 构建提示信息
        val message = buildString {
            append("识别结果：\n")
            if (shopName != null) {
                append("商家：$shopName\n")
            }
            if (amount != null) {
                append("金额：¥${String.format("%.2f", amount)}\n")
            }
            if (items.isNotEmpty()) {
                append("\n商品明细：\n")
                items.take(5).forEach { item ->
                    append("• ${item.name} ¥${item.subtotal}\n")
                }
                if (items.size > 5) {
                    append("...等${items.size}件商品")
                }
            }
        }

        AlertDialog.Builder(this)
            .setTitle("小票识别完成")
            .setMessage(message)
            .setPositiveButton("使用此金额") { _, _ ->
                if (amount != null && amount > 0) {
                    // 自动填充金额
                    binding.etAmount.setText(String.format("%.2f", amount))
                    // 设置为支出
                    viewModel.setRecordType(RecordType.EXPENSE)
                    // 添加备注
                    if (shopName != null) {
                        binding.etNote.setText(shopName)
                    }
                    Toast.makeText(this, "已自动填充金额", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "未识别到有效金额", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("取消", null)
            .show()

        viewModel.clearOcrState()
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
