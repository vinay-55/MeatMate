package com.example.store.backup

import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.text.style.ForegroundColorSpan
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.store.R
import com.example.store.data.local.DatabaseProvider
import kotlinx.coroutines.launch

import com.example.store.backup.BackupManagerImpl as BackupManager

class FirstSetupActivity : AppCompatActivity() {

    private lateinit var tvMessage: TextView
    private lateinit var btnPrimary: Button
    private lateinit var btnSecondary: Button
    private lateinit var progress: ProgressBar

    /*
     * Possible values:
     *
     * restore
     * new
     * backup_existing
     */
    private var pendingAction: String? = null

    private val prefsName = "backup_prefs"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_first_setup)

        tvMessage =
            findViewById(R.id.fs_message)

        btnPrimary =
            findViewById(R.id.fs_primary)

        btnSecondary =
            findViewById(R.id.fs_secondary)

        progress =
            findViewById(R.id.fs_progress)

        /*
         * Initialize Google authentication.
         */
        AuthManager.init(this)

        /*
         * Show Terms & Privacy acceptance only once.
         */
        showTermsAndPrivacyIfRequired()
    }

    // ============================================================
    // TERMS & PRIVACY ACCEPTANCE
    // ============================================================

    private fun showTermsAndPrivacyIfRequired() {

        val prefs =
            getSharedPreferences(
                prefsName,
                MODE_PRIVATE
            )

        val termsAccepted =
            prefs.getBoolean(
                "terms_privacy_accepted",
                false
            )

        /*
         * Already accepted previously.
         * Continue normally.
         */
        if (termsAccepted) {

            checkExistingLocalData()

            return
        }

        /*
         * First installation/setup.
         *
         * Only the words "Terms and Conditions"
         * and "Privacy Policy" are displayed as links.
         *
         * The actual URLs are hidden from the user.
         */
        val consentText =
            "By continuing, you agree to the Terms and Conditions and acknowledge the Privacy Policy."

        val spannable =
            SpannableString(consentText)

        // --------------------------------------------------------
        // TERMS AND CONDITIONS
        // --------------------------------------------------------

        val termsText =
            "Terms and Conditions"

        val termsStart =
            consentText.indexOf(termsText)

        val termsEnd =
            termsStart + termsText.length

        val termsClick =
            object : ClickableSpan() {

                override fun onClick(widget: View) {

                    val intent =
                        Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse(
                                "https://vinay-55.github.io/store-app/terms.html"
                            )
                        )

                    startActivity(intent)
                }
            }

        spannable.setSpan(
            termsClick,
            termsStart,
            termsEnd,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        spannable.setSpan(
            ForegroundColorSpan(
                Color.rgb(
                    25,
                    103,
                    210
                )
            ),
            termsStart,
            termsEnd,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        // --------------------------------------------------------
        // PRIVACY POLICY
        // --------------------------------------------------------

        val privacyText =
            "Privacy Policy"

        val privacyStart =
            consentText.indexOf(privacyText)

        val privacyEnd =
            privacyStart + privacyText.length

        val privacyClick =
            object : ClickableSpan() {

                override fun onClick(widget: View) {

                    val intent =
                        Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse(
                                "https://vinay-55.github.io/store-app/privacy.html"
                            )
                        )

                    startActivity(intent)
                }
            }

        spannable.setSpan(
            privacyClick,
            privacyStart,
            privacyEnd,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        spannable.setSpan(
            ForegroundColorSpan(
                Color.rgb(
                    25,
                    103,
                    210
                )
            ),
            privacyStart,
            privacyEnd,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        // --------------------------------------------------------
        // CONSENT TEXT VIEW
        // --------------------------------------------------------

        val message =
            TextView(this).apply {

                text =
                    spannable

                setPadding(
                    50,
                    30,
                    50,
                    10
                )

                textSize =
                    16f

                movementMethod =
                    LinkMovementMethod.getInstance()

                highlightColor =
                    Color.TRANSPARENT
            }

        // --------------------------------------------------------
        // CONSENT DIALOG
        // --------------------------------------------------------

        AlertDialog.Builder(this)
            .setTitle(
                "Terms & Privacy"
            )
            .setView(message)

            /*
             * User accepts.
             */
            .setPositiveButton(
                "Accept"
            ) { dialog, _ ->

                prefs.edit()
                    .putBoolean(
                        "terms_privacy_accepted",
                        true
                    )
                    .apply()

                dialog.dismiss()

                /*
                 * Continue with normal first setup.
                 */
                checkExistingLocalData()
            }

            /*
             * User declines.
             */
            .setNegativeButton(
                "Decline"
            ) { dialog, _ ->

                dialog.dismiss()

                /*
                 * Do not allow the user to continue.
                 */
                finishAffinity()
            }

            /*
             * User cannot dismiss the dialog by tapping
             * outside it or pressing the Back button.
             */
            .setCancelable(false)

            .show()
    }

    // ============================================================
    // CHECK LOCAL DATABASE
    // ============================================================

    private fun checkExistingLocalData() {

        lifecycleScope.launch {

            progress.visibility =
                View.VISIBLE

            try {

                val db =
                    DatabaseProvider.getDatabase(
                        applicationContext
                    )

                val existing =
                    db.businessDao()
                        .getAllBusiness()

                progress.visibility =
                    View.GONE

                if (existing.isNotEmpty()) {

                    /*
                     * Existing installation/user.
                     *
                     * Do NOT delete local data.
                     */
                    showExistingUserScreen()

                } else {

                    /*
                     * New/empty local database.
                     *
                     * Ask whether the user wants
                     * to restore an old backup.
                     */
                    showNewUserScreen()
                }

            } catch (e: Exception) {

                progress.visibility =
                    View.GONE

                Toast.makeText(
                    this@FirstSetupActivity,
                    "Unable to check local data: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    // ============================================================
    // EXISTING USER
    // ============================================================

    private fun showExistingUserScreen() {

        tvMessage.text =
            "Protect your existing business data"

        btnPrimary.text =
            "Sign in and Back up"

        btnSecondary.text =
            "Maybe later"

        btnPrimary.setOnClickListener {

            pendingAction =
                "backup_existing"

            AuthManager.startSignIn()
        }

        btnSecondary.setOnClickListener {

            finishSetup()
        }
    }

    // ============================================================
    // NEW USER
    // ============================================================

    private fun showNewUserScreen() {

        tvMessage.text =
            "Do you have previous data?"

        btnPrimary.text =
            "Restore Previous Data"

        btnSecondary.text =
            "I'm a New User"

        /*
         * Restore previous backup.
         */
        btnPrimary.setOnClickListener {

            pendingAction =
                "restore"

            AuthManager.startSignIn()
        }

        /*
         * Start as a completely new user.
         */
        btnSecondary.setOnClickListener {

            pendingAction =
                "new"

            AuthManager.startSignIn()
        }
    }

    // ============================================================
    // HANDLE SIGN-IN RESULT
    // ============================================================

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onResume() {

        super.onResume()

        val action =
            pendingAction
                ?: return

        /*
         * AuthManager receives the sign-in result.
         *
         * If account is available, continue the
         * action selected by the user.
         */
        val account =
            AuthManager.getSignedInAccount()
                ?: return

        lifecycleScope.launch {

            /*
             * Prevent executing the same action
             * repeatedly on subsequent onResume calls.
             */
            pendingAction = null

            when (action) {

                "backup_existing" -> {

                    backupExistingUserData()
                }

                "restore" -> {

                    restorePreviousData()
                }

                "new" -> {

                    handleNewUser()
                }
            }
        }
    }

    // ============================================================
    // EXISTING USER -> BACKUP
    // ============================================================

    @RequiresApi(Build.VERSION_CODES.O)
    private suspend fun backupExistingUserData() {

        progress.visibility =
            View.VISIBLE

        try {

            val result =
                BackupManager.backupNow(
                    applicationContext,
                    forceReplace = true
                )

            progress.visibility =
                View.GONE

            if (result.success) {

                Toast.makeText(
                    this,
                    "Initial backup completed",
                    Toast.LENGTH_LONG
                ).show()

                finishSetup()

            } else {

                Toast.makeText(
                    this,
                    "Backup failed: ${result.message}",
                    Toast.LENGTH_LONG
                ).show()
            }

        } catch (e: Exception) {

            progress.visibility =
                View.GONE

            Toast.makeText(
                this,
                "Backup failed: ${e.message}",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    // ============================================================
    // RESTORE PREVIOUS DATA
    // ============================================================

    private suspend fun restorePreviousData() {

        progress.visibility =
            View.VISIBLE

        try {

            val result =
                BackupManager.restoreIfAvailable(
                    applicationContext
                )

            progress.visibility =
                View.GONE

            if (result.success) {

                Toast.makeText(
                    this,
                    "Restored: ${result.imported} records " +
                            "(skipped: ${result.skipped})",
                    Toast.LENGTH_LONG
                ).show()

            } else {

                Toast.makeText(
                    this,
                    result.error
                        ?: "No previous backup found",
                    Toast.LENGTH_LONG
                ).show()
            }

            finishSetup()

        } catch (e: Exception) {

            progress.visibility =
                View.GONE

            Toast.makeText(
                this,
                "Restore failed: ${e.message}",
                Toast.LENGTH_LONG
            ).show()

            finishSetup()
        }
    }

    // ============================================================
    // NEW USER
    // ============================================================

    @RequiresApi(Build.VERSION_CODES.O)
    private suspend fun handleNewUser() {

        progress.visibility =
            View.VISIBLE

        try {

            val token =
                AuthManager.getAccessToken()

            /*
             * Check whether this Google account
             * already contains a backup.
             */
            val meta =
                if (!token.isNullOrEmpty()) {

                    BackupManager
                        .getBackupFileMetadata(token)

                } else {

                    null
                }

            progress.visibility =
                View.GONE

            if (meta?.id != null) {

                /*
                 * The account already has data.
                 *
                 * Give the user a choice.
                 */
                showExistingCloudBackupDialog()

            } else {

                /*
                 * No cloud backup.
                 *
                 * Start completely new.
                 */
                startNewUser()
            }

        } catch (e: Exception) {

            progress.visibility =
                View.GONE

            Toast.makeText(
                this,
                "Unable to check backup: ${e.message}",
                Toast.LENGTH_LONG
            ).show()

            startNewUser()
        }
    }

    // ============================================================
    // CLOUD BACKUP ALREADY EXISTS
    // ============================================================

    private fun showExistingCloudBackupDialog() {

        AlertDialog.Builder(this)
            .setTitle(
                "Existing data found"
            )
            .setMessage(
                "This Google account already has backup data. " +
                        "What would you like to do?"
            )
            .setNegativeButton(
                "Start new"
            ) { dialog, _ ->

                dialog.dismiss()

                startNewUser()
            }
            .setPositiveButton(
                "Continue with old data"
            ) { dialog, _ ->

                dialog.dismiss()

                lifecycleScope.launch {

                    restorePreviousData()
                }
            }
            .setCancelable(false)
            .show()
    }

    // ============================================================
    // START NEW USER
    // ============================================================

    private fun startNewUser() {

        lifecycleScope.launch {

            try {

                val db =
                    DatabaseProvider.getDatabase(
                        applicationContext
                    )

                /*
                 * Clear local data only when the user
                 * explicitly chooses "Start new".
                 */
                db.clearAllTables()

                Toast.makeText(
                    this@FirstSetupActivity,
                    "Started new user",
                    Toast.LENGTH_SHORT
                ).show()

            } catch (e: Exception) {

                Toast.makeText(
                    this@FirstSetupActivity,
                    "Could not start new user: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }

            finishSetup()
        }
    }

    // ============================================================
    // COMPLETE SETUP
    // ============================================================

    private fun finishSetup() {

        getSharedPreferences(
            prefsName,
            MODE_PRIVATE
        )
            .edit()
            .putBoolean(
                "setup_complete",
                true
            )
            .apply()

        finish()
    }
}