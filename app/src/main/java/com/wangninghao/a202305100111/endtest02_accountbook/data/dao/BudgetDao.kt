package com.wangninghao.a202305100111.endtest02_accountbook.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.wangninghao.a202305100111.endtest02_accountbook.data.entity.Budget
import kotlinx.coroutines.flow.Flow

/**
 * 预算数据访问对象
 */
@Dao
interface BudgetDao {

    /**
     * 插入预算
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBudget(budget: Budget): Long

    /**
     * 更新预算
     */
    @Update
    suspend fun updateBudget(budget: Budget)

    /**
     * 删除预算
     */
    @Delete
    suspend fun deleteBudget(budget: Budget)

    /**
     * 获取用户指定月份的所有预算
     */
    @Query("SELECT * FROM budgets WHERE userId = :userId AND month = :month")
    fun getBudgetsByMonth(userId: String, month: String): Flow<List<Budget>>

    /**
     * 获取用户指定月份的总预算
     */
    @Query("SELECT * FROM budgets WHERE userId = :userId AND month = :month AND category IS NULL LIMIT 1")
    suspend fun getTotalBudget(userId: String, month: String): Budget?

    /**
     * 获取用户指定月份指定分类的预算
     */
    @Query("SELECT * FROM budgets WHERE userId = :userId AND month = :month AND category = :category LIMIT 1")
    suspend fun getCategoryBudget(userId: String, month: String, category: String): Budget?

    /**
     * 获取用户指定月份的分类预算列表
     */
    @Query("SELECT * FROM budgets WHERE userId = :userId AND month = :month AND category IS NOT NULL")
    fun getCategoryBudgets(userId: String, month: String): Flow<List<Budget>>

    /**
     * 观察用户指定月份的总预算
     */
    @Query("SELECT * FROM budgets WHERE userId = :userId AND month = :month AND category IS NULL")
    fun observeTotalBudget(userId: String, month: String): Flow<Budget?>

    /**
     * 删除用户指定月份指定分类的预算
     */
    @Query("DELETE FROM budgets WHERE userId = :userId AND month = :month AND category = :category")
    suspend fun deleteCategoryBudget(userId: String, month: String, category: String)

    /**
     * 获取用户所有预算（用于备份）
     */
    @Query("SELECT * FROM budgets WHERE userId = :userId")
    suspend fun getAllBudgets(userId: String): List<Budget>

    /**
     * 批量插入预算
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBudgets(budgets: List<Budget>)

    /**
     * 删除用户所有预算
     */
    @Query("DELETE FROM budgets WHERE userId = :userId")
    suspend fun deleteAllBudgets(userId: String)
}
