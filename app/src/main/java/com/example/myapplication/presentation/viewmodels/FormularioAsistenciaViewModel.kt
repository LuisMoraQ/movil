package com.example.myapplication.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.models.AsistenciaModel
import com.example.myapplication.network.RetrofitClient
import com.example.myapplication.utils.PreferencesManager
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import android.content.Context

class FormularioAsistenciaViewModel : ViewModel() {

    // Estados con LiveData
    private val _tipoAsistencia = MutableLiveData("")
    val tipoAsistencia: LiveData<String> = _tipoAsistencia

    private val _datosEmpleado = MutableLiveData<Map<String, String>?>(null)
    val datosEmpleado: LiveData<Map<String, String>?> = _datosEmpleado

    private val _showQRScanner = MutableLiveData(false)
    val showQRScanner: LiveData<Boolean> = _showQRScanner

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _registroExitoso = MutableLiveData(false)
    val registroExitoso: LiveData<Boolean> = _registroExitoso

    private val _errorMessage = MutableLiveData("")
    val errorMessage: LiveData<String> = _errorMessage

    private val _showErrorAlert = MutableLiveData(false)
    val showErrorAlert: LiveData<Boolean> = _showErrorAlert

    private val _listaAsistencias = MutableLiveData<List<AsistenciaModel>>(emptyList())
    val listaAsistencias: LiveData<List<AsistenciaModel>> = _listaAsistencias

    private val _isLoadingAsistencias = MutableLiveData(false)
    val isLoadingAsistencias: LiveData<Boolean> = _isLoadingAsistencias

    // Variables privadas
    private lateinit var preferencesManager: PreferencesManager
    private var proyectoId: String = ""
    private var idSupervisor: String = ""

    // Métodos públicos para modificar estados
    fun updateTipoAsistencia(tipo: String) {
        _tipoAsistencia.value = tipo
    }

    fun updateShowQRScanner(show: Boolean) {
        _showQRScanner.value = show
    }

    fun updateShowErrorAlert(show: Boolean) {
        _showErrorAlert.value = show
    }

    // Inicialización
    fun inicializar(context: Context, proyectoId: String) {
        this.proyectoId = proyectoId
        preferencesManager = PreferencesManager(context)
        val userId = preferencesManager.getUserId()
        idSupervisor = userId?.toString() ?: ""
    }

    // Cargar asistencias
    fun cargarAsistencias() {
        viewModelScope.launch {
            _isLoadingAsistencias.value = true
            try {
                val token = preferencesManager.getToken()
                if (token.isNullOrEmpty()) return@launch

                val apiService = RetrofitClient.getAuthenticatedApiService(token)

                // Obtener fecha actual
                val sdfFecha = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val fechaActual = sdfFecha.format(Date())

                val response = apiService.listarAsistencias(
                    idProyecto = proyectoId,
                    idArea = "",
                    idCargo = "",
                    fechaInicio = fechaActual,
                    fechaFin = fechaActual
                )

                _listaAsistencias.value = response
            } catch (e: Exception) {
                _errorMessage.value = "Error al cargar asistencias: ${e.message}"
                _showErrorAlert.value = true
            } finally {
                _isLoadingAsistencias.value = false
            }
        }
    }

