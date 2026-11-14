package com.kiraya.diary.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.kiraya.diary.domain.model.RentStatus
import com.kiraya.diary.ui.theme.KirayaColors

@Composable
fun StatusPill(text: String, color: Color, modifier: Modifier = Modifier) {
    Text(
        text = text,
        color = color,
        style = MaterialTheme.typography.labelMedium,
        modifier = modifier
            .background(color.copy(alpha = 0.14f), RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 3.dp),
    )
}

@Composable
fun RentStatusPill(status: RentStatus, modifier: Modifier = Modifier) {
    val color = when (status) {
        RentStatus.PAID -> KirayaColors.status.paid
        RentStatus.PARTIAL -> KirayaColors.status.partial
        RentStatus.UNPAID -> KirayaColors.status.unpaid
    }
    StatusPill(stringResource(status.labelRes), color, modifier)
}
