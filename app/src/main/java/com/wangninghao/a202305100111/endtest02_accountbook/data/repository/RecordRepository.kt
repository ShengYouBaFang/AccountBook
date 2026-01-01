package com.wangninghao.a202305100111.endtest02_accountbook.data.repository

import com.wangninghao.a202305100111.endtest02_accountbook.data.dao.CategoryStat
import com.wangninghao.a202305100111.endtest02_accountbook.data.dao.CategoryStatWithCount
import com.wangninghao.a202305100111.endtest02_accountbook.data.dao.DailyStat
import com.wangninghao.a202305100111.endtest02_accountbook.data.dao.RecordDao
import com.wangninghao.a202305100111.endtest02_accountbook.data.entity.Record
import com.wangninghao.a202305100111.endtest02_accountbook.data.entity.RecordType
import com.wangninghao.a202305100111.endtest02_accountbook.util.DateUtils
import kotlinx.coroutines.flow.Flow

/**
 * 记账记录数据仓库
 */
class RecordRepository(private val recordDao: RecordDao) {

    /**
     * 添加记录
     */
    suspend fun addRecord(record: Record): Long {
        return recordDao.insertRecord(record)
    }

    /**
     * 更新记录
     */
    suspend fun updateRecord(record: Record) {
        recordDao.updateRecord(record)
    }

    /**
     * 删除记录
     */
    suspend fun deleteRecord(record: Record) {
        recordDao.deleteRecord(record)
    }

    /**
     * 根据ID获取记录
     */
    suspend fun getRecordById(id: Long): Record? {
        return recordDao.getRecordById(id)
    }

    /**
     * 获取用户所有记录
     */
    fun getAllRecords(userId: String): Flow<List<Record>> {
        return recordDao.getAllRecords(userId)
    }

    /**
     * 获取用户指定月份的记录
     * @param month 格式: "2025-01"
     */
    fun getRecordsByMonth(userId: String, month: String): Flow<List<Record>> {
        val (startTime, endTime) = DateUtils.getMonthRange(month)
        return recordDao.getRecordsByMonth(userId, startTime, endTime)
    }

    /**
     * 获取用户指定月份和类型的记录
     */
    fun getRecordsByMonthAndType(
        userId: String,
        month: String,
        type: RecordType
    ): Flow<List<Record>> {
        val (startTime, endTime) = DateUtils.getMonthRange(month)
        return recordDao.getRecordsByMonthAndType(userId, startTime, endTime, type)
    }

    /**
     * 获取用户指定月份、类型和分类的记录
     */
    fun getRecordsByMonthTypeAndCategory(
        userId: String,
        month: String,
        type: RecordType,
        category: String
    ): Flow<List<Record>> {
        val (startTime, endTime) = DateUtils.getMonthRange(month)
        return recordDao.getRecordsByMonthTypeAndCategory(userId, startTime, endTime, type, category)
    }

    /**
     * 获取月份总支出
     */
    suspend fun getMonthlyExpense(userId: String, month: String): Double {
        val (startTime, endTime) = DateUtils.getMonthRange(month)
        return recordDao.getMonthlyExpense(userId, startTime, endTime)
    }

    /**
     * 获取月份总收入
     */
    suspend fun getMonthlyIncome(userId: String, month: String): Double {
        val (startTime, endTime) = DateUtils.getMonthRange(month)
        return recordDao.getMonthlyIncome(userId, startTime, endTime)
    }

    /**
     * 获取月份分类统计
     */
    suspend fun getCategoryStatsByMonth(
        userId: String,
        month: String,
        type: RecordType
    ): List<CategoryStat> {
        val (startTime, endTime) = DateUtils.getMonthRange(month)
        return recordDao.getCategoryStatsByMonth(userId, startTime, endTime, type)
    }

    /**
     * 获取月份分类统计（含笔数）
     */
    suspend fun getCategoryStatsWithCountByMonth(
        userId: String,
        month: String,
        type: RecordType
    ): List<CategoryStatWithCount> {
        val (startTime, endTime) = DateUtils.getMonthRange(month)
        return recordDao.getCategoryStatsWithCountByMonth(userId, startTime, endTime, type)
    }

    /**
     * 获取月份每日统计
     */
    suspend fun getDailyStatsByMonth(
        userId: String,
        month: String,
        type: RecordType
    ): List<DailyStat> {
        val (startTime, endTime) = DateUtils.getMonthRange(month)
        return recordDao.getDailyStatsByMonth(userId, startTime, endTime, type)
    }

    /**
     * 获取所有记录（用于导出）
     */
    suspend fun getAllRecordsForExport(userId: String): List<Record> {
        return recordDao.getAllRecordsForExport(userId)
    }

    /**
     * 获取指定月份分类的总支出
     */
    suspend fun getCategoryExpenseByMonth(
        userId: String,
        month: String,
        category: String
    ): Double {
        val (startTime, endTime) = DateUtils.getMonthRange(month)
        return recordDao.getCategoryExpenseByMonth(userId, startTime, endTime, category)
    }
}
