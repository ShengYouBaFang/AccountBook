package com.wangninghao.a202305100111.endtest02_accountbook.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 分类实体类
 * @property id 自增主键
 * @property userId 关联用户手机号
 * @property name 分类名称
 * @property type 分类类型（支出/收入）
 * @property icon 图标资源名称
 * @property isCustom 是否为用户自定义分类
 */
@Entity(
    tableName = "categories",
    foreignKeys = [
        ForeignKey(
            entity = User::class,
            parentColumns = ["phone"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["userId"]), Index(value = ["type"])]
)
data class Category(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: String,
    val name: String,
    val type: RecordType,
    val icon: String = "ic_category_other",  // 默认图标
    val isCustom: Boolean = true
)
