package com.focuslock.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.focuslock.app.data.*
import com.focuslock.app.service.FocusForegroundService
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class FocusViewModel(app: Application) : AndroidViewModel(app) {

    private val prefs = PreferencesManager.get(app)
    private val appRepo = AppRepository(app)

    val activeSession: StateFlow<ActiveSession?> =
        prefs.activeSessionFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val history: StateFlow<List<SessionHistoryEntry>> =
        prefs.historyFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val streak: StateFlow<Int> =
        prefs.streakFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val quickStart: StateFlow<QuickStartConfig?> =
        prefs.quickStartFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val settings: StateFlow<AppSettings> =
        prefs.settingsFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppSettings())

    val selectedPackages: StateFlow<Set<String>> =
        prefs.blockedPackagesFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    private val _installedApps = MutableStateFlow<List<BlockableApp>>(emptyList())
    val installedApps: StateFlow<List<BlockableApp>> = _installedApps

    // Draft state for the "create session" flow, kept here so the app-picker screen can be a
    // separate destination without losing what the user already chose.
    val draftTask = MutableStateFlow("")
    val draftMinutes = MutableStateFlow(30)
    val draftMode = MutableStateFlow(FocusModeType.CUSTOM)
    val draftPomodoro = MutableStateFlow(false)

    init {
        loadApps()
    }

    fun loadApps() {
        viewModelScope.launch { _installedApps.value = appRepo.getLaunchableApps() }
    }

    fun appLabel(pkg: String) = appRepo.getAppLabel(pkg)

    fun toggleSelectedPackage(pkg: String) {
        viewModelScope.launch {
            val current = selectedPackages.value
            prefs.setBlockedPackages(if (pkg in current) current - pkg else current + pkg)
        }
    }

    fun setSelectedPackages(pkgs: Set<String>) {
        viewModelScope.launch { prefs.setBlockedPackages(pkgs) }
    }

    fun startSession(
        task: String,
        minutes: Int,
        mode: FocusModeType,
        blocked: Set<String>,
        pomodoro: Boolean,
        pomodoroFocusMin: Int,
        pomodoroBreakMin: Int
    ) {
        val now = System.currentTimeMillis()
        val durationMin = if (pomodoro) pomodoroFocusMin else minutes
        val session = ActiveSession(
            task = task.ifBlank { "Focus Session" },
            modeType = if (pomodoro) FocusModeType.POMODORO_FOCUS else mode,
            startTimeMillis = now,
            endTimeMillis = now + durationMin * 60_000L,
            blockedPackages = blocked,
            isPomodoro = pomodoro,
            pomodoroFocusMinutes = pomodoroFocusMin,
            pomodoroBreakMinutes = pomodoroBreakMin,
            isBreak = false
        )
        viewModelScope.launch {
            prefs.setActiveSession(session)
            prefs.setQuickStart(QuickStartConfig(session.task, minutes, blocked))
        }
        FocusForegroundService.start(app)
    }

    fun endSessionEarly() {
        FocusForegroundService.stop(app)
    }

    fun updateSettings(update: (AppSettings) -> AppSettings) {
        viewModelScope.launch { prefs.updateSettings(update) }
    }

    fun resetAllData() {
        viewModelScope.launch { prefs.resetAll() }
    }

    private val app get() = getApplication<Application>()
}
