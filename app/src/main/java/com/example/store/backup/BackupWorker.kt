package com.example.store.backup

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class BackupWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    companion object {
        private const val TAG = "BackupWorker"
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override suspend fun doWork(): Result {

        return withContext(Dispatchers.IO) {

            try {

                Log.i(TAG, "================================")
                Log.i(TAG, "AUTO BACKUP STARTED")
                Log.i(TAG, "================================")

                // ------------------------------------------------
                // Check Google account
                // ------------------------------------------------

                val token =
                    AuthManager.getAccessToken()

                if (token.isNullOrEmpty()) {

                    Log.w(
                        TAG,
                        "No Google access token. Retrying."
                    )

                    return@withContext Result.retry()
                }

                Log.i(
                    TAG,
                    "Google access token available"
                )

                // ------------------------------------------------
                // Backup complete current Room database
                // ------------------------------------------------

                /*
                 * forceReplace = true
                 *
                 * Existing backup:
                 *     -> replace with latest data
                 *
                 * No backup:
                 *     -> create new backup
                 */
                val result =
                    BackupManagerImpl.backupNow(
                        applicationContext,
                        forceReplace = true
                    )

                // ------------------------------------------------
                // Result
                // ------------------------------------------------

                if (result.success) {

                    Log.i(
                        TAG,
                        "================================"
                    )

                    Log.i(
                        TAG,
                        "AUTO BACKUP SUCCESSFUL"
                    )

                    Log.i(
                        TAG,
                        result.message
                    )

                    Log.i(
                        TAG,
                        "================================"
                    )

                    Result.success()

                } else {

                    Log.w(
                        TAG,
                        "================================"
                    )

                    Log.w(
                        TAG,
                        "AUTO BACKUP FAILED"
                    )

                    Log.w(
                        TAG,
                        result.message
                    )

                    Log.w(
                        TAG,
                        "================================"
                    )

                    /*
                     * Ask WorkManager to try again.
                     */
                    Result.retry()
                }

            } catch (e: Exception) {

                Log.e(
                    TAG,
                    "Auto backup worker crashed",
                    e
                )

                /*
                 * Temporary failure -> retry.
                 */
                Result.retry()
            }
        }
    }
}