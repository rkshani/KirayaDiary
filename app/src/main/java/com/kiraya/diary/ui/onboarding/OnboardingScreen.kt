package com.kiraya.diary.ui.onboarding

import android.app.Activity
import android.os.Build
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kiraya.diary.R
import com.kiraya.diary.ui.components.TextInput
import com.kiraya.diary.ui.settings.SettingsViewModel
import kotlinx.coroutines.launch

private data class Page(@DrawableRes val icon: Int, @StringRes val title: Int, @StringRes val body: Int)

private val pages = listOf(
    Page(R.drawable.ic_property, R.string.onboarding_1_title, R.string.onboarding_1_body),
    Page(R.drawable.ic_payment, R.string.onboarding_2_title, R.string.onboarding_2_body),
    Page(R.drawable.ic_lock, R.string.onboarding_3_title, R.string.onboarding_3_body),
)

/** Three intro pages shown on first launch. Page 1 picks the language, page 3 asks the landlord's name. */
@Composable
fun OnboardingScreen(viewModel: SettingsViewModel = hiltViewModel()) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val pager = rememberPagerState { pages.size }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var name by rememberSaveable { mutableStateOf("") }

    Surface(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .imePadding()
                .padding(24.dp),
        ) {
            HorizontalPager(state = pager, modifier = Modifier.weight(1f)) { index ->
                val page = pages[index]
                Column(
                    Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Box(
                        Modifier
                            .size(112.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            painterResource(page.icon),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(56.dp),
                        )
                    }
                    Spacer(Modifier.height(32.dp))
                    Text(stringResource(page.title), style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(12.dp))
                    Text(
                        stringResource(page.body),
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(24.dp))
                    when (index) {
                        0 -> Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            listOf("en" to R.string.language_english, "ur" to R.string.language_urdu).forEach { (code, label) ->
                                FilterChip(
                                    selected = settings.appLanguage == code,
                                    onClick = {
                                        if (settings.appLanguage != code) {
                                            viewModel.setLanguage(code)
                                            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) (context as? Activity)?.recreate()
                                        }
                                    },
                                    label = { Text(stringResource(label)) },
                                )
                            }
                        }
                        2 -> TextInput(
                            value = name,
                            onValueChange = { name = it },
                            label = stringResource(R.string.settings_landlord_name),
                            supportingText = stringResource(R.string.settings_landlord_hint),
                        )
                        else -> Unit
                    }
                }
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    repeat(pages.size) { i ->
                        Box(
                            Modifier
                                .size(if (i == pager.currentPage) 12.dp else 8.dp)
                                .clip(CircleShape)
                                .background(
                                    if (i == pager.currentPage) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                                ),
                        )
                    }
                }
                if (pager.currentPage < pages.lastIndex) {
                    TextButton(onClick = { viewModel.finishOnboarding(name) }) { Text(stringResource(R.string.onboarding_skip)) }
                    Button(onClick = { scope.launch { pager.animateScrollToPage(pager.currentPage + 1) } }) {
                        Text(stringResource(R.string.onboarding_next))
                    }
                } else {
                    Button(onClick = { viewModel.finishOnboarding(name) }) { Text(stringResource(R.string.onboarding_start)) }
                }
            }
        }
    }
}
