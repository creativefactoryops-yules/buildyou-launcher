package force.buildyou.launcher

import android.content.Context

/**
 * Persisted launcher preferences: hidden packages and icon order.
 * SharedPreferences (not DataStore) keeps v1 dependency-free; the spec allows either.
 */
class LauncherPrefs(context: Context) {

    private val prefs = context.getSharedPreferences("buildyou_launcher", Context.MODE_PRIVATE)

    fun loadHidden(): Set<String> = prefs.getStringSet(KEY_HIDDEN, emptySet()) ?: emptySet()

    fun saveHidden(hidden: Set<String>) {
        prefs.edit().putStringSet(KEY_HIDDEN, hidden).apply()
    }

    fun loadOrder(): List<String> =
        prefs.getString(KEY_ORDER, null)?.split(',')?.filter { it.isNotBlank() } ?: emptyList()

    fun saveOrder(order: List<String>) {
        prefs.edit().putString(KEY_ORDER, order.joinToString(",")).apply()
    }

    private companion object {
        const val KEY_HIDDEN = "hidden_packages"
        const val KEY_ORDER = "deck_order"
    }
}
