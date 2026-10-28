// Autor :  Sinelnikov Oleg 2026
// ModelT16 Detect - Нижняя панель управления и переключения режимов

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.GpsOff
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.modelt16.detect.model.ScanFilter
import com.modelt16.detect.ui.theme.HudCardBorder
import com.modelt16.detect.ui.theme.HudCyan
import com.modelt16.detect.ui.theme.HudGreen
import com.modelt16.detect.ui.theme.HudLockRed
import com.modelt16.detect.ui.theme.HudPanelBg
import com.modelt16.detect.ui.theme.TextPrimary
import com.modelt16.detect.ui.theme.TextSecondary

@Composable
fun BottomControlsBar(
    activeFilter: ScanFilter,
    hasLockedTarget: Boolean,
    isTorchOn: Boolean,
    onFilterChange: (ScanFilter) -> Unit,
    onUnlockTarget: () -> Unit,
    onToggleTorch: () -> Unit,
    onSwitchCamera: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .background(HudPanelBg, RoundedCornerShape(20.dp))
            .border(1.dp, HudCardBorder, RoundedCornerShape(20.dp))
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (hasLockedTarget) {
            Button(
                onClick = onUnlockTarget,
                colors = ButtonDefaults.buttonColors(
                    containerColor = HudLockRed,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.GpsOff,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "СБРОСИТЬ ЗАХВАТ ЦЕЛИ",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    letterSpacing = 1.sp
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterTab(
                title = "Все",
                isSelected = activeFilter == ScanFilter.ALL,
                onClick = { onFilterChange(ScanFilter.ALL) },
                modifier = Modifier.weight(1f)
            )
            FilterTab(
                title = "Люди",
                isSelected = activeFilter == ScanFilter.PEOPLE,
                onClick = { onFilterChange(ScanFilter.PEOPLE) },
                modifier = Modifier.weight(1f)
            )
            FilterTab(
                title = "Предметы",
                isSelected = activeFilter == ScanFilter.ITEMS,
                onClick = { onFilterChange(ScanFilter.ITEMS) },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onToggleTorch,
                modifier = Modifier
                    .size(46.dp)
                    .background(
                        if (isTorchOn) HudCyan.copy(alpha = 0.25f) else Color(0x331F2937),
                        CircleShape
                    )
            ) {
                Icon(
                    imageVector = if (isTorchOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                    contentDescription = "Фонарик",
                    tint = if (isTorchOn) HudCyan else TextSecondary
                )
            }

            IconButton(
                onClick = onSwitchCamera,
                modifier = Modifier
                    .size(46.dp)
                    .background(Color(0x331F2937), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Cameraswitch,
                    contentDescription = "Сменить камеру",
                    tint = TextPrimary
                )
            }

            IconButton(
                onClick = onOpenSettings,
                modifier = Modifier
                    .size(46.dp)
                    .background(Color(0x331F2937), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Tune,
                    contentDescription = "Настройки",
                    tint = TextPrimary
                )
            }
        }
    }
}

@Composable
private fun FilterTab(
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
            .height(36.dp)
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
