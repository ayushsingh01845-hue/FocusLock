package com.focuslock.app.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

private val Context.dataStore by preferencesDataStore(name = "focuslock_prefs")

/**
 * Everything FocusLock persists lives on-device in DataStore. Nothing here is ever
 * uploaded anywhere - there is no network code in this app at all.
 */
class PreferencesManager(private val context: Context) {

    private val json = Json { ignoreUnknownKeys = true }

    private object Keys {
        val ACTIVE_SESSION = stringPreferencesKey("active_session_json")
        val HISTORY = stringPreferencesKey("history_json")
        val QUICK_START = stringPreferencesKey("quick_start_json")
        val SETTINGS = stringPreferencesKey("settings_json")
        val BLOCKED_PACKAGES = stringSetPreferencesKey("blocked_packages")
        val STREAK = intPreferencesKey("streak")
        val LAST_SESSION_DAY = stringPreferencesKey("last_session_day")
        val ONBOARDED = booleanPreferencesKey("onboarded")
    }

    // ---------- Active session ----------

    val activeSessionFlow: Flow<ActiveSession?> = context.dataStore.data.map { prefs ->
        prefs[Keys.ACTIVE_SESSION]?.let { runCatching { json.decodeFromString<ActiveSession>(it) }.getOrNull() }
    }

    suspend fun setActiveSession(session: ActiveSession?) {
        context.dataStore.edit { prefs ->
            if (session == null) prefs.remove(Keys.ACTIVE_SESSION)
            else prefs[Keys.ACTIVE_SESSION] = json.encodeToString(session)
        }
    }

    suspend fun getActiveSessionOnce(): ActiveSession? = activeSessionFlow.first()

    // ---------- History ----------

    val historyFlow: Flow<List<SessionHistoryEntry>> = context.dataStore.data.map { prefs ->
        prefs[Keys.HISTORY]?.let {
            runCatching { json.decodeFromString(ListSerializer(SessionHistoryEntry.serializer()), it) }.getOrNull()
        } ?: emptyList()
    }

    suspend fun addHistoryEntry(entry: SessionHistoryEntry) {
        context.dataStore.edit { prefs ->
            val current = prefs[Keys.HISTORY]?.let {
                runCatching { json.decodeFromString(ListSerializer(SessionHistoryEntry.serializer()), it) }.getOrNull()
            } ?: emptyList()
            val updated = (listOf(entry) + current).take(500)
            prefs[Keys.HISTORY] = json.encodeToString(ListSerializer(SessionHistoryEntry.serializer()), updated)
        }
        if (entry.completed) bumpStreak(entry.dateMillis)
    }

    suspend fun clearHistory() {
        context.dataStore.edit { prefs -> prefs.remove(Keys.HISTORY) }
    }

    // ---------- Streak ----------

    val streakFlow: Flow<Int> = context.dataStore.data.map { it[Keys.STREAK] ?: 0 }

    private suspend fun bumpStreak(dateMillis: Long) {
        val today = dayKey(dateMillis)
        context.dataStore.edit { prefs ->
            val lastDay = prefs[Keys.LAST_SESSION_DAY]
            val currentStreak = prefs[Keys.STREAK] ?: 0
            if (lastDay == today) return@edit // already counted today
            val yesterday = dayKey(dateMillis - 24L * 60 * 60 * 1000)
            val newStreak = if (lastDay == yesterday) currentStreak + 1 else 1
            prefs[Keys.STREAK] = newStreak
            prefs[Keys.LAST_SESSION_DAY] = today
        }
    }

    private fun dayKey(millis: Long): String {
        val cal = java.util.Calendar.getInstance()
        cal.timeInMillis = millis
        return "${cal.get(java.util.Calendar.YEAR)}-${cal.get(java.util.Calendar.DAY_OF_YEAR)}"
    }

    // ---------- Quick start ----------

    val quickStartFlow: Flow<QuickStartConfig?> = context.dataStore.data.map { prefs ->
        prefs[Keys.QUICK_START]?.let { runCatching { json.decodeFromString<QuickStartConfig>(it) }.getOrNull() }
    }

    suspend fun setQuickStart(config: QuickStartConfig) {
        context.dataStore.edit { prefs -> prefs[Keys.QUICK_START] = json.encodeToString(config) }
    }

    // ---------- Blocked app selection (persists between session setups) ----------

    val blockedPackagesFlow: Flow<Set<String>> = context.dataStore.data.map { it[Keys.BLOCKED_PACKAGES] ?: emptySet() }

    suspend fun setBlockedPackages(packages: Set<String>) {
        context.dataStore.edit { prefs -> prefs[Keys.BLOCKED_PACKAGES] = packages }
    }

    // ---------- Settings ----------

    val settingsFlow: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        prefs[Keys.SETTINGS]?.let { runCatching { json.decodeFromString<AppSettings>(it) }.getOrNull() } ?: AppSettings()
    }

    suspend fun updateSettings(update: (AppSettings) -> AppSettings) {
        context.dataStore.edit { prefs ->
            val current = prefs[Keys.SETTINGS]?.let {
                runCatching { json.decodeFromString<AppSettings>(it) }.getOrNull()
            } ?: AppSettings()
            prefs[Keys.SETTINGS] = json.encodeToString(update(current))
        }
    }

    suspend fun resetAll() {
        context.dataStore.edit { it.clear() }
    }

    companion object {
        @Volatile private var INSTANCE: PreferencesManager? = null
        fun get(context: Context): PreferencesManager =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: PreferencesManager(context.applicationContext).also { INSTANCE = it }
            }
    }
}
