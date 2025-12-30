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
import com.wangninghao.a202305100111.endtest02_accountbook.data.repository.RecordRepository
import com.wangninghao.a202305100111.endtest02_accountbook.databinding.FragmentProfileBinding
import com.wangninghao.a202305100111.endtest02_accountbook.ui.login.LoginActivity
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

        // 显示加载提示
        Toast.makeText(context, "正在导出数据...", Toast.LENGTH_SHORT).show()

        lifecycleScope.launch {
            try {
                // 在IO线程获取所有记录
                val records = withContext(Dispatchers.IO) {
                    recordRepository.getAllRecordsForExport(userId)
                }

                if (records.isEmpty()) {
                    Toast.makeText(context, "没有可导出的数据", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                // 在IO线程执行导出
                val result = withContext(Dispatchers.IO) {
                    ExcelExporter.exportRecords(requireContext(), records)
                }

                result.fold(
                    onSuccess = { filePath ->
                        showExportSuccessDialog(filePath, records.size)
                    },
                    onFailure = { e ->
                        Toast.makeText(context, "导出失败: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                )
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, "导出失败: ${e.message}", Toast.LENGTH_SHORT).show()
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
