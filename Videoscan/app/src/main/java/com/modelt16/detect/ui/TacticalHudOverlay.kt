// Autor :  Sinelnikov Oleg 2026
// ModelT16 Detect - Тактический Canvas оверлей с прицелом и рамками захвата

package com.modelt16.detect.ui

import android.graphics.Paint
import android.graphics.RectF
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
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
    Canvas(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(targets) {
                detectTapGestures { offset ->
                    onTapAt(offset)
                }
            }
    ) {
        val centerX = size.width / 2f
        val centerY = size.height / 2f

        drawOpticalCrosshair(centerX, centerY)

        for (target in targets) {
            if (!target.isLocked) {
                drawNormalTarget(target)
            }
        }

        if (lockedTarget != null) {
            drawLockedTarget(lockedTarget, centerX, centerY)
        }
    }
}

private fun DrawScope.drawOpticalCrosshair(cx: Float, cy: Float) {
    val crossSize = 22f
    val centerHole = 6f
    val crossColor = Color(0x7738BDF8)

    drawLine(
        color = crossColor,
        start = Offset(cx - crossSize, cy),
        end = Offset(cx - centerHole, cy),
        strokeWidth = 2f
    )
    drawLine(
        color = crossColor,
        start = Offset(cx + centerHole, cy),
        end = Offset(cx + crossSize, cy),
        strokeWidth = 2f
    )

    drawLine(
        color = crossColor,
        start = Offset(cx, cy - crossSize),
        end = Offset(cx, cy - centerHole),
        strokeWidth = 2f
    )
    drawLine(
        color = crossColor,
        start = Offset(cx, cy + centerHole),
        end = Offset(cx, cy + crossSize),
        strokeWidth = 2f
    )

    drawCircle(
        color = Color(0x3338BDF8),
        radius = 32f,
        center = Offset(cx, cy),
        style = Stroke(width = 1.2f)
    )
}

private fun DrawScope.drawNormalTarget(target: DetectedTarget) {
    val box = target.boundingBox
    val isPerson = target.label.contains("Человек", ignoreCase = true)
    val color = if (isPerson) HudGreen else HudCyan

    drawCornerBrackets(box, color, strokeWidth = 3f, cornerLen = 18f)

    val dash = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
    drawRect(
        color = color.copy(alpha = 0.35f),
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

    drawLine(
        color = HudLockAmber.copy(alpha = 0.75f),
        start = Offset(screenCenterX, screenCenterY),
        end = Offset(tgtCenterX, tgtCenterY),
        strokeWidth = 2f,
        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f), 0f)
    )

    drawRect(
        color = HudLockRedGlow,
        topLeft = Offset(box.left, box.top),
        size = Size(box.width(), box.height())
    )

    drawCornerBrackets(box, HudLockRed, strokeWidth = 5f, cornerLen = 28f)

    val crossLen = 14f
    drawLine(
        color = HudLockRed,
        start = Offset(tgtCenterX - crossLen, tgtCenterY),
        end = Offset(tgtCenterX + crossLen, tgtCenterY),
        strokeWidth = 2.5f
    )
    drawLine(
        color = HudLockRed,
        start = Offset(tgtCenterX, tgtCenterY - crossLen),
        end = Offset(tgtCenterX, tgtCenterY + crossLen),
        strokeWidth = 2.5f
    )
    drawCircle(
        color = HudLockRed,
        radius = 8f,
        center = Offset(tgtCenterX, tgtCenterY),
        style = Stroke(width = 2f)
    )

    val lockTag = "🎯 LOCKED #${target.id} : ${target.label}"
    drawLabelBadge(
        text = lockTag,
        x = box.left,
        y = box.top - 8f,
        bgColor = HudLockRed,
        textColor = Color.White
    )

    val dxPct = (target.centerDeltaX * 100).toInt()
    val dyPct = (target.centerDeltaY * 100).toInt()
    val telemetryTag = "ΔX: ${dxPct}%  ΔY: ${dyPct}%"
    drawLabelBadge(
        text = telemetryTag,
        x = box.left,
        y = box.bottom + 22f,
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
        val padding = 8f

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

        drawRoundRect(left, top, right, bottom, 8f, 8f, bgPaint)
        drawText(text, left + padding, bottom - padding - 4f, paintText)
    }
}
