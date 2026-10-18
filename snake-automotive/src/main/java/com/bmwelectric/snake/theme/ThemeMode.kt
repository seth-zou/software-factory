package com.bmwelectric.snake.theme

/**
 * Manually selectable cockpit theme. A null manual selection means the app follows the Android
 * system theme.
 */
enum class ThemeMode(val storageValue: String, val dayMode: Boolean) {
    Day("day", true),
    Dark("dark", false),
    ;

    companion object {
        /**
         * Parses a persisted manual selection. Blank or unknown values are treated as "no
         * preference" so a corrupted entry safely falls back to the system theme.
         */
        fun fromStorage(raw: String?): ThemeMode? = when (raw?.trim()?.lowercase()) {
            Day.storageValue -> Day
            Dark.storageValue -> Dark
            else -> null
        }
    }
}

/**
 * Resolves the effective theme: an explicit manual selection always wins; without one the app
 * follows the current Android system dark mode.
 */
fun resolveDayMode(manual: ThemeMode?, systemDarkMode: Boolean): Boolean =
    manual?.dayMode ?: !systemDarkMode

internal const val THEME_PREFS_NAME = "snake-local-theme"
internal const val THEME_PREFERENCE_KEY = "bmwSnakeThemePreference"
