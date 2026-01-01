package com.wangninghao.a202305100111.endtest02_accountbook.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.wangninghao.a202305100111.endtest02_accountbook.data.entity.Record
import com.wangninghao.a202305100111.endtest02_accountbook.data.entity.RecordType
import kotlinx.coroutines.flow.Flow

/**
 * 记账记录数据访问对象
 */
@Dao
interface RecordDao {

    /**
     * 插入记录
     */
    @Insert
    suspend fun insertRecord(record: Record): Long

    /**
     * 更新记录
     */
    @Update
    suspend fun updateRecord(record: Record)

    /**
     * 删除记录
     */
    @Delete
    suspend fun deleteRecord(record: Record)

    /**
     * 根据ID查询记录
     */
    @Query("SELECT * FROM records WHERE id = :id")
    suspend fun getRecordById(id: Long): Record?

    /**
     * 获取用户所有记录（按时间倒序）
     */
    @Query("SELECT * FROM records WHERE userId = :userId ORDER BY timestamp DESC")
    fun getAllRecords(userId: String): Flow<List<Record>>

    /**
     * 获取用户指定月份的记录
     * @param startTime 月份开始时间戳
     * @param endTime 月份结束时间戳
     */
    @Query("""
        SELECT * FROM records
        WHERE userId = :userId
        AND timestamp >= :startTime
        AND timestamp < :endTime
        ORDER BY timestamp DESC
    """)
    fun getRecordsByMonth(userId: String, startTime: Long, endTime: Long): Flow<List<Record>>

    /**
     * 获取用户指定月份和类型的记录
     */
    @Query("""
        SELECT * FROM records
        WHERE userId = :userId
        AND timestamp >= :startTime
        AND timestamp < :endTime
        AND type = :type
        ORDER BY timestamp DESC
    """)
    fun getRecordsByMonthAndType(
        userId: String,
        startTime: Long,
        endTime: Long,
        type: RecordType
    ): Flow<List<Record>>

    /**
     * 获取用户指定月份、类型和分类的记录
     */
    @Query("""
        SELECT * FROM records
        WHERE userId = :userId
        AND timestamp >= :startTime
        AND timestamp < :endTime
        AND type = :type
        AND category = :category
        ORDER BY timestamp DESC
    """)
    fun getRecordsByMonthTypeAndCategory(
        userId: String,
        startTime: Long,
        endTime: Long,
        type: RecordType,
        category: String
    ): Flow<List<Record>>

    /**
     * 获取月份总支出
     */
    @Query("""
        SELECT COALESCE(SUM(amount), 0) FROM records
        WHERE userId = :userId
        AND timestamp >= :startTime
        AND timestamp < :endTime
        AND type = 'EXPENSE'
    """)
    suspend fun getMonthlyExpense(userId: String, startTime: Long, endTime: Long): Double

    /**
     * 获取月份总收入
     */
    @Query("""
        SELECT COALESCE(SUM(amount), 0) FROM records
        WHERE userId = :userId
        AND timestamp >= :startTime
        AND timestamp < :endTime
        AND type = 'INCOME'
    """)
    suspend fun getMonthlyIncome(userId: String, startTime: Long, endTime: Long): Double

    /**
     * 获取月份分类统计（用于图表）
     */
    @Query("""
        SELECT category, SUM(amount) as total FROM records
        WHERE userId = :userId
        AND timestamp >= :startTime
        AND timestamp < :endTime
        AND type = :type
        GROUP BY category
        ORDER BY total DESC
    """)
    suspend fun getCategoryStatsByMonth(
        userId: String,
        startTime: Long,
        endTime: Long,
        type: RecordType
    ): List<CategoryStat>

    /**
     * 获取月份分类统计（含笔数，用于排行榜）
     */
    @Query("""
        SELECT category, SUM(amount) as total, COUNT(*) as count FROM records
        WHERE userId = :userId
        AND timestamp >= :startTime
        AND timestamp < :endTime
        AND type = :type
        GROUP BY category
        ORDER BY total DESC
    """)
    suspend fun getCategoryStatsWithCountByMonth(
        userId: String,
        startTime: Long,
        endTime: Long,
        type: RecordType
    ): List<CategoryStatWithCount>

    /**
     * 获取月份每日统计（用于图表）
     */
    @Query("""
        SELECT strftime('%d', datetime(timestamp/1000, 'unixepoch', 'localtime')) as day,
               SUM(amount) as total
        FROM records
        WHERE userId = :userId
        AND timestamp >= :startTime
        AND timestamp < :endTime
        AND type = :type
        GROUP BY day
        ORDER BY day
    """)
    suspend fun getDailyStatsByMonth(
        userId: String,
        startTime: Long,
        endTime: Long,
        type: RecordType
    ): List<DailyStat>

    /**
     * 获取用户所有记录（非Flow，用于导出）
     */
    @Query("SELECT * FROM records WHERE userId = :userId ORDER BY timestamp DESC")
    suspend fun getAllRecordsForExport(userId: String): List<Record>

    /**
     * 获取指定月份分类的总支出（用于预算计算）
     */
    @Query("""
        SELECT COALESCE(SUM(amount), 0) FROM records
        WHERE userId = :userId
        AND timestamp >= :startTime
        AND timestamp < :endTime
        AND type = 'EXPENSE'
        AND category = :category
    """)
    suspend fun getCategoryExpenseByMonth(
        userId: String,
        startTime: Long,
        endTime: Long,
        category: String
    ): Double

    /**
     * 批量插入记录
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecords(records: List<Record>)

    /**
     * 删除用户所有记录
     */
    @Query("DELETE FROM records WHERE userId = :userId")
    suspend fun deleteAllRecords(userId: String)
}

/**
 * 分类统计数据类
 */
data class CategoryStat(
    val category: String,
    val total: Double
)

/**
 * 每日统计数据类
 */
data class DailyStat(
    val day: String,
    val total: Double
)

/**
 * 分类统计数据类（含笔数）
 */
data class CategoryStatWithCount(
    val category: String,
    val total: Double,
    val count: Int
)
