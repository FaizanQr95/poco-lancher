package com.pocoos.launcher.categorizer

import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager

enum class AppCategory(val title: String) {
    SOCIAL("Social"),
    PRODUCTIVITY("Productivity"),
    ENTERTAINMENT("Entertainment"),
    UTILITIES("Utilities"),
    SYSTEM("Poco System")
}

data class CategorizedApp(
    val label: String,
    val packageName: String,
    val category: AppCategory
)

object AppCategorizer {
    fun loadAndGroupApps(pm: PackageManager): Map<AppCategory, List<CategorizedApp>> {
        val intent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val apps = pm.queryIntentActivities(intent, 0).map { resolveInfo ->
            val appInfo = resolveInfo.activityInfo.applicationInfo
            val pkg = appInfo.packageName
            CategorizedApp(
                label = resolveInfo.loadLabel(pm).toString(),
                packageName = pkg,
                category = inferCategory(appInfo, pkg)
            )
        }.sortedBy { it.label }

        return apps.groupBy { it.category }
    }

    private fun inferCategory(info: ApplicationInfo, pkg: String): AppCategory {
        return when {
            pkg.contains("miui") || pkg.contains("xiaomi") || pkg.contains("poco") -> AppCategory.SYSTEM
            info.category == ApplicationInfo.CATEGORY_SOCIAL || pkg.contains("whatsapp") || pkg.contains("instagram") -> AppCategory.SOCIAL
            info.category == ApplicationInfo.CATEGORY_AUDIO || info.category == ApplicationInfo.CATEGORY_VIDEO || pkg.contains("youtube") || pkg.contains("spotify") -> AppCategory.ENTERTAINMENT
            info.category == ApplicationInfo.CATEGORY_PRODUCTIVITY || pkg.contains("gmail") || pkg.contains("docs") -> AppCategory.PRODUCTIVITY
            else -> AppCategory.UTILITIES
        }
    }
}
