package com.pocoos.launcher.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class ControlCenterOverlayService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var cameraManager: CameraManager
    private lateinit var audioManager: AudioManager
    private var torchEnabled = false

    override fun onCreate() {
        super.onCreate()
        startForegroundChannel()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        attachTopRightSwipeTrigger()
    }

    private fun attachTopRightSwipeTrigger() {
        val triggerParams = WindowManager.LayoutParams(
            260,
            68,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.END
        }

        val triggerView = ComposeView(this).apply {
            setContent {
                Box(modifier = Modifier.fillMaxSize().clickable { openControlCenterSheet() })
            }
        }
        windowManager.addView(triggerView, triggerParams)
    }

    private fun openControlCenterSheet() {
        val sheetParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_DIM_BEHIND,
            PixelFormat.TRANSLUCENT
        ).apply {
            dimAmount = 0.45f
        }

        var sheetView: ComposeView? = null
        sheetView = ComposeView(this).apply {
            setContent {
                var brightness by remember { mutableStateOf(0.75f) }
                var volume by remember { mutableStateOf(0.6f) }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.58f))
                        .clickable { sheetView?.let { windowManager.removeView(it) } }
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Spacer(modifier = Modifier.height(36.dp))
                    Text("Control Center · POCO X3 Pro", color = Color.White, fontSize = 15.sp)

                    // Brightness Slider (requires WRITE_SETTINGS)
                    Slider(
                        value = brightness,
                        onValueChange = { value ->
                            brightness = value
                            if (Settings.System.canWrite(this@ControlCenterOverlayService)) {
                                Settings.System.putInt(
                                    contentResolver,
                                    Settings.System.SCREEN_BRIGHTNESS,
                                    (value * 255).toInt()
                                )
                            }
                        }
                    )

                    // Media Volume Slider
                    Slider(
                        value = volume,
                        onValueChange = { value ->
                            volume = value
                            val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, (value * maxVol).toInt(), 0)
                        }
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color.White.copy(alpha = 0.16f))
                                .clickable { toggleFlashlight() }
                                .padding(horizontal = 18.dp, vertical = 14.dp)
                        ) {
                            Text("Torch", color = Color.White)
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color.White.copy(alpha = 0.16f))
                                .clickable { startActivity(Intent(Settings.Panel.ACTION_INTERNET_CONNECTIVITY).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
                                .padding(horizontal = 18.dp, vertical = 14.dp)
                        ) {
                            Text("Wi-Fi & 5G", color = Color.White)
                        }
                    }
                }
            }
        }
        windowManager.addView(sheetView, sheetParams)
    }

    private fun toggleFlashlight() {
        val rearCameraId = cameraManager.cameraIdList.firstOrNull() ?: return
        torchEnabled = !torchEnabled
        cameraManager.setTorchMode(rearCameraId, torchEnabled)
    }

    private fun startForegroundChannel() {
        val channelId = "pocoos_cc_channel"
        val channel = NotificationChannel(channelId, "PocoOS Control Center", NotificationManager.IMPORTANCE_LOW)
        (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(channel)
        startForeground(
            102,
            Notification.Builder(this, channelId).setContentTitle("PocoOS Control Center").setSmallIcon(android.R.drawable.ic_menu_manage).build()
        )
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
