package com.wangninghao.a202305100111.endtest02_accountbook.ui.login

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
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
        playEnterAnimation()
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

    private fun playEnterAnimation() {
        // 初始状态
        binding.ivLogo.alpha = 0f
        binding.ivLogo.scaleX = 0.5f
        binding.ivLogo.scaleY = 0.5f
        binding.tvTitle.alpha = 0f
        binding.tvSubtitle.alpha = 0f
        binding.cardLogin.alpha = 0f
        binding.cardLogin.translationY = 100f

        // Logo动画
        val logoAnimator = AnimatorSet().apply {
            playTogether(
                ObjectAnimator.ofFloat(binding.ivLogo, View.ALPHA, 0f, 1f),
                ObjectAnimator.ofFloat(binding.ivLogo, View.SCALE_X, 0.5f, 1f),
                ObjectAnimator.ofFloat(binding.ivLogo, View.SCALE_Y, 0.5f, 1f)
            )
            duration = 600
            interpolator = OvershootInterpolator(1.5f)
        }

        // 标题动画
        val titleAnimator = AnimatorSet().apply {
            playTogether(
                ObjectAnimator.ofFloat(binding.tvTitle, View.ALPHA, 0f, 1f),
                ObjectAnimator.ofFloat(binding.tvTitle, View.TRANSLATION_Y, -20f, 0f)
            )
            duration = 400
            startDelay = 200
        }

        // 副标题动画
        val subtitleAnimator = ObjectAnimator.ofFloat(binding.tvSubtitle, View.ALPHA, 0f, 1f).apply {
            duration = 400
            startDelay = 300
        }

        // 登录卡片动画
        val cardAnimator = AnimatorSet().apply {
            playTogether(
                ObjectAnimator.ofFloat(binding.cardLogin, View.ALPHA, 0f, 1f),
                ObjectAnimator.ofFloat(binding.cardLogin, View.TRANSLATION_Y, 100f, 0f)
            )
            duration = 500
            startDelay = 400
            interpolator = DecelerateInterpolator()
        }

        // 执行所有动画
        AnimatorSet().apply {
            playTogether(logoAnimator, titleAnimator, subtitleAnimator, cardAnimator)
            start()
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
