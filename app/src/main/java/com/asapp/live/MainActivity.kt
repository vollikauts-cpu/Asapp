package com.asapp.live

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureRequest
import android.os.Bundle
import android.view.Surface
import android.view.TextureView
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

/**
 * Visible, user-controlled camera preview.
 * No background capture or network transmission is performed.
 */
class MainActivity : Activity() {
    private lateinit var preview: TextureView
    private lateinit var status: TextView
    private lateinit var manager: CameraManager
    private var camera: CameraDevice? = null
    private var session: CameraCaptureSession? = null
    private var lens = CameraCharacteristics.LENS_FACING_BACK
    private var active = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        manager = getSystemService(CAMERA_SERVICE) as CameraManager
        preview = TextureView(this)
        status = TextView(this).apply {
            text = "Asapp · Камера выключена"
            textSize = 18f
            setPadding(20, 18, 20, 18)
        }
        val start = Button(this).apply {
            text = "Включить камеру"
            setOnClickListener {
                if (active) stopCamera() else startCamera()
                text = if (active) "Выключить камеру" else "Включить камеру"
            }
        }
        val switch = Button(this).apply {
            text = "Передняя / задняя"
            setOnClickListener {
                lens = if (lens == CameraCharacteristics.LENS_FACING_BACK)
                    CameraCharacteristics.LENS_FACING_FRONT
                else CameraCharacteristics.LENS_FACING_BACK
                if (active) {
                    stopCamera()
                    startCamera()
                }
            }
        }
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(status)
            addView(preview, LinearLayout.LayoutParams(-1, 0, 1f))
            addView(start)
            addView(switch)
        }
        setContentView(layout)
        preview.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
            override fun onSurfaceTextureAvailable(s: SurfaceTexture, w: Int, h: Int) = Unit
            override fun onSurfaceTextureSizeChanged(s: SurfaceTexture, w: Int, h: Int) = Unit
            override fun onSurfaceTextureDestroyed(s: SurfaceTexture): Boolean {
                stopCamera()
                return true
            }
            override fun onSurfaceTextureUpdated(s: SurfaceTexture) = Unit
        }
    }

    private fun startCamera() {
        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.CAMERA), 7)
            return
        }
        if (!preview.isAvailable) {
            status.text = "Подожди: поверхность камеры ещё не готова"
            return
        }
        try {
            val id = manager.cameraIdList.firstOrNull {
                manager.getCameraCharacteristics(it)
                    .get(CameraCharacteristics.LENS_FACING) == lens
            } ?: run {
                status.text = "Выбранная камера не найдена"
                return
            }
            active = true
            status.text = "Камера включена · только локальный просмотр"
            manager.openCamera(id, object : CameraDevice.StateCallback() {
                override fun onOpened(device: CameraDevice) {
                    if (!active) {
                        device.close()
                        return
                    }
                    camera = device
                    try {
                        val texture = preview.surfaceTexture ?: return
                        texture.setDefaultBufferSize(1280, 720)
                        val surface = Surface(texture)
                        device.createCaptureSession(listOf(surface), object : CameraCaptureSession.StateCallback() {
                            override fun onConfigured(s: CameraCaptureSession) {
                                if (!active) {
                                    s.close()
                                    return
                                }
                                session = s
                                try {
                                    val request = device.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW).apply {
                                        addTarget(surface)
                                        set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_AUTO)
                                    }
                                    s.setRepeatingRequest(request.build(), null, null)
                                } catch (e: Exception) {
                                    status.text = "Ошибка предпросмотра: ${e.message}"
                                    stopCamera()
                                }
                            }
                            override fun onConfigureFailed(s: CameraCaptureSession) {
                                status.text = "Не удалось настроить камеру"
                                stopCamera()
                            }
                        }, null)
                    } catch (e: Exception) {
                        status.text = "Ошибка камеры: ${e.message}"
                        stopCamera()
                    }
                }
                override fun onDisconnected(device: CameraDevice) {
                    device.close()
                    stopCamera()
                }
                override fun onError(device: CameraDevice, error: Int) {
                    device.close()
                    status.text = "Ошибка камеры: $error"
                    stopCamera()
                }
            }, null)
        } catch (e: Exception) {
            status.text = "Ошибка запуска: ${e.message}"
            stopCamera()
        }
    }

    private fun stopCamera() {
        active = false
        try { session?.close() } catch (_: Exception) {}
        try { camera?.close() } catch (_: Exception) {}
        session = null
        camera = null
        if (::status.isInitialized && !status.text.startsWith("Ошибка"))
            status.text = "Asapp · Камера выключена"
    }

    override fun onPause() {
        stopCamera()
        super.onPause()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 7 && grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) {
            startCamera()
        } else if (requestCode == 7) {
            status.text = "Для предпросмотра нужно разрешение камеры"
        }
    }
}
