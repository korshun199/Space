// Autor :  Sinelnikov Oleg 2026
// ModelT16 Detect - Нижняя панель управления с 3 кнопками-значками: Скан, Захват, Настройки

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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.FilterCenterFocus
import androidx.compose.material.icons.filled.GpsOff
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.modelt16.detect.model.ScanFilter
import com.modelt16.detect.ui.theme.HudCardBorder
import com.modelt16.detect.ui.theme.HudCyan
import com.modelt16.detect.ui.theme.HudGreen
import com.modelt16.detect.ui.theme.HudLockAmber
import com.modelt16.detect.ui.theme.HudLockRed
import com.modelt16.detect.ui.theme.HudPanelBg
import com.modelt16.detect.ui.theme.TextPrimary
import com.modelt16.detect.ui.theme.TextSecondary

@Composable
fun BottomControlsBar(
    activeFilter: ScanFilter,
    hasLockedTarget: Boolean,
    onCycleScan: () -> Unit,
    onToggleLock: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 12.dp)
            .background(HudPanelBg.copy(alpha = 0.90f), RoundedCornerShape(26.dp))
            .border(1.5.dp, HudCardBorder, RoundedCornerShape(26.dp))
            .padding(horizontal = 16.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 1. Кнопка "Скан" (переключение фильтра сканирования Все/Люди/Предметы)
            val scanColor = when (activeFilter) {
                ScanFilter.ALL -> HudCyan
                ScanFilter.PEOPLE -> HudGreen
                ScanFilter.ITEMS -> HudLockAmber
            }
            IconButtonWithLabel(
                icon = Icons.Default.FilterCenterFocus,
                label = "СКАН: ${activeFilter.title.uppercase()}",
                tint = scanColor,
                bgColor = scanColor.copy(alpha = 0.15f),
                borderColor = scanColor.copy(alpha = 0.6f),
                size = 54.dp,
                onClick = onCycleScan
            )

            // 2. Кнопка "Захват" (захват в прицел / сброс захвата)
            if (hasLockedTarget) {
                IconButtonWithLabel(
                    icon = Icons.Default.GpsOff,
                    label = "СБРОС",
                    tint = Color.White,
                    bgColor = HudLockRed,
                    borderColor = Color.White,
                    size = 64.dp,
                    onClick = onToggleLock
                )
            } else {
                IconButtonWithLabel(
                    icon = Icons.Default.CenterFocusStrong,
                    label = "ЗАХВАТ",
                    tint = HudCyan,
                    bgColor = HudCyan.copy(alpha = 0.20f),
                    borderColor = HudCyan,
                    size = 64.dp,
                    onClick = onToggleLock
                )
            }

            // 3. Кнопка "Настройки" (вызов отдельного окна настроек)
            IconButtonWithLabel(
                icon = Icons.Default.Tune,
                label = "НАСТРОЙКИ",
                tint = TextPrimary,
                bgColor = Color(0x331F2937),
                borderColor = Color(0x55374151),
                size = 54.dp,
                onClick = onOpenSettings
            )
        }
    }
}

@Composable
private fun IconButtonWithLabel(
    icon: ImageVector,
    label: String,
    tint: Color,
    bgColor: Color,
    borderColor: Color,
    size: androidx.compose.ui.unit.Dp,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(bgColor)
                .border(2.dp, borderColor, CircleShape)
                .clickable { onClick() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = tint,
                modifier = Modifier.size(size * 0.52f)
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = tint,
            letterSpacing = 0.5.sp
        )
    }
}
