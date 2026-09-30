package com.example.sensor

import android.app.Service
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Binder
import android.os.IBinder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.atan2
import kotlin.math.sqrt

data class MotionTiltState(
    val tiltX: Float = 0f,           // Normalized: -1.0 (left) to 1.0 (right)
    val tiltY: Float = 0f,           // Normalized: -1.0 (forward) to 1.0 (backward)
    val rollDeg: Float = 0f,         // Angle in degrees (-90 to 90)
    val pitchDeg: Float = 0f,        // Angle in degrees (-90 to 90)
    val isSensorAvailable: Boolean = false,
    val isCalibrated: Boolean = false,
    val sensitivity: Float = 1.0f,
    val deadzone: Float = 0.04f,
    val sensorName: String = "Acelerômetro 3D"
)

class MotionSensorService : Service(), SensorEventListener {

    private val binder = LocalBinder()
    private var sensorManager: SensorManager? = null
    private var accelerometer: Sensor? = null
    private var gravitySensor: Sensor? = null

    private var filterAlpha = 0.22f
    private var smoothedX = 0f
    private var smoothedY = 0f
    private var smoothedZ = 9.8f

    // Calibration offsets
    private var offsetX = 0f
    private var offsetY = 0f

    companion object {
        private val _motionState = MutableStateFlow(MotionTiltState())
        val motionState: StateFlow<MotionTiltState> = _motionState.asStateFlow()

        var currentSensitivity: Float = 1.0f
            set(value) {
                field = value
                _motionState.value = _motionState.value.copy(sensitivity = value)
            }

        var currentDeadzone: Float = 0.04f
            set(value) {
                field = value
                _motionState.value = _motionState.value.copy(deadzone = value)
            }

        fun calibrateRestingPosition() {
            instance?.calibrateCurrentZero()
        }

        private var instance: MotionSensorService? = null

        fun startService(context: Context) {
            val intent = Intent(context, MotionSensorService::class.java)
            context.startService(intent)
        }

        fun stopService(context: Context) {
            val intent = Intent(context, MotionSensorService::class.java)
            context.stopService(intent)
        }
    }

    inner class LocalBinder : Binder() {
        fun getService(): MotionSensorService = this@MotionSensorService
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        gravitySensor = sensorManager?.getDefaultSensor(Sensor.TYPE_GRAVITY)
        accelerometer = gravitySensor ?: sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        val available = accelerometer != null
        val sensorName = accelerometer?.name ?: "Indisponível"

        _motionState.value = _motionState.value.copy(
            isSensorAvailable = available,
            sensorName = sensorName,
            sensitivity = currentSensitivity,
            deadzone = currentDeadzone
        )

        registerSensors()
    }

    private fun registerSensors() {
        accelerometer?.let {
            sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        registerSensors()
        return START_STICKY
    }

    fun calibrateCurrentZero() {
        offsetX = smoothedX
        offsetY = smoothedY
        _motionState.value = _motionState.value.copy(isCalibrated = true)
    }

    fun resetCalibration() {
        offsetX = 0f
        offsetY = 0f
        _motionState.value = _motionState.value.copy(isCalibrated = false)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return

        val rawX = event.values[0]
        val rawY = event.values[1]
        val rawZ = event.values[2]

        // Low-pass filter for smooth motion mapping
        smoothedX += filterAlpha * (rawX - smoothedX)
        smoothedY += filterAlpha * (rawY - smoothedY)
        smoothedZ += filterAlpha * (rawZ - smoothedZ)

        // Apply zero calibration offset
        val adjustedX = smoothedX - offsetX
        val adjustedY = smoothedY - offsetY

        // Compute pitch and roll angles in degrees
        val rollRad = atan2(-adjustedX.toDouble(), sqrt((adjustedY * adjustedY + smoothedZ * smoothedZ).toDouble()))
        val pitchRad = atan2(adjustedY.toDouble(), sqrt((adjustedX * adjustedX + smoothedZ * smoothedZ).toDouble()))

        val rollDeg = Math.toDegrees(rollRad).toFloat()
        val pitchDeg = Math.toDegrees(pitchRad).toFloat()

        // Normalize to -1.0 to 1.0 (assuming ~45° is maximum comfortable handheld steering tilt)
        var normX = (-adjustedX / 6.5f) * currentSensitivity
        var normY = (adjustedY / 6.5f) * currentSensitivity

        // Apply deadzone
        normX = if (kotlin.math.abs(normX) < currentDeadzone) 0f else normX
        normY = if (kotlin.math.abs(normY) < currentDeadzone) 0f else normY

        normX = normX.coerceIn(-1.5f, 1.5f)
        normY = normY.coerceIn(-1.5f, 1.5f)

        _motionState.value = _motionState.value.copy(
            tiltX = normX,
            tiltY = normY,
            rollDeg = rollDeg,
            pitchDeg = pitchDeg,
            isSensorAvailable = true
        )
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onDestroy() {
        super.onDestroy()
        sensorManager?.unregisterListener(this)
        instance = null
    }
}
