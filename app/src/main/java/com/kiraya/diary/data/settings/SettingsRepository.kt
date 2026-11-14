package com.kiraya.diary.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
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
) {
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
}
