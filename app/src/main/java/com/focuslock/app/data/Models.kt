package com.focuslock.app.data

import kotlinx.serialization.Serializable

/** One installed app the user can choose to block. */
@Serializable
data class BlockableApp(
    val packageName: String,
    val label: String
)

@Serializable
enum class FocusModeType { STUDY, WORK, DEEP_FOCUS, CUSTOM, POMODORO_FOCUS, POMODORO_BREAK }

/** The session currently running (or null if no session is active). */
@Serializable
data class ActiveSession(
    val task: String,
    val modeType: FocusModeType,
    val startTimeMillis: Long,
    val endTimeMillis: Long,
    val blockedPackages: Set<String>,
    val isPomodoro: Boolean = false,
    val pomodoroFocusMinutes: Int = 25,
    val pomodoroBreakMinutes: Int = 5,
    val isBreak: Boolean = false,
    /** A short break the person took by choice in the middle of a normal (non-pomodoro) session. */
    val manualBreak: Boolean = false,
    val breakStartMillis: Long = 0L,
    val focusRemainingMillis: Long = 0L
) {
    val durationMillis: Long get() = endTimeMillis - startTimeMillis

    /** Pauses blocking for [minutes] and remembers how much focus time was left. */
    fun startedBreak(now: Long, minutes: Int): ActiveSession = copy(
        isBreak = true,
        manualBreak = true,
        breakStartMillis = now,
        focusRemainingMillis = (endTimeMillis - now).coerceAtLeast(0L),
        endTimeMillis = now + minutes * 60_000L
    )

    /** Ends the break: focus time continues exactly where it stopped, so the break never eats into it. */
    fun resumedAfterBreak(now: Long): ActiveSession {
        val breakLen = (now - breakStartMillis).coerceAtLeast(0L)
        return copy(
            isBreak = false,
            manualBreak = false,
            startTimeMillis = startTimeMillis + breakLen,
            endTimeMillis = now + focusRemainingMillis,
            breakStartMillis = 0L,
            focusRemainingMillis = 0L
        )
    }
}

/** A completed or ended session, kept for history + stats. */
@Serializable
data class SessionHistoryEntry(
    val id: String,
    val task: String,
    val dateMillis: Long,
    val plannedDurationMinutes: Int,
    val actualDurationMinutes: Int,
    val completed: Boolean,
    val blockedAppCount: Int,
    val blockedPackages: List<String> = emptyList()
)

/** The user's last-used session config, for Quick Start. */
@Serializable
data class QuickStartConfig(
    val task: String,
    val durationMinutes: Int,
    val blockedPackages: Set<String>
)

data class FocusModePreset(
    val type: FocusModeType,
    val title: String,
    val emoji: String,
    val defaultMinutes: Int
)

val FOCUS_MODE_PRESETS = listOf(
    FocusModePreset(FocusModeType.STUDY, "Study Mode", "\uD83D\uDCDA", 45),
    FocusModePreset(FocusModeType.WORK, "Work Mode", "\uD83D\uDCBB", 50),
    FocusModePreset(FocusModeType.DEEP_FOCUS, "Deep Focus", "\uD83E\uDDE0", 90),
    FocusModePreset(FocusModeType.CUSTOM, "Custom Mode", "\u2699\uFE0F", 30)
)

@Serializable
enum class AppTheme { SYSTEM, LIGHT, DARK }

@Serializable
enum class AccentTheme { VIOLET, BLUE, GREEN, ORANGE }

@Serializable
data class AppSettings(
    val defaultDurationMinutes: Int = 30,
    val defaultBlockedPackages: Set<String> = emptySet(),
    val theme: AppTheme = AppTheme.SYSTEM,
    val accentTheme: AccentTheme = AccentTheme.VIOLET,
    val soundEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val notificationsEnabled: Boolean = true,
    val pomodoroFocusMinutes: Int = 25,
    val pomodoroBreakMinutes: Int = 5,
    val profileName: String = "",
    val profilePhotoVersion: Long = 0L
)
