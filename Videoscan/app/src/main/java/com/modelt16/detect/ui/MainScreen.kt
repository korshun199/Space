// Autor :  Sinelnikov Oleg 2026
// ModelT16 Detect - Видоискатель с жирной рамкой и крестиком по центру для будущего захвата

package com.modelt16.detect.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
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
            // 1. Полноэкранный видеопоток с камеры
            CameraPreviewView(
                analyzer = viewModel.analyzer,
                isFrontCamera = uiState.isFrontCamera,
                isTorchOn = uiState.isTorchOn,
                onCameraBound = { viewModel.onCameraBound(it) },
                onViewSizeChanged = { w, h -> viewModel.onViewSizeChanged(w, h) },
                modifier = Modifier.fillMaxSize()
            )

            // 2. Только жирненькая рамка и крестик по центру для будущего захвата
            Canvas(modifier = Modifier.fillMaxSize()) {
                val cx = size.width / 2f
                val cy = size.height / 2f

                // Размер рамки — 56% от ширины экрана
                val boxSize = minOf(size.width, size.height) * 0.56f
                val half = boxSize / 2f
                val left = cx - half
                val top = cy - half
                val right = cx + half
                val bottom = cy + half

                val reticleColor = Color(0xFF00E5FF) // Яркий неоновый бирюзовый

                // Контур рамки
                drawRect(
                    color = reticleColor.copy(alpha = 0.35f),
                    topLeft = Offset(left, top),
                    size = Size(boxSize, boxSize),
                    style = Stroke(width = 2f)
                )

                // Жирные тактические скобки по углам (толщина 8px, длина 46px)
                val cornerLen = 46f
                val cornerStroke = 8f

                // Верхний левый угол
                drawLine(reticleColor, Offset(left, top), Offset(left + cornerLen, top), cornerStroke, StrokeCap.Square)
                drawLine(reticleColor, Offset(left, top), Offset(left, top + cornerLen), cornerStroke, StrokeCap.Square)

                // Верхний правый угол
                drawLine(reticleColor, Offset(right, top), Offset(right - cornerLen, top), cornerStroke, StrokeCap.Square)
                drawLine(reticleColor, Offset(right, top), Offset(right, top + cornerLen), cornerStroke, StrokeCap.Square)

                // Нижний левый угол
                drawLine(reticleColor, Offset(left, bottom), Offset(left + cornerLen, bottom), cornerStroke, StrokeCap.Square)
                drawLine(reticleColor, Offset(left, bottom), Offset(left, bottom - cornerLen), cornerStroke, StrokeCap.Square)

                // Нижний правый угол
                drawLine(reticleColor, Offset(right, bottom), Offset(right - cornerLen, bottom), cornerStroke, StrokeCap.Square)
                drawLine(reticleColor, Offset(right, bottom), Offset(right, bottom - cornerLen), cornerStroke, StrokeCap.Square)

                // Центральный крестик (+)
                val crossLen = 26f
                val centerGap = 6f
                val crossStroke = 4f

                // Горизонтальная линия крестика
                drawLine(reticleColor, Offset(cx - crossLen, cy), Offset(cx - centerGap, cy), crossStroke, StrokeCap.Round)
                drawLine(reticleColor, Offset(cx + centerGap, cy), Offset(cx + crossLen, cy), crossStroke, StrokeCap.Round)

                // Вертикальная линия крестика
                drawLine(reticleColor, Offset(cx, cy - crossLen), Offset(cx, cy - centerGap), crossStroke, StrokeCap.Round)
                drawLine(reticleColor, Offset(cx, cy + centerGap), Offset(cx, cy + crossLen), crossStroke, StrokeCap.Round)

                // Маленький прицельный кружок в центре
                drawCircle(
                    color = reticleColor.copy(alpha = 0.6f),
                    radius = 12f,
                    center = Offset(cx, cy),
                    style = Stroke(width = 2f)
                )
            }
        }
    }
}
