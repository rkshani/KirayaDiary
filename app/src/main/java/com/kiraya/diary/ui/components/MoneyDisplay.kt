package com.kiraya.diary.ui.components

import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import com.kiraya.diary.core.money.Money

/** How amounts are shown. Comes from settings (currency symbol, grouping, hide amounts). */
@Immutable
data class MoneyDisplay(
    val symbol: String = "Rs",
    val grouping: Boolean = true,
    val hidden: Boolean = false,
) {
    fun format(paisa: Long): String =
        if (hidden) "$symbol ••••" else Money.formatWithSymbol(paisa, symbol, grouping = grouping)

    /** Always shows the real amount (for receipts, messages and exports). */
    fun formatPlain(paisa: Long): String = Money.formatWithSymbol(paisa, symbol, grouping = grouping)
}

val LocalMoneyDisplay = staticCompositionLocalOf { MoneyDisplay() }

@Composable
fun MoneyText(
    paisa: Long,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    color: Color = Color.Unspecified,
) {
    Text(text = LocalMoneyDisplay.current.format(paisa), modifier = modifier, style = style, color = color)
}
