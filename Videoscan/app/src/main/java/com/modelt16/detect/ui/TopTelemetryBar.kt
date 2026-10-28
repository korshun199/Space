// Autor :  Sinelnikov Oleg 2026
// ModelT16 Detect - Верхняя полоска с названием программы

package com.modelt16.detect.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.modelt16.detect.model.ScanUiState
import com.modelt16.detect.ui.theme.HudCardBorder
import com.modelt16.detect.ui.theme.HudCyan
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
            .padding(horizontal = 14.dp, vertical = 6.dp)
            .background(HudPanelBg.copy(alpha = 0.85f), RoundedCornerShape(14.dp))
            .border(1.dp, HudCardBorder, RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
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

            if (state.hasLockedTarget) {
                Box(
                    modifier = Modifier
                        .background(HudLockRed.copy(alpha = 0.25f), RoundedCornerShape(8.dp))
                        .border(1.5.dp, HudLockRed, RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "🎯 ЗАХВАТ #${state.lockedTargetId}",
                        color = HudLockRed,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}
