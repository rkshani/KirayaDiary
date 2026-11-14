package com.kiraya.diary

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.SystemBarStyle
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
import com.kiraya.diary.ui.theme.KirayaTheme
import dagger.hilt.android.AndroidEntryPoint

/** FragmentActivity (not ComponentActivity) because the fingerprint prompt needs it. */
@AndroidEntryPoint
class MainActivity : FragmentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        // Leave the splash theme as soon as the app is ready to draw.
        setTheme(R.style.Theme_KirayaDiary)
        super.onCreate(savedInstanceState)
        // Always light: dark status bar icons even when the phone is in dark mode.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
        )
        setContent {
            val settings by viewModel.settings.collectAsStateWithLifecycle()
            // Used by the app lock, which is hidden for now.
            // val unlocked by viewModel.unlocked.collectAsStateWithLifecycle()
            val s = settings
            // Light mode only for now. Later: KirayaTheme(themeMode = s?.themeMode ?: ThemeMode.SYSTEM)
            KirayaTheme(themeMode = ThemeMode.LIGHT) {
                if (s == null) return@KirayaTheme
                CompositionLocalProvider(
                    LocalMoneyDisplay provides MoneyDisplay(
                        symbol = s.currencySymbol,
                        grouping = s.groupDigits,
                        hidden = s.hideAmounts,
                    ),
                ) {
                    // Onboarding and the app lock are hidden for now. Turn back on in a later update:
                    // if (!s.onboardingDone) {
                    //     OnboardingScreen()
                    // } else if (s.lockEnabled && s.hasPin && !unlocked) {
                    //     LockScreen(
                    //         biometricEnabled = s.biometricEnabled,
                    //         checkPin = viewModel::checkPin,
                    //         onUnlocked = viewModel::unlock,
                    //     )
                    // } else {
                    //     KirayaAppRoot()
                    // }
                    KirayaAppRoot()
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
