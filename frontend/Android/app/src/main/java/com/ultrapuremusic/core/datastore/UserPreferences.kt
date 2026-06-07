package com.ultrapuremusic.core.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_prefs")

@Singleton
class UserPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object Keys {
        val USER_ID = stringPreferencesKey("user_id")
        val USER_NAME = stringPreferencesKey("user_name")
        val USER_EMAIL = stringPreferencesKey("user_email")
        val AVATAR_URL = stringPreferencesKey("avatar_url")
        val IS_LOGGED_IN = booleanPreferencesKey("is_logged_in")
        val THEME = stringPreferencesKey("theme")
        val AUDIO_QUALITY = intPreferencesKey("audio_quality")
        val DOWNLOAD_OVER_WIFI_ONLY = booleanPreferencesKey("download_wifi_only")
        val SHOW_EXPLICIT = booleanPreferencesKey("show_explicit")
        val DYNAMIC_COLOR          = booleanPreferencesKey("dynamic_color")
        val CROSSFADE_DURATION_MS  = longPreferencesKey("crossfade_duration_ms")
        val EQ_PRESET              = stringPreferencesKey("eq_preset")
        val BASS_BOOST_STRENGTH    = intPreferencesKey("bass_boost_strength")
        val LOUDNESS_ENABLED       = booleanPreferencesKey("loudness_enabled")
    }

    val isLoggedIn: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.IS_LOGGED_IN] ?: false
    }

    val userId: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[Keys.USER_ID]
    }

    val userName: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[Keys.USER_NAME] ?: ""
    }

    val userEmail: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[Keys.USER_EMAIL] ?: ""
    }

    val avatarUrl: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[Keys.AVATAR_URL]
    }

    val theme: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[Keys.THEME] ?: "dark"
    }

    val downloadOverWifiOnly: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.DOWNLOAD_OVER_WIFI_ONLY] ?: true
    }

    val audioQuality: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[Keys.AUDIO_QUALITY] ?: 0
    }

    val dynamicColor: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.DYNAMIC_COLOR] ?: false
    }

    val crossfadeDurationMs: Flow<Long> = context.dataStore.data.map { prefs ->
        prefs[Keys.CROSSFADE_DURATION_MS] ?: 0L
    }

    val eqPreset: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[Keys.EQ_PRESET] ?: "FLAT"
    }

    val bassBoostStrength: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[Keys.BASS_BOOST_STRENGTH] ?: 0
    }

    val loudnessEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.LOUDNESS_ENABLED] ?: false
    }

    suspend fun saveUser(id: String, name: String, email: String, avatarUrl: String?) {
        context.dataStore.edit { prefs ->
            prefs[Keys.USER_ID] = id
            prefs[Keys.USER_NAME] = name
            prefs[Keys.USER_EMAIL] = email
            avatarUrl?.let { prefs[Keys.AVATAR_URL] = it }
            prefs[Keys.IS_LOGGED_IN] = true
        }
    }

    suspend fun setTheme(theme: String) {
        context.dataStore.edit { prefs -> prefs[Keys.THEME] = theme }
    }

    suspend fun setDownloadOverWifiOnly(wifiOnly: Boolean) {
        context.dataStore.edit { prefs -> prefs[Keys.DOWNLOAD_OVER_WIFI_ONLY] = wifiOnly }
    }

    suspend fun setAudioQuality(quality: Int) {
        context.dataStore.edit { prefs -> prefs[Keys.AUDIO_QUALITY] = quality }
    }

    suspend fun setDynamicColor(enabled: Boolean) {
        context.dataStore.edit { prefs -> prefs[Keys.DYNAMIC_COLOR] = enabled }
    }

    suspend fun setCrossfadeDuration(ms: Long) {
        context.dataStore.edit { prefs -> prefs[Keys.CROSSFADE_DURATION_MS] = ms }
    }

    suspend fun setEqPreset(preset: String) {
        context.dataStore.edit { prefs -> prefs[Keys.EQ_PRESET] = preset }
    }

    suspend fun setBassBoostStrength(pct: Int) {
        context.dataStore.edit { prefs -> prefs[Keys.BASS_BOOST_STRENGTH] = pct }
    }

    suspend fun setLoudnessEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs -> prefs[Keys.LOUDNESS_ENABLED] = enabled }
    }

    suspend fun clearUser() {
        context.dataStore.edit { prefs ->
            prefs.remove(Keys.USER_ID)
            prefs.remove(Keys.USER_NAME)
            prefs.remove(Keys.USER_EMAIL)
            prefs.remove(Keys.AVATAR_URL)
            prefs[Keys.IS_LOGGED_IN] = false
        }
    }
}
