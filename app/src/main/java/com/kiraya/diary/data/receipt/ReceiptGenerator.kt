package com.kiraya.diary.data.receipt

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.text.Layout
import android.text.StaticLayout
import android.text.TextDirectionHeuristics
import android.text.TextPaint
import com.kiraya.diary.R
import com.kiraya.diary.core.coroutines.IoDispatcher
import com.kiraya.diary.core.date.DateUtils
import com.kiraya.diary.core.money.Money
import com.kiraya.diary.domain.model.Payment
import com.kiraya.diary.domain.model.PaymentMethod
import com.kiraya.diary.domain.model.TenantWithUnit
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

data class ReceiptData(
    val payment: Payment,
    val tenant: TenantWithUnit,
    /** What the tenant still owes after all payments so far. */
    val balanceAfter: Long,
    val landlordName: String = "",
    val currencySymbol: String = "Rs",
)

/** Draws a one-page A5 rent receipt with PdfDocument. Text follows the app language (RTL for Urdu). */
@Singleton
class ReceiptGenerator @Inject constructor(
    @ApplicationContext private val context: Context,
    @IoDispatcher private val io: CoroutineDispatcher,
) {
    suspend fun create(data: ReceiptData, localized: Context = context): File = withContext(io) {
        val dir = File(context.cacheDir, "shared").apply { mkdirs() }
        val file = File(dir, "receipt_${receiptNumber(data.payment.id)}.pdf")
        val doc = PdfDocument()
        val page = doc.startPage(PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, 1).create())
        draw(page.canvas, data, localized)
        doc.finishPage(page)
        file.outputStream().use { doc.writeTo(it) }
        doc.close()
        file
    }

    private fun draw(canvas: Canvas, d: ReceiptData, ctx: Context) {
        val rtl = ctx.resources.configuration.layoutDirection == android.view.View.LAYOUT_DIRECTION_RTL
        val teal = Color.rgb(0, 106, 96)
        val grey = Color.rgb(90, 96, 95)
        fun money(p: Long) = Money.formatWithSymbol(p, d.currencySymbol)

        // Header band
        val band = Paint().apply { color = teal }
        canvas.drawRect(0f, 0f, PAGE_W.toFloat(), 86f, band)
        var y = 22f
        y = text(canvas, ctx.getString(R.string.receipt_heading), 20f, Color.WHITE, true, y, rtl)
        val sub = d.landlordName.ifBlank { ctx.getString(R.string.app_name) }
        text(canvas, sub, 11f, Color.WHITE, false, y + 2f, rtl)

        y = 108f
        y = row(canvas, ctx.getString(R.string.receipt_number), receiptNumber(d.payment.id), y, rtl, grey)
        y = row(canvas, ctx.getString(R.string.receipt_date), DateUtils.formatDate(d.payment.date), y, rtl, grey)
        y = divider(canvas, y)
        y = row(canvas, ctx.getString(R.string.receipt_received_from), d.tenant.tenant.name, y, rtl, grey)
        val place = listOfNotNull(d.tenant.propertyName, d.tenant.unitName).joinToString(" · ")
        if (place.isNotEmpty()) y = row(canvas, ctx.getString(R.string.tenant_unit), place, y, rtl, grey)
        if (d.tenant.tenant.phone.isNotBlank()) {
            y = row(canvas, ctx.getString(R.string.tenant_phone), d.tenant.tenant.phone, y, rtl, grey)
        }
        y = divider(canvas, y)

        // Amount box
        val box = Paint().apply { color = Color.rgb(230, 247, 244) }
        canvas.drawRoundRect(MARGIN, y + 4f, PAGE_W - MARGIN, y + 74f, 10f, 10f, box)
        text(canvas, ctx.getString(R.string.receipt_amount), 11f, grey, false, y + 14f, rtl)
        text(canvas, money(d.payment.amount), 26f, teal, true, y + 32f, rtl)
        y += 88f

        y = row(canvas, ctx.getString(R.string.payment_method), ctx.getString(methodRes(d.payment.method)), y, rtl, grey)
        if (d.payment.note.isNotBlank()) y = row(canvas, ctx.getString(R.string.payment_note), d.payment.note, y, rtl, grey)
        val balance = if (d.balanceAfter > 0) {
            money(d.balanceAfter)
        } else if (d.balanceAfter < 0) {
            ctx.getString(R.string.receipt_advance, money(-d.balanceAfter))
        } else {
            ctx.getString(R.string.receipt_clear)
        }
        y = row(canvas, ctx.getString(R.string.receipt_balance), balance, y, rtl, grey)

        // Signature line and footer
        val line = Paint().apply { color = grey; strokeWidth = 0.8f }
        val sigY = PAGE_H - 92f
        if (rtl) {
            canvas.drawLine(MARGIN, sigY, MARGIN + 150f, sigY, line)
        } else {
            canvas.drawLine(PAGE_W - MARGIN - 150f, sigY, PAGE_W - MARGIN, sigY, line)
        }
        text(canvas, ctx.getString(R.string.receipt_signature), 10f, grey, false, sigY + 4f, !rtl)
        text(canvas, ctx.getString(R.string.receipt_footer), 9f, grey, false, PAGE_H - 40f, rtl, center = true)
    }

    /** Draws wrapped text and returns the y below it. */
    private fun text(
        canvas: Canvas,
        value: String,
        size: Float,
        color: Int,
        bold: Boolean,
        y: Float,
        rtl: Boolean,
        center: Boolean = false,
    ): Float {
        val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            textSize = size
            typeface = if (bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
        }
        val width = (PAGE_W - 2 * MARGIN).toInt()
        val align = if (center) Layout.Alignment.ALIGN_CENTER else Layout.Alignment.ALIGN_NORMAL
        // A fixed direction makes "normal" alignment mean right-aligned in Urdu, even for numbers.
        val direction = if (rtl) TextDirectionHeuristics.RTL else TextDirectionHeuristics.LTR
        val layout = StaticLayout.Builder.obtain(value, 0, value.length, paint, width)
            .setAlignment(align)
            .setTextDirection(direction)
            .build()
        canvas.save()
        canvas.translate(MARGIN, y)
        layout.draw(canvas)
        canvas.restore()
        return y + layout.height
    }

    private fun row(canvas: Canvas, label: String, value: String, y: Float, rtl: Boolean, grey: Int): Float {
        val labelEnd = text(canvas, label, 10f, grey, false, y, rtl)
        return text(canvas, value, 13f, Color.BLACK, true, labelEnd, rtl) + 8f
    }

    private fun divider(canvas: Canvas, y: Float): Float {
        val p = Paint().apply { color = Color.rgb(218, 229, 226); strokeWidth = 1f }
        canvas.drawLine(MARGIN, y + 2f, PAGE_W - MARGIN, y + 2f, p)
        return y + 12f
    }

    private fun methodRes(m: PaymentMethod) = when (m) {
        PaymentMethod.CASH -> R.string.method_cash
        PaymentMethod.BANK -> R.string.method_bank
        PaymentMethod.JAZZCASH -> R.string.method_jazzcash
        PaymentMethod.EASYPAISA -> R.string.method_easypaisa
        PaymentMethod.DEPOSIT -> R.string.method_deposit
        PaymentMethod.OTHER -> R.string.method_other
    }

    companion object {
        // A5 in PostScript points.
        private const val PAGE_W = 420
        private const val PAGE_H = 595
        private const val MARGIN = 28f

        fun receiptNumber(paymentId: Long) = "R-" + paymentId.toString().padStart(6, '0')
    }
}
