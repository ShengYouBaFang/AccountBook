package com.wangninghao.a202305100111.endtest02_accountbook.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 记账记录实体类
 * @property id 自增主键
 * @property userId 关联用户手机号
 * @property type 记录类型（支出/收入）
 * @property amount 金额
 * @property category 分类名称（餐饮、交通等）
 * @property note 备注
 * @property timestamp 记账时间戳
 * @property imageUri 小票图片路径（可选）
 */
@Entity(
    tableName = "records",
    foreignKeys = [
        ForeignKey(
            entity = User::class,
            parentColumns = ["phone"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["userId"]), Index(value = ["timestamp"])]
)
data class Record(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: String,
    val type: RecordType,
    val amount: Double,
    val category: String,
    val note: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val imageUri: String? = null
)
