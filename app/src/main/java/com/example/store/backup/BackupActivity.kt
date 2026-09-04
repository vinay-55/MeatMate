package com.example.store.backup

import android.app.AlertDialog
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.store.R
import com.example.store.backup.BackupManagerImpl as BackupManager
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Calendar
import java.util.concurrent.TimeUnit

class BackupActivity : AppCompatActivity() {

    private lateinit var btnBackup: Button
    private lateinit var tvDataSize: TextView
    private lateinit var tvLastBackup: TextView
    private lateinit var spinnerAuto: Spinner
    private lateinit var btnAutoSave: Button

    private val prefsName = "backup_prefs"

    companion object {
        private const val AUTO_BACKUP_WORK = "auto_backup_work"
        private const val TAG = "BackupActivity"
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_backup)

        btnBackup = findViewById(R.id.bu_backup_now)
        tvDataSize = findViewById(R.id.bu_data_size)
        tvLastBackup = findViewById(R.id.bu_last_backup)
        spinnerAuto = findViewById(R.id.bu_auto_spinner)
        btnAutoSave = findViewById(R.id.bu_auto_save)

        BackupManager.init(this)
        AuthManager.init(this)

        refreshBackupInfo()
        setupAutoSpinner()

        // ========================================================
        // MANUAL BACKUP
        // ========================================================

        btnBackup.setOnClickListener {

            lifecycleScope.launch {

                btnBackup.isEnabled = false

                try {

                    val token =
                        AuthManager.getAccessToken()

                    if (token.isNullOrEmpty()) {

                        Toast.makeText(
                            this@BackupActivity,
                            "Not signed in",
                            Toast.LENGTH_LONG
                        ).show()

                        btnBackup.isEnabled = true

                        return@launch
                    }

                    val meta =
                        BackupManager.getBackupFileMetadata(
                            token
                        )

                    if (meta?.id != null) {

                        /*
                         * Backup already exists.
                         * Ask before replacing it.
                         */

                        AlertDialog.Builder(
                            this@BackupActivity
                        )
                            .setTitle(
                                "Backup already exists"
                            )
                            .setMessage(
                                "A backup for this Google account already exists. " +
                                        "Replacing it will overwrite the previous backup " +
                                        "with the current device data."
                            )
                            .setNegativeButton(
                                "Cancel"
                            ) { dialog, _ ->

                                dialog.dismiss()

                                btnBackup.isEnabled = true
                            }
                            .setPositiveButton(
                                "Replace backup"
                            ) { dialog, _ ->

                                dialog.dismiss()

                                lifecycleScope.launch {

                                    val result =
                                        BackupManager.backupNow(
                                            applicationContext,
                                            forceReplace = true
                                        )

                                    Toast.makeText(
                                        this@BackupActivity,
                                        result.message,
                                        Toast.LENGTH_LONG
                                    ).show()

                                    Log.i(
                                        TAG,
                                        "Manual replace result: " +
                                                "${result.success}, " +
                                                result.message
                                    )

                                    btnBackup.isEnabled = true

                                    refreshBackupInfo()
                                }
                            }
                            .setCancelable(false)
                            .show()

                    } else {

                        /*
                         * No existing backup.
                         * Create the first backup.
                         */

                        val result =
                            BackupManager.backupNow(
                                applicationContext,
                                forceReplace = false
                            )

                        Toast.makeText(
                            this@BackupActivity,
                            result.message,
                            Toast.LENGTH_LONG
                        ).show()

                        Log.i(
                            TAG,
                            "Manual backup result: " +
                                    "${result.success}, " +
                                    result.message
                        )

                        btnBackup.isEnabled = true

                        refreshBackupInfo()
                    }

                } catch (e: Exception) {

                    Log.e(
                        TAG,
                        "Manual backup failed",
                        e
                    )

                    Toast.makeText(
                        this@BackupActivity,
                        "Backup failed: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()

                    btnBackup.isEnabled = true
                }
            }
        }

        // ========================================================
        // SAVE AUTO BACKUP SETTING
        // ========================================================

        btnAutoSave.setOnClickListener {

            val selection =
                spinnerAuto.selectedItem as String

            val mode =
                selection.lowercase()

            val prefs =
                getSharedPreferences(
                    prefsName,
                    Context.MODE_PRIVATE
                )

            prefs.edit()
                .putString(
                    "auto_backup_mode",
                    mode
                )
                .apply()

            /*
             * Schedule according to selected mode.
             */
            scheduleAutoBackup(mode)

            Toast.makeText(
                this,
                "Auto backup setting saved",
                Toast.LENGTH_SHORT
            ).show()

            Log.i(
                TAG,
                "Auto backup mode saved: $mode"
            )
        }
    }

