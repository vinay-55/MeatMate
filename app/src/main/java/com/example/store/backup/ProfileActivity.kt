package com.example.store.backup

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import androidx.work.WorkManager
import com.example.store.BaseActivity
import com.example.store.R
import com.example.store.data.catalog.ProductCatalogRepository
import com.example.store.data.local.DatabaseProvider
import com.example.store.backup.BackupManagerImpl as BackupManager
import com.google.android.gms.auth.api.signin.GoogleSignIn
import kotlinx.coroutines.launch
import java.io.File

class ProfileActivity : BaseActivity() {

    companion object {
        private const val TAG = "ProfileActivity"
    }

    private lateinit var btnLogout: Button
    private lateinit var tvStatus: TextView
    private lateinit var backButton: ImageButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        btnLogout = findViewById(R.id.btnLogout)
        tvStatus = findViewById(R.id.bu_status)
        backButton = findViewById(R.id.backButton)

        AuthManager.init(this)
        BackupManager.init(this)

        backButton.setOnClickListener {
            finish()
        }

        btnLogout.setOnClickListener {
            showLogoutConfirmationDialog()
        }

        updateStatus()
    }

    private fun showLogoutConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle(R.string.logout_warning_title)
            .setMessage(R.string.logout_warning_message)
            .setPositiveButton(R.string.logout_backup_and_logout) { _, _ ->
                backupAndLogout()
            }
            .setNeutralButton(R.string.logout_anyway) { _, _ ->
                performLogout()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun backupAndLogout() {
        btnLogout.isEnabled = false
        val progressDialog = AlertDialog.Builder(this)
            .setMessage(R.string.backup_in_progress)
            .setCancelable(false)
            .create()
        progressDialog.show()

        lifecycleScope.launch {
            var backupSuccess = false
            var failureMessage: String? = null

            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val result = BackupManager.backupNow(applicationContext, forceReplace = true)
                    backupSuccess = result.success
                    if (!result.success) {
                        failureMessage = result.message
                    }
                } else {
                    failureMessage = "Unsupported Android version"
                }
            } catch (e: Exception) {
                backupSuccess = false
                failureMessage = e.message ?: "Unknown error"
            } finally {
                try {
                    progressDialog.dismiss()
                } catch (_: Exception) {}
            }

            if (backupSuccess) {
                Toast.makeText(this@ProfileActivity, getString(R.string.initial_backup_completed), Toast.LENGTH_SHORT).show()
                performLogout()
            } else {
                btnLogout.isEnabled = true
                val explanation = failureMessage ?: getString(R.string.unable_to_check_backup_status)
                AlertDialog.Builder(this@ProfileActivity)
                    .setTitle(R.string.unable_to_check_backup_status)
                    .setMessage(getString(R.string.backup_failed_format, explanation))
                    .setPositiveButton(R.string.retry) { _, _ ->
                        backupAndLogout()
                    }
                    .setNeutralButton(R.string.logout_anyway) { _, _ ->
                        performLogout()
                    }
                    .setNegativeButton(R.string.cancel, null)
                    .show()
            }
        }
    }

    private fun performLogout() {
        val appContext = applicationContext

        // 1. Capture the currently authenticated Google account's stable storage key BEFORE sign out
        val accountKey = AuthManager.getActiveAccountStorageKey(appContext)
        val signedInAccount = AuthManager.getSignedInAccount()
            ?: GoogleSignIn.getLastSignedInAccount(appContext)
        val accountEmail = signedInAccount?.email ?: AuthManager.getSelectedAccountEmail()

        // 2. Cancel active background auto-backup so it does not run or recreate data
        try {
            WorkManager.getInstance(appContext).cancelUniqueWork("auto_backup_work")
        } catch (e: Exception) {
            Log.w(TAG, "Error cancelling background work", e)
        }

        // 3. Delete all local data belonging exclusively to the logging-out account
        if (!accountKey.isNullOrEmpty()) {
            val dbDeleted = DatabaseProvider.deleteAccountDatabase(appContext, accountKey)
            val catalogDeleted = ProductCatalogRepository.deleteAccountCatalog(appContext, accountKey)
            val pricesDeleted = deleteAccountPricePreferences(appContext, accountKey)
            val legacyCleaned = cleanupLegacyAccountFilesIfOwned(appContext, accountEmail)

            if (!dbDeleted || !catalogDeleted || !pricesDeleted || !legacyCleaned) {
                Log.e(TAG, "Account cleanup failed: db=$dbDeleted, catalog=$catalogDeleted, prices=$pricesDeleted, legacy=$legacyCleaned")
                AlertDialog.Builder(this)
                    .setTitle(R.string.unable_to_check_backup_status)
                    .setMessage(R.string.cleanup_failed_message)
                    .setPositiveButton(R.string.close, null)
                    .show()
                btnLogout.isEnabled = true
                return
            }
        } else {
            DatabaseProvider.closeCurrentDatabase()
            ProductCatalogRepository.resetInstance()
        }

        // 4. Complete the Google sign-out flow
        AuthManager.signOut()

        // 5. Clear authenticated session state and account-specific preferences
        // Preserves shared application settings such as selected language and terms_privacy_accepted
        getSharedPreferences("backup_prefs", Context.MODE_PRIVATE)
            .edit()
            .putBoolean("setup_complete", false)
            .putBoolean("is_logged_out", true)
            .putString("auto_backup_mode", "off")
            .remove("selected_account_email")
            .remove("selected_account_id")
            .remove("auto_backup_custom_days")
            .apply()

        // 6. Navigate to FirstSetupActivity
        val intent = Intent(this, FirstSetupActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finishAffinity()
    }

    private fun deleteAccountPricePreferences(context: Context, accountKey: String): Boolean {
        return try {
            val prefsName = "price_prefs_$accountKey"
            val prefs = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
            prefs.edit().clear().commit()
            val prefsFile = File(context.applicationInfo.dataDir, "shared_prefs/${prefsName}.xml")
            if (prefsFile.exists()) {
                prefsFile.delete()
            }
            !prefsFile.exists()
        } catch (e: Exception) {
            Log.e(TAG, "Failed deleting price preferences for $accountKey", e)
            false
        }
    }

    private fun cleanupLegacyAccountFilesIfOwned(context: Context, accountEmail: String?): Boolean {
        if (accountEmail.isNullOrBlank()) return true
        return try {
            val backupPrefs = context.getSharedPreferences("backup_prefs", Context.MODE_PRIVATE)
            val recordedOwner = backupPrefs.getString("legacy_recorded_owner_email", null)
            if (recordedOwner != null && recordedOwner.equals(accountEmail, ignoreCase = true)) {
                val legacyBackupDb = context.getDatabasePath("business_database_legacy_backup")
                if (legacyBackupDb.exists()) {
                    context.deleteDatabase("business_database_legacy_backup")
                    val wal = File(legacyBackupDb.parentFile, "business_database_legacy_backup-wal")
                    val shm = File(legacyBackupDb.parentFile, "business_database_legacy_backup-shm")
                    val journal = File(legacyBackupDb.parentFile, "business_database_legacy_backup-journal")
                    if (wal.exists()) wal.delete()
                    if (shm.exists()) shm.delete()
                    if (journal.exists()) journal.delete()
                }
                val legacyCatBackup = File(context.filesDir, "product_catalog_legacy_backup.json")
                if (legacyCatBackup.exists()) {
                    legacyCatBackup.delete()
                }
                backupPrefs.edit().remove("legacy_recorded_owner_email").apply()
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed cleaning up legacy files for $accountEmail", e)
            false
        }
    }

    private fun updateStatus() {
        val account = AuthManager.getSignedInAccount()
        tvStatus.text = if (account != null) {
            "Signed in: ${account.email}"
        } else {
            getString(R.string.not_signed_in)
        }
    }

    override fun onResume() {
        super.onResume()
        updateStatus()
    }
}
