package com.example.store

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.View
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import com.example.store.backup.BackupActivity
import com.example.store.backup.ProfileActivity
import com.example.store.compliance.BusinessComplianceActivity

class SettingsActivity : BaseActivity() {

    private var activeLanguageCode: String = ""

    private val languageLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val newLang = LocaleHelper.getSavedLanguage(this)
            if (newLang != activeLanguageCode) {
                recreate()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activeLanguageCode = LocaleHelper.getSavedLanguage(this)
        setContentView(R.layout.activity_settings)

        findViewById<View>(R.id.language_item).setOnClickListener {
            val intent = Intent(this, LanguageSelectionActivity::class.java)
            intent.putExtra(LanguageSelectionActivity.EXTRA_IS_FROM_SETTINGS, true)
            languageLauncher.launch(intent)
        }

        findViewById<View>(R.id.profile_item).setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }

        findViewById<View>(R.id.backup_item).setOnClickListener {
            startActivity(Intent(this, BackupActivity::class.java))
        }

        findViewById<View>(R.id.past_data_item).setOnClickListener {
            startActivity(Intent(this, CalendarActivity::class.java))
        }

        findViewById<View>(R.id.business_compliance_item).setOnClickListener {
            startActivity(Intent(this, BusinessComplianceActivity::class.java))
        }

        findViewById<View>(R.id.feedback_item)?.setOnClickListener {
            showFeedbackDialog()
        }
    }

    private fun showFeedbackDialog() {
        val email = "meatmate.support@gmail.com"
        val message = getString(R.string.settings_feedback_desc) + "\n\n" + email
        val spannable = SpannableString(message)
        val emailStart = message.indexOf(email)
        val emailEnd = emailStart + email.length

        var dialog: AlertDialog? = null

        val clickableSpan = object : ClickableSpan() {
            override fun onClick(widget: View) {
                dialog?.dismiss()
                openFeedbackEmail()
            }
        }
        spannable.setSpan(clickableSpan, emailStart, emailEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

        val textView = TextView(this).apply {
            text = spannable
            setPadding(60, 40, 60, 20)
            textSize = 16f
            movementMethod = LinkMovementMethod.getInstance()
            setTextColor(ContextCompat.getColor(this@SettingsActivity, R.color.color_text_primary))
            setLinkTextColor(ContextCompat.getColor(this@SettingsActivity, R.color.color_primary))
        }

        dialog = AlertDialog.Builder(this)
            .setTitle(R.string.settings_feedback)
            .setView(textView)
            .setPositiveButton(R.string.send_email) { d, _ ->
                d.dismiss()
                openFeedbackEmail()
            }
            .setNegativeButton(R.string.close) { d, _ ->
                d.dismiss()
            }
            .create()

        dialog.show()
    }

    private fun openFeedbackEmail() {
        val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:meatmate.support@gmail.com")
            putExtra(Intent.EXTRA_EMAIL, arrayOf("meatmate.support@gmail.com"))
            putExtra(Intent.EXTRA_SUBJECT, "MeatMate Feedback / Suggestions")
        }
        try {
            startActivity(emailIntent)
        } catch (_: Exception) {
            try {
                val chooser = Intent.createChooser(emailIntent, getString(R.string.send_email))
                startActivity(chooser)
            } catch (_: Exception) {
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val currentLang = LocaleHelper.getSavedLanguage(this)
        if (activeLanguageCode.isNotEmpty() && currentLang != activeLanguageCode) {
            activeLanguageCode = currentLang
            recreate()
        }
    }
}
