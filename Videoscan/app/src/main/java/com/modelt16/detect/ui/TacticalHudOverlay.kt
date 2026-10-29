// Autor :  Sinelnikov Oleg 2026
// ModelT16 Detect - Тактический Canvas оверлей с прицелом и рамками захвата

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

        // 2. Отрисовка обычных обнаруженных целей
        for (target in targets) {
            if (!target.isLocked) {
                drawNormalTarget(target)
            }
        }

        // 3. Отрисовка захваченной цели (LOCKED TARGET)
        if (lockedTarget != null) {
            drawLockedTarget(lockedTarget, centerX, centerY)
        } else {
            // Подсказка на экране при отсутствии захвата
            drawPromptHint(centerX, centerY + 70f)
        }
    }
}

private fun DrawScope.drawPromptHint(cx: Float, cy: Float) {
    drawContext.canvas.nativeCanvas.apply {
        val paint = Paint().apply {
            color = android.graphics.Color.argb(180, 200, 220, 240)
            textSize = 28f
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        drawText("Нажмите на предмет на экране или кнопку [ЗАХВАТ]", cx, cy, paint)
    }
}

private fun DrawScope.drawOpticalCrosshair(cx: Float, cy: Float) {
    val crossSize = 26f
    val centerHole = 6f
    val crossColor = Color(0x9938BDF8)

    drawLine(
        color = crossColor,
        start = Offset(cx - crossSize, cy),
        end = Offset(cx - centerHole, cy),
        strokeWidth = 2.5f
    )
    drawLine(
        color = crossColor,
        start = Offset(cx + centerHole, cy),
        end = Offset(cx + crossSize, cy),
        strokeWidth = 2.5f
    )

    drawLine(
        color = crossColor,
        start = Offset(cx, cy - crossSize),
        end = Offset(cx, cy - centerHole),
        strokeWidth = 2.5f
    )
    drawLine(
        color = crossColor,
        start = Offset(cx, cy + centerHole),
        end = Offset(cx, cy + crossSize),
        strokeWidth = 2.5f
    )

    drawCircle(
        color = Color(0x4438BDF8),
        radius = 36f,
        center = Offset(cx, cy),
        style = Stroke(width = 1.5f)
    )
}

private fun DrawScope.drawNormalTarget(target: DetectedTarget) {
    val box = target.boundingBox
    val isPerson = target.label.contains("Человек", ignoreCase = true)
    val color = if (isPerson) HudGreen else HudCyan

    drawCornerBrackets(box, color, strokeWidth = 3.5f, cornerLen = 22f)

    val dash = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
    drawRect(
        color = color.copy(alpha = 0.30f),
        topLeft = Offset(box.left, box.top),
        size = Size(box.width(), box.height()),
        style = Stroke(width = 1.2f, pathEffect = dash)
    )

    val tagText = "${target.label} ${(target.confidence * 100).toInt()}%"
    drawLabelBadge(
        text = tagText,
        x = box.left,
        y = box.top - 6f,
        bgColor = HudDarkBg,
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
        strokeWidth = 2.5f,
        pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)
    )

    // 2. Яркая неоновая подсветка области захвата
    drawRect(
        color = HudLockRedGlow,
        topLeft = Offset(box.left, box.top),
        size = Size(box.width(), box.height())
    )

    // 3. Массивные тактические угловые скобки захвата
    drawCornerBrackets(box, HudLockRed, strokeWidth = 6f, cornerLen = 32f)

    // 4. Тактическое перекрестие на цели
    val crossLen = 18f
    drawLine(
        color = HudLockRed,
        start = Offset(tgtCenterX - crossLen, tgtCenterY),
        end = Offset(tgtCenterX + crossLen, tgtCenterY),
        strokeWidth = 3f
    )
    drawLine(
        color = HudLockRed,
        start = Offset(tgtCenterX, tgtCenterY - crossLen),
        end = Offset(tgtCenterX, tgtCenterY + crossLen),
        strokeWidth = 3f
    )
    drawCircle(
        color = HudLockRed,
        radius = 12f,
        center = Offset(tgtCenterX, tgtCenterY),
        style = Stroke(width = 2.5f)
    )

    // 5. Верхняя плашка "LOCKED"
    val lockTag = "🎯 ЗАХВАЧЕНО: ${target.label} [#${target.id}]"
    drawLabelBadge(
        text = lockTag,
        x = box.left,
        y = box.top - 10f,
        bgColor = HudLockRed,
        textColor = Color.White
    )

    // 6. Телеметрия отклонения (ΔX, ΔY)
    val dxPct = (target.centerDeltaX * 100).toInt()
    val dyPct = (target.centerDeltaY * 100).toInt()
    val telemetryTag = "ΔX: ${dxPct}%  ΔY: ${dyPct}%  [РАЗМЕР: ${(box.width()).toInt()}x${(box.height()).toInt()}]"
    drawLabelBadge(
        text = telemetryTag,
        x = box.left,
        y = box.bottom + 24f,
        bgColor = HudDarkBg,
        textColor = HudLockAmber
    )
}

private fun DrawScope.drawCornerBrackets(box: RectF, color: Color, strokeWidth: Float, cornerLen: Float) {
    val len = minOf(cornerLen, box.width() / 3f, box.height() / 3f)

    drawLine(color, Offset(box.left, box.top), Offset(box.left + len, box.top), strokeWidth, StrokeCap.Square)
    drawLine(color, Offset(box.left, box.top), Offset(box.left, box.top + len), strokeWidth, StrokeCap.Square)

    drawLine(color, Offset(box.right, box.top), Offset(box.right - len, box.top), strokeWidth, StrokeCap.Square)
    drawLine(color, Offset(box.right, box.top), Offset(box.right, box.top + len), strokeWidth, StrokeCap.Square)

    drawLine(color, Offset(box.left, box.bottom), Offset(box.left + len, box.bottom), strokeWidth, StrokeCap.Square)
    drawLine(color, Offset(box.left, box.bottom), Offset(box.left, box.bottom - len), strokeWidth, StrokeCap.Square)

    drawLine(color, Offset(box.right, box.bottom), Offset(box.right - len, box.bottom), strokeWidth, StrokeCap.Square)
    drawLine(color, Offset(box.right, box.bottom), Offset(box.right, box.bottom - len), strokeWidth, StrokeCap.Square)
}

private fun DrawScope.drawLabelBadge(text: String, x: Float, y: Float, bgColor: Color, textColor: Color) {
    drawContext.canvas.nativeCanvas.apply {
        val paintText = Paint().apply {
            color = android.graphics.Color.rgb(
                (textColor.red * 255).toInt(),
                (textColor.green * 255).toInt(),
                (textColor.blue * 255).toInt()
            )
            textSize = 34f
            isFakeBoldText = true
            isAntiAlias = true
        }

        val textWidth = paintText.measureText(text)
        val textHeight = 34f
        val padding = 10f

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

        val left = maxOf(8f, x)
        val top = maxOf(30f, y - textHeight - padding)
        val right = left + textWidth + padding * 2
        val bottom = top + textHeight + padding

        drawRoundRect(left, top, right, bottom, 10f, 10f, bgPaint)
        drawText(text, left + padding, bottom - padding - 4f, paintText)
    }
}
