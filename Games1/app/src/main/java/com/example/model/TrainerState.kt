package com.example.model

enum class ObstacleDensity(val label: String, val description: String) {
    LOW("Низкая", "Разминка: просторные коридоры и минимум препятствий"),
    MEDIUM("Средняя", "Баланс: оптимальное сочетание фигур, штырей и тоннелей"),
    HIGH("Высокая", "Концентрация: плотная расстановка для ювелирного контроля")
}

enum class GameStatus {
    READY,      // Ready to launch / calibrated
    RUNNING,    // Ball is rolling
    WON,        // Landed in center-bottom pocket
    MISSED      // Dropped off the bottom edge outside pocket
}

data class TrainerStats(
    val sessionDurationMs: Long = 0L,
    val bouncesCount: Int = 0,
    val stabilityScore: Int = 100, // 0..100 based on low acceleration jerk / tremor
    val attemptsCount: Int = 1,
    val winsCount: Int = 0
)

data class TrainerState(
    val status: GameStatus = GameStatus.READY,
    val density: ObstacleDensity = ObstacleDensity.MEDIUM,
    val ball: Ball = Ball(),
    val pocket: Pocket = Pocket(Vector2D()),
    val obstacles: List<Obstacle> = emptyList(),
    val bounds: TableBounds = TableBounds(1f, 1f, Pocket(Vector2D())),
    val stats: TrainerStats = TrainerStats(),
    val pitchDeg: Float = 0f,
    val rollDeg: Float = 0f,
    val isCalibrated: Boolean = false,
    val sensitivity: Float = 1.0f
)
