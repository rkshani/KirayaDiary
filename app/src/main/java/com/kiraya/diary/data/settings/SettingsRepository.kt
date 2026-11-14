package com.kiraya.diary.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.kiraya.diary.domain.model.ThemeMode
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

data class AppSettings(
    val landlordName: String = "",
    val landlordPhone: String = "",
    val currencySymbol: String = DEFAULT_SYMBOL,
    val groupDigits: Boolean = true,
    val remindersEnabled: Boolean = true,
    /** Days before the due date to remind (0 = on the due day). */
    val reminderDaysBefore: Int = 1,
    /** Warn this many days before a lease ends. */
    val leaseAlertDays: Int = 30,
    /** "en" or "ur": language of the WhatsApp reminder message. */
    val messageLanguage: String = "ur",
    // App lock
    val lockEnabled: Boolean = false,
    val pinHash: String = "",
    val pinSalt: String = "",
    val biometricEnabled: Boolean = false,
    val hideAmounts: Boolean = false,
    // Appearance and first run
    /** "" = follow the phone, "en" or "ur". */
    val appLanguage: String = "",
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val onboardingDone: Boolean = false,
) {
    val hasPin: Boolean get() = pinHash.isNotEmpty()

    companion object {
        const val DEFAULT_SYMBOL = "Rs"
    }
}

@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private object Keys {
        val LANDLORD_NAME = stringPreferencesKey("landlord_name")
        val LANDLORD_PHONE = stringPreferencesKey("landlord_phone")
        val CURRENCY = stringPreferencesKey("currency_symbol")
        val GROUP_DIGITS = booleanPreferencesKey("group_digits")
        val REMINDERS = booleanPreferencesKey("reminders_enabled")
        val REMINDER_DAYS = intPreferencesKey("reminder_days_before")
        val LEASE_ALERT_DAYS = intPreferencesKey("lease_alert_days")
        val MESSAGE_LANG = stringPreferencesKey("message_language")
        val LOCK_ENABLED = booleanPreferencesKey("lock_enabled")
        val PIN_HASH = stringPreferencesKey("pin_hash")
        val PIN_SALT = stringPreferencesKey("pin_salt")
        val BIOMETRIC = booleanPreferencesKey("biometric_enabled")
        val HIDE_AMOUNTS = booleanPreferencesKey("hide_amounts")
        val APP_LANGUAGE = stringPreferencesKey("app_language")
        val THEME = stringPreferencesKey("theme_mode")
        val ONBOARDING_DONE = booleanPreferencesKey("onboarding_done")
    }

    val settings: Flow<AppSettings> = context.dataStore.data.map { p ->
        AppSettings(
            landlordName = p[Keys.LANDLORD_NAME].orEmpty(),
            landlordPhone = p[Keys.LANDLORD_PHONE].orEmpty(),
            currencySymbol = p[Keys.CURRENCY] ?: AppSettings.DEFAULT_SYMBOL,
            groupDigits = p[Keys.GROUP_DIGITS] ?: true,
            remindersEnabled = p[Keys.REMINDERS] ?: true,
            reminderDaysBefore = p[Keys.REMINDER_DAYS] ?: 1,
            leaseAlertDays = p[Keys.LEASE_ALERT_DAYS] ?: 30,
            messageLanguage = p[Keys.MESSAGE_LANG] ?: "ur",
            lockEnabled = p[Keys.LOCK_ENABLED] ?: false,
            pinHash = p[Keys.PIN_HASH].orEmpty(),
            pinSalt = p[Keys.PIN_SALT].orEmpty(),
            biometricEnabled = p[Keys.BIOMETRIC] ?: false,
            hideAmounts = p[Keys.HIDE_AMOUNTS] ?: false,
            appLanguage = p[Keys.APP_LANGUAGE].orEmpty(),
            themeMode = p[Keys.THEME]?.let { name -> ThemeMode.entries.firstOrNull { it.name == name } } ?: ThemeMode.SYSTEM,
            onboardingDone = p[Keys.ONBOARDING_DONE] ?: false,
        )
    }

    suspend fun current(): AppSettings = settings.first()

    suspend fun setLandlord(name: String, phone: String) = context.dataStore.edit {
        it[Keys.LANDLORD_NAME] = name.trim()
        it[Keys.LANDLORD_PHONE] = phone.trim()
    }

    suspend fun setCurrency(symbol: String, groupDigits: Boolean) = context.dataStore.edit {
        it[Keys.CURRENCY] = symbol.trim().ifEmpty { AppSettings.DEFAULT_SYMBOL }
        it[Keys.GROUP_DIGITS] = groupDigits
    }

    suspend fun setReminders(enabled: Boolean, daysBefore: Int, leaseAlertDays: Int, messageLanguage: String) =
        context.dataStore.edit {
            it[Keys.REMINDERS] = enabled
            it[Keys.REMINDER_DAYS] = daysBefore.coerceIn(0, 10)
            it[Keys.LEASE_ALERT_DAYS] = leaseAlertDays.coerceIn(1, 120)
            it[Keys.MESSAGE_LANG] = messageLanguage
        }

    /** Saves a new PIN and turns the lock on. */
    suspend fun setPin(hash: String, salt: String) = context.dataStore.edit {
        it[Keys.PIN_HASH] = hash
        it[Keys.PIN_SALT] = salt
        it[Keys.LOCK_ENABLED] = true
    }

    /** Turns the lock off and forgets the PIN. */
    suspend fun clearLock() = context.dataStore.edit {
        it[Keys.LOCK_ENABLED] = false
        it[Keys.PIN_HASH] = ""
        it[Keys.PIN_SALT] = ""
        it[Keys.BIOMETRIC] = false
    }

    suspend fun setBiometric(enabled: Boolean) = context.dataStore.edit { it[Keys.BIOMETRIC] = enabled }

    suspend fun setHideAmounts(hidden: Boolean) = context.dataStore.edit { it[Keys.HIDE_AMOUNTS] = hidden }

    suspend fun setAppLanguage(code: String) = context.dataStore.edit { it[Keys.APP_LANGUAGE] = code }

    suspend fun setThemeMode(mode: ThemeMode) = context.dataStore.edit { it[Keys.THEME] = mode.name }

    suspend fun setOnboardingDone() = context.dataStore.edit { it[Keys.ONBOARDING_DONE] = true }
}
