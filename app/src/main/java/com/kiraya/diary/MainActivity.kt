package com.kiraya.diary

import android.content.Context
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kiraya.diary.core.locale.LocaleHelper
import com.kiraya.diary.domain.model.ThemeMode
import com.kiraya.diary.ui.KirayaAppRoot
import com.kiraya.diary.ui.components.LocalMoneyDisplay
import com.kiraya.diary.ui.components.MoneyDisplay
import com.kiraya.diary.ui.lock.LockScreen
import com.kiraya.diary.ui.onboarding.OnboardingScreen
import com.kiraya.diary.ui.theme.KirayaTheme
import dagger.hilt.android.AndroidEntryPoint

/** FragmentActivity (not ComponentActivity) because the fingerprint prompt needs it. */
@AndroidEntryPoint
class MainActivity : FragmentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val settings by viewModel.settings.collectAsStateWithLifecycle()
            val unlocked by viewModel.unlocked.collectAsStateWithLifecycle()
            val s = settings
            KirayaTheme(themeMode = s?.themeMode ?: ThemeMode.SYSTEM) {
                if (s == null) return@KirayaTheme
                CompositionLocalProvider(
                    LocalMoneyDisplay provides MoneyDisplay(
                        symbol = s.currencySymbol,
                        grouping = s.groupDigits,
                        hidden = s.hideAmounts,
                    ),
                ) {
                    if (!s.onboardingDone) {
                        OnboardingScreen()
                    } else if (s.lockEnabled && s.hasPin && !unlocked) {
                        LockScreen(
                            biometricEnabled = s.biometricEnabled,
                            checkPin = viewModel::checkPin,
                            onUnlocked = viewModel::unlock,
                        )
                    } else {
                        KirayaAppRoot()
                    }
                }
            }
        }
    }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.wrap(newBase))
    }

    override fun onStart() {
        super.onStart()
        viewModel.onForeground()
    }

    override fun onStop() {
        super.onStop()
        // Do not count a screen rotation as leaving the app.
        if (!isChangingConfigurations) viewModel.onBackground()
    }
}
