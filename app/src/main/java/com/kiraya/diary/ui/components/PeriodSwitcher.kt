package com.kiraya.diary.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kiraya.diary.R
import com.kiraya.diary.core.date.DateUtils
import com.kiraya.diary.domain.usecase.Period

/** Month/Year toggle with previous and next arrows. */
@Composable
fun PeriodSwitcher(
    period: Period,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onYearMode: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxWidth()) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = !period.isYear,
                onClick = { onYearMode(false) },
                label = { Text(stringResource(R.string.period_month)) },
            )
            FilterChip(
                selected = period.isYear,
                onClick = { onYearMode(true) },
                label = { Text(stringResource(R.string.period_year)) },
            )
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onPrevious) {
                Icon(painterResource(R.drawable.ic_back), stringResource(R.string.month_previous))
            }
            Text(
                if (period.isYear) period.year.toString() else DateUtils.formatMonth(period.monthKey),
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onNext, enabled = !period.next().isAfterNow()) {
                Icon(
                    painterResource(R.drawable.ic_back),
                    stringResource(R.string.month_next),
                    modifier = Modifier.scale(scaleX = -1f, scaleY = 1f),
                )
            }
        }
    }
}
