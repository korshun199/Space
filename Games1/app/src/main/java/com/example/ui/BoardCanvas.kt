package com.example.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.example.R
import com.example.model.Ball
import com.example.model.ArcWallObstacle
import com.example.model.BoxObstacle
import com.example.model.BumperObstacle
import com.example.model.CrossObstacle
import com.example.model.DiamondObstacle
import com.example.model.FunnelObstacle
import com.example.model.HexagonObstacle
import com.example.model.PillObstacle
import com.example.model.PolygonObstacle
import com.example.model.SpinnerObstacle
import com.example.model.StarObstacle
import com.example.model.Obstacle
import com.example.model.PegObstacle
import com.example.model.Pocket
import com.example.model.TableBounds
import com.example.model.TunnelObstacle
import com.example.model.WedgeObstacle
import com.example.ui.theme.AccentPrimary
import com.example.ui.theme.AccentPrimarySoft
import com.example.ui.theme.BallHigh
import com.example.ui.theme.BallLow
import com.example.ui.theme.BallMid
import com.example.ui.theme.BallShadow
import com.example.ui.theme.BallSpecular
import com.example.ui.theme.BoardBorder
import com.example.ui.theme.BoardGrid
import com.example.ui.theme.DangerRed
import com.example.ui.theme.PegBase
import com.example.ui.theme.PegCap
import com.example.ui.theme.PegRim
import com.example.ui.theme.SurfaceBoard
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.TunnelFill
import com.example.ui.theme.TunnelGuide
import com.example.ui.theme.WedgeFill
import com.example.ui.theme.WedgeStroke
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun BoardCanvas(
    ball: Ball,
    bounds: TableBounds,
    obstacles: List<Obstacle>,
    pocket: Pocket,
    pitchDeg: Float,
    rollDeg: Float,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        if (width <= 0f || height <= 0f) return@Canvas

        // 1. Board Background Surface & Ambient Vignette
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(SurfaceBoard, SurfaceDark),
                center = Offset(width / 2f, height / 2f),
                radius = maxOf(width, height) * 0.8f
            ),
            size = size
        )

        // Subtle precision grid lines
        val gridStep = 48f
        var x = gridStep
        while (x < width) {
            drawLine(
                color = BoardGrid,
                start = Offset(x, 0f),
                end = Offset(x, height),
                strokeWidth = 1f
            )
            x += gridStep
        }
        var y = gridStep
        while (y < height) {
            drawLine(
                color = BoardGrid,
                start = Offset(0f, y),
                end = Offset(width, y),
                strokeWidth = 1f
            )
            y += gridStep
        }

        // 2. Rigid Table Boundaries (Top, Left, Right)
        val wallThickness = 10f
        // Top Wall
        drawRect(
            color = BoardBorder,
            topLeft = Offset(0f, 0f),
            size = Size(width, wallThickness)
        )
        // Top Wall Highlight
        drawLine(
            color = Color(0x33FFFFFF),
            start = Offset(0f, wallThickness),
            end = Offset(width, wallThickness),
            strokeWidth = 1.5f
        )

        // Left Wall
        drawRect(
            color = BoardBorder,
            topLeft = Offset(0f, 0f),
            size = Size(wallThickness, height)
        )
        drawLine(
            color = Color(0x33FFFFFF),
            start = Offset(wallThickness, 0f),
            end = Offset(wallThickness, height),
            strokeWidth = 1.5f
        )

        // Right Wall
        drawRect(
            color = BoardBorder,
            topLeft = Offset(width - wallThickness, 0f),
            size = Size(wallThickness, height)
        )
        drawLine(
            color = Color(0x33FFFFFF),
            start = Offset(width - wallThickness, 0f),
            end = Offset(width - wallThickness, height),
            strokeWidth = 1.5f
        )

        // Bottom Edge: Danger Out-of-Bounds Zones (left and right of pocket)
        val holeLeftX = pocket.center.x - pocket.captureRadius
        val holeRightX = pocket.center.x + pocket.captureRadius
        val dashEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)

        // Left danger zone
        drawLine(
            color = DangerRed.copy(alpha = 0.5f),
            start = Offset(wallThickness, height - 2f),
            end = Offset(holeLeftX, height - 2f),
            strokeWidth = 4f,
            pathEffect = dashEffect
        )
        // Right danger zone
        drawLine(
            color = DangerRed.copy(alpha = 0.5f),
            start = Offset(holeRightX, height - 2f),
            end = Offset(width - wallThickness, height - 2f),
            strokeWidth = 4f,
            pathEffect = dashEffect
        )

        // 3. Draw Target Pocket in bottom center
        drawTargetPocket(pocket)

        // 4. Draw Obstacles (All 10+ geometric shapes)
        for (obstacle in obstacles) {
            when (obstacle) {
                is BumperObstacle -> drawBumper(obstacle)
                is PegObstacle -> drawPeg(obstacle)
                is TunnelObstacle -> drawTunnel(obstacle)
                is ArcWallObstacle -> drawArcWall(obstacle)
                is SpinnerObstacle -> drawSpinner(obstacle)
                is PolygonObstacle -> drawPolygonObstacle(obstacle)
            }
        }

        // 5. Draw Dynamic Specular Metallic Ball
        drawMetallicBall(ball, pitchDeg, rollDeg)
        }
        Image(
            painter = painterResource(id = R.drawable.billiard_pocket_bottom),
            contentDescription = "Бильярдная лунка",
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth(),
            contentScale = ContentScale.FillWidth
        )
    }
}

