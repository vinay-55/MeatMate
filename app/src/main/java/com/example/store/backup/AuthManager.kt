package com.example.store.backup

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import com.google.android.gms.tasks.Task
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object AuthManager {

    private const val TAG = "AuthManager"

    private const val PREFS_NAME = "backup_prefs"
    private const val SELECTED_ACCOUNT = "selected_account_email"

    private var initialized = false

    private var activity: ComponentActivity? = null

    private lateinit var signInLauncher: ActivityResultLauncher<Intent>
    private lateinit var googleClient: GoogleSignInClient

    private var lastAccount: GoogleSignInAccount? = null

    /*
     * Application context is important because
     * BackupWorker can run when no Activity is open.
     */
    private var appContext: Context? = null


    // ============================================================
    // INITIALIZATION
    // ============================================================

    fun init(activity: ComponentActivity) {

        this.activity = activity
        this.appContext = activity.applicationContext

        Log.i(
            TAG,
            "AuthManager.init: ${activity::class.java.simpleName}"
        )

        // Google Drive App Data scope
        val driveAppDataScope =
            Scope(
                "https://www.googleapis.com/auth/drive.appdata"
            )

        val gso =
            GoogleSignInOptions.Builder(
                GoogleSignInOptions.DEFAULT_SIGN_IN
            )
                .requestEmail()
                .requestScopes(driveAppDataScope)
                .build()

        googleClient =
            GoogleSignIn.getClient(
                activity,
                gso
            )

        /*
         * Register Google Sign-In result launcher.
         */
        signInLauncher =
            activity.registerForActivityResult(
                StartActivityForResult()
            ) { result ->

                Log.i(
                    TAG,
                    "Sign-in result received: ${result.resultCode}"
                )

                try {

                    val intentData = result.data

                    if (intentData == null) {

                        Log.w(
                            TAG,
                            "Sign-in returned null intent data"
                        )

                        return@registerForActivityResult
                    }

                    val task:
                            Task<GoogleSignInAccount> =
                        GoogleSignIn
                            .getSignedInAccountFromIntent(
                                intentData
                            )

                    try {

                        val account =
                            task.getResult(
                                ApiException::class.java
                            )

                        if (account != null) {

                            lastAccount = account

                            saveAccountEmail(
                                account.email
                            )

                            Log.i(
                                TAG,
                                "Google sign-in successful: ${account.email}"
                            )

                            /*
                             * IMPORTANT:
                             *
                             * Do NOT call BackupActivity.updateStatus()
                             * from here.
                             *
                             * AuthManager is responsible only for
                             * authentication.
                             */

                            return@registerForActivityResult
                        }

                    } catch (e: ApiException) {

                        Log.w(
                            TAG,
                            "Google sign-in failed: " +
                                    "${e.statusCode} ${e.message}"
                        )
                    }

                    /*
                     * Fallback to the last Google account.
                     */
                    val currentActivity =
                        activity

                    if (currentActivity != null) {

                        val last =
                            GoogleSignIn
                                .getLastSignedInAccount(
                                    currentActivity
                                )

                        if (last != null) {

                            lastAccount = last

                            saveAccountEmail(
                                last.email
                            )

                            Log.i(
                                TAG,
                                "Fallback Google account: ${last.email}"
                            )
                        }
                    }

                } catch (e: Exception) {

                    Log.e(
                        TAG,
                        "Error processing Google sign-in result",
                        e
                    )
                }
            }


        /*
         * Load already signed-in Google account.
         */
        lastAccount =
            GoogleSignIn
                .getLastSignedInAccount(activity)

        if (lastAccount != null) {

            saveAccountEmail(
                lastAccount?.email
            )

            Log.i(
                TAG,
                "Existing Google account: ${lastAccount?.email}"
            )
        }

        initialized = true

        Log.i(
            TAG,
            "AuthManager initialized"
        )
    }


    // ============================================================
    // SAVE SELECTED ACCOUNT
    // ============================================================

    private fun saveAccountEmail(
        email: String?
    ) {

        if (email.isNullOrEmpty()) {
            return
        }

        val context =
            appContext
                ?: return

        context
            .getSharedPreferences(
                PREFS_NAME,
                Context.MODE_PRIVATE
            )
            .edit()
            .putString(
                SELECTED_ACCOUNT,
                email
            )
            .apply()

        Log.i(
            TAG,
            "Saved account email: $email"
        )
    }


    // ============================================================
    // GET SAVED ACCOUNT EMAIL
    // ============================================================

    fun getSelectedAccountEmail(): String? {

        val context =
            appContext
                ?: activity
                ?: return null

        return context
            .getSharedPreferences(
                PREFS_NAME,
                Context.MODE_PRIVATE
            )
            .getString(
                SELECTED_ACCOUNT,
                null
            )
    }


    // ============================================================
    // CHECK SIGN-IN
    // ============================================================

    fun isSignedIn(): Boolean {

        val context =
            appContext
                ?: activity
                ?: return false

        return GoogleSignIn
            .getLastSignedInAccount(context) != null
    }


    // ============================================================
    // START GOOGLE SIGN-IN
    // ============================================================

    fun startSignIn() {

        try {

            if (!initialized) {

                Log.e(
                    TAG,
                    "AuthManager is not initialized"
                )

                return
            }

            if (!::signInLauncher.isInitialized) {

                Log.e(
                    TAG,
                    "Sign-in launcher is not initialized"
                )

                return
            }

            if (!::googleClient.isInitialized) {

                Log.e(
                    TAG,
                    "GoogleSignInClient is not initialized"
                )

                return
            }

            val intent =
                googleClient.signInIntent

            signInLauncher.launch(intent)

            Log.i(
                TAG,
                "Google sign-in started"
            )

        } catch (e: Exception) {

            Log.e(
                TAG,
                "Failed to start Google sign-in",
                e
            )
        }
    }


    // ============================================================
    // GET SIGNED-IN ACCOUNT
    // ============================================================

    fun getSignedInAccount():
            GoogleSignInAccount? {

        val context =
            appContext
                ?: activity
                ?: return lastAccount

        val account =
            GoogleSignIn
                .getLastSignedInAccount(
                    context
                )

        if (account != null) {

            lastAccount = account

            return account
        }

        return lastAccount
    }


    // ============================================================
    // GET GOOGLE ACCESS TOKEN
    // ============================================================

    /*
     * This function is safe to call from BackupWorker.
     *
     * BackupWorker does NOT need an Activity.
     */
    suspend fun getAccessToken():
            String? {

        return withContext(
            Dispatchers.IO
        ) {

            try {

                val context =
                    appContext
                        ?: activity?.applicationContext
                        ?: return@withContext null

                /*
                 * Get currently signed-in Google account.
                 */
                val account =
                    GoogleSignIn
                        .getLastSignedInAccount(
                            context
                        )
                        ?: lastAccount
                        ?: return@withContext null

                val email =
                    account.email
                        ?: return@withContext null

                /*
                 * Drive App Data OAuth scope.
                 */
                val scope =
                    "oauth2:https://www.googleapis.com/auth/drive.appdata"

                /*
                 * Get access token.
                 */
                val token =
                    com.google.android.gms.auth.GoogleAuthUtil
                        .getToken(
                            context,
                            email,
                            scope
                        )

                Log.i(
                    TAG,
                    "Access token obtained for $email"
                )

                token

            } catch (e: Exception) {

                Log.e(
                    TAG,
                    "Failed to obtain Google access token",
                    e
                )

                null
            }
        }
    }


    // ============================================================
    // SIGN OUT
    // ============================================================

    fun signOut() {

        try {

            if (::googleClient.isInitialized) {

                googleClient
                    .signOut()
                    .addOnCompleteListener {

                        Log.i(
                            TAG,
                            "Google sign-out completed"
                        )
                    }
            }

            lastAccount = null

            val context =
                appContext
                    ?: activity
                    ?: return

            context
                .getSharedPreferences(
                    PREFS_NAME,
                    Context.MODE_PRIVATE
                )
                .edit()
                .remove(
                    SELECTED_ACCOUNT
                )
                .apply()

            Log.i(
                TAG,
                "Local account information cleared"

            )

        } catch (e: Exception) {

            Log.w(
                TAG,
                "Sign-out failed",
                e
            )
        }
    }
}