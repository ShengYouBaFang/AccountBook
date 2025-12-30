package com.wangninghao.a202305100111.endtest02_accountbook.data.repository

import com.wangninghao.a202305100111.endtest02_accountbook.data.dao.BudgetDao
import com.wangninghao.a202305100111.endtest02_accountbook.data.entity.Budget
import kotlinx.coroutines.flow.Flow

/**
 * 预算数据仓库
 */
class BudgetRepository(private val budgetDao: BudgetDao) {

    /**
     * 设置或更新预算
     */
    suspend fun setBudget(budget: Budget): Long {
        return budgetDao.insertBudget(budget)
    }

    /**
     * 更新预算
     */
    suspend fun updateBudget(budget: Budget) {
        budgetDao.updateBudget(budget)
    }

    /**
     * 删除预算
     */
    suspend fun deleteBudget(budget: Budget) {
        budgetDao.deleteBudget(budget)
    }

    /**
     * 获取用户指定月份的所有预算
     */
    fun getBudgetsByMonth(userId: String, month: String): Flow<List<Budget>> {
        return budgetDao.getBudgetsByMonth(userId, month)
    }

    /**
     * 获取月度总预算
     */
    suspend fun getTotalBudget(userId: String, month: String): Budget? {
        return budgetDao.getTotalBudget(userId, month)
    }

    /**
     * 获取分类预算
     */
    suspend fun getCategoryBudget(userId: String, month: String, category: String): Budget? {
        return budgetDao.getCategoryBudget(userId, month, category)
    }

    /**
     * 获取分类预算列表
     */
    fun getCategoryBudgets(userId: String, month: String): Flow<List<Budget>> {
        return budgetDao.getCategoryBudgets(userId, month)
    }

    /**
     * 观察月度总预算
     */
    fun observeTotalBudget(userId: String, month: String): Flow<Budget?> {
        return budgetDao.observeTotalBudget(userId, month)
    }

    /**
     * 删除分类预算
     */
    suspend fun deleteCategoryBudget(userId: String, month: String, category: String) {
        budgetDao.deleteCategoryBudget(userId, month, category)
    }

    /**
     * 设置月度总预算
     */
    suspend fun setTotalBudget(userId: String, month: String, amount: Double): Long {
        val budget = Budget(
            userId = userId,
            category = null,
            amount = amount,
            month = month
        )
        return budgetDao.insertBudget(budget)
    }

    /**
     * 设置分类预算
     */
    suspend fun setCategoryBudget(
        userId: String,
        month: String,
        category: String,
        amount: Double
    ): Long {
        val budget = Budget(
            userId = userId,
            category = category,
            amount = amount,
            month = month
        )
        return budgetDao.insertBudget(budget)
    }
}
