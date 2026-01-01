package com.wangninghao.a202305100111.endtest02_accountbook.ui.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.wangninghao.a202305100111.endtest02_accountbook.data.entity.Record
import com.wangninghao.a202305100111.endtest02_accountbook.data.entity.RecordType
import com.wangninghao.a202305100111.endtest02_accountbook.data.repository.RecordRepository
import com.wangninghao.a202305100111.endtest02_accountbook.util.DateUtils
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * 主页ViewModel
 */
class HomeViewModel(
    private val recordRepository: RecordRepository,
    private val userId: String
) : ViewModel() {

    // 当前选择的月份
    private val _currentMonth = MutableLiveData(DateUtils.getCurrentMonth())
    val currentMonth: LiveData<String> = _currentMonth

    // 当前筛选的分类
    private val _filterCategory = MutableLiveData<String?>(null)
    val filterCategory: LiveData<String?> = _filterCategory

    // 当前筛选的类型
    private val _filterType = MutableLiveData<RecordType?>(null)
    val filterType: LiveData<RecordType?> = _filterType

    // 按天分组的记录
    private val _groupedRecords = MutableLiveData<List<DayRecords>>()
    val groupedRecords: LiveData<List<DayRecords>> = _groupedRecords

    // 月度总支出
    private val _monthlyExpense = MutableLiveData(0.0)
    val monthlyExpense: LiveData<Double> = _monthlyExpense

    // 月度总收入
    private val _monthlyIncome = MutableLiveData(0.0)
    val monthlyIncome: LiveData<Double> = _monthlyIncome

    // 加载状态
    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    init {
        loadRecords()
    }

    /**
     * 加载记录
     */
    fun loadRecords() {
        viewModelScope.launch {
            _isLoading.value = true
            val month = _currentMonth.value ?: DateUtils.getCurrentMonth()

            // 获取月度统计
            _monthlyExpense.value = recordRepository.getMonthlyExpense(userId, month)
            _monthlyIncome.value = recordRepository.getMonthlyIncome(userId, month)

            // 获取记录列表
            recordRepository.getRecordsByMonth(userId, month).collectLatest { records ->
                // 根据筛选条件过滤
                val filteredRecords = filterRecords(records)
                // 按天分组
                _groupedRecords.value = groupRecordsByDay(filteredRecords)
                _isLoading.value = false
            }
        }
    }

    /**
     * 筛选记录
     */
    private fun filterRecords(records: List<Record>): List<Record> {
        var result = records

        // 按类型筛选
        _filterType.value?.let { type ->
            result = result.filter { it.type == type }
        }

        // 按分类筛选
        _filterCategory.value?.let { category ->
            result = result.filter { it.category == category }
        }

        return result
    }

    /**
     * 按天分组记录
     */
    private fun groupRecordsByDay(records: List<Record>): List<DayRecords> {
        return records
            .groupBy { DateUtils.formatDate(it.timestamp) }
            .map { (date, dayRecords) ->
                val firstRecord = dayRecords.first()
                val expense = dayRecords.filter { it.type == RecordType.EXPENSE }.sumOf { it.amount }
                val income = dayRecords.filter { it.type == RecordType.INCOME }.sumOf { it.amount }

                DayRecords(
                    date = date,
                    displayDate = DateUtils.getFriendlyDate(firstRecord.timestamp),
                    weekDay = DateUtils.getWeekDay(firstRecord.timestamp),
                    expense = expense,
                    income = income,
                    records = dayRecords.sortedByDescending { it.timestamp }
                )
            }
            .sortedByDescending { it.date }
    }

    /**
     * 设置月份
     */
    fun setMonth(month: String) {
        _currentMonth.value = month
        loadRecords()
    }

    /**
     * 上一个月
     */
    fun previousMonth() {
        val current = _currentMonth.value ?: DateUtils.getCurrentMonth()
        setMonth(DateUtils.getPreviousMonth(current))
    }

    /**
     * 下一个月
     */
    fun nextMonth() {
        val current = _currentMonth.value ?: DateUtils.getCurrentMonth()
        setMonth(DateUtils.getNextMonth(current))
    }

    /**
     * 设置类型筛选
     */
    fun setFilterType(type: RecordType?) {
        _filterType.value = type
        loadRecords()
    }

    /**
     * 设置分类筛选
     */
    fun setFilterCategory(category: String?) {
        _filterCategory.value = category
        loadRecords()
    }

    /**
     * 清除筛选
     */
    fun clearFilter() {
        _filterType.value = null
        _filterCategory.value = null
        loadRecords()
    }

    /**
     * 删除记录
     */
    fun deleteRecord(record: Record) {
        viewModelScope.launch {
            recordRepository.deleteRecord(record)
            loadRecords()
        }
    }
}

/**
 * 按天分组的记录
 */
data class DayRecords(
    val date: String,           // 日期 yyyy-MM-dd
    val displayDate: String,    // 显示日期（今天/昨天/MM月dd日）
    val weekDay: String,        // 星期几
    val expense: Double,        // 当天总支出
    val income: Double,         // 当天总收入
    val records: List<Record>   // 当天的记录
)

/**
 * ViewModel工厂
 */
class HomeViewModelFactory(
    private val recordRepository: RecordRepository,
    private val userId: String
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
            return HomeViewModel(recordRepository, userId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
