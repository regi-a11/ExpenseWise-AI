package com.pennywiseai.tracker.ui.viewmodel

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pennywiseai.tracker.data.preferences.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PermissionViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(PermissionUiState())
    val uiState: StateFlow<PermissionUiState> = _uiState.asStateFlow()
    
    init {
        checkPermissionStatus()
        observeUserPreferences()
    }
    
    private fun checkPermissionStatus() {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_SMS
        ) == PackageManager.PERMISSION_GRANTED
        
        _uiState.update { it.copy(hasPermission = hasPermission) }
    }
    
    private fun observeUserPreferences() {
        userPreferencesRepository.userPreferences
            .map { it.hasSkippedSmsPermission }
            .onEach { hasSkipped ->
                _uiState.update { it.copy(hasSkippedPermission = hasSkipped) }
            }
            .launchIn(viewModelScope)
    }
    
    fun onPermissionResult(granted: Boolean) {
        _uiState.update { it.copy(hasPermission = granted) }
        if (granted) {
            // Clear the skip flag if permission is granted
            viewModelScope.launch {
                userPreferencesRepository.updateSkippedSmsPermission(false)
            }
        }
    }
    
    fun onSkipPermission() {
        viewModelScope.launch {
            userPreferencesRepository.updateSkippedSmsPermission(true)
        }
    }
    
    fun onPermissionDenied() {
        _uiState.update { it.copy(showRationale = true) }
    }
}

data class PermissionUiState(
    val hasPermission: Boolean = false,
    val hasSkippedPermission: Boolean = false,
    val showRationale: Boolean = false
)