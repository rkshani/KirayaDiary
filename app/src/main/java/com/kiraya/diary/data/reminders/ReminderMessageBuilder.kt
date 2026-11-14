package com.kiraya.diary.data.reminders

import android.content.Context
import android.content.res.Configuration
import com.kiraya.diary.R
import com.kiraya.diary.core.date.DateUtils
import com.kiraya.diary.core.money.Money
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/** Builds the WhatsApp/SMS rent reminder in English or Urdu, whatever the app language is. */
@Singleton
class ReminderMessageBuilder @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun build(
        language: String,
        tenantName: String,
        amount: Long,
        months: List<Int>,
        currencySymbol: String,
        landlordName: String,
        unitName: String? = null,
    ): String {
        val locale = Locale.forLanguageTag(if (language == "ur") "ur" else "en")
        val ctx = localized(locale)
        val monthsText = months.joinToString(", ") { DateUtils.formatMonth(it, locale) }
        val amountText = Money.formatWithSymbol(amount, currencySymbol, locale = Locale.US)
        val lines = buildList {
            add(ctx.getString(R.string.msg_greeting, tenantName))
            if (unitName.isNullOrBlank()) {
                add(ctx.getString(R.string.msg_due, amountText, monthsText))
            } else {
                add(ctx.getString(R.string.msg_due_unit, unitName, amountText, monthsText))
            }
            add(ctx.getString(R.string.msg_request))
            add(ctx.getString(R.string.msg_thanks))
            if (landlordName.isNotBlank()) add(landlordName)
        }
        return lines.joinToString("\n")
    }

    private fun localized(locale: Locale): Context {
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        return context.createConfigurationContext(config)
    }
}
