// Autor :  Sinelnikov Oleg 2026
// ModelT16 Detect - Тактический Canvas оверлей с прицелом и яркими жирными рамками захвата

package com.modelt16.detect.ui

import android.graphics.Paint
import android.graphics.RectF
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import com.modelt16.detect.model.DetectedTarget
import com.modelt16.detect.ui.theme.HudCyan
import com.modelt16.detect.ui.theme.HudDarkBg
import com.modelt16.detect.ui.theme.HudGreen
import com.modelt16.detect.ui.theme.HudLockAmber
import com.modelt16.detect.ui.theme.HudLockRed
import com.modelt16.detect.ui.theme.HudLockRedGlow

@Composable
fun TacticalHudOverlay(
    targets: List<DetectedTarget>,
    lockedTarget: DetectedTarget?,
    onTapAt: (Offset) -> Unit,
    modifier: Modifier = Modifier
) {
    val currentOnTapAt by rememberUpdatedState(onTapAt)

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    currentOnTapAt(offset)
                }
            }
    ) {
        val centerX = size.width / 2f
        val centerY = size.height / 2f

        // 1. Центральное перекрестие видоискателя
        drawOpticalCrosshair(centerX, centerY)

        // 2. Отрисовка обычных обнаруженных целей (яркие, жирные рамки)
        for (target in targets) {
            if (!target.isLocked) {
                drawNormalTarget(target)
            }
        }

        // 3. Отрисовка захваченной цели (LOCKED TARGET)
        if (lockedTarget != null) {
            drawLockedTarget(lockedTarget, centerX, centerY)
        } else {
            // Подсказка на экране в прицеле
            drawCrosshairHint(centerX, centerY + 80f)
        }
    }
}

private fun DrawScope.drawCrosshairHint(cx: Float, cy: Float) {
    drawContext.canvas.nativeCanvas.apply {
        val paint = Paint().apply {
            color = android.graphics.Color.argb(220, 0, 229, 255)
            textSize = 30f
            isFakeBoldText = true
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        drawText("НАВЕДИТЕ ПРИЦЕЛ ИЛИ НАЖМИТЕ НА ПРЕДМЕТ", cx, cy, paint)
    }
}

private fun DrawScope.drawOpticalCrosshair(cx: Float, cy: Float) {
    val crossSize = 34f
    val centerHole = 8f
    val crossColor = Color(0xCC00E5FF)

    // Горизонтальные риски
    drawLine(
        color = crossColor,
        start = Offset(cx - crossSize, cy),
        end = Offset(cx - centerHole, cy),
        strokeWidth = 3.5f,
        cap = StrokeCap.Round
    )
    drawLine(
        color = crossColor,
        start = Offset(cx + centerHole, cy),
        end = Offset(cx + crossSize, cy),
        strokeWidth = 3.5f,
        cap = StrokeCap.Round
    )

    // Вертикальные риски
    drawLine(
        color = crossColor,
        start = Offset(cx, cy - crossSize),
        end = Offset(cx, cy - centerHole),
        strokeWidth = 3.5f,
        cap = StrokeCap.Round
    )
    drawLine(
        color = crossColor,
        start = Offset(cx, cy + centerHole),
        end = Offset(cx, cy + crossSize),
        strokeWidth = 3.5f,
        cap = StrokeCap.Round
    )

    // Внутреннее и внешнее кольцо прицела
    drawCircle(
        color = Color(0x6600E5FF),
        radius = 48f,
        center = Offset(cx, cy),
        style = Stroke(width = 2.5f)
    )

    // Угловые метки зоны захвата вокруг центра
    val zoneBox = RectF(cx - 70f, cy - 70f, cx + 70f, cy + 70f)
    drawCornerBrackets(zoneBox, Color(0x9900E5FF), strokeWidth = 3f, cornerLen = 18f)
}

private fun DrawScope.drawNormalTarget(target: DetectedTarget) {
    val box = target.boundingBox
    val isPerson = target.label.contains("Человек", ignoreCase = true)
    val color = if (isPerson) HudGreen else HudCyan

    // 1. Полупрозрачная неоновая заливка, чтобы предмет четко выделялся
    drawRect(
        color = color.copy(alpha = 0.18f),
        topLeft = Offset(box.left, box.top),
        size = Size(box.width(), box.height())
    )

    // 2. Жирный контур рамки
    drawRect(
        color = color.copy(alpha = 0.85f),
        topLeft = Offset(box.left, box.top),
        size = Size(box.width(), box.height()),
        style = Stroke(width = 4.5f)
    )

    // 3. Массивные яркие тактические скобки по углам
    drawCornerBrackets(box, color, strokeWidth = 7f, cornerLen = 34f)

    // 4. Яркий читаемый бейдж с названием и уверенностью
    val tagText = "${target.label.uppercase()} ${(target.confidence * 100).toInt()}%"
    drawLabelBadge(
        text = tagText,
        x = box.left,
        y = box.top - 8f,
        bgColor = HudDarkBg,
        borderColor = color,
        textColor = color
    )
}

private fun DrawScope.drawLockedTarget(target: DetectedTarget, screenCenterX: Float, screenCenterY: Float) {
    val box = target.boundingBox
    val tgtCenterX = box.centerX()
    val tgtCenterY = box.centerY()

    // 1. Вектор сопровождения от центра видоискателя к цели
    drawLine(
        color = HudLockAmber,
        start = Offset(screenCenterX, screenCenterY),
        end = Offset(tgtCenterX, tgtCenterY),
        strokeWidth = 3.5f,
        pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 10f), 0f)
    )

    // 2. Яркая неоновая заливка области захвата
    drawRect(
        color = HudLockRedGlow,
        topLeft = Offset(box.left, box.top),
        size = Size(box.width(), box.height())
    )

    // 3. Сверхжирный контур захвата
    drawRect(
        color = HudLockRed,
        topLeft = Offset(box.left, box.top),
        size = Size(box.width(), box.height()),
        style = Stroke(width = 6f)
    )

    // 4. Сверхжирные тактические угловые скобки захвата
    drawCornerBrackets(box, HudLockRed, strokeWidth = 9f, cornerLen = 44f)

    // 5. Тактический прицел на центре цели
    val crossLen = 22f
    drawLine(
        color = HudLockRed,
        start = Offset(tgtCenterX - crossLen, tgtCenterY),
        end = Offset(tgtCenterX + crossLen, tgtCenterY),
        strokeWidth = 4f,
        cap = StrokeCap.Round
    )
    drawLine(
        color = HudLockRed,
        start = Offset(tgtCenterX, tgtCenterY - crossLen),
        end = Offset(tgtCenterX, tgtCenterY + crossLen),
        strokeWidth = 4f,
        cap = StrokeCap.Round
    )
    drawCircle(
        color = HudLockRed,
        radius = 16f,
        center = Offset(tgtCenterX, tgtCenterY),
        style = Stroke(width = 3.5f)
    )

    // 6. Верхняя плашка "🎯 ЗАХВАЧЕНО"
    val lockTag = "🎯 ЗАХВАЧЕНО: ${target.label.uppercase()} [#${target.id}]"
    drawLabelBadge(
        text = lockTag,
        x = box.left,
        y = box.top - 12f,
        bgColor = HudLockRed,
        borderColor = Color.White,
        textColor = Color.White
    )

    // 7. Телеметрия отклонения (ΔX, ΔY)
    val dxPct = (target.centerDeltaX * 100).toInt()
    val dyPct = (target.centerDeltaY * 100).toInt()
    val telemetryTag = "ΔX: ${if (dxPct >= 0) "+$dxPct" else "$dxPct"}%   ΔY: ${if (dyPct >= 0) "+$dyPct" else "$dyPct"}%   [${(box.width()).toInt()}x${(box.height()).toInt()}]"
    drawLabelBadge(
        text = telemetryTag,
        x = box.left,
        y = box.bottom + 28f,
        bgColor = HudDarkBg,
        borderColor = HudLockAmber,
        textColor = HudLockAmber
    )
}

