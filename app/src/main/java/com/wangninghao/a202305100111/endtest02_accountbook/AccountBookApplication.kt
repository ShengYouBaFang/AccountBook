package com.wangninghao.a202305100111.endtest02_accountbook

import android.app.Application
import com.wangninghao.a202305100111.endtest02_accountbook.data.database.AccountBookDatabase

/**
 * 应用程序类
 * 用于初始化全局组件
 */
class AccountBookApplication : Application() {

    // 延迟初始化数据库
    val database: AccountBookDatabase by lazy {
        AccountBookDatabase.getInstance(this)
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        lateinit var instance: AccountBookApplication
            private set
    }
}
