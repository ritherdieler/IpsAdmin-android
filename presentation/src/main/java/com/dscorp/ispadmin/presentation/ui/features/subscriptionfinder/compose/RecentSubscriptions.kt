package com.dscorp.ispadmin.presentation.ui.features.subscriptionfinder.compose

import com.dscorp.ispadmin.domain.model.ServiceStatus

data class RecentSubscription(val id: Int, val name: String, val subtitle: String, val status: ServiceStatus)

interface RecentSubscriptionsStore {
    fun load(): List<RecentSubscription>
    fun save(items: List<RecentSubscription>)
}

data class QuickAccess(val status: ServiceStatus, val label: String, val count: Long)

class SharedPrefsRecentSubscriptionsStore(private val prefs: android.content.SharedPreferences) : RecentSubscriptionsStore {
    private val gson = com.google.gson.Gson()

    override fun load(): List<RecentSubscription> {
        val json = prefs.getString(KEY, null) ?: return emptyList()
        return runCatching {
            gson.fromJson(json, Array<RecentSubscription>::class.java)?.toList().orEmpty()
        }.getOrDefault(emptyList())
    }

    override fun save(items: List<RecentSubscription>) {
        prefs.edit().putString(KEY, gson.toJson(items)).apply()
    }

    companion object {
        const val KEY = "subscription_search_recents"
    }
}
