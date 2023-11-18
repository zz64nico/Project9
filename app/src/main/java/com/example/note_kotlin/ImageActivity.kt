package com.example.note_kotlin

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.os.Vibrator
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.bumptech.glide.Glide
import com.example.note_kotlin.databinding.ActivityImageBinding


class ImageActivity:AppCompatActivity() {

    lateinit var binding:ActivityImageBinding
    private var sensorManager: SensorManager? = null
    private var vibrator: Vibrator? = null
    var url:String? = null

    private val TAG = "TestSensorActivity"
    private val SENSOR_SHAKE = 10


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this,R.layout.activity_image);
        url = intent.getStringExtra("url")
        Glide.with(this).load(url).into(binding.image)

        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager?;
        vibrator = getSystemService(VIBRATOR_SERVICE) as Vibrator
    }

    override fun onResume() {
        super.onResume()
        if (sensorManager != null) { // 注册监听器
            sensorManager!!.registerListener(
                sensorEventListener,
                sensorManager!!.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                SensorManager.SENSOR_DELAY_NORMAL
            )
            // 第一个参数是Listener，第二个参数是所得传感器类型，第三个参数值获取传感器信息的频率
        }
    }

    override fun onPause() {
        super.onPause()
        if (sensorManager != null) { // 取消监听器
            sensorManager!!.unregisterListener(sensorEventListener)
        }
    }

    /**
     * 重力感应监听
     */
    private val sensorEventListener: SensorEventListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            // 传感器信息改变时执行该方法
            val values = event.values
            val x = values[0] // x轴方向的重力加速度，向右为正
            val y = values[1] // y轴方向的重力加速度，向前为正
            val z = values[2] // z轴方向的重力加速度，向上为正
            Log.i(TAG, "x轴方向的重力加速度$x；y轴方向的重力加速度$y；z轴方向的重力加速度$z")
            // 一般在这三个方向的重力加速度达到40就达到了摇晃手机的状态。
            val medumValue = 19 // 三星 i9250怎么晃都不会超过20，没办法，只设置19了
            if (Math.abs(x) > medumValue ||( Math.abs(y) > medumValue )|| Math.abs(z) > medumValue) {
                vibrator!!.vibrate(200)
                val msg = Message()
                msg.what = SENSOR_SHAKE
                handler.sendMessage(msg)
            }
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    }

    /**
     * 动作执行
     */
    var handler: Handler = object : Handler() {
        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
            when (msg.what) {
                SENSOR_SHAKE -> {
                    Toast.makeText(this@ImageActivity,
                            "Shaking detected, proceed!", Toast.LENGTH_SHORT)
                        .show()
                    var current = 0;
                    if (App.stringList!=null){
                        for(i in 0..App.stringList.size-1){
                            if (url!!.equals(App.stringList.get(i))){
                                current = i;
                            }
                        }
                        if (current < App.stringList.size-1){
                             url = App.stringList.get(current+1)
                            Glide.with(this@ImageActivity).load(url).into(binding.image)
                        }else{
                            url = App.stringList.get(0)
                            Glide.with(this@ImageActivity).load(url).into(binding.image)
                        }
                    }
                }
            }
        }
    }
}