package com.wangninghao.a202305100111.endtest02_accountbook.util

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.wangninghao.a202305100111.endtest02_accountbook.data.entity.Budget
import com.wangninghao.a202305100111.endtest02_accountbook.data.entity.Category
import com.wangninghao.a202305100111.endtest02_accountbook.data.entity.Record
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 数据备份管理器
 * 支持导出和导入JSON格式的备份文件
 */
object DataBackupManager {

    private val gson: Gson = GsonBuilder()
        .setPrettyPrinting()
        .create()

    /**
     * 备份数据模型
     */
    data class BackupData(
        val version: Int = 1,
        val backupTime: Long = System.currentTimeMillis(),
        val userId: String,
        val records: List<Record>,
        val budgets: List<Budget>,
        val categories: List<Category>
    )

    /**
     * 导出备份数据到Downloads目录
     * @param context 上下文
     * @param userId 用户ID
     * @param records 记账记录列表
     * @param budgets 预算列表
     * @param categories 分类列表
     * @return 成功返回文件路径，失败返回异常
     */
    fun exportBackup(
        context: Context,
        userId: String,
        records: List<Record>,
        budgets: List<Budget>,
        categories: List<Category>
    ): Result<String> {
        return try {
            val backupData = BackupData(
                userId = userId,
                records = records,
                budgets = budgets,
                categories = categories
            )

            val jsonContent = gson.toJson(backupData)
            val fileName = "记账本备份_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}.json"

            val filePath = saveToDownloads(context, jsonContent, fileName)
            Result.success(filePath)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    /**
     * 保存文件到Downloads目录
     */
    private fun saveToDownloads(context: Context, content: String, fileName: String): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android 10+ 使用MediaStore
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "application/json")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }

            val uri = context.contentResolver.insert(
                MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                contentValues
            ) ?: throw Exception("无法创建文件")

            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                outputStream.write(content.toByteArray(Charsets.UTF_8))
            } ?: throw Exception("无法打开文件流")

            "Downloads/$fileName"
        } else {
            // Android 9及以下
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val file = File(downloadsDir, fileName)
            FileOutputStream(file).use { fos ->
                fos.write(content.toByteArray(Charsets.UTF_8))
            }
            file.absolutePath
        }
    }

    /**
     * 从Uri读取备份数据
     * @param context 上下文
     * @param uri 备份文件Uri
     * @return 解析后的备份数据
     */
    fun readBackup(context: Context, uri: Uri): Result<BackupData> {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
                ?: return Result.failure(Exception("无法读取文件"))

            val content = BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8)).use { reader ->
                reader.readText()
            }

            val backupData = gson.fromJson(content, BackupData::class.java)
                ?: return Result.failure(Exception("备份文件格式错误"))

            Result.success(backupData)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    /**
     * 验证备份数据
     */
    fun validateBackup(backupData: BackupData, currentUserId: String): ValidationResult {
        return when {
            backupData.version > 1 -> ValidationResult.UnsupportedVersion
            backupData.userId != currentUserId -> ValidationResult.DifferentUser(backupData.userId)
            else -> ValidationResult.Valid
        }
    }

    /**
     * 备份验证结果
     */
    sealed class ValidationResult {
        object Valid : ValidationResult()
        object UnsupportedVersion : ValidationResult()
        data class DifferentUser(val backupUserId: String) : ValidationResult()
    }
}
