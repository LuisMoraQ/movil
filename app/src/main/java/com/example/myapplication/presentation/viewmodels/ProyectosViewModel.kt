package com.example.myapplication.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.network.RetrofitClient
import com.example.myapplication.utils.PreferencesManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// ✅ CORRECTO: Es una Class (hereda de ViewModel)
class ProyectosViewModel(context: Context) : ViewModel() {
    private val preferencesManager = PreferencesManager(context)

    private val _proyectos = MutableStateFlow<List<com.example.myapplication.models.Proyecto>>(emptyList())
    val proyectos: StateFlow<List<com.example.myapplication.models.Proyecto>> = _proyectos.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun cargarProyectos() {
        val userId = preferencesManager.getUserId().toString()
        val token = preferencesManager.getToken()

        if (userId == "-1" || token.isEmpty()) {
            _error.value = "Usuario no autenticado o token inválido"
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                // Para debug
                println("DEBUG: UserId: $userId")
                println("DEBUG: Token length: ${token.length}")

                // ✅ USAR el nuevo método con token
                val apiService = RetrofitClient.getAuthenticatedApiService(token)

                // Para verificar
                RetrofitClient.debugHeaders(token)

                val response = apiService.listarProyectos(
                    idUsuario = userId,
                    fuente = "1"
                )

                println("DEBUG: Proyectos recibidos: ${response.size}")
                _proyectos.value = response

            } catch (e: Exception) {
                println("DEBUG: Error en cargarProyectos: ${e.message}")

                when {
                    e is retrofit2.HttpException -> {
                        when (e.code()) {
                            401 -> _error.value = "Sesión expirada (401)"
                            404 -> _error.value = "Endpoint no encontrado (404)"
                            403 -> _error.value = "Acceso denegado (403)"
                            else -> _error.value = "Error HTTP ${e.code()}: ${e.message}"
                        }
                        // Debug: ver error body
                        val errorBody = e.response()?.errorBody()?.string()
                        println("DEBUG: Error Body: $errorBody")
                    }
                    else -> {
                        _error.value = "Error de conexión: ${e.message}"
                    }
                }
            } finally {
                _isLoading.value = false
            }
        }
    }
    fun refrescar() {
        cargarProyectos()
    }
}