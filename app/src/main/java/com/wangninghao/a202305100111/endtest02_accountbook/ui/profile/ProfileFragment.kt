package com.wangninghao.a202305100111.endtest02_accountbook.ui.profile

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.wangninghao.a202305100111.endtest02_accountbook.AccountBookApplication
import com.wangninghao.a202305100111.endtest02_accountbook.data.entity.Budget
import com.wangninghao.a202305100111.endtest02_accountbook.data.entity.Category
import com.wangninghao.a202305100111.endtest02_accountbook.data.entity.Record
import com.wangninghao.a202305100111.endtest02_accountbook.data.repository.RecordRepository
import com.wangninghao.a202305100111.endtest02_accountbook.databinding.FragmentProfileBinding
import com.wangninghao.a202305100111.endtest02_accountbook.ui.login.LoginActivity
import com.wangninghao.a202305100111.endtest02_accountbook.util.DataBackupManager
import com.wangninghao.a202305100111.endtest02_accountbook.util.DateUtils
import com.wangninghao.a202305100111.endtest02_accountbook.util.ExcelExporter
import com.wangninghao.a202305100111.endtest02_accountbook.util.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 我的页面Fragment
 */
class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private lateinit var sessionManager: SessionManager
    private lateinit var recordRepository: RecordRepository

    // 存储权限请求
    private val storagePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            performExport()
        } else {
            Toast.makeText(context, "需要存储权限才能导出文件", Toast.LENGTH_SHORT).show()
        }
    }

    // 选择备份文件
    private val pickBackupFileLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { performRestore(it) }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sessionManager = SessionManager(requireContext())
        recordRepository = RecordRepository(AccountBookApplication.instance.database.recordDao())

        setupUserInfo()
        setupClickListeners()
    }

    private fun setupUserInfo() {
        val phone = sessionManager.getCurrentUserPhone() ?: ""
        // 隐藏中间数字
        val maskedPhone = if (phone.length == 11) {
            "${phone.substring(0, 3)}****${phone.substring(7)}"
        } else {
            phone
        }
        binding.tvPhone.text = maskedPhone

        // 显示注册时间
        val loginTime = sessionManager.getLoginTime()
        if (loginTime > 0) {
            binding.tvRegisterTime.text = "登录于 ${DateUtils.formatDate(loginTime)}"
        }
    }

    private fun setupClickListeners() {
        // 导出Excel
        binding.layoutExport.setOnClickListener {
            checkStoragePermissionAndExport()
        }

        // 备份数据
        binding.layoutBackup.setOnClickListener {
            performBackup()
        }

        // 恢复数据
        binding.layoutRestore.setOnClickListener {
            showRestoreConfirmDialog()
        }

        // 关于
        binding.layoutAbout.setOnClickListener {
            showAboutDialog()
        }

        // 退出登录
        binding.btnLogout.setOnClickListener {
            showLogoutDialog()
        }
    }

    /**
     * 检查存储权限并导出
     */
    private fun checkStoragePermissionAndExport() {
        // Android 10+ 不需要存储权限来写入Downloads目录
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            performExport()
        } else {
            when {
                ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED -> {
                    performExport()
                }
                else -> {
                    storagePermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                }
            }
        }
    }

    /**
     * 执行导出操作
     */
    private fun performExport() {
        val userId = sessionManager.getCurrentUserPhone() ?: return
        val ctx = context ?: return

        // 显示加载提示
        Toast.makeText(ctx, "正在导出数据...", Toast.LENGTH_SHORT).show()

        lifecycleScope.launch {
            try {
                // 在IO线程获取所有记录
                val records = withContext(Dispatchers.IO) {
                    recordRepository.getAllRecordsForExport(userId)
                }

                if (records.isEmpty()) {
                    Toast.makeText(ctx, "没有可导出的数据", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                // 在IO线程执行导出
                val result = withContext(Dispatchers.IO) {
                    ExcelExporter.exportRecords(ctx, records)
                }

                result.fold(
                    onSuccess = { filePath ->
                        showExportSuccessDialog(filePath, records.size)
                    },
                    onFailure = { e ->
                        Toast.makeText(ctx, "导出失败: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                )
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(ctx, "导出失败: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * 执行备份操作
     */
    private fun performBackup() {
        val userId = sessionManager.getCurrentUserPhone() ?: return
        val ctx = context ?: return
        val database = AccountBookApplication.instance.database

        Toast.makeText(ctx, "正在备份数据...", Toast.LENGTH_SHORT).show()

        lifecycleScope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    val records = database.recordDao().getAllRecordsForExport(userId)
                    val budgets = database.budgetDao().getAllBudgets(userId)
                    val categories = database.categoryDao().getAllCategoriesSync(userId)

                    DataBackupManager.exportBackup(ctx, userId, records, budgets, categories)
                }

                result.fold(
                    onSuccess = { filePath ->
                        AlertDialog.Builder(ctx)
                            .setTitle("备份成功")
                            .setMessage("数据已备份到:\n$filePath")
                            .setPositiveButton("确定", null)
                            .show()
                    },
                    onFailure = { e ->
                        Toast.makeText(ctx, "备份失败: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                )
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(ctx, "备份失败: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * 显示恢复确认对话框
     */
    private fun showRestoreConfirmDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("恢复数据")
            .setMessage("请选择备份文件（.json格式）\n\n注意：恢复操作将覆盖当前所有数据！")
            .setPositiveButton("选择文件") { _, _ ->
                pickBackupFileLauncher.launch("application/json")
            }
            .setNegativeButton("取消", null)
            .show()
    }

    /**
     * 执行恢复操作
     */
    private fun performRestore(uri: android.net.Uri) {
        val userId = sessionManager.getCurrentUserPhone() ?: return
        val ctx = context ?: return
        val database = AccountBookApplication.instance.database

        lifecycleScope.launch {
            try {
                // 读取备份文件
                val readResult = withContext(Dispatchers.IO) {
                    DataBackupManager.readBackup(ctx, uri)
                }

                readResult.fold(
                    onSuccess = { backupData ->
                        // 验证备份数据
                        when (val validation = DataBackupManager.validateBackup(backupData, userId)) {
                            is DataBackupManager.ValidationResult.Valid -> {
                                // 显示确认对话框
                                showRestoreDataConfirmDialog(backupData)
                            }
                            is DataBackupManager.ValidationResult.UnsupportedVersion -> {
                                Toast.makeText(ctx, "不支持的备份版本", Toast.LENGTH_SHORT).show()
                            }
                            is DataBackupManager.ValidationResult.DifferentUser -> {
                                AlertDialog.Builder(ctx)
                                    .setTitle("用户不匹配")
                                    .setMessage("备份文件属于其他用户（${validation.backupUserId}），是否仍要恢复？")
                                    .setPositiveButton("恢复") { _, _ ->
                                        executeRestore(backupData)
                                    }
                                    .setNegativeButton("取消", null)
                                    .show()
                            }
                        }
                    },
                    onFailure = { e ->
                        Toast.makeText(ctx, "读取备份失败: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                )
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(ctx, "恢复失败: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * 显示恢复数据确认对话框
     */
    private fun showRestoreDataConfirmDialog(backupData: DataBackupManager.BackupData) {
        val ctx = context ?: return
        val backupTime = DateUtils.formatDateTime(backupData.backupTime)

        AlertDialog.Builder(ctx)
            .setTitle("确认恢复")
            .setMessage("""
                备份时间: $backupTime
                记录数量: ${backupData.records.size} 条
                预算数量: ${backupData.budgets.size} 条
                分类数量: ${backupData.categories.size} 个

                恢复将覆盖当前所有数据，确定继续？
            """.trimIndent())
            .setPositiveButton("恢复") { _, _ ->
                executeRestore(backupData)
            }
            .setNegativeButton("取消", null)
            .show()
    }

    /**
     * 执行数据恢复
     */
    private fun executeRestore(backupData: DataBackupManager.BackupData) {
        val userId = sessionManager.getCurrentUserPhone() ?: return
        val ctx = context ?: return
        val database = AccountBookApplication.instance.database

        Toast.makeText(ctx, "正在恢复数据...", Toast.LENGTH_SHORT).show()

        lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    // 删除现有数据
                    database.recordDao().deleteAllRecords(userId)
                    database.budgetDao().deleteAllBudgets(userId)
                    database.categoryDao().deleteAllCategories(userId)

                    // 恢复数据（更新userId为当前用户）
                    val records = backupData.records.map { it.copy(id = 0, userId = userId) }
                    val budgets = backupData.budgets.map { it.copy(id = 0, userId = userId) }
                    val categories = backupData.categories.map { it.copy(id = 0, userId = userId) }

                    database.categoryDao().insertCategories(categories)
                    database.recordDao().insertRecords(records)
                    database.budgetDao().insertBudgets(budgets)
                }

                AlertDialog.Builder(ctx)
                    .setTitle("恢复成功")
                    .setMessage("已成功恢复 ${backupData.records.size} 条记录")
                    .setPositiveButton("确定", null)
                    .show()

            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(ctx, "恢复失败: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * 显示导出成功对话框
     */
    private fun showExportSuccessDialog(filePath: String, recordCount: Int) {
        AlertDialog.Builder(requireContext())
            .setTitle("导出成功")
            .setMessage("已成功导出 $recordCount 条记录\n\n文件位置：$filePath")
            .setPositiveButton("确定", null)
            .show()
    }

    private fun showAboutDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("关于")
            .setMessage("""
                记账本 v1.0

                一款简洁易用的个人记账应用

                开发者：王宁皓
                学号：202305100111

                功能特性：
                • 快速记账
                • 数据可视化
                • 预算管理
                • OCR小票识别
                • Excel导出
                • 数据备份与恢复
            """.trimIndent())
            .setPositiveButton("确定", null)
            .show()
    }

    private fun showLogoutDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("退出登录")
            .setMessage("确定要退出登录吗？")
            .setPositiveButton("确定") { _, _ ->
                logout()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun logout() {
        sessionManager.clearLoginState()
        val intent = Intent(requireContext(), LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
