package com.example.model

import kotlin.math.cos
import kotlin.math.sin

sealed interface Obstacle {
    val id: Int
    val restitution: Float
}

/**
 * Rigid cylindrical peg (Plinko pin).
 */
data class PegObstacle(
    override val id: Int,
    val center: Vector2D,
    val radius: Float = 14f,
    override val restitution: Float = 0.85f
) : Obstacle

/**
 * Rigid triangular deflector wedge.
 */
data class WedgeObstacle(
    override val id: Int,
    val p1: Vector2D,
    val p2: Vector2D,
    val p3: Vector2D,
    override val restitution: Float = 0.82f
) : Obstacle {
    val edges: List<LineSegment> = listOf(
        LineSegment(p1, p2),
        LineSegment(p2, p3),
        LineSegment(p3, p1)
    )
    val center: Vector2D = Vector2D(
        (p1.x + p2.x + p3.x) / 3f,
        (p1.y + p2.y + p3.y) / 3f
    )
}

/**
 * Rigid straight tunnel with entrance, exit, and two parallel walls.
 */
data class TunnelObstacle(
    override val id: Int,
    val center: Vector2D,
    val width: Float = 55f,   // Internal corridor width
    val length: Float = 140f, // Length of the tunnel
    val angleRad: Float = 0f, // Orientation (0 = vertical)
    override val restitution: Float = 0.80f
) : Obstacle {
    // Direction along tunnel length
    private val forward = Vector2D(sin(angleRad), cos(angleRad))
    // Perpendicular vector across width
    private val right = Vector2D(cos(angleRad), -sin(angleRad))

    val halfLen = length / 2f
    val halfWidth = width / 2f

    // Left wall: from entrance to exit
    val leftWall: LineSegment = LineSegment(
        center - right * halfWidth - forward * halfLen,
        center - right * halfWidth + forward * halfLen
    )

    // Right wall: from entrance to exit
    val rightWall: LineSegment = LineSegment(
        center + right * halfWidth - forward * halfLen,
        center + right * halfWidth + forward * halfLen
    )

    val edges: List<LineSegment> = listOf(leftWall, rightWall)

    val entranceCenter: Vector2D = center - forward * halfLen
    val exitCenter: Vector2D = center + forward * halfLen
}
