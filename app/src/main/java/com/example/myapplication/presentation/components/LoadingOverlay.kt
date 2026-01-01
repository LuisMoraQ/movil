package com.example.myapplication.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@Composable
fun LoadingOverlay(
    isLoading: Boolean,
    message: String = "Cargando...",
    backgroundColor: Color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
    showAsDialog: Boolean = false
) {
    if (!isLoading) return

    if (showAsDialog) {
        // Versión como diálogo modal (se puede cerrar con back button)
        Dialog(
            onDismissRequest = { /* No hacer nada, solo se cierra cuando isLoading = false */ },
            properties = DialogProperties(
                dismissOnBackPress = false,
                dismissOnClickOutside = false
            )
        ) {
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.wrapContentSize()
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(48.dp),
                        strokeWidth = 3.dp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    } else {
        // Versión como overlay que cubre toda la pantalla (recomendado para login)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(enabled = false, onClick = {}) // Bloquea clicks
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = backgroundColor
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(64.dp),
                        strokeWidth = 4.dp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = message,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        }
    }
}