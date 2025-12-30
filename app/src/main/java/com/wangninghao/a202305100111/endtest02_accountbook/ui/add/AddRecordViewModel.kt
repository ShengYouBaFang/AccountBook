package com.wangninghao.a202305100111.endtest02_accountbook.ui.add

import android.content.Context
import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.wangninghao.a202305100111.endtest02_accountbook.data.entity.Category
import com.wangninghao.a202305100111.endtest02_accountbook.data.entity.Record
import com.wangninghao.a202305100111.endtest02_accountbook.data.entity.RecordType
import com.wangninghao.a202305100111.endtest02_accountbook.data.repository.CategoryRepository
import com.wangninghao.a202305100111.endtest02_accountbook.data.repository.OCRRepository
import com.wangninghao.a202305100111.endtest02_accountbook.data.repository.RecordRepository
import com.wangninghao.a202305100111.endtest02_accountbook.network.OCRParsedResult
import com.wangninghao.a202305100111.endtest02_accountbook.util.ImageUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 记账页面ViewModel
 */
class AddRecordViewModel(
    private val recordRepository: RecordRepository,
    private val categoryRepository: CategoryRepository,
    private val ocrRepository: OCRRepository,
    private val userId: String
) : ViewModel() {

    // 编辑模式：要编辑的记录ID
    private var editRecordId: Long? = null
    private var originalRecord: Record? = null

    // 是否为编辑模式
    private val _isEditMode = MutableLiveData(false)
    val isEditMode: LiveData<Boolean> = _isEditMode

    // 记录类型（支出/收入）
    private val _recordType = MutableLiveData(RecordType.EXPENSE)
    val recordType: LiveData<RecordType> = _recordType

    // 选择的日期时间戳
    private val _selectedDate = MutableLiveData(System.currentTimeMillis())
    val selectedDate: LiveData<Long> = _selectedDate

    // 分类列表
    private val _categories = MutableLiveData<List<Category>>()
    val categories: LiveData<List<Category>> = _categories

    // 选择的分类
    private val _selectedCategory = MutableLiveData<Category?>()
    val selectedCategory: LiveData<Category?> = _selectedCategory

    // 保存状态
    private val _saveState = MutableLiveData<SaveState>()
    val saveState: LiveData<SaveState> = _saveState

    // 加载状态
    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    // OCR识别结果
    private val _ocrResult = MutableLiveData<OCRParsedResult?>()
    val ocrResult: LiveData<OCRParsedResult?> = _ocrResult

    // OCR状态
    private val _ocrState = MutableLiveData<OCRState>()
    val ocrState: LiveData<OCRState> = _ocrState

    // 编辑模式下加载的记录数据
    private val _editRecordData = MutableLiveData<Record?>()
    val editRecordData: LiveData<Record?> = _editRecordData

    init {
        loadCategories()
    }

    /**
     * 设置编辑模式，加载指定记录
     */
    fun setEditMode(recordId: Long) {
        editRecordId = recordId
        _isEditMode.value = true
        loadRecordForEdit(recordId)
    }

    /**
     * 加载要编辑的记录
     */
    private fun loadRecordForEdit(recordId: Long) {
        viewModelScope.launch {
            val record = recordRepository.getRecordById(recordId)
            if (record != null) {
                originalRecord = record
                _editRecordData.value = record

                // 设置记录类型
                _recordType.value = record.type
                loadCategories()

                // 设置日期
                _selectedDate.value = record.timestamp

                // 设置分类（需要先等分类加载完成）
                viewModelScope.launch {
                    categoryRepository.getCategoriesByType(userId, record.type).collectLatest { categories ->
                        _categories.value = categories
                        // 找到对应的分类
                        val category = categories.find { it.name == record.category }
                        _selectedCategory.value = category
                    }
                }
            }
        }
    }

    /**
     * 加载分类
     */
    private fun loadCategories() {
        viewModelScope.launch {
            val type = _recordType.value ?: RecordType.EXPENSE
            categoryRepository.getCategoriesByType(userId, type).collectLatest { categories ->
                _categories.value = categories
                // 如果之前选择的分类不在新列表中，清除选择
                val selected = _selectedCategory.value
                if (selected != null && categories.none { it.id == selected.id }) {
                    _selectedCategory.value = null
                }
            }
        }
    }

    /**
     * 设置记录类型
     */
    fun setRecordType(type: RecordType) {
        if (_recordType.value != type) {
            _recordType.value = type
            _selectedCategory.value = null
            loadCategories()
        }
    }

    /**
     * 设置日期
     */
    fun setDate(timestamp: Long) {
        _selectedDate.value = timestamp
    }

    /**
     * 选择分类
     */
    fun selectCategory(category: Category) {
        _selectedCategory.value = category
    }

    /**
     * 保存记录（新建或更新）
     */
    fun saveRecord(amount: Double, note: String) {
        val category = _selectedCategory.value
        if (category == null) {
            _saveState.value = SaveState.Error("请选择分类")
            return
        }

        if (amount <= 0) {
            _saveState.value = SaveState.Error("请输入正确的金额")
            return
        }

        _isLoading.value = true
        viewModelScope.launch {
            try {
                if (_isEditMode.value == true && editRecordId != null) {
                    // 更新模式
                    val record = Record(
                        id = editRecordId!!,
                        userId = userId,
                        type = _recordType.value ?: RecordType.EXPENSE,
                        amount = amount,
                        category = category.name,
                        note = note,
                        timestamp = _selectedDate.value ?: System.currentTimeMillis()
                    )
                    recordRepository.updateRecord(record)
                } else {
                    // 新建模式
                    val record = Record(
                        userId = userId,
                        type = _recordType.value ?: RecordType.EXPENSE,
                        amount = amount,
                        category = category.name,
                        note = note,
                        timestamp = _selectedDate.value ?: System.currentTimeMillis()
                    )
                    recordRepository.addRecord(record)
                }
                _saveState.value = SaveState.Success
            } catch (e: Exception) {
                _saveState.value = SaveState.Error(e.message ?: "保存失败")
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * 自动保存OCR识别结果
     */
    fun saveOcrRecord(amount: Double, note: String, date: Long? = null) {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                // OCR记录默认为支出，类型为购物
                val record = Record(
                    userId = userId,
                    type = RecordType.EXPENSE,
                    amount = amount,
                    category = "购物",
                    note = note,
                    timestamp = date ?: System.currentTimeMillis()
                )
                recordRepository.addRecord(record)
                _saveState.value = SaveState.Success
            } catch (e: Exception) {
                _saveState.value = SaveState.Error(e.message ?: "保存失败")
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * 重置保存状态
     */
    fun resetSaveState() {
        _saveState.value = SaveState.Idle
    }

    /**
     * 添加自定义分类
     */
    fun addCustomCategory(name: String) {
        viewModelScope.launch {
            val type = _recordType.value ?: RecordType.EXPENSE
            val result = categoryRepository.addCustomCategory(userId, name, type)
            result.fold(
                onSuccess = {
                    // 分类添加成功，Flow会自动更新
                },
                onFailure = { e ->
                    _saveState.value = SaveState.Error(e.message ?: "添加分类失败")
                }
            )
        }
    }

    /**
     * 执行OCR识别
     */
    fun recognizeReceipt(context: Context, imageUri: Uri) {
        _ocrState.value = OCRState.Loading

        viewModelScope.launch {
            try {
                // 在IO线程处理图片（不做URL编码，Retrofit会自动处理）
                val base64Image = withContext(Dispatchers.IO) {
                    ImageUtils.uriToBase64(context, imageUri)
                }

                if (base64Image == null) {
                    _ocrState.value = OCRState.Error("图片处理失败，请重试")
                    return@launch
                }

                // 调用OCR API
                val result = withContext(Dispatchers.IO) {
                    ocrRepository.recognizeReceipt(base64Image)
                }

                _ocrResult.value = result

                if (result.success) {
                    _ocrState.value = OCRState.Success(result)
                } else {
                    _ocrState.value = OCRState.Error(result.errorMessage ?: "识别失败")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _ocrState.value = OCRState.Error("识别失败: ${e.message}")
            }
        }
    }

    /**
     * 清除OCR状态
     */
    fun clearOcrState() {
        _ocrState.value = OCRState.Idle
        _ocrResult.value = null
    }

    /**
     * 应用OCR结果到表单
     */
    fun applyOcrResult(amount: Double) {
        // 设置为支出类型
        setRecordType(RecordType.EXPENSE)
        // OCR结果由Activity处理金额填充
    }
}

/**
 * 保存状态
 */
sealed class SaveState {
    object Idle : SaveState()
    object Success : SaveState()
    data class Error(val message: String) : SaveState()
}

/**
 * OCR状态
 */
sealed class OCRState {
    object Idle : OCRState()
    object Loading : OCRState()
    data class Success(val result: OCRParsedResult) : OCRState()
    data class Error(val message: String) : OCRState()
}

/**
 * ViewModel工厂
 */
class AddRecordViewModelFactory(
    private val recordRepository: RecordRepository,
    private val categoryRepository: CategoryRepository,
    private val ocrRepository: OCRRepository,
    private val userId: String
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AddRecordViewModel::class.java)) {
            return AddRecordViewModel(recordRepository, categoryRepository, ocrRepository, userId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
