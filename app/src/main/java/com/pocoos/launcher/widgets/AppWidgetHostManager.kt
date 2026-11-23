package com.pocoos.launcher.widgets

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.content.Context

class AppWidgetHostManager(context: Context) {
    companion object {
        private const val HOST_ID = 1048
    }

    val appWidgetManager: AppWidgetManager = AppWidgetManager.getInstance(context)
    val appWidgetHost: AppWidgetHost = AppWidgetHost(context, HOST_ID)

    fun startListening() = appWidgetHost.startListening()
    fun stopListening() = appWidgetHost.stopListening()
}
