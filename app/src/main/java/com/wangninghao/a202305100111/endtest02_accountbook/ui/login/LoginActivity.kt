package com.wangninghao.a202305100111.endtest02_accountbook.ui.login

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.doAfterTextChanged
import com.wangninghao.a202305100111.endtest02_accountbook.AccountBookApplication
import com.wangninghao.a202305100111.endtest02_accountbook.MainActivity
import com.wangninghao.a202305100111.endtest02_accountbook.data.repository.CategoryRepository
import com.wangninghao.a202305100111.endtest02_accountbook.data.repository.UserRepository
import com.wangninghao.a202305100111.endtest02_accountbook.databinding.ActivityLoginBinding
import com.wangninghao.a202305100111.endtest02_accountbook.util.SessionManager

/**
 * 登录页面
 * 显示开发者信息，支持手机号密码登录/注册
 */
class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var sessionManager: SessionManager

    private val viewModel: LoginViewModel by viewModels {
        val database = AccountBookApplication.instance.database
        val userRepository = UserRepository(
            database.userDao(),
            database.categoryDao()
        )
        LoginViewModelFactory(userRepository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 处理系统栏
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        sessionManager = SessionManager(this)

        // 检查是否已登录
        if (sessionManager.isLoggedIn()) {
            navigateToMain()
            return
        }

        setupViews()
        observeViewModel()
    }

    private fun setupViews() {
        // 清除输入错误提示
        binding.etPhone.doAfterTextChanged {
            binding.tilPhone.error = null
        }
        binding.etPassword.doAfterTextChanged {
            binding.tilPassword.error = null
        }

        // 登录按钮点击
        binding.btnLogin.setOnClickListener {
            val phone = binding.etPhone.text?.toString()?.trim() ?: ""
            val password = binding.etPassword.text?.toString() ?: ""
            viewModel.loginOrRegister(phone, password)
        }
    }

    private fun observeViewModel() {
        // 观察登录状态
        viewModel.loginState.observe(this) { state ->
            when (state) {
                is LoginState.Success -> {
                    sessionManager.saveLoginState(state.user.phone)
                    Toast.makeText(this, "登录成功", Toast.LENGTH_SHORT).show()
                    navigateToMain()
                }
                is LoginState.Error -> {
                    showError(state.message)
                }
                is LoginState.Idle -> {
                    // 空闲状态
                }
            }
        }

        // 观察加载状态
        viewModel.isLoading.observe(this) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            binding.btnLogin.isEnabled = !isLoading
            binding.etPhone.isEnabled = !isLoading
            binding.etPassword.isEnabled = !isLoading
        }
    }

    private fun showError(message: String) {
        when {
            message.contains("手机号") -> {
                binding.tilPhone.error = message
            }
            message.contains("密码") -> {
                binding.tilPassword.error = message
            }
            else -> {
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun navigateToMain() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }
}
