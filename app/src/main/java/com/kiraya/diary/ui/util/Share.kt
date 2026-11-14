package com.kiraya.diary.ui.util

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.kiraya.diary.core.phone.PhoneNumbers
import java.io.File

object Share {
    private const val WHATSAPP = "com.whatsapp"
    private const val WHATSAPP_BUSINESS = "com.whatsapp.w4b"

    fun uriFor(context: Context, file: File): Uri =
        FileProvider.getUriForFile(context, context.packageName + ".fileprovider", file)

    /** Opens the share sheet for a file. */
    fun shareFile(context: Context, file: File, mime: String, chooserTitle: String, text: String? = null) {
        val uri = uriFor(context, file)
        val send = Intent(Intent.ACTION_SEND).apply {
            type = mime
            putExtra(Intent.EXTRA_STREAM, uri)
            if (text != null) putExtra(Intent.EXTRA_TEXT, text)
            clipData = ClipData.newRawUri(file.name, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        Intents.start(context, Intent.createChooser(send, chooserTitle))
    }

    /**
     * Sends a file straight to WhatsApp (to the tenant's chat when a phone is known).
     * Falls back to the share sheet if WhatsApp is not installed.
     */
    fun shareFileToWhatsApp(
        context: Context,
        file: File,
        mime: String,
        phone: String?,
        chooserTitle: String,
        text: String? = null,
    ) {
        val uri = uriFor(context, file)
        val jid = phone?.let(PhoneNumbers::toInternational)?.let { "$it@s.whatsapp.net" }
        for (pkg in listOf(WHATSAPP, WHATSAPP_BUSINESS)) {
            val send = Intent(Intent.ACTION_SEND).apply {
                type = mime
                setPackage(pkg)
                putExtra(Intent.EXTRA_STREAM, uri)
                if (text != null) putExtra(Intent.EXTRA_TEXT, text)
                if (jid != null) putExtra("jid", jid)
                clipData = ClipData.newRawUri(file.name, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            if (Intents.start(context, send)) return
        }
        shareFile(context, file, mime, chooserTitle, text)
    }

    /** Opens a WhatsApp chat with a ready message. Falls back to the share sheet. */
    fun textToWhatsApp(context: Context, phone: String?, message: String, chooserTitle: String) {
        val number = phone?.let(PhoneNumbers::toInternational)
        if (number != null) {
            val uri = Uri.parse("https://wa.me/$number?text=" + Uri.encode(message))
            for (pkg in listOf(WHATSAPP, WHATSAPP_BUSINESS)) {
                if (Intents.start(context, Intent(Intent.ACTION_VIEW, uri).setPackage(pkg))) return
            }
        } else {
            for (pkg in listOf(WHATSAPP, WHATSAPP_BUSINESS)) {
                val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, message).setPackage(pkg)
                if (Intents.start(context, send)) return
            }
        }
        shareText(context, message, chooserTitle)
    }

    fun shareText(context: Context, message: String, chooserTitle: String) {
        val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, message)
        Intents.start(context, Intent.createChooser(send, chooserTitle))
    }

    /** Opens the SMS app with a ready message. */
    fun sms(context: Context, phone: String, message: String) {
        val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:" + phone.filter { it.isDigit() || it == '+' }))
            .putExtra("sms_body", message)
        Intents.start(context, intent)
    }
}
