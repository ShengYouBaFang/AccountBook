package com.wangninghao.a202305100111.endtest02_accountbook.ui.budget

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.wangninghao.a202305100111.endtest02_accountbook.data.entity.Budget
import com.wangninghao.a202305100111.endtest02_accountbook.data.repository.BudgetRepository
import com.wangninghao.a202305100111.endtest02_accountbook.data.repository.RecordRepository
import com.wangninghao.a202305100111.endtest02_accountbook.util.DateUtils
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * 预算管理ViewModel
 */
class BudgetViewModel(
    private val budgetRepository: BudgetRepository,
    private val recordRepository: RecordRepository,
    private val userId: String
) : ViewModel() {

    private val _currentMonth = MutableLiveData(DateUtils.getCurrentMonth())
    val currentMonth: LiveData<String> = _currentMonth

    private val _totalBudget = MutableLiveData<Budget?>()
    val totalBudget: LiveData<Budget?> = _totalBudget

    private val _monthlyExpense = MutableLiveData(0.0)
    val monthlyExpense: LiveData<Double> = _monthlyExpense

    // 分类预算列表
    private val _categoryBudgets = MutableLiveData<List<CategoryBudgetItem>>()
    val categoryBudgets: LiveData<List<CategoryBudgetItem>> = _categoryBudgets

    // 支出分类列表（用于选择）
    val expenseCategories = listOf(
        "餐饮", "交通", "购物", "娱乐", "教育", "医疗", "住房", "通讯", "其他"
    )

    init {
        loadData()
        loadCategoryBudgets()
    }

    /**
     * 刷新所有数据，用于页面返回时重新加载
     */
    fun refreshData() {
        loadData()
        loadCategoryBudgets()
    }

    fun setMonth(month: String) {
        _currentMonth.value = month
        loadData()
        loadCategoryBudgets()
    }

    private fun loadData() {
        viewModelScope.launch {
            val month = _currentMonth.value ?: DateUtils.getCurrentMonth()

            // 加载总预算
            _totalBudget.value = budgetRepository.getTotalBudget(userId, month)

            // 加载月度支出
            _monthlyExpense.value = recordRepository.getMonthlyExpense(userId, month)
        }
    }

    /**
     * 加载分类预算数据
     */
    private fun loadCategoryBudgets() {
        viewModelScope.launch {
            val month = _currentMonth.value ?: DateUtils.getCurrentMonth()
            budgetRepository.getCategoryBudgets(userId, month).collectLatest { budgets ->
                // 为每个预算获取已使用金额
                val items = budgets.map { budget ->
                    val used = recordRepository.getCategoryExpenseByMonth(
                        userId, month, budget.category ?: ""
                    )
                    CategoryBudgetItem(
                        budget = budget,
                        used = used
                    )
                }
                _categoryBudgets.value = items
            }
        }
    }

    fun setTotalBudget(amount: Double) {
        viewModelScope.launch {
            val month = _currentMonth.value ?: DateUtils.getCurrentMonth()
            budgetRepository.setTotalBudget(userId, month, amount)
            loadData()
        }
    }

    /**
     * 设置分类预算
     */
    fun setCategoryBudget(category: String, amount: Double) {
        viewModelScope.launch {
            val month = _currentMonth.value ?: DateUtils.getCurrentMonth()
            budgetRepository.setCategoryBudget(userId, month, category, amount)
            loadCategoryBudgets()
        }
    }

    /**
     * 删除分类预算
     */
    fun deleteCategoryBudget(category: String) {
        viewModelScope.launch {
            val month = _currentMonth.value ?: DateUtils.getCurrentMonth()
            budgetRepository.deleteCategoryBudget(userId, month, category)
            loadCategoryBudgets()
        }
    }

    /**
     * 获取尚未设置预算的分类
     */
    fun getAvailableCategories(): List<String> {
        val existingCategories = _categoryBudgets.value?.mapNotNull { it.budget.category } ?: emptyList()
        return expenseCategories.filter { it !in existingCategories }
    }
}

/**
 * 分类预算项数据类
 */
data class CategoryBudgetItem(
    val budget: Budget,
    val used: Double
) {
    val category: String get() = budget.category ?: ""
    val budgetAmount: Double get() = budget.amount
    val remaining: Double get() = (budgetAmount - used).coerceAtLeast(0.0)
    val progress: Float get() = if (budgetAmount > 0) (used / budgetAmount * 100).toFloat().coerceIn(0f, 100f) else 0f
    val isOverBudget: Boolean get() = used > budgetAmount
}

class BudgetViewModelFactory(
    private val budgetRepository: BudgetRepository,
    private val recordRepository: RecordRepository,
    private val userId: String
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(BudgetViewModel::class.java)) {
            return BudgetViewModel(budgetRepository, recordRepository, userId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