    // ============================================================
    // AUTO BACKUP SPINNER
    // ============================================================

    private fun setupAutoSpinner() {

        /*
         * Only these four options.
         */
        val options =
            listOf(
                "Off",
                "Daily",
                "Weekly",
                "Custom"
            )

        val adapter =
            ArrayAdapter(
                this,
                android.R.layout.simple_spinner_item,
                options
            )

        adapter.setDropDownViewResource(
            android.R.layout.simple_spinner_dropdown_item
        )

        spinnerAuto.adapter = adapter

        val prefs =
            getSharedPreferences(
                prefsName,
                Context.MODE_PRIVATE
            )

        val mode =
            prefs.getString(
                "auto_backup_mode",
                "off"
            ) ?: "off"

        val index =
            options.indexOfFirst {
                it.equals(
                    mode,
                    ignoreCase = true
                )
            }

        spinnerAuto.setSelection(
            if (index >= 0) index else 0
        )
    }

    // ============================================================
    // SCHEDULE AUTO BACKUP
    // ============================================================

    private fun scheduleAutoBackup(
        mode: String
    ) {

        val workManager =
            WorkManager.getInstance(
                applicationContext
            )

        when (mode) {

            // ----------------------------------------------------
            // OFF
            // ----------------------------------------------------

            "off" -> {

                workManager.cancelUniqueWork(
                    AUTO_BACKUP_WORK
                )

                Log.i(
                    TAG,
                    "Automatic backup disabled"
                )
            }

            // ----------------------------------------------------
            // DAILY
            // ----------------------------------------------------

            "daily" -> {

                val delay =
                    calculateDelayUntilNextMidnight()

                val request =
                    PeriodicWorkRequestBuilder<BackupWorker>(
                        1,
                        TimeUnit.DAYS
                    )
                        .setInitialDelay(
                            delay,
                            TimeUnit.MILLISECONDS
                        )
                        .build()

                workManager.enqueueUniquePeriodicWork(
                    AUTO_BACKUP_WORK,
                    ExistingPeriodicWorkPolicy.REPLACE,
                    request
                )

                Log.i(
                    TAG,
                    "Daily backup scheduled. " +
                            "First run in ${delay / 1000 / 60} minutes"
                )
            }

            // ----------------------------------------------------
            // WEEKLY
            // ----------------------------------------------------

            "weekly" -> {

                val delay =
                    calculateDelayUntilNextMondayMidnight()

                val request =
                    PeriodicWorkRequestBuilder<BackupWorker>(
                        7,
                        TimeUnit.DAYS
                    )
                        .setInitialDelay(
                            delay,
                            TimeUnit.MILLISECONDS
                        )
                        .build()

                workManager.enqueueUniquePeriodicWork(
                    AUTO_BACKUP_WORK,
                    ExistingPeriodicWorkPolicy.REPLACE,
                    request
                )

                Log.i(
                    TAG,
                    "Weekly backup scheduled for " +
                            "Monday midnight. " +
                            "First run in ${delay / 1000 / 60} minutes"
                )
            }

            // ----------------------------------------------------
            // CUSTOM
            // ----------------------------------------------------

            "custom" -> {

                val prefs =
                    getSharedPreferences(
                        prefsName,
                        Context.MODE_PRIVATE
                    )

                val days =
                    prefs.getInt(
                        "auto_backup_custom_days",
                        7
                    )

                if (days < 1) {

                    Toast.makeText(
                        this,
                        "Custom days must be at least 1",
                        Toast.LENGTH_LONG
                    ).show()

                    return
                }

                val request =
                    PeriodicWorkRequestBuilder<BackupWorker>(
                        days.toLong(),
                        TimeUnit.DAYS
                    ).build()

                workManager.enqueueUniquePeriodicWork(
                    AUTO_BACKUP_WORK,
                    ExistingPeriodicWorkPolicy.REPLACE,
                    request
                )

                Log.i(
                    TAG,
                    "Custom backup scheduled every $days days"
                )
            }

            else -> {

                workManager.cancelUniqueWork(
                    AUTO_BACKUP_WORK
                )
            }
        }
    }

