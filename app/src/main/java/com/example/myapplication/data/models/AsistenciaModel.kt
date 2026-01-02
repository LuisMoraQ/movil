// Archivo: AsistenciaModel.kt en la carpeta models
package com.example.myapplication.models

import com.google.gson.annotations.SerializedName

data class AsistenciaModel(
    @SerializedName("id_asistencia") val idAsistencia: Int?,
    @SerializedName("id_proyecto") val idProyecto: Int?,
    @SerializedName("id_usuario") val idUsuario: Int?,
    @SerializedName("nombres_usuario") val nombresUsuario: String?,
    @SerializedName("apellidos_usuario") val apellidosUsuario: String?,
    @SerializedName("tipo_asistencia") val tipoAsistencia: Int?,
    @SerializedName("fecha_hora") val fechaHora: String?,
    @SerializedName("nombre_area") val nombreArea: String?,
    @SerializedName("nombre_cargo") val nombreCargo: String?,
    @SerializedName("nombres_supervisor") val nombresSupervisor: String?,
    @SerializedName("apellidos_supervisor") val apellidosSupervisor: String?
)