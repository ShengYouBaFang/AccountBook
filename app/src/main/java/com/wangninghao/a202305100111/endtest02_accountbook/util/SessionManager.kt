package com.wangninghao.a202305100111.endtest02_accountbook.util

import android.content.Context
import android.content.SharedPreferences

/**
 * 会话管理器
 * 用于管理用户登录状态
 */
class SessionManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "account_book_prefs"
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
        private const val KEY_USER_PHONE = "user_phone"
        private const val KEY_LOGIN_TIME = "login_time"
    }

    /**
     * 保存登录状态
     */
    fun saveLoginState(phone: String) {
        prefs.edit().apply {
            putBoolean(KEY_IS_LOGGED_IN, true)
            putString(KEY_USER_PHONE, phone)
            putLong(KEY_LOGIN_TIME, System.currentTimeMillis())
            apply()
        }
    }

    /**
     * 清除登录状态
     */
    fun clearLoginState() {
        prefs.edit().apply {
            putBoolean(KEY_IS_LOGGED_IN, false)
            remove(KEY_USER_PHONE)
            remove(KEY_LOGIN_TIME)
            apply()
        }
    }

    /**
     * 检查是否已登录
     */
    fun isLoggedIn(): Boolean {
        return prefs.getBoolean(KEY_IS_LOGGED_IN, false)
    }

    /**
     * 获取当前登录用户的手机号
     */
    fun getCurrentUserPhone(): String? {
        return if (isLoggedIn()) {
            prefs.getString(KEY_USER_PHONE, null)
        } else {
            null
        }
    }

    /**
     * 获取登录时间
     */
    fun getLoginTime(): Long {
        return prefs.getLong(KEY_LOGIN_TIME, 0)
    }
}
