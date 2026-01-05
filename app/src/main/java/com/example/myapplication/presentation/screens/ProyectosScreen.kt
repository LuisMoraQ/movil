package com.example.myapplication.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myapplication.viewmodel.ProyectosViewModel
import androidx.compose.runtime.collectAsState
import java.text.SimpleDateFormat
import java.util.*

// Data class para agrupar proyectos
data class GrupoProyectos(
    val grupoId: Int,
    val nombre: String,
    val proyectos: List<com.example.myapplication.models.Proyecto>
)

// Función para procesar proyectos y agruparlos
fun procesarGruposYProyectos(proyectos: List<com.example.myapplication.models.Proyecto>): List<GrupoProyectos> {
    val gruposMap = mutableMapOf<Int, GrupoProyectos>()

    // Primero identificar los grupos
    proyectos.forEach { proyecto ->
        if (proyecto.nro?.isNotEmpty() == true && proyecto.id_proyecto.isNullOrEmpty()) {
            val grupoId = proyecto.id_grupo ?: -1
            val grupoNombre = proyecto.descripcion ?: "Sin nombre"

            if (grupoId != -1 && !gruposMap.containsKey(grupoId)) {
                gruposMap[grupoId] = GrupoProyectos(
                    grupoId = grupoId,
                    nombre = grupoNombre,
                    proyectos = mutableListOf()
                )
            }
        }
    }

    // Luego agregar los proyectos a sus grupos correspondientes
    proyectos.forEach { proyecto ->
        if (proyecto.id_proyecto?.isNotEmpty() == true) {
            val grupoId = proyecto.id_grupo ?: -1

            if (grupoId != -1) {
                val grupo = gruposMap[grupoId]
                if (grupo != null) {
                    val proyectosActualizados = grupo.proyectos.toMutableList().apply {
                        add(proyecto)
                    }
                    gruposMap[grupoId] = grupo.copy(proyectos = proyectosActualizados)
                }
            }
        }
    }

    return gruposMap.values.toList()
}

// Función para formatear fecha
fun formatearFecha(fechaString: String?): String {
    if (fechaString.isNullOrEmpty()) return "Sin fecha"

    return try {
        val formatoEntrada = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val formatoSalida = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
        val fecha = formatoEntrada.parse(fechaString)
        if (fecha != null) {
            formatoSalida.format(fecha)
        } else {
            fechaString.substringBefore(" ")
        }
    } catch (e: Exception) {
        fechaString.substringBefore(" ")
    }
}

