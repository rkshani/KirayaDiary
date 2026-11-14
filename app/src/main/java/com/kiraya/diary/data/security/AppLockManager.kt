package com.kiraya.diary.data.security

import android.os.SystemClock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Remembers whether the app is unlocked in this session.
 * The app locks again when it was in the background longer than [RELOCK_AFTER_MS].
 */
@Singleton
class AppLockManager @Inject constructor() {
    private val _unlocked = MutableStateFlow(false)
    val unlocked: StateFlow<Boolean> = _unlocked.asStateFlow()

    private var backgroundAt: Long? = null

    fun unlock() {
        _unlocked.value = true
    }

    fun onAppBackground() {
        backgroundAt = SystemClock.elapsedRealtime()
    }

    fun onAppForeground() {
        val since = backgroundAt ?: return
        backgroundAt = null
        if (SystemClock.elapsedRealtime() - since > RELOCK_AFTER_MS) _unlocked.value = false
    }

    /** Used right after the user sets a PIN, so they are not asked for it at once. */
    fun markUnlocked() = unlock()

    companion object {
        const val RELOCK_AFTER_MS = 30_000L
    }
}
