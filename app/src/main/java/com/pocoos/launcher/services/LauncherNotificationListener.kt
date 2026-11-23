package com.pocoos.launcher.services

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class GroupedAlert(
    val packageName: String,
    val title: String,
    val messages: List<String>,
    val postTime: Long
)

class LauncherNotificationListener : NotificationListenerService() {

    companion object {
        private val _groupedAlerts = MutableStateFlow<List<GroupedAlert>>(emptyList())
        val groupedAlerts: StateFlow<List<GroupedAlert>> = _groupedAlerts
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        refreshGroupedNotifications()
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        refreshGroupedNotifications()
    }

    private fun refreshGroupedNotifications() {
        val active = activeNotifications ?: return
        val grouped = active
            .filter { !it.isOngoing }
            .groupBy { it.groupKey ?: it.packageName }
            .map { (_, items) ->
                val latest = items.maxByOrNull { it.postTime }!!
                val extras = latest.notification.extras
                GroupedAlert(
                    packageName = latest.packageName,
                    title = extras.getString("android.title") ?: latest.packageName,
                    messages = items.mapNotNull { it.notification.extras.getCharSequence("android.text")?.toString() },
                    postTime = latest.postTime
                )
            }
            .sortedByDescending { it.postTime }

        _groupedAlerts.value = grouped
    }
}
