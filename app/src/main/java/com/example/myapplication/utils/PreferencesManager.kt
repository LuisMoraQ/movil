package com.example.myapplication.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.google.gson.Gson

class PreferencesManager(context: Context) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    // Usar EncryptedSharedPreferences para mayor seguridad
    private val sharedPreferences: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        "app_secure_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    // Alternativa simple si no quieres usar encryption:
    // private val sharedPreferences = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)

    private val gson = Gson()

    // Guardar datos básicos
    fun saveUserId(id: Int) {
        sharedPreferences.edit().putInt("idusuario", id).apply()
    }

    fun saveUserRole(role: String) {
        sharedPreferences.edit().putString("role", role).apply()
    }

    fun saveUserName(name: String) {
        sharedPreferences.edit().putString("nombre", name).apply()
    }

    fun saveToken(token: String) {
        sharedPreferences.edit().putString("token", token).apply()
    }

    fun saveAccessTree(tree: String) {
        sharedPreferences.edit().putString("accesos", tree).apply()
    }

    fun saveConfigTheme(config: String) {
        sharedPreferences.edit().putString("configtema", config).apply()
    }

    // Guardar objeto complejo como JSON
    fun saveObject(key: String, obj: Any) {
        val json = gson.toJson(obj)
        sharedPreferences.edit().putString(key, json).apply()
    }

    // Leer datos
    fun getUserId(): Int = sharedPreferences.getInt("idusuario", -1)
    fun getUserRole(): String = sharedPreferences.getString("role", "") ?: ""
    fun getUserName(): String = sharedPreferences.getString("nombre", "") ?: ""
    fun getToken(): String = sharedPreferences.getString("token", "") ?: ""
    fun getAccessTree(): String = sharedPreferences.getString("accesos", "[]") ?: "[]"
    fun getConfigTheme(): String = sharedPreferences.getString("configtema", "") ?: ""

    fun getObject(key: String, type: Class<*>): Any? {
        val json = sharedPreferences.getString(key, null)
        return json?.let { gson.fromJson(it, type) }
    }

    // Verificar si el usuario está logueado
    fun isLoggedIn(): Boolean {
        return getToken().isNotEmpty() && getUserId() != -1
    }

    // Limpiar sesión (logout)
    fun clearSession() {
        sharedPreferences.edit().clear().apply()
    }
}