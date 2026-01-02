// Nuevo modelo: ApiResponseGeneral.kt
package com.example.myapplication.models

import com.google.gson.annotations.SerializedName

data class ApiResponseGeneral(
    @SerializedName("success") val success: Boolean?,
    @SerializedName("message") val message: String?,
    @SerializedName("data") val data: List<Map<String, Any>>? = null
)