// Autor :  Sinelnikov Oleg 2026
// ModelT16 Detect - Диалог настроек чувствительности и информации

package com.modelt16.detect.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.modelt16.detect.ui.theme.HudCardBorder
import com.modelt16.detect.ui.theme.HudCyan
import com.modelt16.detect.ui.theme.HudLockAmber
import com.modelt16.detect.ui.theme.HudPanelBg
import com.modelt16.detect.ui.theme.TextPrimary
import com.modelt16.detect.ui.theme.TextSecondary

@Composable
fun SettingsDialog(
    minConfidence: Float,
    onConfidenceChange: (Float) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = HudPanelBg),
            border = androidx.compose.foundation.BorderStroke(1.5.dp, HudCardBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "ПАРАМЕТРЫ ВИДОИСКАТЕЛЯ",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = HudCyan,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "Autor :  Sinelnikov Oleg 2026",
                    fontSize = 11.sp,
                    color = TextSecondary
                )

                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Порог уверенности:",
                        fontSize = 13.sp,
                        color = TextPrimary
                    )
                    Text(
                        text = "${(minConfidence * 100).toInt()}%",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = HudLockAmber
                    )
                }

                Slider(
                    value = minConfidence,
                    onValueChange = onConfidenceChange,
                    valueRange = 0.20f..0.85f,
                    steps = 12,
                    colors = SliderDefaults.colors(
                        thumbColor = HudCyan,
                        activeTrackColor = HudCyan,
                        inactiveTrackColor = Color(0x33374151)
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0x331E293B), RoundedCornerShape(10.dp))
                        .padding(12.dp)
                ) {
                    Text(
                        text = "🎯 Как работает захват цели:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = HudCyan
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Коснитесь пальцем любого объекта в кадре для фиксации прицела. Приложение будет непрерывно удерживать захват цели и передавать телеметрию отклонения (ΔX, ΔY).",
                        fontSize = 11.sp,
                        color = TextSecondary,
                        lineHeight = 16.sp
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = HudCyan),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "ЗАКРЫТЬ",
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                }
            }
        }
    }
}
