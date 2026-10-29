// Autor :  Sinelnikov Oleg 2026
// ModelT16 Detect - Видоискатель с системой захвата и слежения за объектом

package com.modelt16.detect.ui

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.RectF
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

@Composable
fun MainScreen(
    viewModel: MainViewModel
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val trackedBox by viewModel.trackedBox.collectAsState()

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

            // 2. Система захвата и слежения
            TargetTrackingOverlay(
                trackedBox = trackedBox,
                onLock = { frame -> viewModel.lock(frame) },
                onUnlock = { viewModel.unlock() }
            )
        }
    }
}

@Composable
private fun TargetTrackingOverlay(
    trackedBox: RectF?,
    onLock: (RectF) -> Unit,
    onUnlock: () -> Unit
) {
    val currentOnLock by rememberUpdatedState(onLock)
    val currentOnUnlock by rememberUpdatedState(onUnlock)
    val isTracking = trackedBox != null

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(isTracking) {
                detectTapGestures { tapOffset ->
                    val cx = size.width / 2f
                    val cy = size.height / 2f
                    // Уменьшенная в 2 раза рамка (28% от ширины)
                    val boxSize = minOf(size.width, size.height) * 0.28f
                    val half = boxSize / 2f
                    val centerBox = RectF(cx - half, cy - half, cx + half, cy + half)

                    if (!isTracking) {
                        // Нажатие на рамку или область вокруг нее производит захват
                        val distToCenter = kotlin.math.hypot(tapOffset.x - cx, tapOffset.y - cy)
                        if (centerBox.contains(tapOffset.x, tapOffset.y) || distToCenter < boxSize * 0.75f) {
                            currentOnLock(centerBox)
                        }
                    } else {
                        // Касание во время слежения сбрасывает захват
                        currentOnUnlock()
                    }
                }
            }
    ) {
        val cx = size.width / 2f
        val cy = size.height / 2f

        if (trackedBox == null) {
            // === РЕЖИМ 1: ДО ЗАХВАТА ===
            // Уменьшенная в 2 раза центральная рамка (28%)
            val boxSize = minOf(size.width, size.height) * 0.28f
            val half = boxSize / 2f
            val left = cx - half
            val top = cy - half
            val right = cx + half
            val bottom = cy + half
            val centerBox = RectF(left, top, right, bottom)

            val reticleColor = Color(0xFF00E5FF) // Бирюзовый

            // Тонкий контур
            drawRect(
                color = reticleColor.copy(alpha = 0.30f),
                topLeft = Offset(left, top),
                size = Size(boxSize, boxSize),
                style = Stroke(width = 1.5f)
            )

            // Жирные угловые скобки (толщина 6px, длина 22px)
            drawCornerBrackets(centerBox, reticleColor, strokeWidth = 6f, cornerLen = 22f)

            // Крестик по центру
            drawCrosshair(cx, cy, reticleColor, crossLen = 16f, centerGap = 4f, strokeWidth = 3f)
        } else {
            // === РЕЖИМ 2: СЛЕЖЕНИЕ (ОБЪЕКТ ЗАХВАЧЕН) ===
            val tx = trackedBox.centerX()
            val ty = trackedBox.centerY()
            val lockRed = Color(0xFFFF1744) // Жирный красный цвет

            // 1. Крестик в центре экрана (точка отсчета)
            drawCrosshair(cx, cy, Color(0x9900E5FF), crossLen = 14f, centerGap = 4f, strokeWidth = 2.5f)

            // 2. Линия от центра экрана к захваченному объекту
            drawLine(
                color = lockRed,
                start = Offset(cx, cy),
                end = Offset(tx, ty),
                strokeWidth = 4.5f,
                cap = StrokeCap.Round
            )

            // 3. Красная рамка вокруг захваченного объекта
            drawCornerBrackets(trackedBox, lockRed, strokeWidth = 6.5f, cornerLen = 22f)

            // 4. Красная точка ровно на объекте
            drawCircle(
                color = lockRed,
                radius = 6f,
                center = Offset(tx, ty)
            )
        }
    }
}

private fun DrawScope.drawCrosshair(
    cx: Float,
    cy: Float,
    color: Color,
    crossLen: Float,
    centerGap: Float,
    strokeWidth: Float
) {
    // Горизонталь
    drawLine(color, Offset(cx - crossLen, cy), Offset(cx - centerGap, cy), strokeWidth, StrokeCap.Round)
    drawLine(color, Offset(cx + centerGap, cy), Offset(cx + crossLen, cy), strokeWidth, StrokeCap.Round)

    // Вертикаль
    drawLine(color, Offset(cx, cy - crossLen), Offset(cx, cy - centerGap), strokeWidth, StrokeCap.Round)
    drawLine(color, Offset(cx, cy + centerGap), Offset(cx, cy + crossLen), strokeWidth, StrokeCap.Round)
}

private fun DrawScope.drawCornerBrackets(box: RectF, color: Color, strokeWidth: Float, cornerLen: Float) {
    val len = minOf(cornerLen, box.width() / 2.2f, box.height() / 2.2f)

    // Верхний левый угол
    drawLine(color, Offset(box.left, box.top), Offset(box.left + len, box.top), strokeWidth, StrokeCap.Square)
    drawLine(color, Offset(box.left, box.top), Offset(box.left, box.top + len), strokeWidth, StrokeCap.Square)

    // Верхний правый угол
    drawLine(color, Offset(box.right, box.top), Offset(box.right - len, box.top), strokeWidth, StrokeCap.Square)
    drawLine(color, Offset(box.right, box.top), Offset(box.right, box.top + len), strokeWidth, StrokeCap.Square)

    // Нижний левый угол
    drawLine(color, Offset(box.left, box.bottom), Offset(box.left + len, box.bottom), strokeWidth, StrokeCap.Square)
    drawLine(color, Offset(box.left, box.bottom), Offset(box.left, box.bottom - len), strokeWidth, StrokeCap.Square)

    // Нижний правый угол
    drawLine(color, Offset(box.right, box.bottom), Offset(box.right - len, box.bottom), strokeWidth, StrokeCap.Square)
    drawLine(color, Offset(box.right, box.bottom), Offset(box.right, box.bottom - len), strokeWidth, StrokeCap.Square)
}
