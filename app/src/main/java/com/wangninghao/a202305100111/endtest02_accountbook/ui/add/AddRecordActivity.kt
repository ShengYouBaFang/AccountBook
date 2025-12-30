package com.wangninghao.a202305100111.endtest02_accountbook.ui.add

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
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

    companion object {
        const val EXTRA_RECORD_ID = "extra_record_id"
    }

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

        // 检查是否为编辑模式
        val recordId = intent.getLongExtra(EXTRA_RECORD_ID, -1L)
        if (recordId != -1L) {
            viewModel.setEditMode(recordId)
        }

        setupToolbar()
        setupViews()
        setupCategoryList()
        observeViewModel()
        playEnterAnimation()
    }

    /**
     * 播放入场动画
     */
    private fun playEnterAnimation() {
        // 获取内容区域的子视图
        val scrollContent = findViewById<View>(R.id.toggleGroup).parent.parent as? View ?: return

        // 初始状态
        scrollContent.alpha = 0f
        scrollContent.translationY = 30f

        // 内容区域动画 - 延迟等待页面跳转动画结束
        AnimatorSet().apply {
            playTogether(
                ObjectAnimator.ofFloat(scrollContent, View.ALPHA, 0f, 1f),
                ObjectAnimator.ofFloat(scrollContent, View.TRANSLATION_Y, 30f, 0f)
            )
            duration = 300
            startDelay = 100  // 减少延迟，让动画更快开始
            interpolator = DecelerateInterpolator()
            start()
        }

        // 保存按钮动画
        binding.btnSave.scaleX = 0.9f
        binding.btnSave.scaleY = 0.9f
        binding.btnSave.alpha = 0f

        binding.btnSave.postDelayed({
            AnimatorSet().apply {
                playTogether(
                    ObjectAnimator.ofFloat(binding.btnSave, View.SCALE_X, 0.9f, 1f),
                    ObjectAnimator.ofFloat(binding.btnSave, View.SCALE_Y, 0.9f, 1f),
                    ObjectAnimator.ofFloat(binding.btnSave, View.ALPHA, 0f, 1f)
                )
                duration = 250
                interpolator = OvershootInterpolator(1.2f)
                start()
            }
        }, 250)
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }

        // 编辑模式下更改标题
        viewModel.isEditMode.observe(this) { isEdit ->
            if (isEdit) {
                binding.toolbar.title = "编辑记录"
            }
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
        categoryAdapter = CategoryAdapter(
            onCategoryClick = { category ->
                viewModel.selectCategory(category)
            },
            onAddClick = {
                showAddCategoryDialog()
            }
        )
        binding.rvCategories.adapter = categoryAdapter
    }

    /**
     * 显示添加自定义分类对话框
     */
    private fun showAddCategoryDialog() {
        val editText = android.widget.EditText(this).apply {
            hint = "请输入分类名称"
            inputType = android.text.InputType.TYPE_CLASS_TEXT
            setPadding(48, 32, 48, 32)
        }

        val typeText = if (viewModel.recordType.value == RecordType.EXPENSE) "支出" else "收入"

        AlertDialog.Builder(this)
            .setTitle("添加$typeText 分类")
            .setView(editText)
            .setPositiveButton("添加") { _, _ ->
                val name = editText.text.toString().trim()
                if (name.isNotEmpty()) {
                    if (name.length > 6) {
                        Toast.makeText(this, "分类名称不能超过6个字符", Toast.LENGTH_SHORT).show()
                    } else {
                        viewModel.addCustomCategory(name)
                        Toast.makeText(this, "添加成功", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this, "请输入分类名称", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("取消", null)
            .show()
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

        // 观察编辑模式数据加载
        viewModel.editRecordData.observe(this) { record ->
            if (record != null) {
                // 填充金额
                binding.etAmount.setText(String.format("%.2f", record.amount))
                // 填充备注
                binding.etNote.setText(record.note)
            }
        }

        // 观察保存状态
        viewModel.saveState.observe(this) { state ->
            when (state) {
                is SaveState.Success -> {
                    val message = if (viewModel.isEditMode.value == true) "修改成功" else getString(R.string.save_success)
                    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
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
        val consumptionTime = result.consumptionTime
        val consumptionDate = result.date
        val items = result.items

        // 构建完整的识别结果信息（展示所有非空字段）
        val message = buildString {
            append("=== 识别结果 ===\n\n")

            // 基本信息
            if (!shopName.isNullOrBlank()) {
                append("【商家名称】$shopName\n")
            }
            if (amount != null && amount > 0) {
                append("【总金额】¥${String.format("%.2f", amount)}\n")
            }
            if (result.paidAmount != null && result.paidAmount > 0) {
                append("【实收金额】¥${String.format("%.2f", result.paidAmount)}\n")
            }
            if (!result.discount.isNullOrBlank()) {
                append("【优惠/折扣】${result.discount}\n")
            }
            if (!result.change.isNullOrBlank()) {
                append("【找零】${result.change}\n")
            }
            if (!result.currency.isNullOrBlank()) {
                append("【币种】${result.currency}\n")
            }

            // 时间信息
            if (!consumptionDate.isNullOrBlank()) {
                append("【消费日期】$consumptionDate\n")
            }
            if (!consumptionTime.isNullOrBlank()) {
                append("【消费时间】$consumptionTime\n")
            }

            // 小票信息
            if (!result.receiptNum.isNullOrBlank()) {
                append("【小票号】${result.receiptNum}\n")
            }

            // 商品明细
            if (items.isNotEmpty()) {
                append("\n=== 商品明细（${items.size}件）===\n")
                items.forEach { item ->
                    append("• ${item.name}")
                    if (item.quantity.isNotBlank()) {
                        append(" x${item.quantity}")
                    }
                    if (item.subtotal.isNotBlank()) {
                        append(" ¥${item.subtotal}")
                    }
                    append("\n")
                }
            }
        }

        // 创建可滚动的对话框
        val scrollView = android.widget.ScrollView(this).apply {
            layoutParams = android.view.ViewGroup.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        val textView = android.widget.TextView(this).apply {
            text = message
            setPadding(48, 24, 48, 24)
            textSize = 14f
        }

        scrollView.addView(textView)

        AlertDialog.Builder(this)
            .setTitle("小票识别完成")
            .setView(scrollView)
            .setPositiveButton("使用识别结果") { _, _ ->
                applyOcrResult(result)
            }
            .setNegativeButton("取消", null)
            .show()

        viewModel.clearOcrState()
    }

    /**
     * 应用OCR识别结果到表单
     */
    private fun applyOcrResult(result: com.wangninghao.a202305100111.endtest02_accountbook.network.OCRParsedResult) {
        val amount = result.totalAmount
        val shopName = result.shopName
        val consumptionTime = result.consumptionTime
        val consumptionDate = result.date

        // 1. 设置为支出类型
        viewModel.setRecordType(RecordType.EXPENSE)

        // 2. 自动填充金额
        if (amount != null && amount > 0) {
            binding.etAmount.setText(String.format("%.2f", amount))
        }

        // 3. 自动填充日期（如果识别到了消费日期）
        if (!consumptionDate.isNullOrBlank()) {
            try {
                // 尝试解析日期字符串，支持多种格式
                val parsedDate = parseOcrDate(consumptionDate)
                if (parsedDate != null) {
                    viewModel.setDate(parsedDate)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // 4. 自动填充备注（店名 + 消费时间）
        val noteBuilder = StringBuilder()
        if (!shopName.isNullOrBlank()) {
            noteBuilder.append(shopName)
        }
        if (!consumptionTime.isNullOrBlank()) {
            if (noteBuilder.isNotEmpty()) {
                noteBuilder.append(" ")
            }
            noteBuilder.append(consumptionTime)
        }
        if (noteBuilder.isNotEmpty()) {
            binding.etNote.setText(noteBuilder.toString())
        }

        Toast.makeText(this, "已自动填充识别结果", Toast.LENGTH_SHORT).show()
    }

    /**
     * 解析OCR识别的日期字符串
     * 支持格式：2020-10-23, 2020/10/23, 2020.10.23 等
     */
    private fun parseOcrDate(dateStr: String): Long? {
        val formats = listOf(
            "yyyy-MM-dd",
            "yyyy/MM/dd",
            "yyyy.MM.dd",
            "yyyy年MM月dd日",
            "MM-dd-yyyy",
            "dd-MM-yyyy"
        )

        for (format in formats) {
            try {
                val sdf = java.text.SimpleDateFormat(format, java.util.Locale.getDefault())
                sdf.isLenient = false
                val date = sdf.parse(dateStr)
                if (date != null) {
                    return date.time
                }
            } catch (e: Exception) {
                // 继续尝试下一个格式
            }
        }
        return null
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
