package com.kiraya.diary.ui.settings

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kiraya.diary.BuildConfig
import com.kiraya.diary.R
import com.kiraya.diary.core.money.Money
import com.kiraya.diary.ui.components.ChoiceChips
import com.kiraya.diary.ui.components.KirayaTopBar
import com.kiraya.diary.ui.components.SectionTitle
import com.kiraya.diary.ui.components.TextInput
import com.kiraya.diary.ui.lock.SwitchRow

@Composable
fun SettingsScreen(
    // Reminders and app lock are hidden for now. Turn back on in a later update.
    // onReminders: () -> Unit,
    onBackup: () -> Unit,
    // onAppLock: () -> Unit,
    onProperties: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val s by viewModel.settings.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier,
        topBar = { KirayaTopBar(title = stringResource(R.string.nav_settings)) },
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SectionTitle(stringResource(R.string.settings_profile))
            ProfileCard(s.landlordName, s.landlordPhone, viewModel::setLandlord)

            // Language (English only) and theme (light only) are hidden for now.
            // Turn back on in a later update.
//            SectionTitle(stringResource(R.string.settings_appearance))
//            OutlinedCard(Modifier.fillMaxWidth()) {
//                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
//                    ChoiceChips(
//                        options = listOf("", "en", "ur"),
//                        selected = s.appLanguage,
//                        label = {
//                            when (it) {
//                                "en" -> stringResource(R.string.language_english)
//                                "ur" -> stringResource(R.string.language_urdu)
//                                else -> stringResource(R.string.settings_language_system)
//                            }
//                        },
//                        onSelect = { code ->
//                            if (code != s.appLanguage) {
//                                viewModel.setLanguage(code)
//                                // Android 13+ restarts the screen by itself.
//                                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) (context as? Activity)?.recreate()
//                            }
//                        },
//                        title = stringResource(R.string.settings_language),
//                    )
//                    ChoiceChips(
//                        options = ThemeMode.entries,
//                        selected = s.themeMode,
//                        label = {
//                            stringResource(
//                                when (it) {
//                                    ThemeMode.SYSTEM -> R.string.settings_theme_system
//                                    ThemeMode.LIGHT -> R.string.settings_theme_light
//                                    ThemeMode.DARK -> R.string.settings_theme_dark
//                                },
//                            )
//                        },
//                        onSelect = viewModel::setTheme,
//                        title = stringResource(R.string.settings_theme),
//                    )
//                }
//            }

            SectionTitle(stringResource(R.string.settings_currency))
            CurrencyCard(s.currencySymbol, s.groupDigits, viewModel::setCurrency)

            SectionTitle(stringResource(R.string.settings_more))
            OutlinedCard(Modifier.fillMaxWidth()) {
                SettingsRow(R.string.properties_title, R.drawable.ic_property, onProperties)
                HorizontalDivider()
                // Hidden for now. Turn back on in a later update.
                // SettingsRow(R.string.reminders_title, R.drawable.ic_notification, onReminders)
                // HorizontalDivider()
                // SettingsRow(R.string.lock_title, R.drawable.ic_lock, onAppLock)
                // HorizontalDivider()
                SettingsRow(R.string.backup_title, R.drawable.ic_backup, onBackup)
            }
            Text(
                stringResource(R.string.settings_about, BuildConfig.VERSION_NAME),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 16.dp),
            )
        }
    }
}

@Composable
private fun ProfileCard(name: String, phone: String, onSave: (String, String) -> Unit) {
    var n by remember(name) { mutableStateOf(name) }
    var p by remember(phone) { mutableStateOf(phone) }
    OutlinedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            TextInput(n, { n = it }, stringResource(R.string.settings_landlord_name), supportingText = stringResource(R.string.settings_landlord_hint))
            TextInput(p, { p = it }, stringResource(R.string.tenant_phone), keyboardType = KeyboardType.Phone)
            if (n != name || p != phone) {
                TextButton(onClick = { onSave(n, p) }) { Text(stringResource(R.string.action_save)) }
            }
        }
    }
}

@Composable
private fun CurrencyCard(symbol: String, grouping: Boolean, onSave: (String, Boolean) -> Unit) {
    var sym by remember(symbol) { mutableStateOf(symbol) }
    OutlinedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ChoiceChips(
                options = listOf("Rs", "Rs.", "PKR", "₨"),
                selected = symbol,
                label = { it },
                onSelect = { onSave(it, grouping) },
                title = stringResource(R.string.settings_currency_symbol),
            )
            TextInput(
                value = sym,
                onValueChange = { sym = it.take(6) },
                label = stringResource(R.string.settings_currency_custom),
            )
            if (sym.isNotBlank() && sym != symbol) {
                TextButton(onClick = { onSave(sym, grouping) }) { Text(stringResource(R.string.action_save)) }
            }
            SwitchRow(
                title = stringResource(R.string.settings_grouping),
                subtitle = Money.formatWithSymbol(12_500_000, symbol, grouping = grouping),
                checked = grouping,
                onChange = { onSave(symbol, it) },
            )
        }
    }
}

@Composable
private fun SettingsRow(@StringRes title: Int, @DrawableRes icon: Int, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(stringResource(title)) },
        leadingContent = { Icon(painterResource(icon), contentDescription = null) },
        trailingContent = { Icon(painterResource(R.drawable.ic_chevron_right), contentDescription = null) },
        modifier = Modifier.clickable(onClick = onClick),
    )
}
