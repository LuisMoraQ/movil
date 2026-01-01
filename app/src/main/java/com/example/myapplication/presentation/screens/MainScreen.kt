package com.example.myapplication.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    userName: String,
    onLogout: () -> Unit
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .fillMaxHeight()
                    .width(280.dp)
            ) {
                DrawerContent(
                    userName = userName,
                    onLogout = onLogout,
                    navController = navController,
                    currentRoute = currentRoute,
                    onCloseDrawer = {
                        coroutineScope.launch {
                            drawerState.close()
                        }
                    }
                )
            }
        }
    ) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text("Mi Sistema") },
                    navigationIcon = {
                        IconButton(
                            onClick = {
                                coroutineScope.launch {
                                    drawerState.open()
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Menu,
                                contentDescription = "Menú"
                            )
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        titleContentColor = MaterialTheme.colorScheme.primary
                    )
                )
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                NavHost(
                    navController = navController,
                    startDestination = "proyectos"
                ) {
                    composable("proyectos") {
                        ProyectosScreen(
                            onProyectoClick = { proyecto ->
                                navController.navigate(
                                    "formulario_asistencia/${proyecto.id_proyecto}/${proyecto.descripcion ?: "Sin nombre"}"
                                )
                            }
                        )
                    }

                    composable("formulario_asistencia/{proyectoId}/{proyectoNombre}") { backStackEntry ->
                        val proyectoId = backStackEntry.arguments?.getString("proyectoId") ?: ""
                        val proyectoNombre = backStackEntry.arguments?.getString("proyectoNombre") ?: "Sin nombre"

                        FormularioAsistenciaScreen(
                            proyectoId = proyectoId,
                            proyectoNombre = proyectoNombre,
                            onBack = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DrawerContent(
    userName: String,
    onLogout: () -> Unit,
    navController: NavController,
    currentRoute: String?,
    onCloseDrawer: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxHeight()
            .padding(16.dp)
    ) {
        Text(
            text = "Bienvenido",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = userName,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "MÓDULOS",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp)
        )

        NavigationDrawerItem(
            label = { Text("Proyectos") },
            selected = currentRoute == "proyectos",
            onClick = {
                navController.navigate("proyectos") {
                    popUpTo("proyectos") { inclusive = true }
                }
                onCloseDrawer()
            },
            icon = { Icon(Icons.Filled.Home, contentDescription = "Proyectos") }
        )

        Spacer(modifier = Modifier.weight(1f))

        NavigationDrawerItem(
            label = { Text("Cerrar Sesión") },
            selected = false,
            onClick = {
                onLogout()
                onCloseDrawer()
            },
            icon = { Icon(Icons.Filled.ExitToApp, contentDescription = "Cerrar Sesión") }
        )
    }
}