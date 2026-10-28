package com.example.model

data class TableBounds(
    val width: Float,
    val height: Float,
    val pocket: Pocket
) {
    val topWall = LineSegment(Vector2D(0f, 0f), Vector2D(width, 0f))
    val leftWall = LineSegment(Vector2D(0f, 0f), Vector2D(0f, height))
    val rightWall = LineSegment(Vector2D(width, 0f), Vector2D(width, height))

    val rigidWalls: List<LineSegment> = listOf(topWall, leftWall, rightWall)

    /**
     * Checks if ball position has fallen off the bottom edge outside the pocket area.
     */
    fun isBallMissed(ball: Ball): Boolean {
        // If ball center is at or below the bottom line
        if (ball.position.y >= height - ball.radius * 0.3f) {
            val distToPocket = (ball.position - pocket.center).length()
            // If it is not within the pocket's capture sphere, it fell off the table
            if (distToPocket > pocket.captureRadius) {
                return true
            }
        }
        return false
    }
}
