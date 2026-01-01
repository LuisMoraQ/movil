package com.example.myapplication.models

data class LoginRequest(
    val Username: String,
    val Password: String
)

data class LoginResponse(
    val id_usuario: Int? = null,
    val role: String? = null,
    val nombres: String? = null,
    val token: String? = null,
    val arbol: String? = null, // JSON como string
    val fechaact: String? = null,
    val id_admin: Int? = null,
    val menu_proy: String? = null,
    val nroproys: Int? = null,
    val estado: Int? = null,
    val conex: Boolean? = null,
    val fechaserv: String? = null,
    val fechacul: String? = null,
    val configtema: String? = null,

    // Campos para manejo de errores
    val success: Boolean = true,
    val message: String? = null
)

// Modelo para forzar cierre de sesión
data class ForceLogoutRequest(
    val id_usuario: Int,
    val token: String
)

data class ForceLogoutResponse(
    val id: String? = null,
    val success: Boolean = false,
    val message: String? = null
)