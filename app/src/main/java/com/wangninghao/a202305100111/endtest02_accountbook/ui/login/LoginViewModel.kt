package com.wangninghao.a202305100111.endtest02_accountbook.ui.login

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.wangninghao.a202305100111.endtest02_accountbook.data.entity.User
import com.wangninghao.a202305100111.endtest02_accountbook.data.repository.UserRepository
import kotlinx.coroutines.launch

/**
 * 登录页面ViewModel
 */
class LoginViewModel(private val userRepository: UserRepository) : ViewModel() {

    // 登录状态
    private val _loginState = MutableLiveData<LoginState>()
    val loginState: LiveData<LoginState> = _loginState

    // 加载状态
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    /**
     * 登录或注册
     */
    fun loginOrRegister(phone: String, password: String) {
        // 验证输入
        if (phone.isBlank()) {
            _loginState.value = LoginState.Error("请输入手机号")
            return
        }
        if (!isValidPhone(phone)) {
            _loginState.value = LoginState.Error("请输入正确的手机号")
            return
        }
        if (password.isBlank()) {
            _loginState.value = LoginState.Error("请输入密码")
            return
        }
        if (password.length < 6) {
            _loginState.value = LoginState.Error("密码长度至少6位")
            return
        }

        _isLoading.value = true
        viewModelScope.launch {
            try {
                val result = userRepository.loginOrRegister(phone, password)
                result.fold(
                    onSuccess = { user ->
                        _loginState.value = LoginState.Success(user)
                    },
                    onFailure = { exception ->
                        _loginState.value = LoginState.Error(exception.message ?: "登录失败")
                    }
                )
            } catch (e: Exception) {
                _loginState.value = LoginState.Error(e.message ?: "未知错误")
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * 验证手机号格式
     */
    private fun isValidPhone(phone: String): Boolean {
        return phone.length == 11 && phone.startsWith("1")
    }

    /**
     * 重置状态
     */
    fun resetState() {
        _loginState.value = LoginState.Idle
    }
}

/**
 * 登录状态
 */
sealed class LoginState {
    object Idle : LoginState()
    data class Success(val user: User) : LoginState()
    data class Error(val message: String) : LoginState()
}

/**
 * ViewModel工厂
 */
class LoginViewModelFactory(
    private val userRepository: UserRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LoginViewModel::class.java)) {
            return LoginViewModel(userRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
