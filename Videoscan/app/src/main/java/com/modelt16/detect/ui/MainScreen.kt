// Autor :  Sinelnikov Oleg 2026
// ModelT16 Detect - Главный экран видоискателя

package com.modelt16.detect.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.modelt16.detect.ui.theme.HudCardBorder
import com.modelt16.detect.ui.theme.HudCyan
import com.modelt16.detect.ui.theme.HudDarkBg
import com.modelt16.detect.ui.theme.HudPanelBg
import com.modelt16.detect.ui.theme.TextPrimary
import com.modelt16.detect.ui.theme.TextSecondary

@Composable
fun MainScreen(
    viewModel: MainViewModel
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    var showSettings by remember { mutableStateOf(false) }

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
            CameraPreviewView(
                analyzer = viewModel.analyzer,
                isFrontCamera = uiState.isFrontCamera,
                isTorchOn = uiState.isTorchOn,
                onCameraBound = { viewModel.onCameraBound(it) },
                onViewSizeChanged = { w, h -> viewModel.onViewSizeChanged(w, h) }
            )

            TacticalHudOverlay(
                targets = uiState.targets,
                lockedTarget = uiState.lockedTarget,
                onTapAt = { offset -> viewModel.onTapAt(offset) }
            )

            TopTelemetryBar(
                state = uiState,
                modifier = Modifier.align(Alignment.TopCenter)
            )

            BottomControlsBar(
                activeFilter = uiState.filter,
                hasLockedTarget = uiState.hasLockedTarget,
                isTorchOn = uiState.isTorchOn,
                onFilterChange = { viewModel.setFilter(it) },
                onUnlockTarget = { viewModel.unlockTarget() },
                onToggleTorch = { viewModel.toggleTorch() },
                onSwitchCamera = { viewModel.switchCamera() },
                onOpenSettings = { showSettings = true },
                modifier = Modifier.align(Alignment.BottomCenter)
            )

            if (showSettings) {
                SettingsDialog(
                    state = uiState,
                    onFilterChange = { viewModel.setFilter(it) },
                    onConfidenceChange = { viewModel.setMinConfidence(it) },
                    onToggleTorch = { viewModel.toggleTorch() },
                    onSwitchCamera = { viewModel.switchCamera() },
                    onDismiss = { showSettings = false }
                )
            }
        } else {
            PermissionRationaleScreen(
                onRequestPermission = {
                    permissionLauncher.launch(Manifest.permission.CAMERA)
                }
            )
        }
    }
}

@Composable
private fun PermissionRationaleScreen(
    onRequestPermission: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(HudDarkBg)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .background(HudPanelBg, RoundedCornerShape(20.dp))
                .border(1.5.dp, HudCardBorder, RoundedCornerShape(20.dp))
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.CameraAlt,
                contentDescription = null,
                tint = HudCyan,
                modifier = Modifier.size(56.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "MODEL T16 DETECT",
                fontWeight = FontWeight.Black,
                fontSize = 18.sp,
                color = HudCyan,
                letterSpacing = 1.sp
            )
            Text(
                text = "Autor :  Sinelnikov Oleg 2026",
                fontSize = 11.sp,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Для работы видоискателя, локального распознавания людей и захвата целей требуется доступ к камере.",
                fontSize = 13.sp,
                color = TextPrimary,
                textAlign = TextAlign.Center,
                lineHeight = 18.sp
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = onRequestPermission,
                colors = ButtonDefaults.buttonColors(containerColor = HudCyan),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "ПРЕДОСТАВИТЬ ДОСТУП",
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            }
        }
    }
}
