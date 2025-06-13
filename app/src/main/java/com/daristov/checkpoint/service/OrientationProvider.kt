package com.daristov.checkpoint.service

import android.content.Context
import android.hardware.*

class OrientationProvider(context: Context) : SensorEventListener {

    interface Listener {
        fun onOrientationChanged(pitch: Float, roll: Float, azimuth: Float)
    }

    private var listener: Listener? = null

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val magneticSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

    private val accelValues = FloatArray(3)
    private val magneticValues = FloatArray(3)

    fun start(listener: Listener) {
        this.listener = listener
        sensorManager.registerListener(this, accelSensor, SensorManager.SENSOR_DELAY_UI)
        sensorManager.registerListener(this, magneticSensor, SensorManager.SENSOR_DELAY_UI)
    }

    fun stop() {
        sensorManager.unregisterListener(this)
        listener = null
    }

    override fun onSensorChanged(event: SensorEvent) {
        when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> event.values.copyInto(accelValues)
            Sensor.TYPE_MAGNETIC_FIELD -> event.values.copyInto(magneticValues)
        }

        val rotationMatrix = FloatArray(9)
        val success = SensorManager.getRotationMatrix(rotationMatrix, null, accelValues, magneticValues)
        if (success) {
            val orientation = FloatArray(3)
            SensorManager.getOrientation(rotationMatrix, orientation)

            val azimuth = Math.toDegrees(orientation[0].toDouble()).toFloat()  // вращение вокруг Z
            val pitch = Math.toDegrees(orientation[1].toDouble()).toFloat()    // наклон вперед/назад
            val roll = Math.toDegrees(orientation[2].toDouble()).toFloat()     // наклон вбок

            listener?.onOrientationChanged(pitch, roll, azimuth)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
