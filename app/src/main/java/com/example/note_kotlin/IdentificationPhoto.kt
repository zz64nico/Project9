package com.example.note_kotlin

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Matrix
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.util.Rational
import android.util.Size
import android.view.*
import android.widget.ImageView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.core.CameraX.LensFacing
import androidx.camera.core.Preview.OnPreviewOutputUpdateListener
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class IdentificationPhoto : AppCompatActivity() {
    private val REQUEST_CODE_PERMISSIONS = 101
    private val REQUIRED_PERMISSIONS =
        arrayOf("android.permission.CAMERA", "android.permission.WRITE_EXTERNAL_STORAGE")
    var textureView: TextureView? = null
    var cameraFlip: ImageView? = null
    private var backlensfacing = 0
    private var flashLamp = 0
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camerax_demo)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            //透明状态栏
            window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
            //透明导航栏
            window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION)
        }
        textureView = findViewById(R.id.view_camera)
        cameraFlip = findViewById(R.id.btn_switch_camera)
        cameraFlip!!.setOnClickListener(View.OnClickListener {
            if (backlensfacing == 0) {
                startCamera(LensFacing.FRONT)
                backlensfacing = 1
            } else if (backlensfacing == 1) {
                startCamera(LensFacing.BACK)
                backlensfacing = 0
            }
        })
        if (allPermissionsGranted()) {
            startCamera(LensFacing.BACK)
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }
    }

    private fun startCamera(CAMERA_ID: LensFacing) {
        CameraX.unbindAll()
        val aspectRatio = Rational(textureView!!.width, textureView!!.height)
        val screen = Size(textureView!!.width, textureView!!.height)
        val pConfig: PreviewConfig
        val preview: Preview
        pConfig = PreviewConfig.Builder().setLensFacing(CAMERA_ID).setTargetAspectRatio(aspectRatio)
            .setTargetResolution(screen).build()
        preview = Preview(pConfig)
        preview.onPreviewOutputUpdateListener = OnPreviewOutputUpdateListener { output ->
            val parent = textureView!!.parent as ViewGroup
            parent.removeView(textureView)
            parent.addView(textureView, 0)
            textureView!!.setSurfaceTexture(output.surfaceTexture)
            updateTransform()
        }
        val imageCaptureConfig: ImageCaptureConfig
        imageCaptureConfig =
            ImageCaptureConfig.Builder().setCaptureMode(ImageCapture.CaptureMode.MAX_QUALITY)
                .setTargetRotation(
                    windowManager.defaultDisplay.rotation
                ).setLensFacing(CAMERA_ID).build()
        val imgCap = ImageCapture(imageCaptureConfig)
        findViewById<View>(R.id.btn_flash).setOnClickListener {
            if (flashLamp == 0) {
                flashLamp = 1
                imgCap.flashMode = FlashMode.OFF
                Toast.makeText(baseContext, "Flash Disable", Toast.LENGTH_SHORT).show()
            } else if (flashLamp == 1) {
                flashLamp = 0
                imgCap.flashMode = FlashMode.ON
                Toast.makeText(baseContext, "Flash Enable", Toast.LENGTH_SHORT).show()
            }
        }
        findViewById<View>(R.id.btn_takePict).setOnClickListener {
            var image: File? = null
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            //android11 创建文件夹必须使用这种方式
            val timeStampStart = getExternalFilesDir(Environment.DIRECTORY_PICTURES)!!.absolutePath
            image = File("$timeStampStart$timeStamp.jpg")
            if (!image.parentFile.exists()) {
                image.parentFile.mkdirs()
            }
            imgCap.takePicture(image, object : ImageCapture.OnImageSavedListener {
                override fun onImageSaved(file: File) {
                    val msg = "Pic saved at " + file.absolutePath
                    galleryAddPic(file.absolutePath)
                    Toast.makeText(baseContext, msg, Toast.LENGTH_LONG).show()
                }

                override fun onError(
                    useCaseError: ImageCapture.UseCaseError,
                    message: String,
                    cause: Throwable?
                ) {
                    val msg = "Pic saved at $message"
                    if (cause != null) {
                        cause.printStackTrace()
                        Toast.makeText(baseContext, cause.toString(), Toast.LENGTH_LONG).show()
                    }
                }
            })
        }
        CameraX.bindToLifecycle(this, preview, imgCap)
    }

    private fun galleryAddPic(currentFilePath: String) {
        val mediaScanIntent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
        val file = File(currentFilePath)
        App.commonUri = Uri.fromFile(file)
        mediaScanIntent.data = App.commonUri
        this.sendBroadcast(mediaScanIntent)
        setResult(101)
        finish()
        //Toast.makeText(getBaseContext(), "saved to gallery",Toast.LENGTH_LONG).show();
    }

    private fun updateTransform() {
        val mx = Matrix()
        val w = textureView!!.measuredWidth.toFloat()
        val h = textureView!!.measuredHeight.toFloat()
        val cX = w / 2f
        val cY = h / 2f
        val rotationDgr: Int
        val rotation = textureView!!.rotation.toInt()
        rotationDgr = when (rotation) {
            Surface.ROTATION_0 -> 0
            Surface.ROTATION_90 -> 90
            Surface.ROTATION_180 -> 180
            Surface.ROTATION_270 -> 270
            else -> return
        }
        mx.postRotate(rotationDgr.toFloat(), cX, cY)
        textureView!!.setTransform(mx)
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            startCamera(LensFacing.BACK)
        }
    }

    private fun allPermissionsGranted(): Boolean {
        for (permission in REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    permission
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return false
            }
        }
        return true
    }
}