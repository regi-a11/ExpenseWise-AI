package com.pennywiseai.tracker.data.manager

import android.content.Context
import android.util.Log
import androidx.activity.ComponentActivity
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallState
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages Google Play In-App Updates.
 * Checks for available updates and triggers the update flow.
 */
@Singleton
class InAppUpdateManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val appUpdateManager: AppUpdateManager = AppUpdateManagerFactory.create(context)
    private var updateListener: InstallStateUpdatedListener? = null
    
    /**
     * Checks for available updates and starts the update flow if available.
     * Google Play handles all the UI - showing update dialog, progress, etc.
     */
    fun checkForUpdate(activity: ComponentActivity) {
        val appUpdateInfoTask = appUpdateManager.appUpdateInfo
        
        appUpdateInfoTask.addOnSuccessListener { appUpdateInfo ->
            when {
                // Check if update is available and allowed
                appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
                    && appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE) -> {
                    // Start flexible update flow
                    startFlexibleUpdate(activity, appUpdateInfo)
                }
                
                // Check if update was downloaded but not installed
                appUpdateInfo.installStatus() == InstallStatus.DOWNLOADED -> {
                    // Notify user to complete update
                    popupSnackbarForCompleteUpdate()
                }
            }
        }.addOnFailureListener { exception ->
            // Log error but don't show to user - fail silently
            Log.e(TAG, "Update check failed", exception)
        }
    }
    
    /**
     * Starts the flexible update flow.
     * User can continue using the app while the update downloads in background.
     */
    private fun startFlexibleUpdate(activity: ComponentActivity, appUpdateInfo: AppUpdateInfo) {
        // Register listener to monitor update progress
        updateListener = InstallStateUpdatedListener { state ->
            handleUpdateState(state)
        }
        
        updateListener?.let {
            appUpdateManager.registerListener(it)
        }
        
        try {
            appUpdateManager.startUpdateFlowForResult(
                appUpdateInfo,
                activity,
                AppUpdateOptions.newBuilder(AppUpdateType.FLEXIBLE).build(),
                UPDATE_REQUEST_CODE
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start update flow", e)
            unregisterListener()
        }
    }
    
    /**
     * Handles update state changes.
     */
    private fun handleUpdateState(state: InstallState) {
        when (state.installStatus()) {
            InstallStatus.DOWNLOADED -> {
                // Update has been downloaded
                popupSnackbarForCompleteUpdate()
                unregisterListener()
            }
            InstallStatus.INSTALLED -> {
                // Update installed successfully
                unregisterListener()
            }
            InstallStatus.FAILED -> {
                // Update failed
                Log.e(TAG, "Update failed with error code: ${state.installErrorCode()}")
                unregisterListener()
            }
            else -> {
                // Other states like DOWNLOADING, INSTALLING, etc.
                // Google Play handles the UI for these states
            }
        }
    }
    
    /**
     * Shows notification to complete the update.
     * In a real app, this would show a Snackbar or notification.
     * For now, we'll just complete the update automatically.
     */
    private fun popupSnackbarForCompleteUpdate() {
        // In production, you'd show a Snackbar here asking user to restart
        // For simplicity, we'll log and user will get the update on next app restart
        Log.i(TAG, "Update downloaded. App will be updated on next restart.")
    }
    
    /**
     * Completes the update and restarts the app.
     */
    fun completeUpdate() {
        appUpdateManager.completeUpdate()
    }
    
    /**
     * Unregisters the update listener.
     */
    private fun unregisterListener() {
        updateListener?.let {
            appUpdateManager.unregisterListener(it)
            updateListener = null
        }
    }
    
    /**
     * Call this in onDestroy to clean up.
     */
    fun cleanup() {
        unregisterListener()
    }
    
    companion object {
        private const val TAG = "InAppUpdateManager"
        const val UPDATE_REQUEST_CODE = 123
    }
}