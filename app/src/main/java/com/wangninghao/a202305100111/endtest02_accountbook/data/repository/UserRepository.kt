package com.wangninghao.a202305100111.endtest02_accountbook.data.repository

import com.wangninghao.a202305100111.endtest02_accountbook.data.dao.CategoryDao
import com.wangninghao.a202305100111.endtest02_accountbook.data.dao.UserDao
import com.wangninghao.a202305100111.endtest02_accountbook.data.database.CategoryInitializer
import com.wangninghao.a202305100111.endtest02_accountbook.data.entity.User
import kotlinx.coroutines.flow.Flow

/**
 * 用户数据仓库
 * 处理用户登录、注册等业务逻辑
 */
class UserRepository(
    private val userDao: UserDao,
    private val categoryDao: CategoryDao
) {

    /**
     * 登录
     * @return 登录成功返回User，失败返回null
     */
    suspend fun login(phone: String, password: String): User? {
        return userDao.login(phone, password)
    }

    /**
     * 注册（新手机号自动注册）
     * @return 注册成功返回true，手机号已存在返回false
     */
    suspend fun register(phone: String, password: String): Result<User> {
        return try {
            // 检查手机号是否已注册
            if (userDao.isPhoneRegistered(phone) > 0) {
                Result.failure(Exception("该手机号已注册"))
            } else {
                val user = User(phone = phone, password = password)
                userDao.insertUser(user)
                // 为新用户初始化默认分类
                CategoryInitializer.initDefaultCategories(categoryDao, phone)
                Result.success(user)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 登录或注册
     * 手机号存在则验证密码登录，不存在则自动注册
     */
    suspend fun loginOrRegister(phone: String, password: String): Result<User> {
        return try {
            val existingUser = userDao.getUserByPhone(phone)
            if (existingUser != null) {
                // 手机号已存在，验证密码
                if (existingUser.password == password) {
                    Result.success(existingUser)
                } else {
                    Result.failure(Exception("密码错误"))
                }
            } else {
                // 新用户，自动注册
                register(phone, password)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 根据手机号获取用户
     */
    suspend fun getUserByPhone(phone: String): User? {
        return userDao.getUserByPhone(phone)
    }

    /**
     * 观察用户信息变化
     */
    fun observeUser(phone: String): Flow<User?> {
        return userDao.observeUser(phone)
    }
}
