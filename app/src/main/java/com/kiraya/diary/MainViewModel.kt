package com.kiraya.diary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kiraya.diary.core.security.PinHasher
import com.kiraya.diary.data.security.AppLockManager
import com.kiraya.diary.data.settings.AppSettings
import com.kiraya.diary.data.settings.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    settingsRepository: SettingsRepository,
    private val lockManager: AppLockManager,
) : ViewModel() {
    /** Null until settings are first read, so the app does not flash before the lock. */
    val settings: StateFlow<AppSettings?> = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val unlocked: StateFlow<Boolean> = lockManager.unlocked

    fun checkPin(pin: String): Boolean {
        val s = settings.value ?: return false
        return PinHasher.matches(pin, s.pinSalt, s.pinHash)
    }

    fun unlock() = lockManager.unlock()
    fun onBackground() = lockManager.onAppBackground()
    fun onForeground() = lockManager.onAppForeground()
}
