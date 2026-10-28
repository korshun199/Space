package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.generator.ProceduralBoardGenerator
import com.example.model.Ball
import com.example.model.GameStatus
import com.example.model.ObstacleDensity
import com.example.model.Pocket
import com.example.model.TableBounds
import com.example.model.TrainerState
import com.example.model.TrainerStats
import com.example.model.Vector2D
import com.example.physics.RigidPhysicsEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class TrainerViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(TrainerState())
    val uiState: StateFlow<TrainerState> = _uiState.asStateFlow()

    private var currentGravity = Vector2D()
    private var boardWidth = 0f
    private var boardHeight = 0f

    private var sessionStartTime = 0L
    private var isSessionActive = false

    // Haptic callback provided from UI
    var onHapticBounce: ((impulse: Float) -> Unit)? = null
    var onHapticWin: (() -> Unit)? = null
    var onHapticFail: (() -> Unit)? = null

    /**
     * Initializes or re-sizes the table board when Canvas dimensions are measured.
     */
    fun onBoardSizeChanged(width: Float, height: Float) {
        if (width <= 0f || height <= 0f) return
        if (width == boardWidth && height == boardHeight) return

        boardWidth = width
        boardHeight = height
        generateNewBoard(_uiState.value.density)
    }

    /**
     * Procedurally generates a new set of obstacles, tunnels, and pegs for the current density.
     */
    fun generateNewBoard(density: ObstacleDensity = _uiState.value.density) {
        if (boardWidth <= 0f || boardHeight <= 0f) return

        val ballRadius = 17f
        val (bounds, obstacles) = ProceduralBoardGenerator.generateBoard(
            width = boardWidth,
            height = boardHeight,
            density = density,
            ballRadius = ballRadius
        )

        val spawnX = boardWidth / 2f
        val spawnY = boardHeight * 0.08f
        val ball = Ball(
            position = Vector2D(spawnX, spawnY),
            velocity = Vector2D(),
            radius = ballRadius
        )

        isSessionActive = false
        sessionStartTime = System.currentTimeMillis()

        _uiState.update { current ->
            current.copy(
                status = GameStatus.READY,
                density = density,
                ball = ball,
                pocket = bounds.pocket,
                obstacles = obstacles,
                bounds = bounds,
                stats = current.stats.copy(
                    sessionDurationMs = 0L,
                    bouncesCount = 0,
                    stabilityScore = 100
                )
            )
        }
    }

    /**
     * Resets the ball to the top spawn position without changing the obstacle layout.
     */
    fun resetBall() {
        if (boardWidth <= 0f || boardHeight <= 0f) return
        val spawnX = boardWidth / 2f
        val spawnY = boardHeight * 0.08f

        isSessionActive = false
        sessionStartTime = System.currentTimeMillis()

        _uiState.update { current ->
            current.copy(
                status = GameStatus.READY,
                ball = current.ball.copy(
                    position = Vector2D(spawnX, spawnY),
                    velocity = Vector2D()
                ),
                stats = current.stats.copy(
                    sessionDurationMs = 0L,
                    bouncesCount = 0,
                    attemptsCount = current.stats.attemptsCount + 1
                )
            )
        }
    }

    /**
     * Sensor update: sets current gravity vector and tilt angles for HUD.
     */
    fun updateTilt(pitchDeg: Float, rollDeg: Float, gravity: Vector2D, isCalibrated: Boolean) {
        currentGravity = gravity
        _uiState.update { current ->
            current.copy(
                pitchDeg = pitchDeg,
                rollDeg = rollDeg,
                isCalibrated = isCalibrated
            )
        }
    }

    /**
     * Per-frame physics simulation step.
     */
    fun physicsStep(dt: Float, stabilityScore: Int) {
        val state = _uiState.value
        if (state.bounds.width <= 0f || state.bounds.height <= 0f) return

        // Auto-start session if ball starts moving
        if (state.status == GameStatus.READY) {
            if (currentGravity.lengthSquared() > 400f || state.ball.velocity.lengthSquared() > 10f) {
                isSessionActive = true
                sessionStartTime = System.currentTimeMillis()
                _uiState.update { it.copy(status = GameStatus.RUNNING) }
            } else {
                return
            }
        }

        if (state.status != GameStatus.RUNNING) return

        // Step physics engine
        val result = RigidPhysicsEngine.step(
            ball = state.ball,
            bounds = state.bounds,
            obstacles = state.obstacles,
            gravity = currentGravity,
            dt = dt,
            currentStatus = state.status
        )

        // Handle haptics on collisions
        if (result.didBounce) {
            onHapticBounce?.invoke(result.bounceImpulse)
        }

        var newBounces = state.stats.bouncesCount
        if (result.didBounce && result.bounceImpulse > 20f) {
            newBounces++
        }

        val elapsed = if (isSessionActive) System.currentTimeMillis() - sessionStartTime else 0L

        // Check victory or miss
        when (result.newStatus) {
            GameStatus.WON -> {
                isSessionActive = false
                onHapticWin?.invoke()
                _uiState.update { current ->
                    current.copy(
                        ball = result.updatedBall,
                        status = GameStatus.WON,
                        stats = current.stats.copy(
                            sessionDurationMs = elapsed,
                            bouncesCount = newBounces,
                            stabilityScore = stabilityScore,
                            winsCount = current.stats.winsCount + 1
                        )
                    )
                }
            }

            GameStatus.MISSED -> {
                isSessionActive = false
                onHapticFail?.invoke()
                _uiState.update { current ->
                    current.copy(
                        ball = result.updatedBall,
                        status = GameStatus.MISSED,
                        stats = current.stats.copy(
                            sessionDurationMs = elapsed,
                            bouncesCount = newBounces,
                            stabilityScore = stabilityScore
                        )
                    )
                }
            }

            else -> {
                _uiState.update { current ->
                    current.copy(
                        ball = result.updatedBall,
                        status = GameStatus.RUNNING,
                        stats = current.stats.copy(
                            sessionDurationMs = elapsed,
                            bouncesCount = newBounces,
                            stabilityScore = stabilityScore
                        )
                    )
                }
            }
        }
    }

    fun setDensity(density: ObstacleDensity) {
        generateNewBoard(density)
    }

    fun setSensitivity(sensitivity: Float) {
        _uiState.update { it.copy(sensitivity = sensitivity.coerceIn(0.5f, 2.5f)) }
    }
}
