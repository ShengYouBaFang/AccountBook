package com.wangninghao.a202305100111.endtest02_accountbook.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 预算实体类
 * @property id 自增主键
 * @property userId 关联用户手机号
 * @property category 分类名称，null表示总预算
 * @property amount 预算金额
 * @property month 月份，格式"2025-01"
 */
@Entity(
    tableName = "budgets",
    foreignKeys = [
        ForeignKey(
            entity = User::class,
            parentColumns = ["phone"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["userId"]), Index(value = ["month"])]
)
data class Budget(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: String,
    val category: String?,  // null表示月度总预算
    val amount: Double,
    val month: String  // 格式: "2025-01"
)
