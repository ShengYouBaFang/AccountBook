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

    init {
        loadData()
    }

    fun setMonth(month: String) {
        _currentMonth.value = month
        loadData()
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

    fun setTotalBudget(amount: Double) {
        viewModelScope.launch {
            val month = _currentMonth.value ?: DateUtils.getCurrentMonth()
            budgetRepository.setTotalBudget(userId, month, amount)
            loadData()
        }
    }
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
