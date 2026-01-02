package com.example.myapplication.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.background
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import com.example.myapplication.utils.PreferencesManager
import com.example.myapplication.network.RetrofitClient
import com.example.myapplication.models.AsistenciaModel
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.text.style.TextOverflow

// Función para extraer solo la hora de una fecha completa (ej: "14:30")
fun extraerHora(fechaHora: String?): String {
    if (fechaHora.isNullOrEmpty()) return ""
    return try {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val date = dateFormat.parse(fechaHora)
        val horaFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        horaFormat.format(date ?: Date())
    } catch (e: Exception) {
        try {
            fechaHora.substring(11, 16)
        } catch (e2: Exception) {
            fechaHora
        }
    }
}

// Función para extraer la hora como objeto Date para ordenar
fun extraerHoraComoDate(fechaHora: String?): Date? {
    if (fechaHora.isNullOrEmpty()) return null

    return try {
        // Intentar extraer la hora (HH:mm) de la fecha completa
        val horaString = extraerHora(fechaHora)
        if (horaString.isEmpty()) return null

        // Crear un Date con la hora de hoy (la fecha no importa para ordenar por hora)
        val hoy = Calendar.getInstance()
        val partesHora = horaString.split(":")
        if (partesHora.size >= 2) {
            val horas = partesHora[0].toIntOrNull() ?: 0
            val minutos = partesHora[1].toIntOrNull() ?: 0

            hoy.set(Calendar.HOUR_OF_DAY, horas)
            hoy.set(Calendar.MINUTE, minutos)
            hoy.set(Calendar.SECOND, 0)
            hoy.set(Calendar.MILLISECOND, 0)

            return hoy.time
        }
        null
    } catch (e: Exception) {
        null
    }
}

// Función ALTERNATIVA más simple: Extraer minutos desde medianoche para ordenar
fun extraerMinutosDesdeMedianoche(fechaHora: String?): Int {
    if (fechaHora.isNullOrEmpty()) return 0

    try {
        val horaString = extraerHora(fechaHora)
        if (horaString.isEmpty()) return 0

        val partesHora = horaString.split(":")
        if (partesHora.size >= 2) {
            val horas = partesHora[0].toIntOrNull() ?: 0
            val minutos = partesHora[1].toIntOrNull() ?: 0

            // Calcular minutos totales desde medianoche
            return (horas * 60) + minutos
        }
    } catch (e: Exception) {
        // Ignorar error
    }
    return 0
}

// Función para ordenar asistencias por HORA descendente (más tarde primero)
fun ordenarAsistenciasPorHoraDescendente(asistencias: List<AsistenciaModel>): List<AsistenciaModel> {
    return asistencias.sortedByDescending { asistencia ->
        // Usar minutos desde medianoche para ordenar
        extraerMinutosDesdeMedianoche(asistencia.fechaHora)
    }
}

