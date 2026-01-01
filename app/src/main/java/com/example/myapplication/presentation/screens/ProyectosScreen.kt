package com.example.myapplication.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myapplication.viewmodel.ProyectosViewModel
import androidx.compose.runtime.collectAsState

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
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Proyectos",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold
                )
            )

            Row {
                IconButton(
                    onClick = {
                        grupos.forEach { grupo ->
                            expandedGroups[grupo.grupoId] = true
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.ExpandMore,
                        contentDescription = "Expandir todos"
                    )
                }

                IconButton(
                    onClick = {
                        expandedGroups.clear()
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.ExpandLess,
                        contentDescription = "Contraer todos"
                    )
                }

                IconButton(
                    onClick = { viewModel.refrescar() },
                    enabled = !isLoading
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refrescar"
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Cargando proyectos...")
                }
            }
            return
        }

        error?.let { errorMessage ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Error,
                        contentDescription = "Error",
                        tint = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = errorMessage,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        if (grupos.isEmpty() && error == null) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.FolderOpen,
                        contentDescription = "Sin proyectos",
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.outline
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No hay proyectos disponibles",
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
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

// Componente para cada grupo de proyectos
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GrupoProyectosItem(
    grupoId: Int,
    grupoNombre: String,
    proyectos: List<com.example.myapplication.models.Proyecto>,
    isExpanded: Boolean,
    onToggleExpand: (Boolean) -> Unit,
    onProyectoClick: (com.example.myapplication.models.Proyecto) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.animateContentSize(
                animationSpec = tween(durationMillis = 300)
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onToggleExpand(!isExpanded) }
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (isExpanded) "Contraer grupo" else "Expandir grupo",
                        tint = MaterialTheme.colorScheme.primary
                    )

                    Text(
                        text = grupoNombre,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }

                Badge(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                ) {
                    Text(
                        text = "${proyectos.size}",
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }

            AnimatedVisibility(
                visible = isExpanded && proyectos.isNotEmpty()
            ) {
                Column(
                    modifier = Modifier.padding(start = 40.dp, end = 16.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
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
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 40.dp, vertical = 16.dp)
                ) {
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

// Tarjeta individual de proyecto
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProyectoCardItem(
    proyecto: com.example.myapplication.models.Proyecto,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                onClick = onClick,
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
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
                    text = proyecto.descripcion ?: "Sin descripción",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    modifier = Modifier.weight(1f)
                )

                Icon(
                    imageVector = Icons.Default.ArrowForward,
                    contentDescription = "Ver detalles",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = when (proyecto.estado) {
                            "0" -> Icons.Default.HourglassEmpty
                            "1" -> Icons.Default.CheckCircle
                            "2" -> Icons.Default.Cancel
                            else -> Icons.Default.Help
                        },
                        contentDescription = "Estado",
                        modifier = Modifier.size(16.dp),
                        tint = when (proyecto.estado) {
                            "0" -> MaterialTheme.colorScheme.primary
                            "1" -> MaterialTheme.colorScheme.tertiary
                            "2" -> MaterialTheme.colorScheme.error
                            else -> MaterialTheme.colorScheme.outline
                        }
                    )
                    Text(
                        text = when (proyecto.estado) {
                            "0" -> "En proceso"
                            "1" -> "Completado"
                            "2" -> "Cancelado"
                            else -> "Estado desconocido"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = when (proyecto.estado) {
                            "0" -> MaterialTheme.colorScheme.primary
                            "1" -> MaterialTheme.colorScheme.tertiary
                            "2" -> MaterialTheme.colorScheme.error
                            else -> MaterialTheme.colorScheme.outline
                        }
                    )
                }

                Text(
                    text = proyecto.simbolo ?: "",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = proyecto.desc_clasi ?: "Sin clasificación",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = proyecto.fecha_registro?.substringBefore(" ") ?: "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}