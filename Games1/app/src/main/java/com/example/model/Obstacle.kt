package com.example.model

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

sealed interface Obstacle {
    val id: Int
    val restitution: Float
}

sealed interface PolygonObstacle : Obstacle {
    val center: Vector2D
    val edges: List<LineSegment>
    val vertices: List<Vector2D>
}

data class PegObstacle(
    override val id: Int,
    val center: Vector2D,
    val radius: Float = 14f,
    override val restitution: Float = 0.85f
) : Obstacle

data class BumperObstacle(
    override val id: Int,
    val center: Vector2D,
    val radius: Float = 22f,
    override val restitution: Float = 1.35f
) : Obstacle

data class WedgeObstacle(
    override val id: Int,
    val p1: Vector2D,
    val p2: Vector2D,
    val p3: Vector2D,
    override val restitution: Float = 0.82f
) : PolygonObstacle {
    override val vertices: List<Vector2D> = listOf(p1, p2, p3)
    override val edges: List<LineSegment> = listOf(
        LineSegment(p1, p2),
        LineSegment(p2, p3),
        LineSegment(p3, p1)
    )
    override val center: Vector2D = Vector2D(
        (p1.x + p2.x + p3.x) / 3f,
        (p1.y + p2.y + p3.y) / 3f
    )
}

data class BoxObstacle(
    override val id: Int,
    override val center: Vector2D,
    val width: Float = 90f,
    val height: Float = 24f,
    val angleRad: Float = 0f,
    override val restitution: Float = 0.84f
) : PolygonObstacle {
    override val vertices: List<Vector2D>
    override val edges: List<LineSegment>

    init {
        val hw = width / 2f
        val hh = height / 2f
        val cosA = cos(angleRad)
        val sinA = sin(angleRad)

        fun rotate(x: Float, y: Float): Vector2D = Vector2D(
            center.x + (x * cosA - y * sinA),
            center.y + (x * sinA + y * cosA)
        )

        val v1 = rotate(-hw, -hh)
        val v2 = rotate(hw, -hh)
        val v3 = rotate(hw, hh)
        val v4 = rotate(-hw, hh)

        vertices = listOf(v1, v2, v3, v4)
        edges = listOf(
            LineSegment(v1, v2),
            LineSegment(v2, v3),
            LineSegment(v3, v4),
            LineSegment(v4, v1)
        )
    }
}

data class DiamondObstacle(
    override val id: Int,
    override val center: Vector2D,
    val radiusX: Float = 28f,
    val radiusY: Float = 42f,
    val angleRad: Float = 0f,
    override val restitution: Float = 0.85f
) : PolygonObstacle {
    override val vertices: List<Vector2D>
    override val edges: List<LineSegment>

    init {
        val cosA = cos(angleRad)
        val sinA = sin(angleRad)

        fun rotate(x: Float, y: Float): Vector2D = Vector2D(
            center.x + (x * cosA - y * sinA),
            center.y + (x * sinA + y * cosA)
        )

        val top = rotate(0f, -radiusY)
        val right = rotate(radiusX, 0f)
        val bottom = rotate(0f, radiusY)
        val left = rotate(-radiusX, 0f)

        vertices = listOf(top, right, bottom, left)
        edges = listOf(
            LineSegment(top, right),
            LineSegment(right, bottom),
            LineSegment(bottom, left),
            LineSegment(left, top)
        )
    }
}

data class HexagonObstacle(
    override val id: Int,
    override val center: Vector2D,
    val radius: Float = 28f,
    val angleRad: Float = 0f,
    override val restitution: Float = 0.88f
) : PolygonObstacle {
    override val vertices: List<Vector2D>
    override val edges: List<LineSegment>

    init {
        val pts = mutableListOf<Vector2D>()
        for (i in 0 until 6) {
            val a = angleRad + (i * PI.toFloat() / 3f)
            pts.add(Vector2D(center.x + radius * cos(a), center.y + radius * sin(a)))
        }
        vertices = pts
        edges = List(6) { i -> LineSegment(pts[i], pts[(i + 1) % 6]) }
    }
}

data class CrossObstacle(
    override val id: Int,
    override val center: Vector2D,
    val armLength: Float = 48f,
    val armThickness: Float = 16f,
    val angleRad: Float = 0f,
    override val restitution: Float = 0.82f
) : PolygonObstacle {
    override val vertices: List<Vector2D>
    override val edges: List<LineSegment>

    init {
        val l = armLength / 2f
        val t = armThickness / 2f
        val cosA = cos(angleRad)
        val sinA = sin(angleRad)

        fun r(x: Float, y: Float): Vector2D = Vector2D(
            center.x + (x * cosA - y * sinA),
            center.y + (x * sinA + y * cosA)
        )

        val pts = listOf(
            r(-t, -l), r(t, -l), r(t, -t), r(l, -t),
            r(l, t), r(t, t), r(t, l), r(-t, l),
            r(-t, t), r(-l, t), r(-l, -t), r(-t, -t)
        )
        vertices = pts
        edges = List(pts.size) { i -> LineSegment(pts[i], pts[(i + 1) % pts.size]) }
    }
}

data class StarObstacle(
    override val id: Int,
    override val center: Vector2D,
    val outerRadius: Float = 34f,
    val innerRadius: Float = 16f,
    val angleRad: Float = 0f,
    override val restitution: Float = 0.90f
) : PolygonObstacle {
    override val vertices: List<Vector2D>
    override val edges: List<LineSegment>

    init {
        val pts = mutableListOf<Vector2D>()
        for (i in 0 until 10) {
            val r = if (i % 2 == 0) outerRadius else innerRadius
            val a = angleRad + (i * PI.toFloat() / 5f)
            pts.add(Vector2D(center.x + r * cos(a), center.y + r * sin(a)))
        }
        vertices = pts
        edges = List(10) { i -> LineSegment(pts[i], pts[(i + 1) % 10]) }
    }
}

