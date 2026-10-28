// Autor :  Sinelnikov Oleg 2026
// ModelT16 Detect - ViewModel управления сканером, захватом цели и фильтрами

package com.modelt16.detect.ui

import android.app.Application
import android.graphics.RectF
import androidx.camera.core.Camera
import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.AndroidViewModel
import com.modelt16.detect.analyzer.ObjectDetectionAnalyzer
import com.modelt16.detect.model.DetectedTarget
import com.modelt16.detect.model.ScanFilter
import com.modelt16.detect.model.ScanUiState
import com.modelt16.detect.util.HapticHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val haptic = HapticHelper(application)
    private var boundCamera: Camera? = null

    private val _uiState = MutableStateFlow(ScanUiState())
    val uiState: StateFlow<ScanUiState> = _uiState.asStateFlow()

    val analyzer = ObjectDetectionAnalyzer { targets, fps, latencyMs ->
        _uiState.update { current ->
            current.copy(
                targets = targets,
                fps = fps,
                latencyMs = latencyMs,
                statusMessage = if (current.hasLockedTarget) "Сопровождение цели #${current.lockedTargetId}" else "Обнаружено: ${targets.size}"
            )
        }
    }

    fun onCameraBound(camera: Camera) {
        this.boundCamera = camera
        camera.cameraControl.enableTorch(_uiState.value.isTorchOn)
    }

    fun onCameraPermissionResult(granted: Boolean) {
        _uiState.update { it.copy(hasCameraPermission = granted) }
    }

    fun onViewSizeChanged(w: Float, h: Float) {
        analyzer.viewWidth = w
        analyzer.viewHeight = h
    }

    /**
     * Захват предмета в оптическом центре видоискателя
     */
    fun lockCenterTarget() {
        val targets = _uiState.value.targets
        val cx = analyzer.viewWidth / 2f
        val cy = analyzer.viewHeight / 2f

        // 1. Ищем уже обнаруженный объект, ближе всего к центру (в радиусе 280px)
        val closestTarget = targets.minByOrNull { target ->
            val dx = target.centerX - cx
            val dy = target.centerY - cy
            kotlin.math.hypot(dx, dy)
        }?.takeIf { target ->
            val dx = target.centerX - cx
            val dy = target.centerY - cy
            kotlin.math.hypot(dx, dy) < 280f
        }

        if (closestTarget != null) {
            analyzer.lockedTargetId = closestTarget.id
            analyzer.manualLockBox = null
            _uiState.update { it.copy(lockedTargetId = closestTarget.id) }
            haptic.performLockHaptic()
        } else {
            // 2. Если нейросеть пока не разметила объект, создаем рамку захвата прямо в перекрестии
            val boxSize = minOf(analyzer.viewWidth, analyzer.viewHeight) * 0.35f
            val manualBox = RectF(
                cx - boxSize / 2f,
                cy - boxSize / 2f,
                cx + boxSize / 2f,
                cy + boxSize / 2f
            )
            val manualId = 8888
            analyzer.manualLockBox = manualBox
            analyzer.lockedTargetId = manualId
            _uiState.update { it.copy(lockedTargetId = manualId) }
            haptic.performLockHaptic()
        }
    }

    /**
     * Переключение захвата (захватить цель в прицеле / сбросить захват)
     */
    fun toggleLock() {
        if (_uiState.value.hasLockedTarget) {
            unlockTarget()
        } else {
            lockCenterTarget()
        }
    }

    /**
     * Нажатие на экран пальцем: выбор объекта или создание рамки захвата
     */
    fun onTapAt(tapOffset: Offset) {
        // Если уже захвачена цель и тапнули по ней — сбрасываем захват
        val currentLocked = _uiState.value.lockedTarget
        if (currentLocked != null && currentLocked.boundingBox.contains(tapOffset.x, tapOffset.y)) {
            unlockTarget()
            return
        }

        val targets = _uiState.value.targets
        val directHit = targets.firstOrNull { it.boundingBox.contains(tapOffset.x, tapOffset.y) }
        val nearestTarget = directHit ?: targets.minByOrNull { target ->
            val dx = target.centerX - tapOffset.x
            val dy = target.centerY - tapOffset.y
            kotlin.math.hypot(dx, dy)
        }?.takeIf { target ->
            val dx = target.centerX - tapOffset.x
            val dy = target.centerY - tapOffset.y
            kotlin.math.hypot(dx, dy) < 180f
        }

        if (nearestTarget != null) {
            if (_uiState.value.lockedTargetId == nearestTarget.id) {
                unlockTarget()
            } else {
                analyzer.lockedTargetId = nearestTarget.id
                analyzer.manualLockBox = null
                _uiState.update { it.copy(lockedTargetId = nearestTarget.id) }
                haptic.performLockHaptic()
            }
        } else {
            // Тапнули в произвольное место экрана — фиксируем рамку захвата на этом предмете
            val boxSize = minOf(analyzer.viewWidth, analyzer.viewHeight) * 0.30f
            val manualBox = RectF(
                tapOffset.x - boxSize / 2f,
                tapOffset.y - boxSize / 2f,
                tapOffset.x + boxSize / 2f,
                tapOffset.y + boxSize / 2f
            )
            val manualId = 7000 + (System.currentTimeMillis() % 1000).toInt()
            analyzer.manualLockBox = manualBox
            analyzer.lockedTargetId = manualId
            _uiState.update { it.copy(lockedTargetId = manualId) }
            haptic.performLockHaptic()
        }
    }

    /**
     * Сброс текущего захвата
     */
    fun unlockTarget() {
        analyzer.lockedTargetId = null
        analyzer.manualLockBox = null
        _uiState.update { it.copy(lockedTargetId = null) }
        haptic.performUnlockHaptic()
    }

    /**
     * Циклическое переключение фильтра (Все -> Люди -> Предметы -> Все)
     */
    fun cycleFilter() {
        val next = when (_uiState.value.filter) {
            ScanFilter.ALL -> ScanFilter.PEOPLE
            ScanFilter.PEOPLE -> ScanFilter.ITEMS
            ScanFilter.ITEMS -> ScanFilter.ALL
        }
        setFilter(next)
    }

    fun setFilter(filter: ScanFilter) {
        analyzer.activeFilter = filter
        _uiState.update { it.copy(filter = filter) }
        haptic.performClickHaptic()
    }

    fun setMinConfidence(threshold: Float) {
        analyzer.minConfidence = threshold
        _uiState.update { it.copy(minConfidence = threshold) }
    }

    fun toggleTorch() {
        val newState = !_uiState.value.isTorchOn
        boundCamera?.cameraControl?.enableTorch(newState)
        _uiState.update { it.copy(isTorchOn = newState) }
        haptic.performClickHaptic()
    }

    fun switchCamera() {
        unlockTarget()
        val newIsFront = !_uiState.value.isFrontCamera
        _uiState.update { it.copy(isFrontCamera = newIsFront, isTorchOn = false) }
        haptic.performClickHaptic()
    }

    override fun onCleared() {
        super.onCleared()
        analyzer.close()
    }
}
