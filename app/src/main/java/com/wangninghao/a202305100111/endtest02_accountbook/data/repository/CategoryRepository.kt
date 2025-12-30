package com.wangninghao.a202305100111.endtest02_accountbook.data.repository

import com.wangninghao.a202305100111.endtest02_accountbook.data.dao.CategoryDao
import com.wangninghao.a202305100111.endtest02_accountbook.data.entity.Category
import com.wangninghao.a202305100111.endtest02_accountbook.data.entity.RecordType
import kotlinx.coroutines.flow.Flow

/**
 * 分类数据仓库
 */
class CategoryRepository(private val categoryDao: CategoryDao) {

    /**
     * 添加分类
     */
    suspend fun addCategory(category: Category): Long {
        return categoryDao.insertCategory(category)
    }

    /**
     * 删除分类
     */
    suspend fun deleteCategory(category: Category) {
        categoryDao.deleteCategory(category)
    }

    /**
     * 获取用户所有分类
     */
    fun getAllCategories(userId: String): Flow<List<Category>> {
        return categoryDao.getAllCategories(userId)
    }

    /**
     * 获取用户指定类型的分类
     */
    fun getCategoriesByType(userId: String, type: RecordType): Flow<List<Category>> {
        return categoryDao.getCategoriesByType(userId, type)
    }

    /**
     * 获取用户指定类型的分类（同步）
     */
    suspend fun getCategoriesByTypeSync(userId: String, type: RecordType): List<Category> {
        return categoryDao.getCategoriesByTypeSync(userId, type)
    }

    /**
     * 检查分类是否存在
     */
    suspend fun isCategoryExists(userId: String, name: String, type: RecordType): Boolean {
        return categoryDao.isCategoryExists(userId, name, type) > 0
    }

    /**
     * 添加自定义分类
     */
    suspend fun addCustomCategory(
        userId: String,
        name: String,
        type: RecordType,
        icon: String = "ic_category_other"
    ): Result<Long> {
        return try {
            if (isCategoryExists(userId, name, type)) {
                Result.failure(Exception("分类已存在"))
            } else {
                val category = Category(
                    userId = userId,
                    name = name,
                    type = type,
                    icon = icon,
                    isCustom = true
                )
                Result.success(categoryDao.insertCategory(category))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 根据名称获取分类
     */
    suspend fun getCategoryByName(userId: String, name: String, type: RecordType): Category? {
        return categoryDao.getCategoryByName(userId, name, type)
    }
}
