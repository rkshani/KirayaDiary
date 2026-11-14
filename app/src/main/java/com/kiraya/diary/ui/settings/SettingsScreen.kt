package com.kiraya.diary.ui.settings

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.clickable
import com.kiraya.diary.R
import com.kiraya.diary.ui.components.KirayaTopBar

@Composable
fun SettingsScreen(
    onReminders: () -> Unit,
    onBackup: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = { KirayaTopBar(title = stringResource(R.string.nav_settings)) },
    ) { padding ->
        Column(Modifier.padding(padding)) {
            SettingsRow(R.string.reminders_title, R.drawable.ic_notification, onReminders)
            SettingsRow(R.string.backup_title, R.drawable.ic_backup, onBackup)
        }
    }
}

@Composable
private fun SettingsRow(@StringRes title: Int, @DrawableRes icon: Int, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(stringResource(title)) },
        leadingContent = { Icon(painterResource(icon), contentDescription = null) },
        modifier = Modifier.clickable(onClick = onClick),
    )
}