/**
 * Draws the deep center-bottom pocket with gradient well and glowing halo.
 */
private fun DrawScope.drawTargetPocket(pocket: Pocket) {
    val center = Offset(pocket.center.x, pocket.center.y)

    // Outer attraction aura
    drawCircle(
        color = AccentPrimarySoft,
        radius = pocket.captureRadius,
        center = center
    )

    // Outer glow ring
    drawCircle(
        color = AccentPrimary,
        radius = pocket.holeRadius + 6f,
        center = center,
        style = Stroke(width = 2.5f)
    )

    // Deep pocket well
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color(0xFF030508), Color(0xFF0C131D), SurfaceDark),
            center = center,
            radius = pocket.holeRadius
        ),
        radius = pocket.holeRadius,
        center = center
    )

    // Inner rim line
    drawCircle(
        color = AccentPrimary.copy(alpha = 0.7f),
        radius = pocket.holeRadius * 0.45f,
        center = center,
        style = Stroke(width = 1.5f)
    )
}

/**
 * Draws a rigid cylindrical peg with shadow and metallic top.
 */
private fun DrawScope.drawPeg(peg: PegObstacle) {
    val center = Offset(peg.center.x, peg.center.y)
    val r = peg.radius

    // Soft ambient shadow
    drawCircle(
        color = BallShadow,
        radius = r + 3.5f,
        center = center + Offset(1.5f, 2.5f)
    )

    // Base body
    drawCircle(
        color = PegBase,
        radius = r,
        center = center
    )

    // Outer rim stroke
    drawCircle(
        color = PegRim,
        radius = r,
        center = center,
        style = Stroke(width = 1.5f)
    )

    // Metallic cap
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color(0xFFCBD5E1), PegCap, PegBase),
            center = center - Offset(r * 0.25f, r * 0.25f),
            radius = r * 0.8f
        ),
        radius = r * 0.7f,
        center = center
    )
}

/**
 * Draws a rigid triangular deflector wedge with chamfers and soft shadow.
 */
private fun DrawScope.drawWedge(wedge: WedgeObstacle) {
    val path = Path().apply {
        moveTo(wedge.p1.x, wedge.p1.y)
        lineTo(wedge.p2.x, wedge.p2.y)
        lineTo(wedge.p3.x, wedge.p3.y)
        close()
    }

    // Shadow
    val shadowPath = Path().apply {
        moveTo(wedge.p1.x + 2f, wedge.p1.y + 3f)
        lineTo(wedge.p2.x + 2f, wedge.p2.y + 3f)
        lineTo(wedge.p3.x + 2f, wedge.p3.y + 3f)
        close()
    }
    drawPath(shadowPath, color = BallShadow)

    // Fill
    drawPath(path, color = WedgeFill)

    // Outer rigid stroke
    drawPath(
        path,
        color = WedgeStroke,
        style = Stroke(width = 2.5f, cap = StrokeCap.Round)
    )

    // Center pivot accent
    drawCircle(
        color = AccentPrimary.copy(alpha = 0.6f),
        radius = 3.5f,
        center = Offset(wedge.center.x, wedge.center.y)
    )
}

