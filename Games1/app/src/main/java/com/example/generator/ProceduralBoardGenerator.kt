package com.example.generator

import com.example.model.ArcWallObstacle
import com.example.model.BoxObstacle
import com.example.model.BumperObstacle
import com.example.model.CrossObstacle
import com.example.model.DiamondObstacle
import com.example.model.FunnelObstacle
import com.example.model.HexagonObstacle
import com.example.model.Obstacle
import com.example.model.ObstacleDensity
import com.example.model.PegObstacle
import com.example.model.PillObstacle
import com.example.model.Pocket
import com.example.model.SpinnerObstacle
import com.example.model.StarObstacle
import com.example.model.TableBounds
import com.example.model.TunnelObstacle
import com.example.model.Vector2D
import com.example.model.WedgeObstacle
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

object ProceduralBoardGenerator {

    /**
     * Генерирует процедурное поле на ВЕСЬ экран (True Edge-to-Edge)
     * с богатым набором разнообразных геометрических фигур.
     */
    fun generateBoard(
        width: Float,
        height: Float,
        density: ObstacleDensity,
        ballRadius: Float = 17f,
        seed: Long = System.currentTimeMillis()
    ): Pair<TableBounds, List<Obstacle>> {
        val random = Random(seed)

        val pocketHoleRadius = ballRadius * 3.2f
        val pocketCaptureRadius = ballRadius * 4.6f
        val pocketX = width / 2f
        val pocketY = height - (pocketHoleRadius + 16f)

        val pocket = Pocket(
            center = Vector2D(pocketX, pocketY),
            holeRadius = pocketHoleRadius,
            captureRadius = pocketCaptureRadius,
            maxCaptureSpeed = 680f,
            pullStrength = 1450f
        )
        val bounds = TableBounds(width, height, pocket)

        val obstacles = mutableListOf<Obstacle>()
        var nextId = 1

        val topMargin = height * 0.08f
        val bottomMargin = pocketY - pocketCaptureRadius - 20f
        val horizontalMargin = width * 0.05f
        val minObstacleSpacing = ballRadius * 2.8f

        val occupiedZones = mutableListOf<Pair<Vector2D, Float>>()

        fun isAreaFree(center: Vector2D, radius: Float): Boolean {
            if (center.distanceTo(pocket.center) < (pocket.captureRadius + radius + 15f)) return false
            if (center.x - radius < horizontalMargin || center.x + radius > width - horizontalMargin) return false
            if (center.y - radius < topMargin || center.y + radius > bottomMargin) return false
            return occupiedZones.none { (pos, r) -> pos.distanceTo(center) < (r + radius + minObstacleSpacing) }
        }

        val targetFeatureCount = when (density) {
            ObstacleDensity.LOW -> 10
            ObstacleDensity.MEDIUM -> 18
            ObstacleDensity.HIGH -> 26
        }

        var placedCount = 0
        var attempts = 0
        while (placedCount < targetFeatureCount && attempts < 400) {
            attempts++
            val cx = horizontalMargin + 30f + random.nextFloat() * (width - 2 * horizontalMargin - 60f)
            val cy = topMargin + random.nextFloat() * (bottomMargin - topMargin)
            val center = Vector2D(cx, cy)
            val angleRad = random.nextFloat() * 2f * PI.toFloat()

            val shapeType = random.nextInt(12)
            when (shapeType) {
                0 -> {
                    val r = 24f + random.nextFloat() * 6f
                    if (isAreaFree(center, r)) {
                        obstacles.add(BumperObstacle(nextId++, center, r, restitution = 1.35f))
                        occupiedZones.add(center to r)
                        placedCount++
                    }
                }
                1 -> {
                    val r = 26f + random.nextFloat() * 8f
                    if (isAreaFree(center, r)) {
                        obstacles.add(HexagonObstacle(nextId++, center, r, angleRad))
                        occupiedZones.add(center to r)
                        placedCount++
                    }
                }
                2 -> {
                    val w = 65f + random.nextFloat() * 45f
                    val h = 18f + random.nextFloat() * 8f
                    val boundR = (w / 2f) + 5f
                    if (isAreaFree(center, boundR)) {
                        obstacles.add(BoxObstacle(nextId++, center, w, h, angleRad))
                        occupiedZones.add(center to boundR)
                        placedCount++
                    }
                }
                3 -> {
                    val rx = 24f + random.nextFloat() * 10f
                    val ry = 36f + random.nextFloat() * 12f
                    val boundR = maxOf(rx, ry)
                    if (isAreaFree(center, boundR)) {
                        obstacles.add(DiamondObstacle(nextId++, center, rx, ry, angleRad))
                        occupiedZones.add(center to boundR)
                        placedCount++
                    }
                }
                4 -> {
                    val bladeL = 36f + random.nextFloat() * 10f
                    if (isAreaFree(center, bladeL)) {
                        obstacles.add(SpinnerObstacle(nextId++, center, bladeLength = bladeL, angleRad = angleRad))
                        occupiedZones.add(center to bladeL)
                        placedCount++
                    }
                }
                5 -> {
                    val len = 65f + random.nextFloat() * 25f
                    val rad = 14f + random.nextFloat() * 4f
                    val boundR = (len / 2f) + rad
                    if (isAreaFree(center, boundR)) {
                        obstacles.add(PillObstacle(nextId++, center, len, rad, angleRad))
                        occupiedZones.add(center to boundR)
                        placedCount++
                    }
                }
                6 -> {
                    val outR = 30f + random.nextFloat() * 8f
                    if (isAreaFree(center, outR)) {
                        obstacles.add(StarObstacle(nextId++, center, outerRadius = outR, innerRadius = outR * 0.45f, angleRad = angleRad))
                        occupiedZones.add(center to outR)
                        placedCount++
                    }
                }
                7 -> {
                    val r = 40f + random.nextFloat() * 14f
                    if (isAreaFree(center, r)) {
                        obstacles.add(ArcWallObstacle(nextId++, center, radius = r, angleRad = angleRad))
                        occupiedZones.add(center to r)
                        placedCount++
                    }
                }
                8 -> {
                    val armL = 46f + random.nextFloat() * 12f
                    if (isAreaFree(center, armL / 2f + 5f)) {
                        obstacles.add(CrossObstacle(nextId++, center, armLength = armL, angleRad = angleRad))
                        occupiedZones.add(center to armL / 2f)
                        placedCount++
                    }
                }
                9 -> {
                    val boundR = 48f
                    if (isAreaFree(center, boundR)) {
                        obstacles.add(FunnelObstacle(nextId++, center, angleRad = (random.nextFloat() - 0.5f) * 0.4f))
                        occupiedZones.add(center to boundR)
                        placedCount++
                    }
                }
                10 -> {
                    val size = 26f + random.nextFloat() * 12f
                    if (isAreaFree(center, size)) {
                        val baseA = angleRad
                        val p1 = center + Vector2D(cos(baseA), sin(baseA)) * size
                        val p2 = center + Vector2D(cos(baseA + 2.094f), sin(baseA + 2.094f)) * size
                        val p3 = center + Vector2D(cos(baseA + 4.188f), sin(baseA + 4.188f)) * size
                        obstacles.add(WedgeObstacle(nextId++, p1, p2, p3))
                        occupiedZones.add(center to size)
                        placedCount++
                    }
                }
                11 -> {
                    val boundR = 60f
                    if (isAreaFree(center, boundR)) {
                        obstacles.add(
                            TunnelObstacle(
                                id = nextId++,
                                center = center,
                                width = ballRadius * 3.4f,
                                length = 110f,
                                angleRad = (random.nextFloat() - 0.5f) * 0.35f
                            )
                        )
                        occupiedZones.add(center to boundR)
                        placedCount++
                    }
                }
            }
        }

        val numPegs = when (density) {
            ObstacleDensity.LOW -> 14
            ObstacleDensity.MEDIUM -> 24
            ObstacleDensity.HIGH -> 36
        }

        var pegsPlaced = 0
        attempts = 0
        while (pegsPlaced < numPegs && attempts < 300) {
            attempts++
            val px = horizontalMargin + 15f + random.nextFloat() * (width - 2 * horizontalMargin - 30f)
            val py = topMargin + random.nextFloat() * (bottomMargin - topMargin)
            val center = Vector2D(px, py)
            val pegR = 11f + random.nextFloat() * 4f

            if (isAreaFree(center, pegR + 6f)) {
                obstacles.add(PegObstacle(nextId++, center, radius = pegR))
                occupiedZones.add(center to pegR + 8f)
                pegsPlaced++
            }
        }

        return bounds to obstacles
    }
}
