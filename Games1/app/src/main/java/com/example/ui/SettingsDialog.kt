package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ObstacleDensity
import com.example.ui.theme.AccentPrimary
import com.example.ui.theme.AccentPrimarySoft
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.SurfaceCardElevated
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.util.Locale

@Composable
fun SettingsDialog(
    currentDensity: ObstacleDensity,
    currentSensitivity: Float,
    isCalibrated: Boolean,
    onDensitySelected: (ObstacleDensity) -> Unit,
    onSensitivityChanged: (Float) -> Unit,
    onNewBoardGenerate: () -> Unit,
    onResetCalibration: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(
                    containerColor = AccentPrimary,
                    contentColor = SurfaceDark
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("close_settings_button")
            ) {
                Text("Готово", fontWeight = FontWeight.Bold)
            }
        },
        title = {
            Text(
                text = "Параметры Тренажера",
                color = TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Section 1: Obstacle Density
                Text(
                    text = "ПЛОТНОСТЬ ПРЕПЯТСТВИЙ",
                    color = TextMuted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                ObstacleDensity.values().forEach { density ->
                    val isSelected = density == currentDensity
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) AccentPrimarySoft else SurfaceCardElevated)
                            .border(
                                width = 1.dp,
                                color = if (isSelected) AccentPrimary else Color(0x1FFFFFFF),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable { onDensitySelected(density) }
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = density.label,
                                color = if (isSelected) AccentPrimary else TextPrimary,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = density.description,
                                color = TextSecondary,
                                fontSize = 11.sp,
                                lineHeight = 14.sp
                            )
                        }
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = AccentPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Section 2: Tilt Sensitivity Slider
                Text(
                    text = "ЧУВСТВИТЕЛЬНОСТЬ НАКЛОНА: ${String.format(Locale.US, "%.1fx", currentSensitivity)}",
                    color = TextMuted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Slider(
                    value = currentSensitivity,
                    onValueChange = onSensitivityChanged,
                    valueRange = 0.6f..1.8f,
                    steps = 5,
                    colors = SliderDefaults.colors(
                        thumbColor = AccentPrimary,
                        activeTrackColor = AccentPrimary,
                        inactiveTrackColor = Color(0x33FFFFFF)
                    ),
                    modifier = Modifier.testTag("sensitivity_slider")
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Actions: New Random Layout
                OutlinedButton(
                    onClick = onNewBoardGenerate,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("new_board_generate_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Casino,
                        contentDescription = null,
                        tint = AccentPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Случайный стол заново",
                        color = TextPrimary,
                        fontSize = 13.sp
                    )
                }

                if (isCalibrated) {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = onResetCalibration,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("reset_calibration_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = null,
                            tint = TextMuted,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Сбросить калибровку нуля",
                            color = TextSecondary,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        },
        containerColor = SurfaceCard,
        shape = RoundedCornerShape(24.dp)
    )
}
