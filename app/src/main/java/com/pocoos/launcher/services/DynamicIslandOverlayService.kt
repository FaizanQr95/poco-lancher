package com.pocoos.launcher.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.IBinder
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.pocoos.launcher.LauncherConfig

class DynamicIslandOverlayService : Service(), LifecycleOwner, SavedStateRegistryOwner {

    private lateinit var windowManager: WindowManager
    private lateinit var mediaSessionManager: MediaSessionManager
    private var islandView: ComposeView? = null

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateController = SavedStateRegistryController.create(this)
    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val savedStateRegistry: SavedStateRegistry get() = savedStateController.savedStateRegistry

    private val activeTitle = mutableStateOf<String?>(null)
    private val activeArtist = mutableStateOf<String?>(null)
    private val isPlaying = mutableStateOf(false)
    private val expanded = mutableStateOf(false)
    private var activeController: MediaController? = null

    override fun onCreate() {
        super.onCreate()
        savedStateController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)

        startForegroundChannel()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        mediaSessionManager = getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager

        attachDynamicIslandWindow()
        observeMediaSessions()
    }

    private fun attachDynamicIslandWindow() {
        val density = resources.displayMetrics.density
        val yOffsetPx = (LauncherConfig.PUNCH_HOLE_Y_OFFSET_DP * density).toInt()

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            y = yOffsetPx
        }

        islandView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@DynamicIslandOverlayService)
            setViewTreeSavedStateRegistryOwner(this@DynamicIslandOverlayService)
            setContent {
                val title by activeTitle
                val artist by activeArtist
                val playing by isPlaying
                val isExpanded by expanded

                Box(
                    modifier = Modifier
                        .animateContentSize(
                            animationSpec = spring(
                                dampingRatio = LauncherConfig.SPRING_DAMPING_RATIO,
                                stiffness = LauncherConfig.SPRING_STIFFNESS
                            )
                        )
                        .clip(RoundedCornerShape(if (isExpanded) 28.dp else 22.dp))
                        .background(Color.Black)
                        .clickable { expanded.value = !expanded.value }
                        .padding(
                            horizontal = if (isExpanded) 18.dp else 14.dp,
                            vertical = if (isExpanded) 14.dp else 6.dp
                        )
                ) {
                    if (isExpanded && title != null) {
                        Column(modifier = Modifier.width(290.dp)) {
                            Text(title ?: "", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(artist ?: "", color = Color.White.copy(alpha = 0.65f), fontSize = 12.sp, maxLines = 1)
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(20.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text("Prev", color = Color.White, modifier = Modifier.clickable { activeController?.transportControls?.skipToPrevious() })
                                Text(
                                    if (playing) "Pause" else "Play",
                                    color = Color(0xFF38BDF8),
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.clickable {
                                        if (playing) activeController?.transportControls?.pause()
                                        else activeController?.transportControls?.play()
                                    }
                                )
                                Text("Next", color = Color.White, modifier = Modifier.clickable { activeController?.transportControls?.skipToNext() })
                            }
                        }
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Spacer(modifier = Modifier.width(18.dp)) // Clears Poco X3 Pro center camera punch-hole
                            if (title != null) {
                                Text(
                                    text = if (playing) "Playing · $title" else title ?: "",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.widthIn(max = 130.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        windowManager.addView(islandView, params)
    }

    private fun observeMediaSessions() {
        val listenerComponent = ComponentName(this, LauncherNotificationListener::class.java)
        try {
            val controllers = mediaSessionManager.getActiveSessions(listenerComponent)
            updateActiveController(controllers.firstOrNull())
            mediaSessionManager.addOnActiveSessionsChangedListener({ list ->
                updateActiveController(list?.firstOrNull())
            }, listenerComponent)
        } catch (_: SecurityException) {
            // Requires Notification Access permission granted by user in MIUI Settings
        }
    }

    private fun updateActiveController(controller: MediaController?) {
        activeController = controller
        val meta = controller?.metadata
        activeTitle.value = meta?.getString(MediaMetadata.METADATA_KEY_TITLE)
        activeArtist.value = meta?.getString(MediaMetadata.METADATA_KEY_ARTIST)
        isPlaying.value = controller?.playbackState?.state == PlaybackState.STATE_PLAYING
    }

    private fun startForegroundChannel() {
        val channelId = "pocoos_island_channel"
        val channel = NotificationChannel(channelId, "PocoOS Dynamic Island", NotificationManager.IMPORTANCE_LOW)
        (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(channel)
        val notification = Notification.Builder(this, channelId)
            .setContentTitle("PocoOS Dynamic Island Active")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .build()
        startForeground(101, notification)
    }

    override fun onDestroy() {
        super.onDestroy()
        islandView?.let { windowManager.removeView(it) }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
