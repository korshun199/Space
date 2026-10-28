package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.GameStatus
import com.example.model.TrainerState
import com.example.ui.theme.AccentPrimary
import com.example.ui.theme.AccentPrimarySoft
import com.example.ui.theme.DangerRed
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.util.Locale

@Composable
fun TopTelemetryHud(
    state: TrainerState,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        color = SurfaceCard.copy(alpha = 0.88f),
        shape = RoundedCornerShape(20.dp),
        shadowElevation = 8.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x22FFFFFF))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // 1. Tilt Angles (Roll & Pitch)
            Column {
                Text(
                    text = "НАКЛОН КИСТИ",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextMuted,
                    letterSpacing = 1.sp
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = String.format(Locale.US, "↔%+.1f°", state.rollDeg),
                        fontSize = 14.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.SemiBold,
                        color = if (kotlin.math.abs(state.rollDeg) > 15f) AccentPrimary else TextPrimary
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = String.format(Locale.US, "↕%+.1f°", state.pitchDeg),
                        fontSize = 14.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.SemiBold,
                        color = if (kotlin.math.abs(state.pitchDeg) > 15f) AccentPrimary else TextPrimary
                    )
                }
            }

            // 2. Stopwatch Timer
            val seconds = state.stats.sessionDurationMs / 1000f
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "ВРЕМЯ",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextMuted,
                    letterSpacing = 1.sp
                )
                Text(
                    text = String.format(Locale.US, "%04.1f c", seconds),
                    fontSize = 16.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = AccentPrimary
                )
            }

            // 3. Stability / Tremor score
            val stability = state.stats.stabilityScore
            val stabilityColor = when {
                stability >= 80 -> SuccessGreen
                stability >= 55 -> Color(0xFFFBBF24)
                else -> DangerRed
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "СТАБИЛЬНОСТЬ",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextMuted,
                    letterSpacing = 1.sp
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(stabilityColor)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "$stability%",
                        fontSize = 14.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }
            }
        }
    }
}

@Composable
fun BottomControlsHud(
    isCalibrated: Boolean,
    densityName: String,
    onCalibrateClick: () -> Unit,
    onRestartClick: () -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        color = SurfaceCard.copy(alpha = 0.92f),
        shape = RoundedCornerShape(24.dp),
        shadowElevation = 10.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x22FFFFFF))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Calibration Action Button
            Button(
                onClick = onCalibrateClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isCalibrated) AccentPrimarySoft else AccentPrimary,
                    contentColor = if (isCalibrated) AccentPrimary else SurfaceDark
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.testTag("calibrate_zero_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Tune,
                    contentDescription = "Калибровать 0°",
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isCalibrated) "Ноль 0° (OK)" else "Калибровать 0°",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }

            // Density and Level Information
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onSettingsClick() }
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            ) {
                Text(
                    text = densityName,
                    fontSize = 12.sp,
                    color = TextSecondary,
                    fontWeight = FontWeight.Medium
                )
            }

            // Quick actions: Settings & Reset
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onSettingsClick,
                    modifier = Modifier.testTag("settings_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Настройки стола",
                        tint = TextSecondary
                    )
                }
                IconButton(
                    onClick = onRestartClick,
                    modifier = Modifier.testTag("reset_ball_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Сбросить шарик",
                        tint = AccentPrimary
                    )
                }
            }
        }
    }
}