/**
 * Draws a guide tunnel with translucent channel and rigid rails.
 */
private fun DrawScope.drawTunnel(tunnel: TunnelObstacle) {
    // Fill corridor
    val corridorPath = Path().apply {
        moveTo(tunnel.leftWall.p1.x, tunnel.leftWall.p1.y)
        lineTo(tunnel.rightWall.p1.x, tunnel.rightWall.p1.y)
        lineTo(tunnel.rightWall.p2.x, tunnel.rightWall.p2.y)
        lineTo(tunnel.leftWall.p2.x, tunnel.leftWall.p2.y)
        close()
    }
    drawPath(corridorPath, color = TunnelFill)

    // Dashed center guiding track
    val dashEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
    drawLine(
        color = TunnelGuide.copy(alpha = 0.35f),
        start = Offset(tunnel.entranceCenter.x, tunnel.entranceCenter.y),
        end = Offset(tunnel.exitCenter.x, tunnel.exitCenter.y),
        strokeWidth = 2f,
        pathEffect = dashEffect
    )

    // Left Rigid Wall
    drawLine(
        color = TunnelGuide,
        start = Offset(tunnel.leftWall.p1.x, tunnel.leftWall.p1.y),
        end = Offset(tunnel.leftWall.p2.x, tunnel.leftWall.p2.y),
        strokeWidth = 3.5f,
        cap = StrokeCap.Round
    )

    // Right Rigid Wall
    drawLine(
        color = TunnelGuide,
        start = Offset(tunnel.rightWall.p1.x, tunnel.rightWall.p1.y),
        end = Offset(tunnel.rightWall.p2.x, tunnel.rightWall.p2.y),
        strokeWidth = 3.5f,
        cap = StrokeCap.Round
    )
}

/**
 * Draws a metallic chrome sphere with tilt-dependent specular reflection.
 */
private fun DrawScope.drawMetallicBall(ball: Ball, pitchDeg: Float, rollDeg: Float) {
    val center = Offset(ball.position.x, ball.position.y)
    val r = ball.radius

    // Shadow shifts dynamically according to tilt angle
    val shadowOffsetX = (-rollDeg * 0.45f).coerceIn(-12f, 12f)
    val shadowOffsetY = (-pitchDeg * 0.45f).coerceIn(-12f, 12f) + 3f
    drawCircle(
        color = BallShadow,
        radius = r * 1.1f,
        center = center + Offset(shadowOffsetX, shadowOffsetY)
    )

    // Dynamic light reflection point (shifts with phone tilt!)
    val lightAngleX = (rollDeg * 0.25f).coerceIn(-r * 0.55f, r * 0.55f)
    val lightAngleY = (pitchDeg * 0.25f).coerceIn(-r * 0.55f, r * 0.55f)
    val lightCenter = center + Offset(lightAngleX - r * 0.25f, lightAngleY - r * 0.25f)

    // Chrome sphere body
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(BallHigh, BallMid, BallLow),
            center = lightCenter,
            radius = r * 1.35f
        ),
        radius = r,
        center = center
    )

    // Specular highlight spot
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(BallSpecular, Color(0x00FFFFFF)),
            center = lightCenter,
            radius = r * 0.45f
        ),
        radius = r * 0.45f,
        center = lightCenter
    )

    // Outer edge rim
    drawCircle(
        color = Color(0x33FFFFFF),
        radius = r,
        center = center,
        style = Stroke(width = 1.0f)
    )
}

/**
 * Отрисовка пинбол-бампера
 */