    // ============================================================
    // NEXT MIDNIGHT
    // ============================================================

    private fun calculateDelayUntilNextMidnight(): Long {

        val now =
            Calendar.getInstance()

        val next =
            Calendar.getInstance()

        next.timeInMillis =
            now.timeInMillis

        next.add(
            Calendar.DAY_OF_YEAR,
            1
        )

        next.set(
            Calendar.HOUR_OF_DAY,
            0
        )

        next.set(
            Calendar.MINUTE,
            0
        )

        next.set(
            Calendar.SECOND,
            0
        )

        next.set(
            Calendar.MILLISECOND,
            0
        )

        return (
                next.timeInMillis -
                        now.timeInMillis
                )
            .coerceAtLeast(0L)
    }

    // ============================================================
    // NEXT MONDAY MIDNIGHT
    // ============================================================

    private fun calculateDelayUntilNextMondayMidnight(): Long {

        val now =
            Calendar.getInstance()

        val next =
            Calendar.getInstance()

        next.timeInMillis =
            now.timeInMillis

        /*
         * Calendar.MONDAY = 2
         *
         * Find the next Monday.
         */
        var daysUntilMonday =
            (
                    Calendar.MONDAY -
                            next.get(Calendar.DAY_OF_WEEK) +
                            7
                    ) % 7

        /*
         * If today is Monday but today's midnight
         * has already passed, schedule next Monday.
         */
        if (daysUntilMonday == 0) {
            daysUntilMonday = 7
        }

        next.add(
            Calendar.DAY_OF_YEAR,
            daysUntilMonday
        )

        next.set(
            Calendar.HOUR_OF_DAY,
            0
        )

        next.set(
            Calendar.MINUTE,
            0
        )

        next.set(
            Calendar.SECOND,
            0
        )

        next.set(
            Calendar.MILLISECOND,
            0
        )

        return (
                next.timeInMillis -
                        now.timeInMillis
                )
            .coerceAtLeast(0L)
    }

    // ============================================================
    // REFRESH BACKUP INFORMATION
    // ============================================================

    @RequiresApi(Build.VERSION_CODES.O)
    private fun refreshBackupInfo() {

        lifecycleScope.launch {

            try {

                /*
                 * Calculate current local Room database size.
                 */
                val content =
                    BackupManager.generateBackupContent(
                        applicationContext
                    )

                val bytes =
                    content.toByteArray(
                        Charsets.UTF_8
                    )

                val kb =
                    bytes.size / 1024.0

                tvDataSize.text =
                    String.format(
                        "Data to back up: %.1f KB",
                        kb
                    )

                /*
                 * Check cloud backup.
                 */
                val token =
                    AuthManager.getAccessToken()

                if (token.isNullOrEmpty()) {

                    tvLastBackup.text =
                        "Last backup: Not signed in"

                    return@launch
                }

                val meta =
                    BackupManager.getBackupFileMetadata(
                        token
                    )

                if (
                    meta?.id != null &&
                    meta.modifiedTime != null
                ) {

                    val dateFormat =
                        DateFormat.getDateTimeInstance()

                    val whenString =
                        dateFormat.format(
                            java.util.Date(
                                meta.modifiedTime
                            )
                        )

                    val sizeKb =
                        (meta.size ?: 0L) /
                                1024.0

                    tvLastBackup.text =
                        "Last backup: $whenString (" +
                                String.format(
                                    "%.1f KB",
                                    sizeKb
                                ) +
                                ")"

                } else {

                    tvLastBackup.text =
                        "Last backup: Never"
                }

            } catch (e: Exception) {

                Log.e(
                    TAG,
                    "Failed to refresh backup info",
                    e
                )
            }
        }
    }

    // ============================================================
    // STATUS
    // ============================================================

    // ============================================================
    // RESUME
    // ============================================================

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onResume() {

        super.onResume()

        refreshBackupInfo()
    }
}