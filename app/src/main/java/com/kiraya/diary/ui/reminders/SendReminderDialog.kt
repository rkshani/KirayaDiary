package com.kiraya.diary.ui.reminders

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.kiraya.diary.R
import com.kiraya.diary.ui.util.Share

/**
 * Shows the reminder message (English or Urdu, editable) and sends it by WhatsApp, SMS or share.
 * [buildMessage] makes the message for a language code ("en" / "ur").
 */
@Composable
fun SendReminderDialog(
    phone: String,
    defaultLanguage: String,
    buildMessage: (String) -> String,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    var language by remember { mutableStateOf(defaultLanguage) }
    var message by remember { mutableStateOf(buildMessage(defaultLanguage)) }
    val chooser = stringResource(R.string.reminder_send_title)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.reminder_send_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("ur" to R.string.language_urdu, "en" to R.string.language_english).forEach { (code, label) ->
                        FilterChip(
                            selected = language == code,
                            onClick = {
                                language = code
                                message = buildMessage(code)
                            },
                            label = { Text(stringResource(label)) },
                        )
                    }
                }
                OutlinedTextField(
                    value = message,
                    onValueChange = { message = it },
                    minLines = 5,
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (phone.isNotBlank()) {
                        TextButton(onClick = {
                            Share.sms(context, phone, message)
                            onDismiss()
                        }) { Text(stringResource(R.string.reminder_sms)) }
                    }
                    TextButton(onClick = {
                        Share.shareText(context, message, chooser)
                        onDismiss()
                    }) { Text(stringResource(R.string.action_share)) }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                Share.textToWhatsApp(context, phone, message, chooser)
                onDismiss()
            }) { Text(stringResource(R.string.receipt_whatsapp)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}
