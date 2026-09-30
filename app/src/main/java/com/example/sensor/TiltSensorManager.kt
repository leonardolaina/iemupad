package com.example.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class TiltData(
    val tiltX: Float = 0f, // -1.0 (left) to 1.0 (right)
    val tiltY: Float = 0f, // -1.0 (down) to 1.0 (up)
    val isAvailable: Boolean = false
)

class TiltSensorManager(context: Context) : SensorEventListener {
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    private val _tiltFlow = MutableStateFlow(TiltData(isAvailable = accelerometer != null))
    val tiltFlow: StateFlow<TiltData> = _tiltFlow.asStateFlow()

    private var isListening = false
    private var lastX = 0f
    private var lastY = 0f

    fun startListening() {
        if (isListening || accelerometer == null) return
        sensorManager?.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME)
        isListening = true
    }

    fun stopListening() {
        if (!isListening) return
        sensorManager?.unregisterListener(this)
        isListening = false
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return
        // Low-pass filter for smooth motion
        val rawX = event.values[0]
        val rawY = event.values[1]

        val alpha = 0.25f
        lastX = lastX + alpha * (rawX - lastX)
        lastY = lastY + alpha * (rawY - lastY)

        // Normalize typical tilt range (-9.8 to 9.8) to approximately -1.0 to 1.0
        val normX = (-lastX / 7.0f).coerceIn(-1.5f, 1.5f)
        val normY = (lastY / 7.0f).coerceIn(-1.5f, 1.5f)

        _tiltFlow.value = TiltData(
            tiltX = normX,
            tiltY = normY,
            isAvailable = true
        )
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
