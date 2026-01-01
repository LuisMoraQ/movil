package com.example.myapplication.screens

import android.Manifest
import android.content.Context
import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberPermissionState
import kotlinx.coroutines.launch
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.style.TextAlign
import com.google.accompanist.permissions.isGranted
import com.journeyapps.barcodescanner.BarcodeCallback
import com.journeyapps.barcodescanner.BarcodeResult
import com.journeyapps.barcodescanner.DecoratedBarcodeView
import com.journeyapps.barcodescanner.DefaultDecoderFactory
import com.google.zxing.BarcodeFormat
import java.util.*

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun QRScannerScreen(
    onQRScanned: (String) -> Unit,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    var isScanning by remember { mutableStateOf(true) }
    var lastScanned by remember { mutableStateOf("") }

    // Permiso de cámara
    val cameraPermissionState = rememberPermissionState(
        permission = Manifest.permission.CAMERA
    )

    // Solicitar permiso si no está concedido
    LaunchedEffect(Unit) {
        if (!cameraPermissionState.status.isGranted) {
            cameraPermissionState.launchPermissionRequest()
        }
    }

    // Mostrar mensaje si no hay permiso
    if (!cameraPermissionState.status.isGranted) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Permiso de cámara requerido",
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Necesitas conceder permiso para usar la cámara y escanear códigos QR",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = { cameraPermissionState.launchPermissionRequest() }) {
                Text("Conceder permiso")
            }
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedButton(onClick = onClose) {
                Text("Cancelar")
            }
        }
        return
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Escanear Código QR",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier.background(Color.Transparent)
                    ) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Volver",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Black.copy(alpha = 0.7f)
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color.Black)
        ) {
            // Vista del escáner QR con ZXing
            AndroidView(
                factory = { ctx ->
                    val barcodeView = DecoratedBarcodeView(ctx)

                    // Configurar formatos (solo QR)
                    val formats = listOf(BarcodeFormat.QR_CODE)
                    barcodeView.barcodeView.decoderFactory = DefaultDecoderFactory(formats)

                    // Callback cuando se escanea un código
                    barcodeView.decodeSingle(object : BarcodeCallback {
                        override fun barcodeResult(result: BarcodeResult?) {
                            result?.text?.let { scannedText ->
                                if (scannedText != lastScanned) {
                                    lastScanned = scannedText
                                    // Llamar al callback
                                    onQRScanned(scannedText)
                                }
                            }
                        }
                    })

                    // Contenedor FrameLayout
                    FrameLayout(ctx).apply {
                        addView(barcodeView)
                    }
                },
                modifier = Modifier.fillMaxSize(),
                update = { view ->
                    val barcodeView = view.getChildAt(0) as DecoratedBarcodeView
                    if (isScanning) {
                        barcodeView.resume()
                    } else {
                        barcodeView.pause()
                    }
                }
            )

            // ... (el resto del código con el marco de escaneo igual) ...
        }
    }
}