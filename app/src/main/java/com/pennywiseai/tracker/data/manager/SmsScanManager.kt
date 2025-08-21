package com.pennywiseai.tracker.data.manager

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.pennywiseai.tracker.worker.SmsReaderWorker
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages SMS scanning operations using WorkManager.
 * For now, this just triggers the worker to read and log SMS messages.
 */
@Singleton
class SmsScanManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val workManager = WorkManager.getInstance(context)
    
    /**
     * Starts a one-time SMS scan that reads and logs messages.
     */
    fun startSmsLoggingScan() {
        val smsReaderWork = OneTimeWorkRequestBuilder<SmsReaderWorker>()
            .addTag("sms_logging")
            .build()
        
        workManager.enqueueUniqueWork(
            SmsReaderWorker.WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            smsReaderWork
        )
    }
    
    /**
     * Cancels any ongoing SMS scanning work.
     */
    fun cancelSmsScanning() {
        workManager.cancelUniqueWork(SmsReaderWorker.WORK_NAME)
    }
}