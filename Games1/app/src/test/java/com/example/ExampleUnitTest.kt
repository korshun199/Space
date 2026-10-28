package com.example

import com.example.generator.ProceduralBoardGenerator
import com.example.model.Ball
import com.example.model.GameStatus
import com.example.model.ObstacleDensity
import com.example.model.PegObstacle
import com.example.model.Pocket
import com.example.model.TableBounds
import com.example.model.Vector2D
import com.example.physics.RigidPhysicsEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun vector_reflection_isCorrect() {
        val incoming = Vector2D(10f, 10f)
        val normal = Vector2D(0f, -1f) // horizontal surface pointing up
        val reflected = incoming.reflect(normal, restitution = 1.0f, friction = 0f)

        assertEquals(10f, reflected.x, 0.001f)
        assertEquals(-10f, reflected.y, 0.001f)
    }

    @Test
    fun peg_collision_reflectsBall() {
        val pocket = Pocket(Vector2D(250f, 800f))
        val bounds = TableBounds(500f, 1000f, pocket)
        val peg = PegObstacle(id = 1, center = Vector2D(250f, 300f), radius = 15f)
        val ball = Ball(position = Vector2D(250f, 280f), velocity = Vector2D(0f, 100f), radius = 10f)

        val result = RigidPhysicsEngine.step(
            ball = ball,
            bounds = bounds,
            obstacles = listOf(peg),
            gravity = Vector2D(0f, 0f),
            dt = 0.02f,
            currentStatus = GameStatus.RUNNING
        )

        // Ball should have bounced upward (negative vy)
        assertTrue(result.updatedBall.velocity.y < 0f)
        assertTrue(result.didBounce)
    }

    @Test
    fun pocket_capture_triggersWon() {
        val pocket = Pocket(Vector2D(250f, 800f), holeRadius = 30f, captureRadius = 50f)
        val bounds = TableBounds(500f, 850f, pocket)
        val ball = Ball(position = Vector2D(250f, 795f), velocity = Vector2D(0f, 50f), radius = 15f)

        val result = RigidPhysicsEngine.step(
            ball = ball,
            bounds = bounds,
            obstacles = emptyList(),
            gravity = Vector2D(0f, 0f),
            dt = 0.02f,
            currentStatus = GameStatus.RUNNING
        )

        assertEquals(GameStatus.WON, result.newStatus)
    }

    @Test
    fun out_of_bounds_triggersMissed() {
        val pocket = Pocket(Vector2D(250f, 800f), holeRadius = 25f, captureRadius = 40f)
        val bounds = TableBounds(500f, 800f, pocket)
        // Ball at bottom far from pocket center
        val ball = Ball(position = Vector2D(50f, 790f), velocity = Vector2D(0f, 200f), radius = 15f)

        val result = RigidPhysicsEngine.step(
            ball = ball,
            bounds = bounds,
            obstacles = emptyList(),
            gravity = Vector2D(0f, 100f),
            dt = 0.02f,
            currentStatus = GameStatus.RUNNING
        )

        assertEquals(GameStatus.MISSED, result.newStatus)
    }

    @Test
    fun board_generator_generatesObstacles() {
        val (bounds, obstacles) = ProceduralBoardGenerator.generateBoard(
            width = 400f,
            height = 800f,
            density = ObstacleDensity.MEDIUM
        )

        assertTrue(obstacles.isNotEmpty())
        assertEquals(400f, bounds.width)
        assertEquals(800f, bounds.height)
    }
}
