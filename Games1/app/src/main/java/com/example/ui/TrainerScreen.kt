package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.model.GameStatus
import com.example.sensor.TiltSensorTracker
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.TextPrimary
import com.example.util.HapticManager

@Composable
fun TrainerScreen(
    viewModel: TrainerViewModel = viewModel()
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val density = LocalDensity.current

    val uiState by viewModel.uiState.collectAsState()
    var showSettingsDialog by remember { mutableStateOf(false) }

    val sensorTracker = remember { TiltSensorTracker(context) }
    val hapticManager = remember { HapticManager(context) }

    // Wire haptic callbacks
    DisposableEffect(Unit) {
        viewModel.onHapticBounce = { impulse -> hapticManager.bounce(impulse) }
        viewModel.onHapticWin = { hapticManager.success() }
        viewModel.onHapticFail = { hapticManager.fail() }
        onDispose {
            viewModel.onHapticBounce = null
            viewModel.onHapticWin = null
            viewModel.onHapticFail = null
        }
    }

    // Connect sensor listener to ViewModel
    DisposableEffect(sensorTracker) {
        sensorTracker.onSensorChangedListener = { pitchDeg, rollDeg, gravity ->
            viewModel.updateTilt(
                pitchDeg = pitchDeg,
                rollDeg = rollDeg,
                gravity = gravity,
                isCalibrated = sensorTracker.isCalibrated
            )
        }
        onDispose {
            sensorTracker.onSensorChangedListener = null
        }
    }

    // Lifecycle observer for sensors
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> sensorTracker.start()
                Lifecycle.Event.ON_PAUSE -> sensorTracker.stop()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            sensorTracker.stop()
        }
    }

    // High-precision physics loop locked to display refresh rate
    LaunchedEffect(Unit) {
        var lastFrameTimeNanos = 0L
        while (true) {
            withFrameNanos { frameTimeNanos ->
                if (lastFrameTimeNanos != 0L) {
                    val dt = (frameTimeNanos - lastFrameTimeNanos) / 1_000_000_000f
                    // Physics tick
                    viewModel.physicsStep(
                        dt = dt,
                        stabilityScore = sensorTracker.calculateStabilityScore()
                    )
                }
                lastFrameTimeNanos = frameTimeNanos
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SurfaceDark)
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val widthPx = constraints.maxWidth.toFloat()
            val heightPx = constraints.maxHeight.toFloat()

            LaunchedEffect(widthPx, heightPx) {
                if (widthPx > 0f && heightPx > 0f) {
                    viewModel.onBoardSizeChanged(widthPx, heightPx)
                }
            }

            // The main table board Canvas
            BoardCanvas(
                ball = uiState.ball,
                bounds = uiState.bounds,
                obstacles = uiState.obstacles,
                pocket = uiState.pocket,
                pitchDeg = uiState.pitchDeg,
                rollDeg = uiState.rollDeg
            )
        }

        // Единственная аккуратная шестеренка настроек в верхнем правом углу
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(16.dp)
        ) {
            IconButton(
                onClick = { showSettingsDialog = true },
                modifier = Modifier
                    .size(46.dp)
                    .background(
                        color = SurfaceCard.copy(alpha = 0.85f),
                        shape = CircleShape
                    )
                    .border(
                        width = 1.dp,
                        color = Color(0x33FFFFFF),
                        shape = CircleShape
                    )
                    .testTag("settings_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Настройки",
                    tint = TextPrimary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        // Victory Dialog
        if (uiState.status == GameStatus.WON) {
            VictoryDialog(
                stats = uiState.stats,
                onNextBoard = {
                    viewModel.generateNewBoard()
                },
                onRepeatSameBoard = {
                    viewModel.resetBall()
                }
            )
        }

        // Missed Out-of-Bounds Overlay
        if (uiState.status == GameStatus.MISSED) {
            MissedOverlay(
                onRetry = {
                    viewModel.resetBall()
                }
            )
        }

        // Trapped in Hole Overlay
        if (uiState.status == GameStatus.TRAPPED) {
            TrappedOverlay(
                onRetry = {
                    viewModel.resetBall()
                }
            )
        }

        // Settings Dialog
        if (showSettingsDialog) {
            SettingsDialog(
                currentDensity = uiState.density,
                currentSensitivity = uiState.sensitivity,
                isCalibrated = uiState.isCalibrated,
                onDensitySelected = { density ->
                    viewModel.setDensity(density)
                },
                onSensitivityChanged = { sensitivity ->
                    viewModel.setSensitivity(sensitivity)
                    sensorTracker.sensitivity = sensitivity
                },
                onNewBoardGenerate = {
                    viewModel.generateNewBoard()
                    showSettingsDialog = false
                },
                onResetCalibration = {
                    sensorTracker.calibrateZero()
                    viewModel.resetBall()
                    showSettingsDialog = false
                },
                onDismiss = {
                    showSettingsDialog = false
                }
            )
        }
    }
}