private fun DrawScope.drawBumper(bumper: BumperObstacle) {
    val center = Offset(bumper.center.x, bumper.center.y)
    val r = bumper.radius
    drawCircle(color = Color(0x33FFB300), radius = r + 8f, center = center)
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color(0xFFFFD54F), Color(0xFFFF8F00)),
            center = center - Offset(3f, 3f),
            radius = r
        ),
        radius = r,
        center = center
    )
    drawCircle(color = Color(0xFFFFF8E1), radius = r * 0.45f, center = center)
    drawCircle(color = Color(0xFFFF6F00), radius = r, center = center, style = Stroke(width = 2.5f))
}

/**
 * Отрисовка полигональных фигур (Hexagon, Box, Diamond, Star, Wedge, Cross, Pill, Funnel)
 */
private fun DrawScope.drawPolygonObstacle(poly: PolygonObstacle) {
    if (poly.vertices.isEmpty()) return
    val (fillColor, strokeColor) = when (poly) {
        is HexagonObstacle -> Color(0xFF1E293B) to Color(0xFF38BDF8)
        is BoxObstacle -> Color(0xFF27272A) to Color(0xFFA1A1AA)
        is DiamondObstacle -> Color(0xFF1E1B4B) to Color(0xFF818CF8)
        is StarObstacle -> Color(0xFF431407) to Color(0xFFF97316)
        is WedgeObstacle -> WedgeFill to WedgeStroke
        is CrossObstacle -> Color(0xFF134E4A) to Color(0xFF2DD4BF)
        is PillObstacle -> Color(0xFF312E81) to Color(0xFFA5B4FC)
        is FunnelObstacle -> Color(0xFF1F2937) to Color(0xFF60A5FA)
        else -> SurfaceCard to AccentPrimary
    }

    val shadowPath = Path().apply {
        moveTo(poly.vertices[0].x + 3f, poly.vertices[0].y + 4f)
        for (i in 1 until poly.vertices.size) {
            lineTo(poly.vertices[i].x + 3f, poly.vertices[i].y + 4f)
        }
        close()
    }
    drawPath(shadowPath, color = BallShadow)

    val path = Path().apply {
        moveTo(poly.vertices[0].x, poly.vertices[0].y)
        for (i in 1 until poly.vertices.size) {
            lineTo(poly.vertices[i].x, poly.vertices[i].y)
        }
        close()
    }
    drawPath(path, color = fillColor)

    for (edge in poly.edges) {
        drawLine(
            color = strokeColor,
            start = Offset(edge.p1.x, edge.p1.y),
            end = Offset(edge.p2.x, edge.p2.y),
            strokeWidth = 2.8f,
            cap = StrokeCap.Round
        )
    }

    drawCircle(
        color = strokeColor.copy(alpha = 0.7f),
        radius = 3.5f,
        center = Offset(poly.center.x, poly.center.y)
    )
}

private fun DrawScope.drawArcWall(arc: ArcWallObstacle) {
    for (edge in arc.edges) {
        drawLine(
            color = Color(0xFFF43F5E),
            start = Offset(edge.p1.x, edge.p1.y),
            end = Offset(edge.p2.x, edge.p2.y),
            strokeWidth = 5.0f,
            cap = StrokeCap.Round
        )
        drawLine(
            color = Color(0xFFFECDD3),
            start = Offset(edge.p1.x, edge.p1.y),
            end = Offset(edge.p2.x, edge.p2.y),
            strokeWidth = 2.0f,
            cap = StrokeCap.Round
        )
    }
}

private fun DrawScope.drawSpinner(spinner: SpinnerObstacle) {
    val center = Offset(spinner.center.x, spinner.center.y)
    drawCircle(color = Color(0xFF10B981), radius = 8f, center = center)
    for (edge in spinner.edges) {
        drawLine(
            color = Color(0xFF34D399),
            start = Offset(edge.p1.x, edge.p1.y),
            end = Offset(edge.p2.x, edge.p2.y),
            strokeWidth = 3.5f,
            cap = StrokeCap.Round
        )
    }
}
