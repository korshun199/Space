// Autor :  Sinelnikov Oleg 2026
// ModelT16 Detect - ML Kit Анализатор видеопотока и трекер объектов

package com.modelt16.detect.analyzer

import android.graphics.Rect
import android.graphics.RectF
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.objects.DetectedObject
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions
import com.modelt16.detect.model.DetectedTarget
import com.modelt16.detect.model.ScanFilter
import java.util.concurrent.atomic.AtomicBoolean

class ObjectDetectionAnalyzer(
    private val onResults: (List<DetectedTarget>, Int, Long) -> Unit
) : ImageAnalysis.Analyzer {

    private val detector = ObjectDetection.getClient(
        ObjectDetectorOptions.Builder()
            .setDetectorMode(ObjectDetectorOptions.STREAM_MODE)
            .enableMultipleObjects()
            .enableClassification()
            .build()
    )

    private val isBusy = AtomicBoolean(false)

    @Volatile var viewWidth: Float = 1080f
    @Volatile var viewHeight: Float = 1920f
    @Volatile var activeFilter: ScanFilter = ScanFilter.ALL
    @Volatile var minConfidence: Float = 0.40f
    @Volatile var lockedTargetId: Int? = null

    private var frameCount = 0
    private var lastFpsTimestamp = System.currentTimeMillis()
    private var currentFps = 0

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage == null || isBusy.get()) {
            imageProxy.close()
            return
        }

        isBusy.set(true)
        val startTime = System.currentTimeMillis()
        val rotationDegrees = imageProxy.imageInfo.rotationDegrees

        val image = InputImage.fromMediaImage(mediaImage, rotationDegrees)

        detector.process(image)
            .addOnSuccessListener { detectedObjects ->
                val latency = System.currentTimeMillis() - startTime

                frameCount++
                val now = System.currentTimeMillis()
                if (now - lastFpsTimestamp >= 1000) {
                    currentFps = frameCount
                    frameCount = 0
                    lastFpsTimestamp = now
                }

                val isRotated = rotationDegrees == 90 || rotationDegrees == 270
                val imgWidth = if (isRotated) imageProxy.height.toFloat() else imageProxy.width.toFloat()
                val imgHeight = if (isRotated) imageProxy.width.toFloat() else imageProxy.height.toFloat()

                val mappedTargets = processDetections(
                    detectedObjects = detectedObjects,
                    imgWidth = imgWidth,
                    imgHeight = imgHeight,
                    vWidth = viewWidth,
                    vHeight = viewHeight,
                    currentLockId = lockedTargetId,
                    filter = activeFilter,
                    threshold = minConfidence
                )

                onResults(mappedTargets, currentFps, latency)
            }
            .addOnFailureListener {
            }
            .addOnCompleteListener {
                isBusy.set(false)
                imageProxy.close()
            }
    }

    private fun processDetections(
        detectedObjects: List<DetectedObject>,
        imgWidth: Float,
        imgHeight: Float,
        vWidth: Float,
        vHeight: Float,
        currentLockId: Int?,
        filter: ScanFilter,
        threshold: Float
    ): List<DetectedTarget> {
        if (imgWidth <= 0f || imgHeight <= 0f || vWidth <= 0f || vHeight <= 0f) {
            return emptyList()
        }

        val scale = maxOf(vWidth / imgWidth, vHeight / imgHeight)
        val offsetX = (vWidth - imgWidth * scale) / 2f
        val offsetY = (vHeight - imgHeight * scale) / 2f

        val viewCenterX = vWidth / 2f
        val viewCenterY = vHeight / 2f

        val results = mutableListOf<DetectedTarget>()

        for (obj in detectedObjects) {
            val id = obj.trackingId ?: obj.hashCode()
            val rawBox = obj.boundingBox

            val mappedBox = RectF(
                rawBox.left * scale + offsetX,
                rawBox.top * scale + offsetY,
                rawBox.right * scale + offsetX,
                rawBox.bottom * scale + offsetY
            )

            val topLabel = obj.labels.maxByOrNull { it.confidence }
            val rawLabelText = topLabel?.text ?: ""
            val confidence = topLabel?.confidence ?: 0.5f

            val friendlyLabel = translateLabel(rawLabelText)
            val isPerson = friendlyLabel == "Человек" || rawLabelText.equals("Person", ignoreCase = true)

            if (confidence < threshold && id != currentLockId) {
                continue
            }
            when (filter) {
                ScanFilter.PEOPLE -> if (!isPerson && id != currentLockId) continue
                ScanFilter.ITEMS -> if (isPerson && id != currentLockId) continue
                ScanFilter.ALL -> { }
            }

            val objCenterX = mappedBox.centerX()
            val objCenterY = mappedBox.centerY()
            val deltaX = if (viewCenterX > 0f) (objCenterX - viewCenterX) / viewCenterX else 0f
            val deltaY = if (viewCenterY > 0f) (objCenterY - viewCenterY) / viewCenterY else 0f

            val isLocked = (id == currentLockId)

            results.add(
                DetectedTarget(
                    id = id,
                    boundingBox = mappedBox,
                    label = friendlyLabel,
                    confidence = confidence,
                    isLocked = isLocked,
                    centerDeltaX = deltaX,
                    centerDeltaY = deltaY
                )
            )
        }

        return results
    }

    private fun translateLabel(raw: String): String {
        return when (raw.lowercase().trim()) {
            "person" -> "Человек"
            "fashion good" -> "Одежда/Аксессуар"
            "food" -> "Еда"
            "home good" -> "Предмет быта"
            "place" -> "Локация"
            "plant" -> "Растение"
            else -> if (raw.isBlank()) "Объект" else raw
        }
    }

    fun close() {
        detector.close()
    }
}
