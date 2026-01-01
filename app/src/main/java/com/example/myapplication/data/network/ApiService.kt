package com.example.myapplication.network

import com.example.myapplication.models.ForceLogoutRequest
import com.example.myapplication.models.ForceLogoutResponse
import com.example.myapplication.models.LoginRequest
import com.example.myapplication.models.LoginResponse
import com.example.myapplication.models.Proyecto
import retrofit2.http.Body
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST

interface ApiService {
    @POST("api/Users/authenticate")
    suspend fun authenticate(@Body request: LoginRequest): List<LoginResponse> // Tu API devuelve un array

    @POST("api/Users/forceLogout") // Ajusta el endpoint según tu backend
    suspend fun forceLogout(@Body request: ForceLogoutRequest): ForceLogoutResponse?

    // Agrega más endpoints según necesites
    @FormUrlEncoded
    @POST("api/recursoshumanos/proyectosmovil")
    suspend fun listarProyectos(
        @Field("id_usuario") idUsuario: String,
        @Field("fuente") fuente: String = "1"
    ): List<Proyecto>
}