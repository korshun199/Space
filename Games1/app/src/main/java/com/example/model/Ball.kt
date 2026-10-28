package com.example.model

data class Ball(
    val position: Vector2D = Vector2D(),
    val velocity: Vector2D = Vector2D(),
    val radius: Float = 17f,
    val mass: Float = 1.0f
)
