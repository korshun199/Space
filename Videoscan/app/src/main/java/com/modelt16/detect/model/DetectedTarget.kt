// Autor :  Sinelnikov Oleg 2026
// ModelT16 Detect - Модель обнаруженной цели

package com.modelt16.detect.model

import android.graphics.RectF

data class DetectedTarget(
    val id: Int,
    val boundingBox: RectF,
    val label: String,
    val confidence: Float,
    val isLocked: Boolean = false,
    val centerDeltaX: Float = 0f,
    val centerDeltaY: Float = 0f
) {
    val width: Float get() = boundingBox.width()
    val height: Float get() = boundingBox.height()
    val centerX: Float get() = boundingBox.centerX()
    val centerY: Float get() = boundingBox.centerY()
}

enum class ScanFilter(val title: String) {
    ALL("Все объекты"),
    PEOPLE("Только люди"),
    ITEMS("Только предметы")
}
