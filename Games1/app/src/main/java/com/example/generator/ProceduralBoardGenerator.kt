package com.example.generator

import com.example.model.Obstacle
import com.example.model.ObstacleDensity
import com.example.model.PegObstacle
import com.example.model.Pocket
import com.example.model.TableBounds
import com.example.model.TunnelObstacle
import com.example.model.Vector2D
import com.example.model.WedgeObstacle
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

object ProceduralBoardGenerator {

    /**
     * Generates a procedurally placed layout of rigid obstacles, tunnels, and pegs
     * guaranteeing passable corridors towards the center-bottom pocket.
     */
    fun generateBoard(
        width: Float,
        height: Float,
        density: ObstacleDensity,
        ballRadius: Float = 17f,
        seed: Long = System.currentTimeMillis()
    ): Pair<TableBounds, List<Obstacle>> {
        val random = Random(seed)

        // Pocket is located centered at the bottom, matching the billiard pocket cutout
        val panelHeight = width * (84f / 784f)
        val pocketX = width / 2f
        val pocketY = height - (panelHeight * 0.65f)
        val pocketHoleRadius = ballRadius * 3.1f // 2x larger
        val pocketCaptureRadius = ballRadius * 4.4f
        val pocket = Pocket(
            center = Vector2D(pocketX, pocketY),
            holeRadius = pocketHoleRadius,
            captureRadius = pocketCaptureRadius,
            maxCaptureSpeed = 650f,
            pullStrength = 1400f
        )
        val bounds = TableBounds(width, height, pocket)

        val obstacles = mutableListOf<Obstacle>()
        var nextId = 1

        val topMargin = height * 0.16f
        val bottomMargin = height - panelHeight - 20f
        val horizontalMargin = width * 0.10f
        val minObstacleSpacing = ballRadius * 3.2f // Ensure ample clearance for ball (17 * 3.2 = ~54px)

        // Determine number of elements by density
        val (numTunnels, numWedges, numPegs) = when (density) {
            ObstacleDensity.LOW -> Triple(1, 3, 12)
            ObstacleDensity.MEDIUM -> Triple(2, 6, 20)
            ObstacleDensity.HIGH -> Triple(3, 9, 30)
        }

        // Occupied positions (center point and radius) to avoid overlapping
        val occupiedZones = mutableListOf<Pair<Vector2D, Float>>()

        // 1. Tunnels (chutes)
        var tunnelsPlaced = 0
        var attempts = 0
        while (tunnelsPlaced < numTunnels && attempts < 100) {
            attempts++
            val tx = horizontalMargin + random.nextFloat() * (width - 2 * horizontalMargin)
            val ty = topMargin + random.nextFloat() * (bottomMargin - topMargin - 120f)
            val angleRad = (random.nextFloat() - 0.5f) * 0.35f // Small tilt ±10 deg
            val center = Vector2D(tx, ty)
            val clearRadius = 75f

            if (occupiedZones.none { (pos, r) -> pos.distanceTo(center) < (r + clearRadius) } &&
                center.distanceTo(pocket.center) > 130f) {
                obstacles.add(
                    TunnelObstacle(
                        id = nextId++,
                        center = center,
                        width = ballRadius * 3.4f,
                        length = 130f,
                        angleRad = angleRad
                    )
                )
                occupiedZones.add(center to clearRadius)
                tunnelsPlaced++
            }
        }

        // 2. Triangular deflector wedges
        var wedgesPlaced = 0
        attempts = 0
        while (wedgesPlaced < numWedges && attempts < 150) {
            attempts++
            val wx = horizontalMargin + random.nextFloat() * (width - 2 * horizontalMargin)
            val wy = topMargin + random.nextFloat() * (bottomMargin - topMargin)
            val center = Vector2D(wx, wy)
            val size = 28f + random.nextFloat() * 14f
            val clearRadius = size + minObstacleSpacing

            if (occupiedZones.none { (pos, r) -> pos.distanceTo(center) < (r + clearRadius) } &&
                center.distanceTo(pocket.center) > 90f) {
                // Generate equilateral or slanted triangle vertices
                val baseAngle = (random.nextInt(4) * 90f) * (Math.PI.toFloat() / 180f)
                val p1 = center + Vector2D(cos(baseAngle), sin(baseAngle)) * size
                val p2 = center + Vector2D(cos(baseAngle + 2.094f), sin(baseAngle + 2.094f)) * size
                val p3 = center + Vector2D(cos(baseAngle + 4.188f), sin(baseAngle + 4.188f)) * size

                obstacles.add(
                    WedgeObstacle(
                        id = nextId++,
                        p1 = p1,
                        p2 = p2,
                        p3 = p3,
                        restitution = 0.84f
                    )
                )
                occupiedZones.add(center to clearRadius * 0.75f)
                wedgesPlaced++
            }
        }

        // 3. Pegs (Plinko pins) placed strategically across vertical rows
        val rows = when (density) {
            ObstacleDensity.LOW -> 5
            ObstacleDensity.MEDIUM -> 7
            ObstacleDensity.HIGH -> 10
        }
        val yStep = (bottomMargin - topMargin) / (rows + 1)

        for (row in 1..rows) {
            val y = topMargin + row * yStep + (random.nextFloat() - 0.5f) * 12f
            val cols = if (row % 2 == 0) 4 else 5
            val xStep = (width - 2 * horizontalMargin) / (cols + 1)

            for (col in 1..cols) {
                val x = horizontalMargin + col * xStep + (random.nextFloat() - 0.5f) * 16f
                val center = Vector2D(x, y)
                val pegRadius = 11f + random.nextFloat() * 4f

                // Avoid blocking pocket or overlapping existing tunnels/wedges
                val overlaps = occupiedZones.any { (pos, r) -> pos.distanceTo(center) < (r + pegRadius + ballRadius) }
                val tooCloseToPocket = center.distanceTo(pocket.center) < (pocket.captureRadius + 40f)

                if (!overlaps && !tooCloseToPocket && obstacles.size < (numTunnels + numWedges + numPegs)) {
                    obstacles.add(
                        PegObstacle(
                            id = nextId++,
                            center = center,
                            radius = pegRadius,
                            restitution = 0.86f
                        )
                    )
                    occupiedZones.add(center to pegRadius + minObstacleSpacing * 0.5f)
                }
            }
        }

        return bounds to obstacles
    }
}
