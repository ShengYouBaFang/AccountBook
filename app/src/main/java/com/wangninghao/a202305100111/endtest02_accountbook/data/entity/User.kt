package com.wangninghao.a202305100111.endtest02_accountbook.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 用户实体类
 * @property phone 手机号（主键）
 * @property password 密码
 * @property createdAt 创建时间戳
 */
@Entity(tableName = "users")
data class User(
    @PrimaryKey
    val phone: String,
    val password: String,
    val createdAt: Long = System.currentTimeMillis()
)
