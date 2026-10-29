// Autor :  Sinelnikov Oleg 2026
// ModelT16 Detect - ML Kit Анализатор видеопотока и непрерывный трекер захваченного объекта

package com.modelt16.detect.analyzer

import android.graphics.RectF
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.objects.DetectedObject
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions
import java.util.concurrent.atomic.AtomicBoolean

class ObjectDetectionAnalyzer(
    private val onTrackedBoxUpdated: (RectF?) -> Unit
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

    // Состояние слежения
    @Volatile var isTracking: Boolean = false
    @Volatile private var lockedTrackingId: Int? = null
    @Volatile private var trackedBox: RectF? = null

    // Кэш последних обнаруженных рамок для мгновенного захвата
    private val cachedDetections = mutableListOf<MappedDetection>()

    private data class MappedDetection(val id: Int, val box: RectF)

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage == null || isBusy.get()) {
            imageProxy.close()
            return
        }

        isBusy.set(true)
        val rotationDegrees = imageProxy.imageInfo.rotationDegrees
        val image = InputImage.fromMediaImage(mediaImage, rotationDegrees)

        detector.process(image)
            .addOnSuccessListener { detectedObjects ->
                val isRotated = rotationDegrees == 90 || rotationDegrees == 270
                val imgWidth = if (isRotated) imageProxy.height.toFloat() else imageProxy.width.toFloat()
                val imgHeight = if (isRotated) imageProxy.width.toFloat() else imageProxy.height.toFloat()

                processDetections(detectedObjects, imgWidth, imgHeight, viewWidth, viewHeight)
            }
            .addOnFailureListener {
                // Игнорируем единичные сбои
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
        vHeight: Float
    ) {
        if (imgWidth <= 0f || imgHeight <= 0f || vWidth <= 0f || vHeight <= 0f) {
            return
        }

        val scale = maxOf(vWidth / imgWidth, vHeight / imgHeight)
        val offsetX = (vWidth - imgWidth * scale) / 2f
        val offsetY = (vHeight - imgHeight * scale) / 2f

        val currentMapped = mutableListOf<MappedDetection>()
        for (obj in detectedObjects) {
            val id = obj.trackingId ?: (obj.hashCode() and 0xFFFF)
            val raw = obj.boundingBox
            val mapped = RectF(
                raw.left * scale + offsetX,
                raw.top * scale + offsetY,
                raw.right * scale + offsetX,
                raw.bottom * scale + offsetY
            )
            currentMapped.add(MappedDetection(id, mapped))
        }

        synchronized(cachedDetections) {
            cachedDetections.clear()
            cachedDetections.addAll(currentMapped)
        }

        if (!isTracking) {
            return
        }

        val currentBox = trackedBox ?: return

        // 1. Ищем объект в текущем кадре по ID или по близости
        var matched = currentMapped.firstOrNull { it.id == lockedTrackingId }

        if (matched == null) {
            matched = currentMapped.minByOrNull { det ->
                val dx = det.box.centerX() - currentBox.centerX()
                val dy = det.box.centerY() - currentBox.centerY()
                kotlin.math.hypot(dx, dy)
            }?.takeIf { det ->
                val dx = det.box.centerX() - currentBox.centerX()
                val dy = det.box.centerY() - currentBox.centerY()
                val dist = kotlin.math.hypot(dx, dy)
                dist < (maxOf(currentBox.width(), det.box.width()) * 1.5f) ||
                    RectF.intersects(currentBox, det.box)
            }
        }

        val updatedBox: RectF
        if (matched != null) {
            // Плавное сглаживание движения рамки (LERP)
            val alpha = 0.55f
            updatedBox = RectF(
                currentBox.left + alpha * (matched.box.left - currentBox.left),
                currentBox.top + alpha * (matched.box.top - currentBox.top),
                currentBox.right + alpha * (matched.box.right - currentBox.right),
                currentBox.bottom + alpha * (matched.box.bottom - currentBox.bottom)
            )
            lockedTrackingId = matched.id
            trackedBox = updatedBox
        } else {
            // Если объект на мгновение выпал из кадра — держим рамку на месте, не теряя слежения
            updatedBox = currentBox
        }

        onTrackedBoxUpdated(updatedBox)
    }

    /**
     * Запомнить объект внутри указанной рамки и начать слежение
     */
    fun lockAt(frame: RectF) {
        var bestMatch: MappedDetection? = null

        synchronized(cachedDetections) {
            bestMatch = cachedDetections.minByOrNull { det ->
                val dx = det.box.centerX() - frame.centerX()
                val dy = det.box.centerY() - frame.centerY()
                kotlin.math.hypot(dx, dy)
            }?.takeIf { det ->
                RectF.intersects(frame, det.box) ||
                    frame.contains(det.box.centerX(), det.box.centerY()) ||
                    det.box.contains(frame.centerX(), frame.centerY()) ||
                    kotlin.math.hypot(
                        det.box.centerX() - frame.centerX(),
                        det.box.centerY() - frame.centerY()
                    ) < frame.width() * 0.9f
            }
        }

        val initialBox = if (bestMatch != null) {
            RectF(bestMatch!!.box)
        } else {
            RectF(frame)
        }

        lockedTrackingId = bestMatch?.id
        trackedBox = initialBox
        isTracking = true

        onTrackedBoxUpdated(initialBox)
    }

    /**
     * Сбросить слежение
     */
    fun unlock() {
        isTracking = false
        lockedTrackingId = null
        trackedBox = null
        onTrackedBoxUpdated(null)
    }

    fun close() {
        detector.close()
    }
}
