package com.wangninghao.a202305100111.endtest02_accountbook.ui.statistics

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.wangninghao.a202305100111.endtest02_accountbook.data.dao.CategoryStatWithCount
import com.wangninghao.a202305100111.endtest02_accountbook.data.entity.RecordType
import com.wangninghao.a202305100111.endtest02_accountbook.data.repository.RecordRepository
import com.wangninghao.a202305100111.endtest02_accountbook.util.DateUtils
import kotlinx.coroutines.launch

/**
 * 统计页面ViewModel
 */
class StatisticsViewModel(
    private val recordRepository: RecordRepository,
    private val userId: String
) : ViewModel() {

    private val _currentMonth = MutableLiveData(DateUtils.getCurrentMonth())
    val currentMonth: LiveData<String> = _currentMonth

    private val _recordType = MutableLiveData(RecordType.EXPENSE)
    val recordType: LiveData<RecordType> = _recordType

    private val _categoryStats = MutableLiveData<List<Pair<String, Double>>>()
    val categoryStats: LiveData<List<Pair<String, Double>>> = _categoryStats

    private val _dailyStats = MutableLiveData<List<Pair<String, Double>>>()
    val dailyStats: LiveData<List<Pair<String, Double>>> = _dailyStats

    // 排行榜数据（含笔数）
    private val _rankingStats = MutableLiveData<List<CategoryStatWithCount>>()
    val rankingStats: LiveData<List<CategoryStatWithCount>> = _rankingStats

    // 月度总额
    private val _monthTotal = MutableLiveData<Double>()
    val monthTotal: LiveData<Double> = _monthTotal

    init {
        loadStats()
    }

    /**
     * 刷新数据，用于页面返回时重新加载
     */
    fun refreshData() {
        loadStats()
    }

    fun setMonth(month: String) {
        _currentMonth.value = month
        loadStats()
    }

    fun setRecordType(type: RecordType) {
        _recordType.value = type
        loadStats()
    }

    private fun loadStats() {
        viewModelScope.launch {
            val month = _currentMonth.value ?: DateUtils.getCurrentMonth()
            val type = _recordType.value ?: RecordType.EXPENSE

            // 加载分类统计（用于饼图和横向柱形条）
            val categoryData = recordRepository.getCategoryStatsByMonth(userId, month, type)
            _categoryStats.value = categoryData.map { it.category to it.total }

            // 计算总额
            val total = categoryData.sumOf { it.total }
            _monthTotal.value = total

            // 加载排行榜数据
            val rankingData = recordRepository.getCategoryStatsWithCountByMonth(userId, month, type)
            _rankingStats.value = rankingData

            // 加载每日统计
            val dailyData = recordRepository.getDailyStatsByMonth(userId, month, type)
            _dailyStats.value = dailyData.map { it.day to it.total }
        }
    }
}

class StatisticsViewModelFactory(
    private val recordRepository: RecordRepository,
    private val userId: String
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(StatisticsViewModel::class.java)) {
            return StatisticsViewModel(recordRepository, userId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
