package com.ultrapuremusic.feature.settings

import android.content.Context
import androidx.lifecycle.viewModelScope
import com.ultrapuremusic.core.datastore.UserPreferences
import com.ultrapuremusic.core.ui.BaseViewModel
import com.ultrapuremusic.core.util.Constants
import com.ultrapuremusic.domain.usecase.auth.LogoutUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import java.io.File
import javax.inject.Inject

data class SettingsUiState(
    val theme: String = "dark",
    val downloadOverWifiOnly: Boolean = true,
    val audioQuality: Int = 0,           // 0=auto, 1=low, 2=medium, 3=high
    val showExplicit: Boolean = true,
    val useDynamicColor: Boolean = false,
    // ── Core Audio ───────────────────────────────────────────
    val crossfadeDurationMs: Long = 0L,  // 0=off
    val eqPreset: String = "FLAT",
    val bassBoostStrength: Int = 0,      // 0–100 %
    val loudnessEnabled: Boolean = false,
    // ── Storage ──────────────────────────────────────────────
    val cacheSizeMb: Long = 0L,
    val downloadsSizeMb: Long = 0L,
    val isLoggingOut: Boolean = false,
    val isClearingCache: Boolean = false,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val userPreferences: UserPreferences,
    private val logoutUseCase: LogoutUseCase,
) : BaseViewModel<SettingsUiState>(SettingsUiState()) {

    private val _navigationEvent = MutableSharedFlow<String>()
    val navigationEvent: SharedFlow<String> = _navigationEvent.asSharedFlow()

    init {
        userPreferences.theme
            .onEach { theme -> updateState { copy(theme = theme) } }
            .launchIn(viewModelScope)

        userPreferences.downloadOverWifiOnly
            .onEach { wifiOnly -> updateState { copy(downloadOverWifiOnly = wifiOnly) } }
            .launchIn(viewModelScope)

        userPreferences.audioQuality
            .onEach { quality -> updateState { copy(audioQuality = quality) } }
            .launchIn(viewModelScope)

        userPreferences.dynamicColor
            .onEach { dynamic -> updateState { copy(useDynamicColor = dynamic) } }
            .launchIn(viewModelScope)

        userPreferences.crossfadeDurationMs
            .onEach { ms -> updateState { copy(crossfadeDurationMs = ms) } }
            .launchIn(viewModelScope)

        userPreferences.eqPreset
            .onEach { p -> updateState { copy(eqPreset = p) } }
            .launchIn(viewModelScope)

        userPreferences.bassBoostStrength
            .onEach { s -> updateState { copy(bassBoostStrength = s) } }
            .launchIn(viewModelScope)

        userPreferences.loudnessEnabled
            .onEach { on -> updateState { copy(loudnessEnabled = on) } }
            .launchIn(viewModelScope)

        refreshStorageInfo()
    }

    private fun refreshStorageInfo() {
        launch {
            val cacheDir = File(context.cacheDir, Constants.CACHE_DIR)
            val cacheSizeMb = if (cacheDir.exists()) cacheDir.walk().sumOf { it.length() } / 1_048_576L else 0L
            val downloadsDir = File(context.filesDir, "downloads")
            val downloadsSizeMb = if (downloadsDir.exists()) downloadsDir.walk().sumOf { it.length() } / 1_048_576L else 0L
            updateState { copy(cacheSizeMb = cacheSizeMb, downloadsSizeMb = downloadsSizeMb) }
        }
    }

    fun setTheme(theme: String) {
        launch { userPreferences.setTheme(theme) }
    }

    fun setDownloadOverWifiOnly(wifiOnly: Boolean) {
        launch { userPreferences.setDownloadOverWifiOnly(wifiOnly) }
    }

    fun setAudioQuality(quality: Int) {
        launch { userPreferences.setAudioQuality(quality) }
    }

    fun setShowExplicit(show: Boolean) {
        updateState { copy(showExplicit = show) }
    }

    fun setDynamicColor(enabled: Boolean) {
        launch { userPreferences.setDynamicColor(enabled) }
    }

    fun setCrossfadeDuration(ms: Long) {
        launch { userPreferences.setCrossfadeDuration(ms) }
    }

    fun setEqPreset(preset: String) {
        launch { userPreferences.setEqPreset(preset) }
    }

    fun setBassBoostStrength(pct: Int) {
        launch { userPreferences.setBassBoostStrength(pct) }
    }

    fun setLoudnessEnabled(enabled: Boolean) {
        launch { userPreferences.setLoudnessEnabled(enabled) }
    }

    fun clearCache() {
        launch {
            updateState { copy(isClearingCache = true) }
            val cacheDir = File(context.cacheDir, Constants.CACHE_DIR)
            if (cacheDir.exists()) cacheDir.deleteRecursively()
            updateState { copy(isClearingCache = false, cacheSizeMb = 0L) }
            refreshStorageInfo()
        }
    }

    fun logout() {
        launch {
            updateState { copy(isLoggingOut = true) }
            logoutUseCase()
            updateState { copy(isLoggingOut = false) }
            _navigationEvent.emit("login")
        }
    }
}
