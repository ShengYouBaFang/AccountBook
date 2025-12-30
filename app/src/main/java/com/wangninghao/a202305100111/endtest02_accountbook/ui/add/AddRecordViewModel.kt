package com.wangninghao.a202305100111.endtest02_accountbook.ui.add

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.wangninghao.a202305100111.endtest02_accountbook.data.entity.Category
import com.wangninghao.a202305100111.endtest02_accountbook.data.entity.Record
import com.wangninghao.a202305100111.endtest02_accountbook.data.entity.RecordType
import com.wangninghao.a202305100111.endtest02_accountbook.data.repository.CategoryRepository
import com.wangninghao.a202305100111.endtest02_accountbook.data.repository.RecordRepository
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * 记账页面ViewModel
 */
class AddRecordViewModel(
    private val recordRepository: RecordRepository,
    private val categoryRepository: CategoryRepository,
    private val userId: String
) : ViewModel() {

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

    init {
        loadCategories()
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
     * 保存记录
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
                val record = Record(
                    userId = userId,
                    type = _recordType.value ?: RecordType.EXPENSE,
                    amount = amount,
                    category = category.name,
                    note = note,
                    timestamp = _selectedDate.value ?: System.currentTimeMillis()
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
 * ViewModel工厂
 */
class AddRecordViewModelFactory(
    private val recordRepository: RecordRepository,
    private val categoryRepository: CategoryRepository,
    private val userId: String
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AddRecordViewModel::class.java)) {
            return AddRecordViewModel(recordRepository, categoryRepository, userId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