    // Registrar asistencia
    fun registrarAsistencia() {
        val datos = _datosEmpleado.value
        val tipo = _tipoAsistencia.value

        if (datos == null || tipo.isNullOrEmpty() || idSupervisor.isEmpty()) {
            _errorMessage.value = "Faltan datos necesarios para el registro"
            _showErrorAlert.value = true
            return
        }

        viewModelScope.launch {
            _isLoading.value = true

            try {
                // Preparar datos
                val idUsuario = datos["id_usuario"] ?: ""
                val idArea = datos["id_area"] ?: ""
                val idCargo = datos["id_cargo"] ?: ""
                val idAsignacion = datos["id_asignacion"] ?: ""

                // Obtener fecha y hora actual
                val sdfHora = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                val sdfFecha = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                val horaActual = sdfHora.format(Date())
                val fechaActual = sdfFecha.format(Date())

                val fechaHoraParaMySQL = convertirFechaParaMySQL(fechaActual, horaActual)
                val modoRegistro = "1"

                // Obtener token
                val token = preferencesManager.getToken()
                if (token.isNullOrEmpty()) {
                    _errorMessage.value = "Sesión expirada"
                    _showErrorAlert.value = true
                    _isLoading.value = false
                    return@launch
                }

                // Llamar al API
                val apiService = RetrofitClient.getAuthenticatedApiService(token)
                val response = apiService.registrarAsistencia(
                    idProyecto = proyectoId,
                    idAsignacion = idAsignacion,
                    idUsuario = idUsuario,
                    idArea = idArea,
                    idCargo = idCargo,
                    tipoAsistencia = tipo,
                    fechaHora = fechaHoraParaMySQL,
                    idSupervisor = idSupervisor,
                    modoRegistro = modoRegistro
                )

                // Procesar respuesta
                if (response.isNotEmpty()) {
                    val respuesta = response[0]
                    val esExito = respuesta.success == true || respuesta.success?.toString() == "true"

                    if (esExito) {
                        _registroExitoso.value = true
                        _errorMessage.value = "Asistencia registrada exitosamente"
                        _showErrorAlert.value = true

                        // Recargar la tabla de asistencias
                        cargarAsistencias()

                        // Limpiar datos
                        _datosEmpleado.value = null
                        _tipoAsistencia.value = ""
                    } else {
                        _errorMessage.value = respuesta.message ?: "Error al registrar"
                        _showErrorAlert.value = true
                    }
                } else {
                    _errorMessage.value = "No se recibió respuesta del servidor"
                    _showErrorAlert.value = true
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error: ${e.message ?: "Desconocido"}"
                _showErrorAlert.value = true
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Procesar QR
    fun procesarQRContent(qrContent: String, proyectoIdActual: String) {
        if (qrContent.isEmpty()) {
            _errorMessage.value = "Código QR vacío"
            _showErrorAlert.value = true
            return
        }

        val datosDesglosados = qrContent.split("|")

        if (datosDesglosados.size < 12) {
            _errorMessage.value = "Código QR inválido. Faltan datos."
            _showErrorAlert.value = true
            return
        }

        // Extraer datos del QR
        val datosMap = mutableMapOf<String, String>()
        datosDesglosados.forEachIndexed { index, dato ->
            when (index) {
                0 -> datosMap["id_usuario"] = dato
                1 -> datosMap["id_proyecto"] = dato
                2 -> datosMap["nombres"] = dato
                3 -> datosMap["apellidos"] = dato
                4 -> datosMap["dni"] = dato
                5 -> datosMap["id_asignacion"] = dato
                6 -> datosMap["descripcion_area"] = dato
                7 -> datosMap["id_area"] = dato
                8 -> datosMap["descripcion_cargo"] = dato
                9 -> datosMap["id_cargo"] = dato
                10 -> datosMap["fecha_inicio"] = dato
                11 -> datosMap["fecha_fin"] = dato
            }
        }

        _datosEmpleado.value = datosMap

        // Validar proyecto
        val proyectoIdQR = datosMap["id_proyecto"] ?: ""
        val proyectoValido = proyectoIdQR == proyectoIdActual

        if (!proyectoValido) {
            _errorMessage.value = "El empleado no pertenece a este proyecto"
            _showErrorAlert.value = true
            return
        }
    }

    // Función privada para convertir fecha
    private fun convertirFechaParaMySQL(fecha: String, hora: String): String {
        val partes = fecha.split("/")
        return if (partes.size == 3) {
            val dia = partes[0].padStart(2, '0')
            val mes = partes[1].padStart(2, '0')
            val anio = partes[2]
            "$anio-$mes-$dia $hora"
        } else {
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            sdf.format(Date())
        }
    }
}