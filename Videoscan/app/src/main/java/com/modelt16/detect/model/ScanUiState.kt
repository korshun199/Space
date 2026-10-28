// Autor :  Sinelnikov Oleg 2026
// ModelT16 Detect - Состояние UI и телеметрия сканера

package com.modelt16.detect.model

data class ScanUiState(
    val hasCameraPermission: Boolean = false,
    val fps: Int = 0,
    val latencyMs: Long = 0L,
    val filter: ScanFilter = ScanFilter.ALL,
    val minConfidence: Float = 0.40f,
    val targets: List<DetectedTarget> = emptyList(),
    val lockedTargetId: Int? = null,
    val isTorchOn: Boolean = false,
    val isFrontCamera: Boolean = false,
    val statusMessage: String = "Поиск объектов в кадре...",
    val authorTag: String = "Autor :  Sinelnikov Oleg 2026"
) {
    val lockedTarget: DetectedTarget?
        get() = targets.firstOrNull { it.id == lockedTargetId }

    val hasLockedTarget: Boolean
        get() = lockedTargetId != null
}