// Pantalla principal de proyectos
@Composable
fun ProyectosScreen(
    onProyectoClick: (com.example.myapplication.models.Proyecto) -> Unit
) {
    val viewModel: ProyectosViewModel = viewModel(
        factory = com.example.myapplication.viewmodel.LoginViewModelFactory(LocalContext.current)
    )

    val proyectos by viewModel.proyectos.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    val expandedGroups = remember { mutableStateMapOf<Int, Boolean>() }

    LaunchedEffect(Unit) {
        viewModel.cargarProyectos()
    }

    val grupos = remember(proyectos) {
        procesarGruposYProyectos(proyectos)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f),
                        MaterialTheme.colorScheme.surface
                    )
                )
            )
    ) {
        // Header con gradiente
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.secondary
                        ),
                        start = androidx.compose.ui.geometry.Offset(0f, 0f),
                        end = androidx.compose.ui.geometry.Offset(1000f, 0f)
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp, vertical = 32.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Proyectos",
                            style = MaterialTheme.typography.displaySmall.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${grupos.size} grupos • ${proyectos.count { it.id_proyecto?.isNotEmpty() == true }} proyectos",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f)
                        )
                    }

                    // Botones de acción
                    Surface(
                        modifier = Modifier.clip(RoundedCornerShape(16.dp)),
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f),
                        tonalElevation = 8.dp
                    ) {
                        Row {
                            IconButton(
                                onClick = {
                                    grupos.forEach { grupo ->
                                        expandedGroups[grupo.grupoId] = true
                                    }
                                },
                                colors = IconButtonDefaults.iconButtonColors(
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.UnfoldMore,
                                    contentDescription = "Expandir todos"
                                )
                            }

                            IconButton(
                                onClick = {
                                    expandedGroups.clear()
                                },
                                colors = IconButtonDefaults.iconButtonColors(
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.UnfoldLess,
                                    contentDescription = "Contraer todos"
                                )
                            }

                            IconButton(
                                onClick = { viewModel.refrescar() },
                                enabled = !isLoading,
                                colors = IconButtonDefaults.iconButtonColors(
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Refrescar"
                                )
                            }
                        }
                    }
                }

                // Buscador (versión simplificada)
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Buscar",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            "Buscar proyectos...",
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }

        // Contenido principal
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .offset(y = (-24).dp)
                .shadow(elevation = 16.dp, shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 4.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 40.dp)
            ) {
                if (isLoading) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(48.dp),
                                strokeWidth = 3.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "Cargando proyectos...",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    error?.let { errorMessage ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.errorContainer,
                            tonalElevation = 4.dp
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.error.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Error,
                                        contentDescription = "Error",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = errorMessage,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }

                    if (grupos.isEmpty() && error == null) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(120.dp)
                                        .clip(RoundedCornerShape(24.dp))
                                        .background(
                                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.FolderOpen,
                                        contentDescription = "Sin proyectos",
                                        modifier = Modifier.size(64.dp),
                                        tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)
                                    )
                                }
                                Text(
                                    text = "No hay proyectos disponibles",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.outline
                                )
                                Text(
                                    text = "Crea tu primer proyecto para comenzar",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp, top = 8.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(grupos) { grupo ->
                                GrupoProyectosItem(
                                    grupoId = grupo.grupoId,
                                    grupoNombre = grupo.nombre,
                                    proyectos = grupo.proyectos,
                                    isExpanded = expandedGroups.getOrDefault(grupo.grupoId, false),
                                    onToggleExpand = { isExpanded ->
                                        expandedGroups[grupo.grupoId] = isExpanded
                                    },
                                    onProyectoClick = onProyectoClick
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// Componente para cada grupo de proyectos
@Composable
fun GrupoProyectosItem(
    grupoId: Int,
    grupoNombre: String,
    proyectos: List<com.example.myapplication.models.Proyecto>,
    isExpanded: Boolean,
    onToggleExpand: (Boolean) -> Unit,
    onProyectoClick: (com.example.myapplication.models.Proyecto) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = if (isExpanded) 8.dp else 4.dp,
                shape = RoundedCornerShape(20.dp),
                spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
            ),
        shape = RoundedCornerShape(20.dp),
        tonalElevation = if (isExpanded) 2.dp else 1.dp,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            width = 1.dp,
            color = if (isExpanded) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
            else MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier.animateContentSize(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            )
        ) {
            // Header del grupo
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onToggleExpand(!isExpanded) }
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isExpanded) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                else MaterialTheme.colorScheme.surfaceVariant
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp
                            else Icons.Default.KeyboardArrowDown,
                            contentDescription = if (isExpanded) "Contraer grupo" else "Expandir grupo",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = grupoNombre,
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        if (proyectos.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            val activos = proyectos.count { it.estado == "0" }
                            Text(
                                text = if (activos > 0) "$activos activos" else "Todos completados",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    tonalElevation = 2.dp
                ) {
                    Text(
                        text = "${proyectos.size}",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }

            // Contenido expandido
            AnimatedVisibility(
                visible = isExpanded && proyectos.isNotEmpty(),
                enter = fadeIn(animationSpec = tween(300)),
                exit = fadeOut(animationSpec = tween(200))
            ) {
                Column(
                    modifier = Modifier.padding(
                        start = 20.dp,
                        end = 20.dp,
                        top = 0.dp,
                        bottom = 20.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    proyectos.forEach { proyecto ->
                        ProyectoCardItem(
                            proyecto = proyecto,
                            onClick = { onProyectoClick(proyecto) }
                        )
                    }
                }
            }

            if (isExpanded && proyectos.isEmpty()) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 20.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Información",
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "No hay proyectos en este grupo",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }
        }
    }
}

// Tarjeta individual de proyecto
@Composable
fun ProyectoCardItem(
    proyecto: com.example.myapplication.models.Proyecto,
    onClick: () -> Unit
) {
    val estadoColor = when (proyecto.estado) {
        "0" -> MaterialTheme.colorScheme.primary // Activo
        "1" -> MaterialTheme.colorScheme.tertiary // Completado
        "2" -> Color(0xFF4CAF50) // Aprobado - Verde
        else -> MaterialTheme.colorScheme.outline
    }

    val estadoIcon = when (proyecto.estado) {
        "0" -> Icons.Default.PlayArrow // Activo
        "1" -> Icons.Default.CheckCircle // Completado
        "2" -> Icons.Default.Verified // Aprobado
        else -> Icons.Default.Help
    }

    val estadoTexto = when (proyecto.estado) {
        "0" -> "En proceso"
        "1" -> "Completado"
        "2" -> "Aprobado"
        else -> "Estado desconocido"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                onClick = onClick,
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            // Fila principal con descripción y estado
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Estado con icono
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = estadoColor.copy(alpha = 0.1f),
                        border = BorderStroke(1.dp, estadoColor.copy(alpha = 0.2f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = estadoIcon,
                                contentDescription = "Estado",
                                modifier = Modifier.size(16.dp),
                                tint = estadoColor
                            )
                            Text(
                                text = estadoTexto,
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = estadoColor
                            )
                        }
                    }

                    // Descripción del proyecto
                    Text(
                        text = proyecto.descripcion ?: "Sin descripción",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold
                        ),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Icono de flecha
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = "Ver detalles",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Fecha de registro
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = "Fecha",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Column {
                        Text(
                            text = "Fecha de registro",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = formatearFecha(proyecto.fecha_registro),
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // Símbolo si existe
                proyecto.simbolo?.let { simbolo ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f))
                    ) {
                        Text(
                            text = simbolo,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }
    }
}