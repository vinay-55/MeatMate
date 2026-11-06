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
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.lifecycle.lifecycleScope
import com.example.store.BaseActivity
import com.example.store.LanguageSelectionActivity
import com.example.store.LocaleHelper
import com.example.store.R
import com.example.store.data.local.DatabaseProvider
import com.example.store.databinding.DialogFinalConfirmationBinding
import kotlinx.coroutines.launch

import com.example.store.backup.BackupManagerImpl as BackupManager

class FirstSetupActivity : BaseActivity() {

    override val requiresAuthentication: Boolean = false

    private lateinit var tvMessage: TextView
    private lateinit var btnPrimary: Button
    private lateinit var btnSecondary: Button
    private lateinit var btnTertiary: Button
    private lateinit var progress: ProgressBar

    /*
     * Possible values:
     * restore
     * new
     * backup_existing
     * reauth_original
     */
    private var pendingAction: String? = null

    private val prefsName = "backup_prefs"

    private val languageSelectionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (LocaleHelper.isLanguageSelected(this)) {
            recreate()
        } else {
            // Re-prompt language selection if not completed
            checkLanguageSelectionThenProceed()
        }
    }

    private var pendingTargetAccount: com.google.android.gms.auth.api.signin.GoogleSignInAccount? = null
    private var pendingFirstSetupAction: String? = null
    private var isViewingNewUserScreenFromRecovery: Boolean = false

    private val recoverableAuthLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        val target = pendingTargetAccount
        val firstSetupAction = pendingFirstSetupAction
        pendingTargetAccount = null
        pendingFirstSetupAction = null

        if (target != null) {
            lifecycleScope.launch {
                if (firstSetupAction != null) {
                    processFirstSetupAccount(target, firstSetupAction)
                } else {
                    verifyAndCompleteOriginalAccount(target)
                }
            }
        } else {
            if (AuthManager.isReauthRequired(this)) {
                showForcedReauthScreen()
            } else {
                showNewUserScreen()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_first_setup)

        tvMessage = findViewById(R.id.fs_message)
        btnPrimary = findViewById(R.id.fs_primary)
        btnSecondary = findViewById(R.id.fs_secondary)
        btnTertiary = findViewById(R.id.fs_tertiary)
        progress = findViewById(R.id.fs_progress)

        /*
         * Initialize Google authentication.
         */
        AuthManager.init(this)

        onBackPressedDispatcher.addCallback(this, object : androidx.activity.OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (isViewingNewUserScreenFromRecovery) {
                    isViewingNewUserScreenFromRecovery = false
                    showForcedReauthScreen()
                } else if (AuthManager.isReauthRequired(this@FirstSetupActivity)) {
                    // Do not allow bypassing forced reauth; minimize/exit safely
                    finishAffinity()
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })

        if (AuthManager.isReauthRequired(this)) {
            showForcedReauthScreen()
            return
        }

        /*
         * Show Terms & Privacy acceptance only once,
         * followed by language selection, then existing restore/new-user flow.
         */
        showTermsAndPrivacyIfRequired()
    }

    private fun showForcedReauthScreen() {
        isViewingNewUserScreenFromRecovery = false
        val originalEmail = AuthManager.getOriginalAccountEmail(this) ?: "your Google account"

        tvMessage.text = getString(R.string.reauth_screen_message, originalEmail)

        btnPrimary.text = getString(R.string.btn_reauth_continue_original)
        btnPrimary.visibility = View.VISIBLE

        btnSecondary.text = getString(R.string.btn_reauth_new_user)
        btnSecondary.visibility = View.VISIBLE

        btnTertiary.visibility = View.GONE

        progress.visibility = View.GONE

        // Option 1: Continue with my Google account
        btnPrimary.setOnClickListener {
            pendingAction = "reauth_original"
            AuthManager.startSignIn(forceAccountChooser = true)
        }

        // Option 2: Start as a new user (with explicit confirmation alert before deletion)
        btnSecondary.setOnClickListener {
            showStartAsNewUserConfirmationDialog()
        }
    }

    private fun showStartAsNewUserConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle(R.string.reauth_start_new_confirm_title)
            .setMessage(R.string.reauth_start_new_confirm_message)
            .setPositiveButton(R.string.btn_proceed) { dialog, _ ->
                dialog.dismiss()
                executeStartAsNewUserProceed()
            }
            .setNegativeButton(R.string.cancel) { dialog, _ ->
                dialog.dismiss()
                // Cancel: Dismiss the dialog.
                // Preserve all local records and product catalogs.
                // Preserve the recovery-required state.
                // Do not navigate to first-time setup.
                // Do not delete any files or preferences.
            }
            .setCancelable(false)
            .show()
    }

    private fun executeStartAsNewUserProceed() {
        val originalId = AuthManager.getOriginalAccountId(this)
        val originalEmail = AuthManager.getOriginalAccountEmail(this)
        val activeKey = AuthManager.getActiveAccountStorageKey(this)

        val targetKey = when {
            !originalId.isNullOrBlank() -> AuthManager.deriveAccountStorageKey(originalId)
            !activeKey.isNullOrBlank() -> activeKey
            !originalEmail.isNullOrBlank() -> AuthManager.deriveAccountStorageKey("legacy_email_${originalEmail.trim().lowercase()}")
            else -> null
        }

        // If identity cannot be determined safely, stop destructive operation and report problem
        if (targetKey.isNullOrBlank()) {
            Toast.makeText(this, getString(R.string.reauth_identity_unresolvable), Toast.LENGTH_LONG).show()
            return
        }

        // Clear only intended local data using existing mechanisms
        DatabaseProvider.closeCurrentDatabase()
        DatabaseProvider.deleteAccountDatabase(this, targetKey)
        com.example.store.data.catalog.ProductCatalogRepository.deleteAccountCatalog(this, targetKey)
        com.example.store.data.catalog.ProductCatalogRepository.resetInstance()

        // Clear obsolete local account-association and recovery state
        AuthManager.clearStoredAccountInfo(this)
        AuthManager.clearReauthRequired(this)
        AuthManager.signOutClientOnly()
        isViewingNewUserScreenFromRecovery = false

        // Navigate to existing first-time setup flow
        showTermsAndPrivacyIfRequired()
    }

    // ============================================================
    // TERMS & PRIVACY ACCEPTANCE
    // ============================================================

    private fun showTermsAndPrivacyIfRequired() {

        val prefs = getSharedPreferences(prefsName, MODE_PRIVATE)
        val termsAccepted = prefs.getBoolean("terms_privacy_accepted", false)

        /*
         * Already accepted previously.
         * Continue to language selection check.
         */
        if (termsAccepted) {
            checkLanguageSelectionThenProceed()
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

        val spannable = SpannableString(consentText)

        // --------------------------------------------------------
        // TERMS AND CONDITIONS
        // --------------------------------------------------------

        val termsText = "Terms and Conditions"
        val termsStart = consentText.indexOf(termsText)
        val termsEnd = termsStart + termsText.length

        val termsClick = object : ClickableSpan() {
            override fun onClick(widget: View) {
                val intent = Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://vinay-55.github.io/store-app/terms.html")
                )
                startActivity(intent)
            }
        }

        spannable.setSpan(termsClick, termsStart, termsEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        spannable.setSpan(
            ForegroundColorSpan(Color.rgb(25, 103, 210)),
            termsStart,
            termsEnd,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        // --------------------------------------------------------
        // PRIVACY POLICY
        // --------------------------------------------------------

        val privacyText = "Privacy Policy"
        val privacyStart = consentText.indexOf(privacyText)
        val privacyEnd = privacyStart + privacyText.length

        val privacyClick = object : ClickableSpan() {
            override fun onClick(widget: View) {
                val intent = Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://vinay-55.github.io/store-app/privacy.html")
                )
                startActivity(intent)
            }
        }

        spannable.setSpan(privacyClick, privacyStart, privacyEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        spannable.setSpan(
            ForegroundColorSpan(Color.rgb(25, 103, 210)),
            privacyStart,
            privacyEnd,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        // --------------------------------------------------------
        // CONSENT TEXT VIEW
        // --------------------------------------------------------

        val message = TextView(this).apply {
            text = spannable
            setPadding(50, 30, 50, 10)
            textSize = 16f
            movementMethod = LinkMovementMethod.getInstance()
            highlightColor = Color.TRANSPARENT
        }

        // --------------------------------------------------------
        // CONSENT DIALOG
        // --------------------------------------------------------

        AlertDialog.Builder(this)
            .setTitle("Terms & Privacy")
            .setView(message)
            .setPositiveButton("Accept") { dialog, _ ->
                prefs.edit().putBoolean("terms_privacy_accepted", true).apply()
                dialog.dismiss()
                checkLanguageSelectionThenProceed()
            }
            .setNegativeButton("Decline") { dialog, _ ->
                dialog.dismiss()
                finishAffinity()
            }
            .setCancelable(false)
            .show()
    }

    // ============================================================
    // LANGUAGE SELECTION (AFTER TERMS & PRIVACY ACCEPTANCE)
    // ============================================================

    private fun checkLanguageSelectionThenProceed() {
        if (!LocaleHelper.isLanguageSelected(this)) {
            val intent = Intent(this, LanguageSelectionActivity::class.java)
            intent.putExtra(LanguageSelectionActivity.EXTRA_IS_FROM_SETTINGS, false)
            languageSelectionLauncher.launch(intent)
        } else {
            checkExistingLocalData()
        }
    }

    // ============================================================
    // CHECK LOCAL DATABASE
    // ============================================================

    private fun checkExistingLocalData() {
        progress.visibility = View.GONE
        showNewUserScreen()
    }

    // ============================================================
    // EXISTING USER
    // ============================================================

    private fun showExistingUserScreen() {
        tvMessage.text = getString(R.string.protect_your_data_title)
        btnPrimary.text = getString(R.string.sign_in_and_backup)
        btnSecondary.text = getString(R.string.restore_previous_data)
        btnSecondary.visibility = View.VISIBLE
        btnTertiary.visibility = View.GONE

        btnPrimary.setOnClickListener {
            pendingAction = "backup_existing"
            AuthManager.startSignIn()
        }

        btnSecondary.setOnClickListener {
            pendingAction = "restore"
            AuthManager.startSignIn(forceAccountChooser = true)
        }
    }

    // ============================================================
    // NEW USER
    // ============================================================

    private fun showNewUserScreen() {

        tvMessage.text = getString(R.string.do_you_have_previous_data)
        btnPrimary.text = getString(R.string.restore_previous_data)
        btnSecondary.text = getString(R.string.im_a_new_user)
        btnSecondary.visibility = View.VISIBLE
        btnTertiary.visibility = View.GONE

        /*
         * Restore previous backup.
         */
        btnPrimary.setOnClickListener {
            pendingAction = "restore"
            AuthManager.startSignIn(forceAccountChooser = true)
        }

        /*
         * Start as a completely new user.
         */
        btnSecondary.setOnClickListener {
            pendingAction = "new"
            AuthManager.startSignIn(forceAccountChooser = true)
        }
    }

    // ============================================================
    // HANDLE SIGN-IN RESULT
    // ============================================================

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onResume() {

        super.onResume()

        val action = pendingAction ?: return

        /*
         * AuthManager receives the sign-in result.
         * If account is available, continue the action selected by the user.
         */
        val account = AuthManager.getSignedInAccount()
        if (account == null) {
            // User cancelled account selection or sign-in failed.
            // Reset pendingAction so user can choose an account again freely.
            pendingAction = null
            return
        }

        lifecycleScope.launch {
            pendingAction = null

            when (action) {
                "forced_reauth", "reauth_original" -> {
                    handleReauthOriginalAccount(account)
                }
                "backup_existing", "restore", "new" -> {
                    processFirstSetupAccount(account, action)
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private suspend fun processFirstSetupAccount(
        account: com.google.android.gms.auth.api.signin.GoogleSignInAccount,
        action: String
    ) {
        progress.visibility = View.VISIBLE
        tvMessage.text = getString(R.string.checking_backup_status)

        val tokenResult = AuthManager.getAccessTokenResult(this, targetAccount = account)

        when (tokenResult) {
            is AuthManager.AuthTokenResult.UserRecoverable -> {
                progress.visibility = View.GONE
                pendingTargetAccount = account
                pendingFirstSetupAction = action
                recoverableAuthLauncher.launch(tokenResult.intent)
                return
            }
            is AuthManager.AuthTokenResult.AuthFailure -> {
                progress.visibility = View.GONE
                AlertDialog.Builder(this)
                    .setTitle(R.string.backup_status_reauth_required)
                    .setMessage(getString(R.string.reauth_failed_message, tokenResult.reason))
                    .setPositiveButton(R.string.retry) { dialog, _ ->
                        dialog.dismiss()
                        pendingAction = action
                        AuthManager.startSignIn(forceAccountChooser = true)
                    }
                    .setNegativeButton(R.string.cancel, null)
                    .setCancelable(false)
                    .show()
                return
            }
            is AuthManager.AuthTokenResult.NetworkError -> {
                progress.visibility = View.GONE
                AlertDialog.Builder(this)
                    .setTitle(R.string.unable_to_check_backup_status)
                    .setMessage(getString(R.string.backup_status_network_error, tokenResult.message))
                    .setPositiveButton(R.string.retry) { dialog, _ ->
                        dialog.dismiss()
                        lifecycleScope.launch {
                            processFirstSetupAccount(account, action)
                        }
                    }
                    .setNegativeButton(R.string.cancel, null)
                    .setCancelable(false)
                    .show()
                return
            }
            is AuthManager.AuthTokenResult.OtherError -> {
                progress.visibility = View.GONE
                AlertDialog.Builder(this)
                    .setTitle(R.string.unable_to_check_backup_status)
                    .setMessage(getString(R.string.backup_status_failed, tokenResult.message))
                    .setPositiveButton(R.string.retry) { dialog, _ ->
                        dialog.dismiss()
                        lifecycleScope.launch {
                            processFirstSetupAccount(account, action)
                        }
                    }
                    .setNegativeButton(R.string.cancel, null)
                    .setCancelable(false)
                    .show()
                return
            }
            is AuthManager.AuthTokenResult.Success -> {
                progress.visibility = View.GONE
                AuthManager.saveAccount(account)

                when (action) {
                    "backup_existing" -> backupExistingUserData()
                    "restore" -> restorePreviousData()
                    "new" -> handleNewUser()
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private suspend fun handleReauthOriginalAccount(account: com.google.android.gms.auth.api.signin.GoogleSignInAccount) {
        val originalId = AuthManager.getOriginalAccountId(this)
            ?: com.google.android.gms.auth.api.signin.GoogleSignIn.getLastSignedInAccount(this)?.id
        val originalEmail = AuthManager.getOriginalAccountEmail(this)
            ?: com.google.android.gms.auth.api.signin.GoogleSignIn.getLastSignedInAccount(this)?.email

        val hasCandidateIdentity = !account.id.isNullOrBlank() || !account.email.isNullOrBlank()
        val hasOriginalIdentity = !originalId.isNullOrBlank() || !originalEmail.isNullOrBlank()

        if (!hasCandidateIdentity || !hasOriginalIdentity) {
            Toast.makeText(this, getString(R.string.reauth_identity_unresolvable), Toast.LENGTH_LONG).show()
            showForcedReauthScreen()
            return
        }

        val isOriginal = AuthManager.isSameAccount(
            originalId = originalId,
            originalEmail = originalEmail,
            candidateId = account.id,
            candidateEmail = account.email
        )

        if (isOriginal) {
            verifyAndCompleteOriginalAccount(account)
        } else {
            val displayOriginalEmail = originalEmail ?: "original account"
            showOption1MismatchDialog(
                originalEmail = displayOriginalEmail,
                selectedAccount = account
            )
        }
    }

    private fun showOption1MismatchDialog(
        originalEmail: String,
        selectedAccount: com.google.android.gms.auth.api.signin.GoogleSignInAccount
    ) {
        val selectedEmail = selectedAccount.email ?: "another account"
        AlertDialog.Builder(this)
            .setTitle(R.string.reauth_mismatch_dialog_title)
            .setMessage(getString(R.string.reauth_mismatch_dialog_message, selectedEmail, originalEmail))
            .setPositiveButton(R.string.btn_choose_original_account) { dialog, _ ->
                dialog.dismiss()
                AuthManager.signOutClientOnly()
                pendingAction = "reauth_original"
                AuthManager.startSignIn(forceAccountChooser = true)
            }
            .setNegativeButton(R.string.btn_return_to_recovery) { dialog, _ ->
                dialog.dismiss()
                AuthManager.signOutClientOnly()
                pendingAction = null
                showForcedReauthScreen()
            }
            .setCancelable(false)
            .show()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private suspend fun verifyAndCompleteOriginalAccount(
        account: com.google.android.gms.auth.api.signin.GoogleSignInAccount,
        isRetryAfterTokenClear: Boolean = false
    ) {
        progress.visibility = View.VISIBLE
        tvMessage.text = getString(R.string.checking_backup_status)

        val tokenResult = AuthManager.getAccessTokenResult(this, targetAccount = account)

        when (tokenResult) {
            is AuthManager.AuthTokenResult.UserRecoverable -> {
                progress.visibility = View.GONE
                pendingTargetAccount = account
                recoverableAuthLauncher.launch(tokenResult.intent)
                return
            }
            is AuthManager.AuthTokenResult.AuthFailure -> {
                progress.visibility = View.GONE
                Toast.makeText(
                    this,
                    getString(R.string.reauth_failed_message, tokenResult.reason),
                    Toast.LENGTH_LONG
                ).show()
                showForcedReauthScreen()
                return
            }
            is AuthManager.AuthTokenResult.NetworkError -> {
                progress.visibility = View.GONE
                Toast.makeText(
                    this,
                    getString(R.string.backup_status_network_error, tokenResult.message),
                    Toast.LENGTH_LONG
                ).show()
                showForcedReauthScreen()
                return
            }
            is AuthManager.AuthTokenResult.OtherError -> {
                progress.visibility = View.GONE
                Toast.makeText(
                    this,
                    getString(R.string.backup_status_failed, tokenResult.message),
                    Toast.LENGTH_LONG
                ).show()
                showForcedReauthScreen()
                return
            }
            is AuthManager.AuthTokenResult.Success -> {
                val driveStatus = BackupManager.checkBackupStatusWithToken(tokenResult.token)
                progress.visibility = View.GONE

                when (driveStatus) {
                    is BackupManager.BackupCheckStatus.Found,
                    is BackupManager.BackupCheckStatus.NotFound -> {
                        AuthManager.saveAccount(account)
                        AuthManager.clearReauthRequired(this)
                        Toast.makeText(this, "Reconnected successfully", Toast.LENGTH_SHORT).show()
                        finishSetup()
                    }
                    is BackupManager.BackupCheckStatus.AuthFailure -> {
                        // Check if cached token expired (HTTP 401) and we haven't retried yet
                        if (!isRetryAfterTokenClear && driveStatus.message.contains("HTTP 401")) {
                            android.util.Log.i("FirstSetupActivity", "Drive returned HTTP 401. Clearing cached token and retrying once...")
                            AuthManager.clearToken(this, tokenResult.token)
                            verifyAndCompleteOriginalAccount(account, isRetryAfterTokenClear = true)
                            return
                        }
                        // For HTTP 403 or repeated auth failure, do not assume token expiry; report failure safely without looping
                        Toast.makeText(
                            this,
                            getString(R.string.reauth_failed_message, driveStatus.message),
                            Toast.LENGTH_LONG
                        ).show()
                        showForcedReauthScreen()
                    }
                    is BackupManager.BackupCheckStatus.NetworkError -> {
                        Toast.makeText(
                            this,
                            getString(R.string.backup_status_network_error, driveStatus.message),
                            Toast.LENGTH_LONG
                        ).show()
                        showForcedReauthScreen()
                    }
                    is BackupManager.BackupCheckStatus.Error -> {
                        Toast.makeText(
                            this,
                            getString(R.string.unable_to_check_backup_message, driveStatus.message),
                            Toast.LENGTH_LONG
                        ).show()
                        showForcedReauthScreen()
                    }
                }
            }
        }
    }

    // ============================================================
    // EXISTING USER -> BACKUP
    // ============================================================

    @RequiresApi(Build.VERSION_CODES.O)
    private suspend fun backupExistingUserData() {

        progress.visibility = View.VISIBLE

        try {
            val result = BackupManager.backupNow(
                applicationContext,
                forceReplace = true
            )

            progress.visibility = View.GONE

            if (result.success) {
                Toast.makeText(
                    this,
                    getString(R.string.initial_backup_completed),
                    Toast.LENGTH_LONG
                ).show()

                finishSetup()
            } else {
                AlertDialog.Builder(this)
                    .setTitle(R.string.unable_to_check_backup_status)
                    .setMessage(getString(R.string.backup_failed_format, result.message))
                    .setPositiveButton(R.string.retry) { dialog, _ ->
                        dialog.dismiss()
                        lifecycleScope.launch {
                            backupExistingUserData()
                        }
                    }
                    .setNegativeButton(R.string.cancel, null)
                    .setCancelable(false)
                    .show()
            }

        } catch (e: Exception) {
            progress.visibility = View.GONE

            val errorMsg = e.message ?: ""
            AlertDialog.Builder(this)
                .setTitle(R.string.unable_to_check_backup_status)
                .setMessage(getString(R.string.backup_failed_format, errorMsg))
                .setPositiveButton(R.string.retry) { dialog, _ ->
                    dialog.dismiss()
                    lifecycleScope.launch {
                        backupExistingUserData()
                    }
                }
                .setNegativeButton(R.string.cancel, null)
                .setCancelable(false)
                .show()
        }
    }

    // ============================================================
    // RESTORE PREVIOUS DATA
    // ============================================================

    private suspend fun restorePreviousData() {

        progress.visibility = View.VISIBLE

        try {
            val result = BackupManager.restoreIfAvailable(applicationContext)

            progress.visibility = View.GONE

            if (result.success) {
                Toast.makeText(
                    this,
                    getString(R.string.restore_success_format, result.imported, result.skipped),
                    Toast.LENGTH_LONG
                ).show()
                finishSetup()
            } else {
                val isNoBackup = result.error == "No backup found" ||
                        result.error?.contains("no backup", ignoreCase = true) == true
                val title = if (isNoBackup) getString(R.string.no_previous_backup_found) else getString(R.string.unable_to_check_backup_status)
                val msg = if (isNoBackup) {
                    getString(R.string.no_previous_backup_found)
                } else {
                    getString(R.string.restore_failed_format, result.error ?: "")
                }

                AlertDialog.Builder(this)
                    .setTitle(title)
                    .setMessage(msg)
                    .setPositiveButton(R.string.retry) { dialog, _ ->
                        dialog.dismiss()
                        lifecycleScope.launch {
                            restorePreviousData()
                        }
                    }
                    .setNegativeButton(R.string.cancel) { dialog, _ ->
                        dialog.dismiss()
                        AuthManager.signOut()
                    }
                    .setCancelable(false)
                    .show()
            }

        } catch (e: Exception) {
            progress.visibility = View.GONE

            val errorMsg = e.message ?: ""
            AlertDialog.Builder(this)
                .setTitle(R.string.unable_to_check_backup_status)
                .setMessage(getString(R.string.restore_failed_format, errorMsg))
                .setPositiveButton(R.string.retry) { dialog, _ ->
                    dialog.dismiss()
                    lifecycleScope.launch {
                        restorePreviousData()
                    }
                }
                .setNegativeButton(R.string.cancel) { dialog, _ ->
                    dialog.dismiss()
                    AuthManager.signOut()
                }
                .setCancelable(false)
                .show()
        }
    }

    // ============================================================
    // NEW USER FLOW WITH SERIOUS WARNING & INTERNAL PRESERVATION
    // ============================================================

    @RequiresApi(Build.VERSION_CODES.O)
    private suspend fun handleNewUser() {
        progress.visibility = View.VISIBLE

        when (val status = BackupManager.checkBackupStatus(applicationContext)) {
            is BackupManager.BackupCheckStatus.Found -> {
                progress.visibility = View.GONE
                showPreviousDataFoundWarningDialog()
            }
            is BackupManager.BackupCheckStatus.NotFound -> {
                progress.visibility = View.GONE
                startNewUser(clearLocalData = false)
            }
            is BackupManager.BackupCheckStatus.Error -> {
                progress.visibility = View.GONE
                showUnableToDetermineBackupDialog(status.message)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun showUnableToDetermineBackupDialog(errorMessage: String) {
        AlertDialog.Builder(this)
            .setTitle(R.string.unable_to_check_backup_status)
            .setMessage(getString(R.string.unable_to_check_backup_message, errorMessage))
            .setPositiveButton(R.string.retry) { dialog, _ ->
                dialog.dismiss()
                lifecycleScope.launch {
                    handleNewUser()
                }
            }
            .setNegativeButton(R.string.cancel) { dialog, _ ->
                dialog.dismiss()
            }
            .setCancelable(false)
            .show()
    }

    // ============================================================
    // STEP 2: DISPLAY SERIOUS PREVIOUS-DATA WARNING
    // ============================================================

    private fun showPreviousDataFoundWarningDialog() {
        AlertDialog.Builder(this)
            .setTitle(R.string.previous_data_found_title)
            .setMessage(R.string.previous_data_found_message)
            .setPositiveButton(R.string.continue_with_restore) { dialog, _ ->
                dialog.dismiss()
                lifecycleScope.launch {
                    restorePreviousData()
                }
            }
            .setNegativeButton(R.string.delete_backup_and_start_new) { dialog, _ ->
                dialog.dismiss()
                showFinalConfirmationPhraseDialog()
            }
            .setCancelable(false)
            .show()
    }

    // ============================================================
    // STEP 4: REQUIRE PHRASE CONFIRMATION
    // ============================================================

    private fun showFinalConfirmationPhraseDialog() {
        val dialogBinding = DialogFinalConfirmationBinding.inflate(layoutInflater)

        val dialog = AlertDialog.Builder(this)
            .setTitle(R.string.final_confirmation_title)
            .setView(dialogBinding.root)
            .setPositiveButton(R.string.confirm_and_start_new, null)
            .setNegativeButton(R.string.cancel) { d, _ -> d.dismiss() }
            .setCancelable(false)
            .create()

        dialog.setOnShowListener {
            val confirmButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            var isConfirming = false

            confirmButton.setOnClickListener {
                if (isConfirming) return@setOnClickListener

                val enteredPhrase = dialogBinding.phraseInput.text?.toString()?.trim().orEmpty()
                val requiredPhrase = "I am deleting my all data"

                if (enteredPhrase != requiredPhrase) {
                    dialogBinding.errorMessage.text = getString(R.string.phrase_mismatch_error)
                    dialogBinding.errorMessage.visibility = View.VISIBLE
                    return@setOnClickListener
                }

                // Phrase matches exactly
                dialogBinding.errorMessage.visibility = View.GONE
                isConfirming = true
                confirmButton.isEnabled = false

                dialog.dismiss()
                startNewUser(clearLocalData = true)
            }
        }

        dialog.show()
    }

    // ============================================================
    // START NEW USER
    // ============================================================

    private fun startNewUser(clearLocalData: Boolean = false) {

        lifecycleScope.launch {
            try {
                if (clearLocalData) {
                    val db = DatabaseProvider.getDatabase(applicationContext)

                    /*
                     * Clear local data only when the user
                     * explicitly chooses "Start new" after confirmation.
                     */
                    db.clearAllTables()
                }

                Toast.makeText(
                    this@FirstSetupActivity,
                    getString(R.string.started_new_user),
                    Toast.LENGTH_SHORT
                ).show()

            } catch (e: Exception) {
                Toast.makeText(
                    this@FirstSetupActivity,
                    getString(R.string.could_not_start_new_user, e.message ?: ""),
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
        if (AuthManager.isReauthRequired(this)) {
            AuthManager.clearReauthRequired(this)
        }

        getSharedPreferences(prefsName, MODE_PRIVATE)
            .edit()
            .putBoolean("setup_complete", true)
            .remove("is_logged_out")
            .apply()

        val intent = Intent(this, com.example.store.MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }
}