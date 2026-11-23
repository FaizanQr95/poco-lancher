package com.pocoos.launcher

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Display
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.view.WindowCompat
import com.pocoos.launcher.categorizer.AppCategorizer
import com.pocoos.launcher.services.ControlCenterOverlayService
import com.pocoos.launcher.services.DynamicIslandOverlayService
import com.pocoos.launcher.services.StatusBarOverlayService
import com.pocoos.launcher.ui.SpringLauncherScreen
import com.pocoos.launcher.widgets.AppWidgetHostManager

class MainActivity : ComponentActivity() {

    private lateinit var widgetHostManager: AppWidgetHostManager

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        requestHighestRefreshRate()

        widgetHostManager = AppWidgetHostManager(this).apply { startListening() }
        val categorizedApps = AppCategorizer.loadAndGroupApps(packageManager)

        startSystemOverlayServices()

        setContent {
            SpringLauncherScreen(
                categorizedApps = categorizedApps,
                widgetHostManager = widgetHostManager,
                onOpenApp = { packageName ->
                    packageManager.getLaunchIntentForPackage(packageName)?.let { intent ->
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        startActivity(intent)
                    }
                }
            )
        }
    }

    /**
     * Requests 120Hz Display.Mode on Xiaomi Poco X3 Pro (vayu) LCD panel
     * so spring animations and gesture swipes run natively at 120fps.
     */
    private fun requestHighestRefreshRate() {
        if (!LauncherConfig.PREFER_120HZ_DISPLAY) return
        val display: Display? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            display
        } else {
            @Suppress("DEPRECATION")
            windowManager.defaultDisplay
        }
        val highestMode = display?.supportedModes?.maxByOrNull { it.refreshRate }
        if (highestMode != null) {
            val params: WindowManager.LayoutParams = window.attributes
            params.preferredDisplayModeId = highestMode.modeId
            window.attributes = params
        }
    }

    private fun startSystemOverlayServices() {
        if (!Settings.canDrawOverlays(this)) return

        if (LauncherConfig.DYNAMIC_ISLAND_ENABLED) {
            startForegroundService(Intent(this, DynamicIslandOverlayService::class.java))
        }
        startForegroundService(Intent(this, ControlCenterOverlayService::class.java))
        startForegroundService(Intent(this, StatusBarOverlayService::class.java))
    }

    override fun onDestroy() {
        super.onDestroy()
        widgetHostManager.stopListening()
    }
}
