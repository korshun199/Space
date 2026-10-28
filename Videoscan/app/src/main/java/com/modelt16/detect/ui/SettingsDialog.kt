// Autor :  Sinelnikov Oleg 2026
// ModelT16 Detect - Отдельное окно всех настроек видоискателя

package com.modelt16.detect.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.modelt16.detect.model.ScanFilter
import com.modelt16.detect.model.ScanUiState
import com.modelt16.detect.ui.theme.HudCardBorder
import com.modelt16.detect.ui.theme.HudCyan
import com.modelt16.detect.ui.theme.HudGreen
import com.modelt16.detect.ui.theme.HudLockAmber
import com.modelt16.detect.ui.theme.HudPanelBg
import com.modelt16.detect.ui.theme.TextPrimary
import com.modelt16.detect.ui.theme.TextSecondary

@Composable
fun SettingsDialog(
    state: ScanUiState,
    onFilterChange: (ScanFilter) -> Unit,
    onConfidenceChange: (Float) -> Unit,
    onToggleTorch: () -> Unit,
    onSwitchCamera: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = HudPanelBg),
            border = androidx.compose.foundation.BorderStroke(1.5.dp, HudCardBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "ПАРАМЕТРЫ СКАНИРОВАНИЯ",
                    fontWeight = FontWeight.Black,
                    fontSize = 16.sp,
                    color = HudCyan,
                    letterSpacing = 1.sp
                )
                Text(
                    text = state.authorTag,
                    fontSize = 11.sp,
                    color = TextSecondary
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Раздел 1: Фильтр объектов
                SectionTitle("РЕЖИМ ОБНАРУЖЕНИЯ")
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterTabItem(
                        title = "Все",
                        isSelected = state.filter == ScanFilter.ALL,
                        onClick = { onFilterChange(ScanFilter.ALL) },
                        modifier = Modifier.weight(1f)
                    )
                    FilterTabItem(
                        title = "Люди",
                        isSelected = state.filter == ScanFilter.PEOPLE,
                        onClick = { onFilterChange(ScanFilter.PEOPLE) },
                        modifier = Modifier.weight(1f)
                    )
                    FilterTabItem(
                        title = "Предметы",
                        isSelected = state.filter == ScanFilter.ITEMS,
                        onClick = { onFilterChange(ScanFilter.ITEMS) },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Раздел 2: Порог чувствительности
                SectionTitle("ЧУВСТВИТЕЛЬНОСТЬ НЕЙРОСЕТИ")
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Порог уверенности:",
                        fontSize = 12.sp,
                        color = TextPrimary
                    )
                    Text(
                        text = "${(state.minConfidence * 100).toInt()}%",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = HudLockAmber
                    )
                }
                Slider(
                    value = state.minConfidence,
                    onValueChange = onConfidenceChange,
                    valueRange = 0.20f..0.85f,
                    steps = 12,
                    colors = SliderDefaults.colors(
                        thumbColor = HudCyan,
                        activeTrackColor = HudCyan,
                        inactiveTrackColor = Color(0x33374151)
                    )
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Раздел 3: Управление камерой и подсветкой
                SectionTitle("УПРАВЛЕНИЕ КАМЕРОЙ")
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ControlButton(
                        title = if (state.isTorchOn) "Фонарик: ВКЛ" else "Фонарик: ВЫКЛ",
                        icon = if (state.isTorchOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                        isActive = state.isTorchOn,
                        onClick = onToggleTorch,
                        modifier = Modifier.weight(1f)
                    )
                    ControlButton(
                        title = if (state.isFrontCamera) "Селфи камера" else "Основная камера",
                        icon = Icons.Default.Cameraswitch,
                        isActive = false,
                        onClick = onSwitchCamera,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Раздел 4: Телеметрия
                SectionTitle("ТЕКУЩАЯ ТЕЛЕМЕТРИЯ")
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    MetricBadge(label = "FPS", value = "${state.fps}", color = HudGreen)
                    MetricBadge(label = "ИНФЕРЕНС", value = "${state.latencyMs} ms", color = HudCyan)
                    MetricBadge(label = "ОБЪЕКТОВ", value = "${state.targets.size}", color = HudLockAmber)
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Кнопка закрытия
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = HudCyan),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                ) {
                    Text(
                        text = "ЗАКРЫТЬ",
                        fontWeight = FontWeight.Bold,
                        color = Color.Black,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        color = HudCyan,
        letterSpacing = 0.8.sp,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun FilterTabItem(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bgColor = if (isSelected) HudCyan else Color(0x22374151)
    val textColor = if (isSelected) Color.Black else TextSecondary
    val borderColor = if (isSelected) HudCyan else Color.Transparent

    Box(
        modifier = modifier
            .height(38.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(10.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            color = textColor,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            fontSize = 12.sp
        )
    }
}

@Composable
private fun ControlButton(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isActive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bgColor = if (isActive) HudCyan.copy(alpha = 0.25f) else Color(0x22374151)
    val borderColor = if (isActive) HudCyan else Color(0x334B5563)

    Row(
        modifier = modifier
            .height(42.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(10.dp))
            .clickable { onClick() }
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = if (isActive) HudCyan else TextPrimary,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = title,
            fontSize = 11.sp,
            color = if (isActive) HudCyan else TextPrimary,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun MetricBadge(label: String, value: String, color: Color) {
    Column(
        modifier = Modifier
            .background(Color(0x221E293B), RoundedCornerShape(8.dp))
            .border(1.dp, color.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = label, fontSize = 9.sp, color = TextSecondary)
        Text(text = value, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = color)
    }
}
