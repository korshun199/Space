// Autor :  Sinelnikov Oleg 2026
// ModelT16 Detect - Чистый экран видоискателя камеры (без оверлеев и кнопок)

package com.modelt16.detect.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

@Composable
fun MainScreen(
    viewModel: MainViewModel
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        viewModel.onCameraPermissionResult(isGranted)
    }

    LaunchedEffect(Unit) {
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
        viewModel.onCameraPermissionResult(granted)
        if (!granted) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (uiState.hasCameraPermission) {
            // Абсолютно чистый экран: только видео с камеры на весь дисплей
            CameraPreviewView(
                analyzer = viewModel.analyzer,
                isFrontCamera = uiState.isFrontCamera,
                isTorchOn = uiState.isTorchOn,
                onCameraBound = { viewModel.onCameraBound(it) },
                onViewSizeChanged = { w, h -> viewModel.onViewSizeChanged(w, h) },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
