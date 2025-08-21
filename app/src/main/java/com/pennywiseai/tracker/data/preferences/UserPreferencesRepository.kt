package com.pennywiseai.tracker.data.preferences

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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

@Singleton
class UserPreferencesRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object PreferencesKeys {
        val DARK_THEME_ENABLED = booleanPreferencesKey("dark_theme_enabled")
        val DYNAMIC_COLOR_ENABLED = booleanPreferencesKey("dynamic_color_enabled")
        val HAS_SKIPPED_SMS_PERMISSION = booleanPreferencesKey("has_skipped_sms_permission")
        val DEVELOPER_MODE_ENABLED = booleanPreferencesKey("developer_mode_enabled")
        val SYSTEM_PROMPT = stringPreferencesKey("system_prompt")
        val HAS_SHOWN_SCAN_TUTORIAL = booleanPreferencesKey("has_shown_scan_tutorial")
        val ACTIVE_DOWNLOAD_ID = longPreferencesKey("active_download_id")
        val SMS_SCAN_MONTHS = intPreferencesKey("sms_scan_months")
    }

    val userPreferences: Flow<UserPreferences> = context.dataStore.data
        .map { preferences ->
            UserPreferences(
                isDarkThemeEnabled = preferences[PreferencesKeys.DARK_THEME_ENABLED],
                isDynamicColorEnabled = preferences[PreferencesKeys.DYNAMIC_COLOR_ENABLED] ?: false,
                hasSkippedSmsPermission = preferences[PreferencesKeys.HAS_SKIPPED_SMS_PERMISSION] ?: false,
                isDeveloperModeEnabled = preferences[PreferencesKeys.DEVELOPER_MODE_ENABLED] ?: false,
                hasShownScanTutorial = preferences[PreferencesKeys.HAS_SHOWN_SCAN_TUTORIAL] ?: false,
                smsScanMonths = preferences[PreferencesKeys.SMS_SCAN_MONTHS] ?: 3 // Default to 3 months
            )
        }
    
    val isDeveloperModeEnabled: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.DEVELOPER_MODE_ENABLED] ?: false
        }

    suspend fun updateDarkThemeEnabled(enabled: Boolean?) {
        context.dataStore.edit { preferences ->
            if (enabled == null) {
                preferences.remove(PreferencesKeys.DARK_THEME_ENABLED)
            } else {
                preferences[PreferencesKeys.DARK_THEME_ENABLED] = enabled
            }
        }
    }

    suspend fun updateDynamicColorEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.DYNAMIC_COLOR_ENABLED] = enabled
        }
    }
    
    suspend fun updateSkippedSmsPermission(skipped: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.HAS_SKIPPED_SMS_PERMISSION] = skipped
        }
    }
    
    suspend fun setDeveloperModeEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.DEVELOPER_MODE_ENABLED] = enabled
        }
    }
    
    suspend fun updateSystemPrompt(prompt: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.SYSTEM_PROMPT] = prompt
        }
    }
    
    fun getSystemPrompt(): Flow<String?> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.SYSTEM_PROMPT]
        }
    
    suspend fun markScanTutorialShown() {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.HAS_SHOWN_SCAN_TUTORIAL] = true
        }
    }
    
    suspend fun saveActiveDownloadId(id: Long) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.ACTIVE_DOWNLOAD_ID] = id
        }
    }
    
    suspend fun getActiveDownloadId(): Long? {
        return context.dataStore.data
            .map { preferences -> preferences[PreferencesKeys.ACTIVE_DOWNLOAD_ID] }
            .first()
    }
    
    suspend fun clearActiveDownloadId() {
        context.dataStore.edit { preferences ->
            preferences.remove(PreferencesKeys.ACTIVE_DOWNLOAD_ID)
        }
    }
    
    val smsScanMonths: Flow<Int> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.SMS_SCAN_MONTHS] ?: 3 // Default to 3 months
        }
    
    suspend fun updateSmsScanMonths(months: Int) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.SMS_SCAN_MONTHS] = months
        }
    }
    
    suspend fun getSmsScanMonths(): Int {
        return context.dataStore.data
            .map { preferences -> preferences[PreferencesKeys.SMS_SCAN_MONTHS] ?: 3 }
            .first()
    }
}

data class UserPreferences(
    val isDarkThemeEnabled: Boolean? = null, // null means follow system
    val isDynamicColorEnabled: Boolean = false, // Default to custom brand colors
    val hasSkippedSmsPermission: Boolean = false,
    val isDeveloperModeEnabled: Boolean = false,
    val hasShownScanTutorial: Boolean = false,
    val smsScanMonths: Int = 3 // Default to 3 months
)