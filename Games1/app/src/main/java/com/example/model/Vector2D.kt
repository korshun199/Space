package com.example.model

import kotlin.math.sqrt

data class Vector2D(
    val x: Float = 0f,
    val y: Float = 0f
) {
    operator fun plus(other: Vector2D) = Vector2D(x + other.x, y + other.y)
    operator fun minus(other: Vector2D) = Vector2D(x - other.x, y - other.y)
    operator fun times(scalar: Float) = Vector2D(x * scalar, y * scalar)
    operator fun div(scalar: Float) = if (scalar != 0f) Vector2D(x / scalar, y / scalar) else Vector2D()

    fun lengthSquared(): Float = x * x + y * y
    fun length(): Float = sqrt(lengthSquared())

    fun normalized(): Vector2D {
        val len = length()
        return if (len > 0.0001f) this / len else Vector2D()
    }

    fun dot(other: Vector2D): Float = x * other.x + y * other.y

    fun distanceTo(other: Vector2D): Float = (this - other).length()

    fun distanceSquaredTo(other: Vector2D): Float = (this - other).lengthSquared()

    /**
     * Reflect vector v against unit normal n: v' = v - 2*(v.dot(n))*n
     * With restitution e (elasticity): normal component scaled by -e, tangent by (1 - friction)
     */
    fun reflect(normal: Vector2D, restitution: Float = 0.85f, friction: Float = 0.02f): Vector2D {
        val n = normal.normalized()
        val vDotN = this.dot(n)
        val normalComp = n * vDotN
        val tangentComp = this - normalComp
        return tangentComp * (1f - friction) - normalComp * restitution
    }
}