data class FunnelObstacle(
    override val id: Int,
    override val center: Vector2D,
    val wideWidth: Float = 110f,
    val narrowWidth: Float = 48f,
    val length: Float = 75f,
    val angleRad: Float = 0f,
    override val restitution: Float = 0.85f
) : PolygonObstacle {
    override val vertices: List<Vector2D>
    override val edges: List<LineSegment>

    init {
        val hwTop = wideWidth / 2f
        val hwBot = narrowWidth / 2f
        val hl = length / 2f
        val cosA = cos(angleRad)
        val sinA = sin(angleRad)

        val w1 = center + Vector2D(-hwTop * cosA + hl * sinA, -hwTop * sinA - hl * cosA)
        val w2 = center + Vector2D(-hwBot * cosA - hl * sinA, -hwBot * sinA + hl * cosA)
        val e1 = center + Vector2D(hwTop * cosA + hl * sinA, hwTop * sinA - hl * cosA)
        val e2 = center + Vector2D(hwBot * cosA - hl * sinA, hwBot * sinA + hl * cosA)

        vertices = listOf(w1, w2, e2, e1)
        edges = listOf(LineSegment(w1, w2), LineSegment(e1, e2))
    }
}

data class ArcWallObstacle(
    override val id: Int,
    override val center: Vector2D,
    val radius: Float = 45f,
    val arcSpanRad: Float = PI.toFloat() * 0.75f,
    val angleRad: Float = 0f,
    override val restitution: Float = 0.92f
) : PolygonObstacle {
    override val vertices: List<Vector2D>
    override val edges: List<LineSegment>

    init {
        val steps = 7
        val pts = mutableListOf<Vector2D>()
        val startA = angleRad - arcSpanRad / 2f
        val stepA = arcSpanRad / steps
        for (i in 0..steps) {
            val a = startA + i * stepA
            pts.add(Vector2D(center.x + radius * cos(a), center.y + radius * sin(a)))
        }
        vertices = pts
        edges = List(steps) { i -> LineSegment(pts[i], pts[i + 1]) }
    }
}

data class SpinnerObstacle(
    override val id: Int,
    override val center: Vector2D,
    val bladeLength: Float = 42f,
    val bladeThickness: Float = 10f,
    val angleRad: Float = 0f,
    override val restitution: Float = 1.15f
) : PolygonObstacle {
    override val vertices: List<Vector2D>
    override val edges: List<LineSegment>

    init {
        val edgesList = mutableListOf<LineSegment>()
        val ptsList = mutableListOf<Vector2D>()
        val blades = 3
        val hw = bladeThickness / 2f

        for (b in 0 until blades) {
            val baseAngle = angleRad + (b * 2f * PI.toFloat() / blades)
            val forward = Vector2D(cos(baseAngle), sin(baseAngle))
            val right = Vector2D(-sin(baseAngle), cos(baseAngle))

            val pLeft = center + right * hw
            val pRight = center - right * hw
            val pTip = center + forward * bladeLength

            ptsList.addAll(listOf(pLeft, pTip, pRight))
            edgesList.add(LineSegment(pLeft, pTip))
            edgesList.add(LineSegment(pTip, pRight))
        }

        vertices = ptsList
        edges = edgesList
    }
}

data class PillObstacle(
    override val id: Int,
    override val center: Vector2D,
    val length: Float = 75f,
    val radius: Float = 16f,
    val angleRad: Float = 0f,
    override val restitution: Float = 1.05f
) : PolygonObstacle {
    override val vertices: List<Vector2D>
    override val edges: List<LineSegment>

    init {
        val hl = length / 2f
        val cosA = cos(angleRad)
        val sinA = sin(angleRad)

        fun r(x: Float, y: Float) = Vector2D(
            center.x + (x * cosA - y * sinA),
            center.y + (x * sinA + y * cosA)
        )

        val p1 = r(-hl, -radius)
        val p2 = r(hl, -radius)
        val p3 = r(hl, radius)
        val p4 = r(-hl, radius)

        vertices = listOf(p1, p2, p3, p4)
        edges = listOf(
            LineSegment(p1, p2),
            LineSegment(p2, p3),
            LineSegment(p3, p4),
            LineSegment(p4, p1)
        )
    }
}

data class TunnelObstacle(
    override val id: Int,
    val center: Vector2D,
    val width: Float = 55f,
    val length: Float = 140f,
    val angleRad: Float = 0f,
    override val restitution: Float = 0.80f
) : Obstacle {
    private val forward = Vector2D(sin(angleRad), cos(angleRad))
    private val right = Vector2D(cos(angleRad), -sin(angleRad))

    val halfLen = length / 2f
    val halfWidth = width / 2f

    val leftWall: LineSegment = LineSegment(
        center - right * halfWidth - forward * halfLen,
        center - right * halfWidth + forward * halfLen
    )

    val rightWall: LineSegment = LineSegment(
        center + right * halfWidth - forward * halfLen,
        center + right * halfWidth + forward * halfLen
    )

    val edges: List<LineSegment> = listOf(leftWall, rightWall)

    val entranceCenter: Vector2D = center - forward * halfLen
    val exitCenter: Vector2D = center + forward * halfLen
}
