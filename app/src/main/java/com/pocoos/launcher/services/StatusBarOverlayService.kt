package com.pocoos.launcher.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.BatteryManager
import android.os.IBinder
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class StatusBarOverlayService : Service() {

    private lateinit var windowManager: WindowManager
    private var statusBarView: ComposeView? = null

    override fun onCreate() {
        super.onCreate()
        startForegroundChannel()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            78,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP
        }

        val batteryMgr = getSystemService(BATTERY_SERVICE) as BatteryManager
        val batteryPct = batteryMgr.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        val timeText = SimpleDateFormat("h:mm", Locale.getDefault()).format(Date())

        statusBarView = ComposeView(this).apply {
            setContent {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 22.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(timeText, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("5G", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color.White.copy(alpha = 0.25f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("$batteryPct%", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
        windowManager.addView(statusBarView, params)
    }

    private fun startForegroundChannel() {
        val channelId = "pocoos_statusbar_channel"
        val channel = NotificationChannel(channelId, "PocoOS Status Bar", NotificationManager.IMPORTANCE_LOW)
        (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(channel)
        startForeground(
            103,
            Notification.Builder(this, channelId).setContentTitle("PocoOS Custom Status Bar").setSmallIcon(android.R.drawable.ic_menu_info_details).build()
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        statusBarView?.let { windowManager.removeView(it) }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
