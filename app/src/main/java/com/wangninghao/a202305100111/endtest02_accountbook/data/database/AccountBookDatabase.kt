package com.wangninghao.a202305100111.endtest02_accountbook.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.wangninghao.a202305100111.endtest02_accountbook.data.converter.Converters
import com.wangninghao.a202305100111.endtest02_accountbook.data.dao.BudgetDao
import com.wangninghao.a202305100111.endtest02_accountbook.data.dao.CategoryDao
import com.wangninghao.a202305100111.endtest02_accountbook.data.dao.RecordDao
import com.wangninghao.a202305100111.endtest02_accountbook.data.dao.UserDao
import com.wangninghao.a202305100111.endtest02_accountbook.data.entity.Budget
import com.wangninghao.a202305100111.endtest02_accountbook.data.entity.Category
import com.wangninghao.a202305100111.endtest02_accountbook.data.entity.Record
import com.wangninghao.a202305100111.endtest02_accountbook.data.entity.User

/**
 * 应用数据库
 * 包含用户、记录、预算、分类四张表
 */
@Database(
    entities = [User::class, Record::class, Budget::class, Category::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AccountBookDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao
    abstract fun recordDao(): RecordDao
    abstract fun budgetDao(): BudgetDao
    abstract fun categoryDao(): CategoryDao

    companion object {
        private const val DATABASE_NAME = "account_book_db"

        @Volatile
        private var INSTANCE: AccountBookDatabase? = null

        fun getInstance(context: Context): AccountBookDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
            }
        }

        private fun buildDatabase(context: Context): AccountBookDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                AccountBookDatabase::class.java,
                DATABASE_NAME
            )
                .addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        // 数据库创建时不需要插入默认分类
                        // 默认分类会在用户注册时通过CategoryInitializer插入
                    }
                })
                .build()
        }
    }
}
