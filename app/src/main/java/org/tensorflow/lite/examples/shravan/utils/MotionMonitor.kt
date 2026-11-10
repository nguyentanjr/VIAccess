package org.tensorflow.lite.examples.shravan.utils

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.sqrt

/**
 * Phát hiện rung tay (PLAN 4.2, ưu tiên 2): độ lớn vận tốc góc > [threshold] rad/s
 * ở bất kỳ mẫu nào trong [windowMs] gần nhất. Không phụ thuộc Android để test được.
 */
class ShakeDetector(
    private val threshold: Float = FeedbackConfig.GYRO_SHAKE_RAD_S,
    private val windowMs: Long = FeedbackConfig.GYRO_SHAKE_WINDOW_MS
) {
    private var lastShakeAtMs: Long? = null

    fun onSample(x: Float, y: Float, z: Float, nowMs: Long) {
        if (sqrt(x * x + y * y + z * z) > threshold) lastShakeAtMs = nowMs
    }

    fun isShaking(nowMs: Long): Boolean {
        val last = lastShakeAtMs ?: return false
        return nowMs - last <= windowMs
    }

    fun reset() {
        lastShakeAtMs = null
    }
}

/** Đọc gyroscope khi màn Đọc chữ đang mở. Máy không có gyroscope → luôn coi là không rung. */
class MotionMonitor(context: Context) : SensorEventListener {
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val gyro: Sensor? = sensorManager?.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
    private val detector = ShakeDetector()

    val isAvailable: Boolean get() = gyro != null

    fun start() {
        val s = gyro ?: return
        detector.reset()
        sensorManager?.registerListener(this, s, SensorManager.SENSOR_DELAY_GAME)
    }

    fun stop() {
        sensorManager?.unregisterListener(this)
        detector.reset()
    }

    @Synchronized
    fun isShaking(): Boolean = detector.isShaking(android.os.SystemClock.elapsedRealtime())

    @Synchronized
    override fun onSensorChanged(event: SensorEvent) {
        detector.onSample(event.values[0], event.values[1], event.values[2], android.os.SystemClock.elapsedRealtime())
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
