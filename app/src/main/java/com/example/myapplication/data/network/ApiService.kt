package com.example.myapplication.network

import com.example.myapplication.models.*
import retrofit2.http.*

interface ApiService {
    @POST("api/Users/authenticate")
    suspend fun authenticate(@Body request: LoginRequest): List<LoginResponse>

    @POST("api/Users/forceLogout")
    suspend fun forceLogout(@Body request: ForceLogoutRequest): ForceLogoutResponse?

    @FormUrlEncoded
    @POST("api/recursoshumanos/proyectosmovil")
    suspend fun listarProyectos(
        @Field("id_usuario") idUsuario: String,
        @Field("fuente") fuente: String = "1"
    ): List<Proyecto>

    @FormUrlEncoded
    @POST("api/recursoshumanos/agregarActualizarAsistenciaMovil")
    suspend fun registrarAsistencia(
        @Field("id_proyecto") idProyecto: String,
        @Field("id_asignacion") idAsignacion: String,
        @Field("id_usuario") idUsuario: String,
        @Field("id_area") idArea: String,
        @Field("id_cargo") idCargo: String,
        @Field("tipo_asistencia") tipoAsistencia: String,
        @Field("fecha_hora") fechaHora: String,
        @Field("id_supervisor") idSupervisor: String,
        @Field("modo_registro") modoRegistro: String
    ): List<ApiResponseGeneral>

    @FormUrlEncoded
    @POST("api/recursoshumanos/listarAsistenciaMovil")
    suspend fun listarAsistencias(
        @Field("id_proyecto") idProyecto: String,
        @Field("id_area") idArea: String,
        @Field("id_cargo") idCargo: String,
        @Field("fecha_inicio") fechaInicio: String,
        @Field("fecha_fin") fechaFin: String
    ): List<AsistenciaModel>  // Ahora AsistenciaModel está en models/
}