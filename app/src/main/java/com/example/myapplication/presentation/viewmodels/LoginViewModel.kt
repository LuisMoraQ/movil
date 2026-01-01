package com.example.myapplication.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.models.LoginResponse
import com.example.myapplication.network.RetrofitClient
import com.example.myapplication.utils.PreferencesManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

import androidx.lifecycle.viewModelScope
import com.example.myapplication.models.LoginRequest
import kotlinx.coroutines.Dispatchers

import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
sealed class LoginState {
    object Idle : LoginState()
    object Loading : LoginState()
    data class Success(val userData: LoginResponse) : LoginState()
    data class Error(val message: String) : LoginState()
}

class LoginViewModel(
    private val context: Context // Necesitamos el contexto para PreferencesManager
) : ViewModel() {
    private val _loginState = MutableStateFlow<LoginState>(LoginState.Idle)
    val loginState: StateFlow<LoginState> = _loginState.asStateFlow()

    private val _username = MutableStateFlow("")
    val username: StateFlow<String> = _username.asStateFlow()

    private val _password = MutableStateFlow("")
    val password: StateFlow<String> = _password.asStateFlow()

    private val preferencesManager = PreferencesManager(context)

    fun updateUsername(username: String) {
        _username.value = username
    }

    fun updatePassword(password: String) {
        _password.value = password
    }

    // En LoginViewModel.kt, actualiza la función login()
    fun login() {
        if (_username.value.isEmpty() || _password.value.isEmpty()) {
            _loginState.value = LoginState.Error("Ingrese usuario y contraseña")
            return
        }

        viewModelScope.launch {
            _loginState.value = LoginState.Loading

            try {
                val request = LoginRequest(
                    Username = _username.value,
                    Password = _password.value
                )

                val response = RetrofitClient.apiService.authenticate(request)

                if (response.isNotEmpty()) {
                    val userData = response[0]

                    // Validaciones
                    if (userData.estado == 2) {
                        _loginState.value = LoginState.Error("No tiene acceso al sistema")
                        return@launch
                    }

                    // Guardar datos en SharedPreferences
                    saveUserData(userData)

                    // Éxito
                    _loginState.value = LoginState.Success(userData)
                } else {
                    _loginState.value = LoginState.Error("Usuario o contraseña incorrectos")
                }

            } catch (e: Exception) {
                _loginState.value = LoginState.Error("Error de conexión: ${e.message}")
            }
        }
    }

    private fun saveUserData(userData: LoginResponse) {
        userData.id_usuario?.let { preferencesManager.saveUserId(it) }
        userData.nombres?.let { preferencesManager.saveUserName(it) }
        userData.token?.let { preferencesManager.saveToken(it) }
        userData.role?.let { preferencesManager.saveUserRole(it) }
        userData.arbol?.let { preferencesManager.saveAccessTree(it) }
        userData.configtema?.let { preferencesManager.saveConfigTheme(it) }
        userData.id_admin?.let { preferencesManager.saveObject("id_admin", it) }

        // Guardar objeto completo
        preferencesManager.saveObject("user_data", userData)

        Log.d("LOGIN", "Usuario guardado: ${userData.nombres}")
    }


    // Verificar si ya hay una sesión activa
    fun checkExistingSession(): Boolean {
        return preferencesManager.isLoggedIn()
    }

    // Cerrar sesión
    fun logout() {
        preferencesManager.clearSession()
    }

    fun resetState() {
        _loginState.value = LoginState.Idle
    }
}