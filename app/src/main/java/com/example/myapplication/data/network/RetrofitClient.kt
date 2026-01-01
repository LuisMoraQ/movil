package com.example.myapplication.network

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {
    const val BASE_URL = "http://192.168.1.38:5000/" //ipcasa
   const val baseSlash = "192.168.1.38:5000"//ipofi
   // const val BASE_URL = "http://192.168.18.102:5000/" //ipofi

    //const val baseSlash = "192.168.18.102:5000"//ipofi
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    // Cliente base SIN token (para login)
    private val baseClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .addInterceptor { chain ->
            val original = chain.request()
            val request = original.newBuilder()
                .header("Host", baseSlash)
                .header("X-Forwarded-Host", "localhost")
                .build()
            chain.proceed(request)
        }
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(baseClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    // API service SIN autenticación (para login)
    val apiService: ApiService by lazy {
        retrofit.create(ApiService::class.java)
    }

    // ✅ NUEVO: Método para obtener API service CON token
    fun getAuthenticatedApiService(token: String): ApiService {
        val authenticatedClient = baseClient.newBuilder()
            .addInterceptor { chain ->
                val original = chain.request()
                // Agregar token de autorización
                val request = original.newBuilder()
                    .header("Authorization", "Bearer $token")
                    .header("Host", baseSlash)
                    .header("X-Forwarded-Host", "localhost")
                    .build()
                chain.proceed(request)
            }
            .build()

        val authenticatedRetrofit = retrofit.newBuilder()
            .client(authenticatedClient)
            .build()

        return authenticatedRetrofit.create(ApiService::class.java)
    }

    // ✅ Para debug: Verificar headers
    fun debugHeaders(token: String) {
        println("=== DEBUG RETROFIT ===")
        println("URL Base: $BASE_URL")
        println("Token (primeros 20 chars): ${token.take(20)}...")
        println("Headers con token: Authorization: Bearer $token")
        println("=== FIN DEBUG ===")
    }
}