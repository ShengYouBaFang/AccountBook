package com.wangninghao.a202305100111.endtest02_accountbook.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.wangninghao.a202305100111.endtest02_accountbook.data.entity.User
import kotlinx.coroutines.flow.Flow

/**
 * 用户数据访问对象
 */
@Dao
interface UserDao {

    /**
     * 插入用户（已存在则替换）
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: User)

    /**
     * 根据手机号查询用户
     */
    @Query("SELECT * FROM users WHERE phone = :phone LIMIT 1")
    suspend fun getUserByPhone(phone: String): User?

    /**
     * 验证登录
     */
    @Query("SELECT * FROM users WHERE phone = :phone AND password = :password LIMIT 1")
    suspend fun login(phone: String, password: String): User?

    /**
     * 检查手机号是否已注册
     */
    @Query("SELECT COUNT(*) FROM users WHERE phone = :phone")
    suspend fun isPhoneRegistered(phone: String): Int

    /**
     * 观察用户信息变化
     */
    @Query("SELECT * FROM users WHERE phone = :phone")
    fun observeUser(phone: String): Flow<User?>
}
