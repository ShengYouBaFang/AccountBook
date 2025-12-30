package com.wangninghao.a202305100111.endtest02_accountbook.data.database

import com.wangninghao.a202305100111.endtest02_accountbook.data.dao.CategoryDao
import com.wangninghao.a202305100111.endtest02_accountbook.data.entity.Category
import com.wangninghao.a202305100111.endtest02_accountbook.data.entity.RecordType

/**
 * 分类初始化工具类
 * 用于为新用户初始化默认分类
 */
object CategoryInitializer {

    // 默认支出分类
    private val DEFAULT_EXPENSE_CATEGORIES = listOf(
        "餐饮" to "ic_category_food",
        "交通" to "ic_category_transport",
        "购物" to "ic_category_shopping",
        "娱乐" to "ic_category_entertainment",
        "教育" to "ic_category_education",
        "医疗" to "ic_category_medical",
        "住房" to "ic_category_housing",
        "通讯" to "ic_category_communication",
        "其他" to "ic_category_other"
    )

    // 默认收入分类
    private val DEFAULT_INCOME_CATEGORIES = listOf(
        "工资" to "ic_category_salary",
        "奖金" to "ic_category_bonus",
        "兼职" to "ic_category_parttime",
        "投资" to "ic_category_investment",
        "红包" to "ic_category_redpacket",
        "其他" to "ic_category_other"
    )

    /**
     * 为用户初始化默认分类
     */
    suspend fun initDefaultCategories(categoryDao: CategoryDao, userId: String) {
        // 检查用户是否已有分类
        if (categoryDao.getCategoryCount(userId) > 0) {
            return
        }

        val categories = mutableListOf<Category>()

        // 添加支出分类
        DEFAULT_EXPENSE_CATEGORIES.forEach { (name, icon) ->
            categories.add(
                Category(
                    userId = userId,
                    name = name,
                    type = RecordType.EXPENSE,
                    icon = icon,
                    isCustom = false
                )
            )
        }

        // 添加收入分类
        DEFAULT_INCOME_CATEGORIES.forEach { (name, icon) ->
            categories.add(
                Category(
                    userId = userId,
                    name = name,
                    type = RecordType.INCOME,
                    icon = icon,
                    isCustom = false
                )
            )
        }

        categoryDao.insertCategories(categories)
    }

    /**
     * 获取默认支出分类名称列表
     */
    fun getDefaultExpenseCategories(): List<String> {
        return DEFAULT_EXPENSE_CATEGORIES.map { it.first }
    }

    /**
     * 获取默认收入分类名称列表
     */
    fun getDefaultIncomeCategories(): List<String> {
        return DEFAULT_INCOME_CATEGORIES.map { it.first }
    }
}
