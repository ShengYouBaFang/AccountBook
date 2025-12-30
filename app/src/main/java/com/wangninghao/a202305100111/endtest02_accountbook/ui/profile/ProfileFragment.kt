package com.wangninghao.a202305100111.endtest02_accountbook.ui.profile

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.wangninghao.a202305100111.endtest02_accountbook.databinding.FragmentProfileBinding
import com.wangninghao.a202305100111.endtest02_accountbook.ui.login.LoginActivity
import com.wangninghao.a202305100111.endtest02_accountbook.util.DateUtils
import com.wangninghao.a202305100111.endtest02_accountbook.util.SessionManager

/**
 * 我的页面Fragment
 */
class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private lateinit var sessionManager: SessionManager

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
            Toast.makeText(context, "Excel导出功能开发中", Toast.LENGTH_SHORT).show()
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
