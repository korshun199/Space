package com.example.physics

import com.example.model.Ball
import com.example.model.GameStatus
import com.example.model.Obstacle
import com.example.model.PegObstacle
import com.example.model.Pocket
import com.example.model.TableBounds
import com.example.model.TunnelObstacle
import com.example.model.Vector2D
import com.example.model.WedgeObstacle
import kotlin.math.min

data class PhysicsStepResult(
    val updatedBall: Ball,
    val newStatus: GameStatus,
    val bounceImpulse: Float = 0f,
    val didBounce: Boolean = false
)

object RigidPhysicsEngine {

    private const val ROLLING_FRICTION = 0.65f // gentle deceleration on flat surface
    private const val MAX_SPEED = 2200f        // px/s limit for numerical stability
    private const val WALL_RESTITUTION = 0.86f // snappy rigid rebound
    private const val SUB_STEPS = 4

    /**
     * Advances the ball physics by dt seconds under the given gravity vector.
     */
    fun step(
        ball: Ball,
        bounds: TableBounds,
        obstacles: List<Obstacle>,
        gravity: Vector2D,
        dt: Float,
        currentStatus: GameStatus
    ): PhysicsStepResult {
        if (currentStatus != GameStatus.RUNNING) {
            return PhysicsStepResult(ball, currentStatus)
        }

        // Clamp dt to avoid huge jumps on frame drops
        val safeDt = min(dt, 0.04f)
        val subDt = safeDt / SUB_STEPS

        var currentPos = ball.position
        var currentVel = ball.velocity
        val radius = ball.radius
        val pocket = bounds.pocket

        var maxBounceImpulse = 0f
        var bouncedThisFrame = false

        for (step in 0 until SUB_STEPS) {
            // 1. Pocket gravity well (active within captureRadius)
            val toPocket = pocket.center - currentPos
            val distToPocket = toPocket.length()

            if (distToPocket < pocket.captureRadius && distToPocket > 0.001f) {
                // Centripetal attraction force towards hole center
                val pullRatio = 1f - (distToPocket / pocket.captureRadius)
                val pullAccel = toPocket.normalized() * (pocket.pullStrength * pullRatio)
                currentVel += pullAccel * subDt

                // Check capture threshold
                if (distToPocket < pocket.holeRadius * 0.75f) {
                    val speed = currentVel.length()
                    if (speed < pocket.maxCaptureSpeed) {
                        // Sucked into hole! Center the ball and complete
                        val finalBall = ball.copy(
                            position = pocket.center,
                            velocity = Vector2D()
                        )
                        return PhysicsStepResult(
                            updatedBall = finalBall,
                            newStatus = GameStatus.WON,
                            bounceImpulse = 1.0f,
                            didBounce = true
                        )
                    } else {
                        // Moving too fast! Lips out around rim, loses some energy
                        currentVel *= 0.90f
                    }
                }
            }

            // 2. Accelerate by gravity vector (from phone tilt) and apply rolling friction
            currentVel += gravity * subDt
            currentVel *= (1f - ROLLING_FRICTION * subDt)

            // Speed cap
            val speedSq = currentVel.lengthSquared()
            if (speedSq > MAX_SPEED * MAX_SPEED) {
                currentVel = currentVel.normalized() * MAX_SPEED
            }

            // 3. Integrate position
            currentPos += currentVel * subDt

            // 4. Rigid Table Boundaries
            // Top wall
            if (currentPos.y - radius < 0f) {
                currentPos = Vector2D(currentPos.x, radius)
                val impulse = kotlin.math.abs(currentVel.y)
                currentVel = Vector2D(currentVel.x * 0.98f, -currentVel.y * WALL_RESTITUTION)
                if (impulse > 25f) {
                    bouncedThisFrame = true
                    maxBounceImpulse = maxOf(maxBounceImpulse, impulse)
                }
            }

            // Left wall
            if (currentPos.x - radius < 0f) {
                currentPos = Vector2D(radius, currentPos.y)
                val impulse = kotlin.math.abs(currentVel.x)
                currentVel = Vector2D(-currentVel.x * WALL_RESTITUTION, currentVel.y * 0.98f)
                if (impulse > 25f) {
                    bouncedThisFrame = true
                    maxBounceImpulse = maxOf(maxBounceImpulse, impulse)
                }
            }

            // Right wall
            if (currentPos.x + radius > bounds.width) {
                currentPos = Vector2D(bounds.width - radius, currentPos.y)
                val impulse = kotlin.math.abs(currentVel.x)
                currentVel = Vector2D(-currentVel.x * WALL_RESTITUTION, currentVel.y * 0.98f)
                if (impulse > 25f) {
                    bouncedThisFrame = true
                    maxBounceImpulse = maxOf(maxBounceImpulse, impulse)
                }
            }

            // Bottom edge: check for Missed
            if (currentPos.y + radius >= bounds.height) {
                // If not within the pocket's capture sphere, ball is lost off the bottom
                val distFromPocketCenter = (currentPos - pocket.center).length()
                if (distFromPocketCenter > pocket.captureRadius * 0.9f) {
                    val finalBall = ball.copy(position = currentPos, velocity = currentVel)
                    return PhysicsStepResult(
                        updatedBall = finalBall,
                        newStatus = GameStatus.MISSED,
                        bounceImpulse = 0.5f,
                        didBounce = true
                    )
                }
            }

            // 5. Collisions with Rigid Obstacles
            for (obstacle in obstacles) {
                when (obstacle) {
                    is PegObstacle -> {
                        val delta = currentPos - obstacle.center
                        val dist = delta.length()
                        val minDist = radius + obstacle.radius
                        if (dist < minDist && dist > 0.0001f) {
                            val normal = delta / dist
                            val penetration = minDist - dist
                            // Separate immediately to avoid stickiness
                            currentPos += normal * penetration

                            val incomingNormalSpeed = currentVel.dot(normal)
                            if (incomingNormalSpeed < 0f) {
                                val impulse = kotlin.math.abs(incomingNormalSpeed)
                                currentVel = currentVel.reflect(normal, obstacle.restitution)
                                if (impulse > 20f) {
                                    bouncedThisFrame = true
                                    maxBounceImpulse = maxOf(maxBounceImpulse, impulse)
                                }
                            }
                        }
                    }

                    is WedgeObstacle -> {
                        for (edge in obstacle.edges) {
                            val cp = edge.closestPointTo(currentPos)
                            val delta = currentPos - cp
                            val dist = delta.length()
                            if (dist < radius && dist > 0.0001f) {
                                val normal = delta / dist
                                val penetration = radius - dist
                                currentPos += normal * penetration

                                val incomingNormalSpeed = currentVel.dot(normal)
                                if (incomingNormalSpeed < 0f) {
                                    val impulse = kotlin.math.abs(incomingNormalSpeed)
                                    currentVel = currentVel.reflect(normal, obstacle.restitution)
                                    if (impulse > 20f) {
                                        bouncedThisFrame = true
                                        maxBounceImpulse = maxOf(maxBounceImpulse, impulse)
                                    }
                                }
                            }
                        }
                    }

                    is TunnelObstacle -> {
                        for (edge in obstacle.edges) {
                            val cp = edge.closestPointTo(currentPos)
                            val delta = currentPos - cp
                            val dist = delta.length()
                            if (dist < radius && dist > 0.0001f) {
                                val normal = delta / dist
                                val penetration = radius - dist
                                currentPos += normal * penetration

                                val incomingNormalSpeed = currentVel.dot(normal)
                                if (incomingNormalSpeed < 0f) {
                                    val impulse = kotlin.math.abs(incomingNormalSpeed)
                                    currentVel = currentVel.reflect(normal, obstacle.restitution)
                                    if (impulse > 20f) {
                                        bouncedThisFrame = true
                                        maxBounceImpulse = maxOf(maxBounceImpulse, impulse)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        return PhysicsStepResult(
            updatedBall = ball.copy(position = currentPos, velocity = currentVel),
            newStatus = currentStatus,
            bounceImpulse = maxBounceImpulse,
            didBounce = bouncedThisFrame
        )
    }
}
