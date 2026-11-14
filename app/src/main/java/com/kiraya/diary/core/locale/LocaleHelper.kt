package com.kiraya.diary.core.locale

import android.app.LocaleManager
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import java.util.Locale

/**
 * In-app language (English / Urdu) without AppCompat.
 * The choice is kept in plain SharedPreferences because it must be read before any screen
 * is created. On Android 13+ the system per-app language is set as well.
 */
object LocaleHelper {
    private const val PREFS = "locale_prefs"
    private const val KEY = "app_language"

    /** The app is English-only for now, whatever the phone language is. */
    private const val ONLY_LANGUAGE = "en"

    /** "" = follow the phone, otherwise "en" or "ur". */
    fun saved(context: Context): String =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, "").orEmpty()

    fun save(context: Context, code: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY, code).apply()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.getSystemService(LocaleManager::class.java)?.applicationLocales =
                if (code.isEmpty()) LocaleList.getEmptyLocaleList() else LocaleList.forLanguageTags(code)
        }
    }

    /** Wraps a base context so resources use the saved language (Android 12 and below). */
    fun wrap(base: Context): Context {
        // Only English for now; Urdu comes back in a later update. Then use the saved choice again:
        // val code = saved(base)
        // if (code.isEmpty() || Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) return base
        val code = ONLY_LANGUAGE
        val locale = Locale.forLanguageTag(code)
        Locale.setDefault(locale)
        val config = Configuration(base.resources.configuration)
        config.setLocale(locale)
        config.setLayoutDirection(locale)
        return base.createConfigurationContext(config)
    }
}
