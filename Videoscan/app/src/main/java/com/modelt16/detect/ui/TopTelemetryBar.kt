// Autor :  Sinelnikov Oleg 2026
// ModelT16 Detect - Верхняя панель тактической телеметрии

package com.modelt16.detect.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.modelt16.detect.model.ScanUiState
import com.modelt16.detect.ui.theme.HudCardBorder
import com.modelt16.detect.ui.theme.HudCyan
import com.modelt16.detect.ui.theme.HudGreen
import com.modelt16.detect.ui.theme.HudLockAmber
import com.modelt16.detect.ui.theme.HudLockRed
import com.modelt16.detect.ui.theme.HudPanelBg
import com.modelt16.detect.ui.theme.TextPrimary
import com.modelt16.detect.ui.theme.TextSecondary

@Composable
fun TopTelemetryBar(
    state: ScanUiState,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .background(HudPanelBg, RoundedCornerShape(16.dp))
            .border(1.dp, HudCardBorder, RoundedCornerShape(16.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "MODEL T16 DETECT",
                        fontWeight = FontWeight.Black,
                        fontSize = 15.sp,
                        color = HudCyan,
                        letterSpacing = 1.2.sp
                    )
                    Text(
                        text = state.authorTag,
                        fontSize = 10.sp,
                        color = TextSecondary,
                        fontWeight = FontWeight.Medium
                    )
                }

                val lockStatusText = if (state.hasLockedTarget) "🎯 TARGET LOCKED" else "👁️ СКАНИРОВАНИЕ"
                val lockStatusColor = if (state.hasLockedTarget) HudLockRed else HudGreen
                Box(
                    modifier = Modifier
                        .background(lockStatusColor.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                        .border(1.dp, lockStatusColor, RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = lockStatusText,
                        color = lockStatusColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                MetricChip(label = "FPS", value = "${state.fps}", valueColor = HudGreen)
                MetricChip(label = "ИНФЕРЕНС", value = "${state.latencyMs} ms", valueColor = HudCyan)
                MetricChip(label = "ЦЕЛЕЙ", value = "${state.targets.size}", valueColor = HudLockAmber)
                MetricChip(label = "РЕЖИМ", value = state.filter.title.take(8).uppercase(), valueColor = TextPrimary)
            }
        }
    }
}

@Composable
private fun MetricChip(
    label: String,
    value: String,
    valueColor: Color
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = "$label: ",
            color = TextSecondary,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = value,
            color = valueColor,
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