// Función para convertir tipo de asistencia numérico a texto
fun tipoAsistenciaTexto(tipo: Int?): String {
    return when (tipo) {
        1 -> "INGRESO 1"
        2 -> "SALIDA 1"
        3 -> "INGRESO 2"
        4 -> "SALIDA 2"
        else -> "DESCONOCIDO"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FormularioAsistenciaScreen(
    proyectoId: String,
    proyectoNombre: String,
    onBack: () -> Unit
) {
    // Estados principales - usando rememberSaveable para sobrevivir a la rotación
    var tipoAsistencia by rememberSaveable { mutableStateOf("") }
    var datosEmpleado by rememberSaveable { mutableStateOf<Map<String, String>?>(null) }
    var showQRScanner by rememberSaveable { mutableStateOf(false) }
    var isLoading by rememberSaveable { mutableStateOf(false) }
    var registroExitoso by rememberSaveable { mutableStateOf(false) }
    var errorMessage by rememberSaveable { mutableStateOf("") }
    var showErrorAlert by rememberSaveable { mutableStateOf(false) }
    var fechaHoraRegistro by rememberSaveable { mutableStateOf("") }

    // Estados para la tabla de asistencias
    var listaAsistencias by rememberSaveable { mutableStateOf<List<AsistenciaModel>>(emptyList()) }
    var isLoadingAsistencias by rememberSaveable { mutableStateOf(false) }

    // Lista ordenada por HORA descendente (más tarde primero)
    val listaAsistenciasOrdenada = remember(listaAsistencias) {
        ordenarAsistenciasPorHoraDescendente(listaAsistencias)
    }

    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val preferencesManager = remember { PreferencesManager(context) }
    val idSupervisor = remember { preferencesManager.getUserId()?.toString() ?: "" }

    // Función para cargar las asistencias
    fun cargarAsistencias() {
        scope.launch {
            isLoadingAsistencias = true
            try {
                val token = preferencesManager.getToken()
                if (token.isEmpty()) return@launch

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

                // Asignar la respuesta directamente, se ordenará automáticamente
                listaAsistencias = response

                // Debug: Mostrar el orden de las horas
                println("=== ASISTENCIAS CARGADAS Y ORDENADAS ===")
                listaAsistenciasOrdenada.forEachIndexed { index, asistencia ->
                    val hora = extraerHora(asistencia.fechaHora)
                    val minutos = extraerMinutosDesdeMedianoche(asistencia.fechaHora)
                    println("${index + 1}: Hora: $hora (minutos: $minutos) - ${asistencia.nombresUsuario}")
                }
                println("=== FIN ASISTENCIAS ===")

            } catch (e: Exception) {
                errorMessage = "Error al cargar asistencias: ${e.message}"
                showErrorAlert = true
            } finally {
                isLoadingAsistencias = false
            }
        }
    }

    // Cargar asistencias al inicio
    LaunchedEffect(proyectoId) {
        cargarAsistencias()
    }

    // Función para convertir fecha
    fun convertirFechaParaMySQL(fecha: String, hora: String): String {
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

    // Función para registrar asistencia
    fun registrarAsistencia() {
        if (datosEmpleado == null || tipoAsistencia.isEmpty() || idSupervisor.isEmpty()) {
            errorMessage = "Faltan datos necesarios para el registro"
            showErrorAlert = true
            return
        }

        scope.launch {
            isLoading = true

            try {
                // Preparar datos
                val idUsuario = datosEmpleado!!["id_usuario"] ?: ""
                val idArea = datosEmpleado!!["id_area"] ?: ""
                val idCargo = datosEmpleado!!["id_cargo"] ?: ""
                val idAsignacion = datosEmpleado!!["id_asignacion"] ?: ""

                // Obtener fecha y hora actual
                val sdfHora = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                val sdfFecha = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                val horaActual = sdfHora.format(Date())
                val fechaActual = sdfFecha.format(Date())
                fechaHoraRegistro = "$fechaActual $horaActual"

                val fechaHoraParaMySQL = convertirFechaParaMySQL(fechaActual, horaActual)
                val modoRegistro = "1"

                // Obtener token
                val token = preferencesManager.getToken()
                if (token.isEmpty()) {
                    errorMessage = "Sesión expirada"
                    showErrorAlert = true
                    isLoading = false
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
                    tipoAsistencia = tipoAsistencia,
                    fechaHora = fechaHoraParaMySQL,
                    idSupervisor = idSupervisor,
                    modoRegistro = modoRegistro
                )

                // Procesar respuesta
                if (response.isNotEmpty()) {
                    val respuesta = response[0]
                    val esExito = respuesta.success == true || respuesta.success?.toString() == "true"

                    if (esExito) {
                        registroExitoso = true
                        errorMessage = "Asistencia registrada exitosamente"
                        showErrorAlert = true

                        // Recargar la tabla de asistencias
                        cargarAsistencias()

                        // Limpiar datos
                        datosEmpleado = null
                        tipoAsistencia = ""
                    } else {
                        errorMessage = respuesta.message ?: "Error al registrar"
                        showErrorAlert = true
                    }
                } else {
                    errorMessage = "No se recibió respuesta del servidor"
                    showErrorAlert = true
                }
            } catch (e: Exception) {
                errorMessage = "Error: ${e.message ?: "Desconocido"}"
                showErrorAlert = true
            } finally {
                isLoading = false
            }
        }
    }

    // Procesar contenido QR
    fun procesarQRContent(qrContent: String) {
        if (qrContent.isEmpty()) {
            errorMessage = "Código QR vacío"
            showErrorAlert = true
            return
        }

        val datosDesglosados = qrContent.split("|")

        if (datosDesglosados.size < 12) {
            errorMessage = "Código QR inválido. Faltan datos."
            showErrorAlert = true
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
        datosEmpleado = datosMap

        // Validar proyecto
        val proyectoIdQR = datosMap["id_proyecto"] ?: ""
        val proyectoValido = proyectoIdQR == proyectoId

        if (!proyectoValido) {
            errorMessage = "El empleado no pertenece a este proyecto"
            showErrorAlert = true
            return
        }
    }

    // Si showQRScanner es true, muestra el escáner
    if (showQRScanner) {
        QRScannerScreen(
            onQRScanned = { qrContent ->
                showQRScanner = false
                registroExitoso = false

                procesarQRContent(qrContent)

                // Registrar automáticamente si ya se seleccionó tipo de asistencia
                if (tipoAsistencia.isNotEmpty()) {
                    registrarAsistencia()
                } else {
                    errorMessage = "Seleccione primero el tipo de asistencia"
                    showErrorAlert = true
                }
            },
            onClose = {
                showQRScanner = false
            }
        )
        return
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            "Registro de Asistencia",
                            style = MaterialTheme.typography.titleLarge
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Volver"
                            )
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        titleContentColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.padding(top = 24.dp)
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. Información del proyecto
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Proyecto",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = proyectoNombre,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                // 2. Selector de Tipo de Asistencia
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Seleccione Tipo de Asistencia *",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        // Primera fila: Ingreso 1 y Salida 1
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = tipoAsistencia == "1",
                                onClick = { tipoAsistencia = "1" },
                                label = { Text("INGRESO 1") },
                                modifier = Modifier.weight(1f),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                )
                            )
                            FilterChip(
                                selected = tipoAsistencia == "2",
                                onClick = { tipoAsistencia = "2" },
                                label = { Text("SALIDA 1") },
                                modifier = Modifier.weight(1f),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Segunda fila: Ingreso 2 y Salida 2
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = tipoAsistencia == "3",
                                onClick = { tipoAsistencia = "3" },
                                label = { Text("INGRESO 2") },
                                modifier = Modifier.weight(1f),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                )
                            )
                            FilterChip(
                                selected = tipoAsistencia == "4",
                                onClick = { tipoAsistencia = "4" },
                                label = { Text("SALIDA 2") },
                                modifier = Modifier.weight(1f),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                )
                            )
                        }

                        // Botón para escanear QR (solo si se seleccionó tipo)
                        if (tipoAsistencia.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { showQRScanner = true },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.secondary,
                                    contentColor = MaterialTheme.colorScheme.onSecondary
                                ),
                                enabled = !isLoading
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.QrCodeScanner,
                                        contentDescription = "Escanear QR"
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Escanear QR para Registrar")
                                }
                            }
                        }
                    }
                }

                // 3. Tabla de Asistencias del Día
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Asistencias del Día",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            IconButton(
                                onClick = { cargarAsistencias() },
                                enabled = !isLoadingAsistencias
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Actualizar"
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        if (isLoadingAsistencias) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(100.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator()
                            }
                        } else if (listaAsistenciasOrdenada.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(100.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.List,
                                        contentDescription = "Sin datos",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(40.dp)
                                    )
                                    Text(
                                        text = "No hay asistencias registradas hoy",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        } else {
                            // Cabecera de la tabla
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "#",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.weight(1f),
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = "Usuario",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.weight(3f),
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = "Tipo",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.weight(2f),
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = "Hora",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.weight(2f),
                                    textAlign = TextAlign.Center
                                )
                            }

                            // Lista de asistencias ORDENADA POR HORA
                            Column(
                                modifier = Modifier
                                    .heightIn(max = 300.dp)
                                    .verticalScroll(rememberScrollState())
                            ) {
                                listaAsistenciasOrdenada.forEachIndexed { index, asistencia ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 10.dp)
                                            .background(
                                                if (index % 2 == 0) MaterialTheme.colorScheme.surface
                                                else MaterialTheme.colorScheme.surfaceVariant
                                            ),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Columna 1: Número (ordenado por hora)
                                        Text(
                                            text = "${index + 1}",
                                            modifier = Modifier.weight(1f),
                                            textAlign = TextAlign.Center,
                                            fontWeight = FontWeight.Medium
                                        )

                                        // Columna 2: Usuario
                                        Text(
                                            text = "${asistencia.nombresUsuario ?: ""} ${asistencia.apellidosUsuario ?: ""}".trim(),
                                            modifier = Modifier.weight(3f),
                                            textAlign = TextAlign.Center,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )

                                        // Columna 3: Tipo de asistencia
                                        Text(
                                            text = tipoAsistenciaTexto(asistencia.tipoAsistencia),
                                            modifier = Modifier.weight(2f),
                                            textAlign = TextAlign.Center,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.primary
                                        )

                                        // Columna 4: Hora (ya formateada)
                                        Text(
                                            text = extraerHora(asistencia.fechaHora),
                                            modifier = Modifier.weight(2f),
                                            textAlign = TextAlign.Center,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }

                                    // Separador
                                    if (index < listaAsistenciasOrdenada.size - 1) {
                                        Divider(
                                            modifier = Modifier.padding(horizontal = 12.dp),
                                            thickness = 0.5.dp,
                                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                        )
                                    }
                                }
                            }

                            // Resumen
                            Text(
                                text = "Total: ${listaAsistenciasOrdenada.size} registro(s) - Ordenado por hora (más reciente primero)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.weight(1f))
            }
        }

        // Overlay de carga
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(48.dp),
                        strokeWidth = 4.dp
                    )
                    Text(
                        text = "Registrando asistencia...",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // Alertas
        if (showErrorAlert) {
            AlertDialog(
                onDismissRequest = { showErrorAlert = false },
                title = {
                    Text(
                        if (registroExitoso) "Éxito" else "Error",
                        color = if (registroExitoso) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.error
                    )
                },
                text = {
                    Text(errorMessage)
                },
                confirmButton = {
                    TextButton(
                        onClick = { showErrorAlert = false }
                    ) {
                        Text("Aceptar")
                    }
                }
            )
        }
    }
}