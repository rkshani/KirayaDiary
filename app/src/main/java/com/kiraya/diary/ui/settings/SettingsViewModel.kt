package com.kiraya.diary.ui.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kiraya.diary.core.locale.LocaleHelper
import com.kiraya.diary.data.settings.AppSettings
import com.kiraya.diary.data.settings.SettingsRepository
import com.kiraya.diary.domain.model.ThemeMode
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    @ApplicationContext private val context: Context,
) : ViewModel() {
    val settings: StateFlow<AppSettings> = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings())

    /** Saves the language. The caller recreates the screen so the new language shows. */
    fun setLanguage(code: String) {
        LocaleHelper.save(context, code)
        viewModelScope.launch { settingsRepository.setAppLanguage(code) }
    }

    fun setTheme(mode: ThemeMode) {
        viewModelScope.launch { settingsRepository.setThemeMode(mode) }
    }

    fun setCurrency(symbol: String, grouping: Boolean) {
        viewModelScope.launch { settingsRepository.setCurrency(symbol, grouping) }
    }

    fun setLandlord(name: String, phone: String) {
        viewModelScope.launch { settingsRepository.setLandlord(name, phone) }
    }

    fun finishOnboarding(name: String) {
        viewModelScope.launch {
            if (name.isNotBlank()) settingsRepository.setLandlord(name, settings.value.landlordPhone)
            settingsRepository.setOnboardingDone()
        }
    }
}