private fun DrawScope.drawCornerBrackets(box: RectF, color: Color, strokeWidth: Float, cornerLen: Float) {
    val len = minOf(cornerLen, box.width() / 2.8f, box.height() / 2.8f)

    // Верхний левый угол
    drawLine(color, Offset(box.left, box.top), Offset(box.left + len, box.top), strokeWidth, StrokeCap.Square)
    drawLine(color, Offset(box.left, box.top), Offset(box.left, box.top + len), strokeWidth, StrokeCap.Square)

    // Верхний правый угол
    drawLine(color, Offset(box.right, box.top), Offset(box.right - len, box.top), strokeWidth, StrokeCap.Square)
    drawLine(color, Offset(box.right, box.top), Offset(box.right, box.top + len), strokeWidth, StrokeCap.Square)

    // Нижний левый угол
    drawLine(color, Offset(box.left, box.bottom), Offset(box.left + len, box.bottom), strokeWidth, StrokeCap.Square)
    drawLine(color, Offset(box.left, box.bottom), Offset(box.left, box.bottom - len), strokeWidth, StrokeCap.Square)

    // Нижний правый угол
    drawLine(color, Offset(box.right, box.bottom), Offset(box.right - len, box.bottom), strokeWidth, StrokeCap.Square)
    drawLine(color, Offset(box.right, box.bottom), Offset(box.right, box.bottom - len), strokeWidth, StrokeCap.Square)
}

private fun DrawScope.drawLabelBadge(
    text: String,
    x: Float,
    y: Float,
    bgColor: Color,
    borderColor: Color,
    textColor: Color
) {
    drawContext.canvas.nativeCanvas.apply {
        val paintText = Paint().apply {
            color = android.graphics.Color.rgb(
                (textColor.red * 255).toInt(),
                (textColor.green * 255).toInt(),
                (textColor.blue * 255).toInt()
            )
            textSize = 32f
            isFakeBoldText = true
            isAntiAlias = true
        }

        val textWidth = paintText.measureText(text)
        val textHeight = 32f
        val padding = 12f

        val bgPaint = Paint().apply {
            color = android.graphics.Color.argb(
                (bgColor.alpha * 255).toInt(),
                (bgColor.red * 255).toInt(),
                (bgColor.green * 255).toInt(),
                (bgColor.blue * 255).toInt()
            )
            style = Paint.Style.FILL
            isAntiAlias = true
        }

        val borderPaint = Paint().apply {
            color = android.graphics.Color.argb(
                (borderColor.alpha * 255).toInt(),
                (borderColor.red * 255).toInt(),
                (borderColor.green * 255).toInt(),
                (borderColor.blue * 255).toInt()
            )
            style = Paint.Style.STROKE
            strokeWidth = 2.5f
            isAntiAlias = true
        }

        val left = maxOf(8f, x)
        val top = maxOf(30f, y - textHeight - padding)
        val right = left + textWidth + padding * 2
        val bottom = top + textHeight + padding

        drawRoundRect(left, top, right, bottom, 10f, 10f, bgPaint)
        drawRoundRect(left, top, right, bottom, 10f, 10f, borderPaint)
        drawText(text, left + padding, bottom - padding - 4f, paintText)
    }
}
