// Autor :  Sinelnikov Oleg 2026
// ModelT16 Detect - ViewModel управления состоянием слежения и камерой

package com.modelt16.detect.ui

import android.app.Application
import android.graphics.RectF
import androidx.camera.core.Camera
import androidx.lifecycle.AndroidViewModel
import com.modelt16.detect.analyzer.ObjectDetectionAnalyzer
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

    private val _trackedBox = MutableStateFlow<RectF?>(null)
    val trackedBox: StateFlow<RectF?> = _trackedBox.asStateFlow()

    val analyzer = ObjectDetectionAnalyzer { box ->
        _trackedBox.value = box
    }

    fun onCameraBound(camera: Camera) {
        this.boundCamera = camera
    }

    fun onCameraPermissionResult(granted: Boolean) {
        _uiState.update { it.copy(hasCameraPermission = granted) }
    }

    fun onViewSizeChanged(w: Float, h: Float) {
        analyzer.viewWidth = w
        analyzer.viewHeight = h
    }

    /**
     * Запомнить объект внутри рамки и начать слежение
     */
    fun lock(frame: RectF) {
        analyzer.lockAt(frame)
        haptic.performLockHaptic()
    }

    /**
     * Сбросить слежение
     */
    fun unlock() {
        analyzer.unlock()
        _trackedBox.value = null
        haptic.performUnlockHaptic()
    }

    override fun onCleared() {
        super.onCleared()
        analyzer.close()
    }
}
