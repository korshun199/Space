package com.example.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import com.example.model.Vector2D
import kotlin.math.abs
import kotlin.math.sin

class TiltSensorTracker(context: Context) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    private val rotationSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
    private val gravitySensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY)
    private val accelSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    private val rotationMatrix = FloatArray(9)
    private val orientationAngles = FloatArray(3)

    // Calibration offsets (in radians)
    var calibratedPitchRad: Float = 0f
        private set
    var calibratedRollRad: Float = 0f
        private set
    var isCalibrated: Boolean = false
        private set

    // Filtered pitch & roll (radians)
    private var filteredPitchRad: Float = 0f
    private var filteredRollRad: Float = 0f

    // Raw current pitch & roll for calibration
    private var rawPitchRad: Float = 0f
    private var rawRollRad: Float = 0f

    // Tremor & stability tracking
    private var lastRollRad: Float = 0f
    private var lastPitchRad: Float = 0f
    private var jitterAccumulator: Float = 0f
    private var sampleCount: Int = 0

    // Sensitivity factor (0.5x to 2.0x, default 1.0)
    var sensitivity: Float = 1.0f

    // Scale from normalized gravity to screen pixel acceleration (px/s^2)
    var gravityScale: Float = 2600f

    var onSensorChangedListener: ((pitchDeg: Float, rollDeg: Float, gravity: Vector2D) -> Unit)? = null

    fun start() {
        val sensorToRegister = rotationSensor ?: gravitySensor ?: accelSensor
        sensorToRegister?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
    }

    fun stop() {
        sensorManager.unregisterListener(this)
    }

    /**
     * Calibrate the current phone orientation as level zero.
     * Allows the user to hold the phone in any wrist posture comfortably.
     */
    fun calibrateZero() {
        calibratedPitchRad = rawPitchRad
        calibratedRollRad = rawRollRad
        isCalibrated = true
    }

    /**
     * Resets calibration to true geometric flat.
     */
    fun resetCalibration() {
        calibratedPitchRad = 0f
        calibratedRollRad = 0f
        isCalibrated = false
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return

        when (event.sensor.type) {
            Sensor.TYPE_ROTATION_VECTOR -> {
                SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                SensorManager.getOrientation(rotationMatrix, orientationAngles)
                // Inverted pitch so tilting bottom down produces positive pitch and pulls ball down (+Y)
                rawPitchRad = -orientationAngles[1]
                rawRollRad = orientationAngles[2]
            }

            Sensor.TYPE_GRAVITY, Sensor.TYPE_ACCELEROMETER -> {
                // Approximate pitch and roll from gravity/accel
                val ax = event.values[0]
                val ay = event.values[1]
                val az = event.values[2]
                rawRollRad = -kotlin.math.atan2(ax.toDouble(), az.toDouble()).toFloat()
                rawPitchRad = -kotlin.math.atan2(ay.toDouble(), az.toDouble()).toFloat()
            }
        }

        // Apply calibration offset
        val targetPitch = rawPitchRad - calibratedPitchRad
        val targetRoll = rawRollRad - calibratedRollRad

        // Low-pass exponential filter to eliminate high-frequency noise (alpha = 0.22)
        val alpha = 0.22f
        filteredPitchRad += alpha * (targetPitch - filteredPitchRad)
        filteredRollRad += alpha * (targetRoll - filteredRollRad)

        // Track tremor/jitter
        val dPitch = abs(filteredPitchRad - lastPitchRad)
        val dRoll = abs(filteredRollRad - lastRollRad)
        jitterAccumulator += (dPitch + dRoll)
        sampleCount++
        lastPitchRad = filteredPitchRad
        lastRollRad = filteredRollRad

        // Compute gravity vector:
        // Rolling phone right (positive roll) -> gravity pulls ball to the right (+X)
        // Tilting phone down (positive pitch) -> gravity pulls ball down (+Y)
        val gx = sin(filteredRollRad) * gravityScale * sensitivity
        val gy = sin(filteredPitchRad) * gravityScale * sensitivity

        val pitchDeg = Math.toDegrees(filteredPitchRad.toDouble()).toFloat()
        val rollDeg = Math.toDegrees(filteredRollRad.toDouble()).toFloat()

        onSensorChangedListener?.invoke(pitchDeg, rollDeg, Vector2D(gx, gy))
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // No-op
    }

    /**
     * Returns an estimated wrist stability index (0 to 100).
     * High values indicate smooth, stable, intentional wrist motion.
     */
    fun calculateStabilityScore(): Int {
        if (sampleCount == 0) return 100
        val avgJitter = jitterAccumulator / sampleCount
        // Reset window
        jitterAccumulator = 0f
        sampleCount = 0
        // Typical smooth jitter is 0.005..0.02 rad/sample. High tremor is > 0.05
        val penalty = (avgJitter * 1400f).toInt()
        return (100 - penalty).coerceIn(20, 100)
    }
}
