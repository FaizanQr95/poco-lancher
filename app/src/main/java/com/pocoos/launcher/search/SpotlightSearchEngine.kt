package com.pocoos.launcher.search

import com.pocoos.launcher.categorizer.CategorizedApp

object SpotlightSearchEngine {
    fun query(rawQuery: String, apps: List<CategorizedApp>): List<CategorizedApp> {
        val normalized = rawQuery.trim().lowercase()
        if (normalized.isEmpty()) return apps.take(8)
        return apps
            .filter {
                it.label.lowercase().contains(normalized) ||
                    it.category.title.lowercase().contains(normalized) ||
                    it.packageName.lowercase().contains(normalized)
            }
            .sortedBy { !it.label.lowercase().startsWith(normalized) }
    }
}
