// Autor :  Sinelnikov Oleg 2026
// ModelT16 Detect - ViewModel управления состоянием сканера и захватом цели

package com.modelt16.detect.ui

import android.app.Application
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

    fun onTapAt(tapOffset: Offset) {
        val targets = _uiState.value.targets
        if (targets.isEmpty()) return

        val directHit = targets.firstOrNull { it.boundingBox.contains(tapOffset.x, tapOffset.y) }

        val selectedTarget = directHit ?: targets.minByOrNull { target ->
            val dx = target.centerX - tapOffset.x
            val dy = target.centerY - tapOffset.y
            kotlin.math.sqrt(dx * dx + dy * dy)
        }?.takeIf { target ->
            val dx = target.centerX - tapOffset.x
            val dy = target.centerY - tapOffset.y
            kotlin.math.sqrt(dx * dx + dy * dy) < 160f
        }

        if (selectedTarget != null) {
            if (_uiState.value.lockedTargetId == selectedTarget.id) {
                unlockTarget()
            } else {
                analyzer.lockedTargetId = selectedTarget.id
                _uiState.update { it.copy(lockedTargetId = selectedTarget.id) }
                haptic.performLockHaptic()
            }
        }
    }

    fun unlockTarget() {
        if (_uiState.value.lockedTargetId != null) {
            analyzer.lockedTargetId = null
            _uiState.update { it.copy(lockedTargetId = null) }
            haptic.performUnlockHaptic()
        }
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
