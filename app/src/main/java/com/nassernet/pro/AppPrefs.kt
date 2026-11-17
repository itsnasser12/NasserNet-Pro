
package com.nassernet.pro

import android.content.Context

object AppPrefs {

    private const val PREFS_NAME = "nassernet_settings"
    private const val KEY_SELECTED_GAMES = "selected_games"

    fun saveSelectedGames(
        context: Context,
        packageNames: Set<String>
    ) {
        context.getSharedPreferences(
            PREFS_NAME,
            Context.MODE_PRIVATE
        ).edit()
            .putStringSet(
                KEY_SELECTED_GAMES,
                packageNames.toSet()
            )
            .apply()
    }

    fun getSelectedGames(context: Context): Set<String> {
        return context.getSharedPreferences(
            PREFS_NAME,
            Context.MODE_PRIVATE
        ).getStringSet(
            KEY_SELECTED_GAMES,
            emptySet()
        )?.toSet() ?: emptySet()
    }
}
