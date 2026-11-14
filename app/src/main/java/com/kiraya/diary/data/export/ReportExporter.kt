package com.kiraya.diary.data.export

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
import android.text.TextUtils
import android.view.View
import com.kiraya.diary.R
import com.kiraya.diary.core.coroutines.IoDispatcher
import com.kiraya.diary.core.date.DateUtils
import com.kiraya.diary.core.money.Money
import com.kiraya.diary.domain.usecase.Report
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/** Writes a report to CSV (opens in Excel / Sheets) or to a PDF. Files go to cache/shared for sharing. */
@Singleton
class ReportExporter @Inject constructor(
    @ApplicationContext private val context: Context,
    @IoDispatcher private val io: CoroutineDispatcher,
) {
    private fun periodLabel(r: Report) =
        if (r.period.isYear) r.period.year.toString() else DateUtils.formatMonth(r.period.monthKey)

    private fun fileName(r: Report, ext: String): String {
        val key = if (r.period.isYear) r.period.year.toString() else r.period.monthKey.toString()
        return "kiraya_report_$key.$ext"
    }

    private fun dir() = File(context.cacheDir, "shared").apply { mkdirs() }

    suspend fun csv(report: Report, ctx: Context, symbol: String): File = withContext(io) {
        fun m(p: Long) = Money.format(p, grouping = false)
        fun q(s: String) = "\"" + s.replace("\"", "\"\"") + "\""
        val sb = StringBuilder()
        sb.append('﻿') // BOM so Excel reads Urdu text correctly.
        sb.appendLine(q(ctx.getString(R.string.report_title) + " - " + periodLabel(report)))
        sb.appendLine(q(ctx.getString(R.string.report_currency, symbol)))
        sb.appendLine()
        sb.appendLine(listOf(R.string.report_charged, R.string.home_collected, R.string.profit_expenses, R.string.profit_net, R.string.report_pending_total).joinToString(",") { q(ctx.getString(it)) })
        sb.appendLine(listOf(report.charged, report.collected, report.expenses, report.profit, report.totalPending).joinToString(",") { m(it) })
        sb.appendLine()
        sb.appendLine(q(ctx.getString(R.string.report_by_property)))
        sb.appendLine(listOf(R.string.bill_property, R.string.report_charged, R.string.home_collected, R.string.profit_expenses, R.string.profit_net).joinToString(",") { q(ctx.getString(it)) })
        report.properties.forEach {
            sb.appendLine(listOf(q(it.propertyName ?: ctx.getString(R.string.profit_no_property)), m(it.charged), m(it.collected), m(it.expenses), m(it.profit)).joinToString(","))
        }
        if (report.months.isNotEmpty()) {
            sb.appendLine()
            sb.appendLine(q(ctx.getString(R.string.report_by_month)))
            sb.appendLine(listOf(R.string.period_month, R.string.report_charged, R.string.home_collected, R.string.profit_expenses).joinToString(",") { q(ctx.getString(it)) })
            report.months.forEach {
                sb.appendLine(listOf(q(DateUtils.formatMonth(it.month)), m(it.charged), m(it.collected), m(it.expenses)).joinToString(","))
            }
        }
        sb.appendLine()
        sb.appendLine(q(ctx.getString(R.string.report_pending)))
        sb.appendLine(listOf(R.string.payment_tenant, R.string.tenant_unit, R.string.tenant_phone, R.string.report_since, R.string.home_pending).joinToString(",") { q(ctx.getString(it)) })
        report.pending.forEach {
            sb.appendLine(listOf(q(it.tenantName), q(it.place), q(it.phone), q(it.oldestMonth?.let(DateUtils::formatMonth).orEmpty()), m(it.amount)).joinToString(","))
        }
        File(dir(), fileName(report, "csv")).apply { writeText(sb.toString(), Charsets.UTF_8) }
    }

    suspend fun pdf(report: Report, ctx: Context, symbol: String): File = withContext(io) {
        val rtl = ctx.resources.configuration.layoutDirection == View.LAYOUT_DIRECTION_RTL
        val writer = PdfWriter(rtl)
        fun m(p: Long) = Money.formatWithSymbol(p, symbol)

        writer.title(ctx.getString(R.string.report_title) + " · " + periodLabel(report))
        writer.table(
            listOf(ctx.getString(R.string.report_charged), ctx.getString(R.string.home_collected), ctx.getString(R.string.profit_expenses), ctx.getString(R.string.profit_net)),
            listOf(listOf(m(report.charged), m(report.collected), m(report.expenses), m(report.profit))),
        )
        if (report.properties.isNotEmpty()) {
            writer.heading(ctx.getString(R.string.report_by_property))
            writer.table(
                listOf(ctx.getString(R.string.bill_property), ctx.getString(R.string.report_charged), ctx.getString(R.string.home_collected), ctx.getString(R.string.profit_expenses), ctx.getString(R.string.profit_net)),
                report.properties.map { listOf(it.propertyName ?: ctx.getString(R.string.profit_no_property), m(it.charged), m(it.collected), m(it.expenses), m(it.profit)) },
            )
        }
        if (report.months.isNotEmpty()) {
            writer.heading(ctx.getString(R.string.report_by_month))
            writer.table(
                listOf(ctx.getString(R.string.period_month), ctx.getString(R.string.report_charged), ctx.getString(R.string.home_collected), ctx.getString(R.string.profit_expenses)),
                report.months.map { listOf(DateUtils.formatMonth(it.month), m(it.charged), m(it.collected), m(it.expenses)) },
            )
        }
        writer.heading(ctx.getString(R.string.report_pending) + " · " + m(report.totalPending))
        if (report.pending.isEmpty()) {
            writer.paragraph(ctx.getString(R.string.report_no_pending))
        } else {
            writer.table(
                listOf(ctx.getString(R.string.payment_tenant), ctx.getString(R.string.tenant_unit), ctx.getString(R.string.report_since), ctx.getString(R.string.home_pending)),
                report.pending.map { listOf(it.tenantName, it.place, it.oldestMonth?.let(DateUtils::formatMonthShort).orEmpty(), m(it.amount)) },
            )
        }
        writer.paragraph(ctx.getString(R.string.report_generated, DateUtils.formatDate(DateUtils.now())))
        val file = File(dir(), fileName(report, "pdf"))
        writer.finish(file)
        file
    }

    /** Very small A4 table writer on top of PdfDocument, with page breaks. */
    private class PdfWriter(private val rtl: Boolean) {
        private val doc = PdfDocument()
        private var pageNo = 0
        private var page: PdfDocument.Page? = null
        private var y = 0f
        private val canvas: Canvas get() = page!!.canvas

        init {
            newPage()
        }

        private fun newPage() {
            page?.let { doc.finishPage(it) }
            pageNo++
            page = doc.startPage(PdfDocument.PageInfo.Builder(W, H, pageNo).create())
            y = MARGIN
        }

        private fun ensure(space: Float) {
            if (y + space > H - MARGIN) newPage()
        }

        private fun paint(size: Float, bold: Boolean, color: Int = Color.BLACK) = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = size
            this.color = color
            typeface = if (bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
        }

        private fun layout(text: String, p: TextPaint, width: Int, singleLine: Boolean = false): StaticLayout {
            val shown = if (singleLine) TextUtils.ellipsize(text, p, width.toFloat(), TextUtils.TruncateAt.END).toString() else text
            return StaticLayout.Builder.obtain(shown, 0, shown.length, p, width)
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setTextDirection(if (rtl) TextDirectionHeuristics.RTL else TextDirectionHeuristics.LTR)
                .build()
        }

        private fun draw(l: StaticLayout, x: Float, top: Float) {
            canvas.save()
            canvas.translate(x, top)
            l.draw(canvas)
            canvas.restore()
        }

        fun title(text: String) {
            val l = layout(text, paint(18f, true, Color.rgb(0, 106, 96)), (W - 2 * MARGIN).toInt())
            ensure(l.height + 12f)
            draw(l, MARGIN, y)
            y += l.height + 12f
        }

        fun heading(text: String) {
            val l = layout(text, paint(13f, true), (W - 2 * MARGIN).toInt())
            ensure(l.height + 40f)
            y += 10f
            draw(l, MARGIN, y)
            y += l.height + 6f
        }

        fun paragraph(text: String) {
            val l = layout(text, paint(10f, false, Color.DKGRAY), (W - 2 * MARGIN).toInt())
            ensure(l.height + 8f)
            draw(l, MARGIN, y)
            y += l.height + 8f
        }

        fun table(header: List<String>, rows: List<List<String>>) {
            val cols = header.size
            val colW = ((W - 2 * MARGIN) / cols).toInt()
            val rowH = 20f
            val headPaint = paint(9.5f, true, Color.WHITE)
            val cellPaint = paint(9.5f, false)
            val band = Paint().apply { color = Color.rgb(0, 106, 96) }
            val stripe = Paint().apply { color = Color.rgb(241, 245, 243) }

            fun drawRow(cells: List<String>, p: TextPaint, bg: Paint?) {
                ensure(rowH)
                bg?.let { canvas.drawRect(MARGIN, y, W - MARGIN, y + rowH, it) }
                cells.forEachIndexed { i, cell ->
                    // In RTL the first column is on the right.
                    val col = if (rtl) cols - 1 - i else i
                    val x = MARGIN + col * colW + 4f
                    draw(layout(cell, p, colW - 8, singleLine = true), x, y + 5f)
                }
                y += rowH
            }

            drawRow(header, headPaint, band)
            rows.forEachIndexed { index, r -> drawRow(r, cellPaint, if (index % 2 == 1) stripe else null) }
            y += 6f
        }

        fun finish(file: File) {
            page?.let { doc.finishPage(it) }
            page = null
            file.outputStream().use { doc.writeTo(it) }
            doc.close()
        }

        companion object {
            private const val W = 595
            private const val H = 842
            private const val MARGIN = 36f
        }
    }
}
