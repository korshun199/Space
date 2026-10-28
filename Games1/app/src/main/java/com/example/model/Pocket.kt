package com.example.model

data class Pocket(
    val center: Vector2D,
    val holeRadius: Float = 26f,
    val captureRadius: Float = 44f,
    val maxCaptureSpeed: Float = 420f,
    val pullStrength: Float = 950f
)
