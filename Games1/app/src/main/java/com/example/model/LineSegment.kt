package com.example.model

import kotlin.math.max
import kotlin.math.min

/**
 * Line segment defined by start and end points p1 and p2.
 */
data class LineSegment(
    val p1: Vector2D,
    val p2: Vector2D
) {
    val vector: Vector2D get() = p2 - p1
    val length: Float get() = vector.length()

    /**
     * Finds the closest point on this segment to point p.
     */
    fun closestPointTo(p: Vector2D): Vector2D {
        val seg = p2 - p1
        val lenSq = seg.lengthSquared()
        if (lenSq < 0.0001f) return p1

        // Project vector (p - p1) onto seg
        val t = max(0f, min(1f, (p - p1).dot(seg) / lenSq))
        return p1 + seg * t
    }

    /**
     * Computes the outward normal perpendicular to the segment (rotated 90 deg clockwise).
     */
    fun normal(): Vector2D {
        val v = vector.normalized()
        return Vector2D(v.y, -v.x)
    }
}
