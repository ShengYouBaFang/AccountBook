package com.wangninghao.a202305100111.endtest02_accountbook.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.wangninghao.a202305100111.endtest02_accountbook.data.entity.Category
import com.wangninghao.a202305100111.endtest02_accountbook.data.entity.RecordType
import kotlinx.coroutines.flow.Flow

/**
 * 分类数据访问对象
 */
@Dao
interface CategoryDao {

    /**
     * 插入分类
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: Category): Long

    /**
     * 批量插入分类
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCategories(categories: List<Category>)

    /**
     * 删除分类
     */
    @Delete
    suspend fun deleteCategory(category: Category)

    /**
     * 获取用户所有分类
     */
    @Query("SELECT * FROM categories WHERE userId = :userId ORDER BY isCustom, id")
    fun getAllCategories(userId: String): Flow<List<Category>>

    /**
     * 获取用户指定类型的分类
     */
    @Query("SELECT * FROM categories WHERE userId = :userId AND type = :type ORDER BY isCustom, id")
    fun getCategoriesByType(userId: String, type: RecordType): Flow<List<Category>>

    /**
     * 获取用户指定类型的分类（非Flow）
     */
    @Query("SELECT * FROM categories WHERE userId = :userId AND type = :type ORDER BY isCustom, id")
    suspend fun getCategoriesByTypeSync(userId: String, type: RecordType): List<Category>

    /**
     * 检查分类是否存在
     */
    @Query("SELECT COUNT(*) FROM categories WHERE userId = :userId AND name = :name AND type = :type")
    suspend fun isCategoryExists(userId: String, name: String, type: RecordType): Int

    /**
     * 根据名称获取分类
     */
    @Query("SELECT * FROM categories WHERE userId = :userId AND name = :name AND type = :type LIMIT 1")
    suspend fun getCategoryByName(userId: String, name: String, type: RecordType): Category?

    /**
     * 获取用户分类数量
     */
    @Query("SELECT COUNT(*) FROM categories WHERE userId = :userId")
    suspend fun getCategoryCount(userId: String): Int
}
