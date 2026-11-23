package com.pocoos.launcher.services

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent

class LauncherAccessibilityService : AccessibilityService() {

    companion object {
        var instance: LauncherAccessibilityService? = null
            private set
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit

    override fun onInterrupt() = Unit

    fun triggerHomeGesture() = performGlobalAction(GLOBAL_ACTION_HOME)
    fun triggerAppSwitcher() = performGlobalAction(GLOBAL_ACTION_RECENTS)
    fun triggerBackGesture() = performGlobalAction(GLOBAL_ACTION_BACK)
    fun triggerLockScreen() = performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)
}
