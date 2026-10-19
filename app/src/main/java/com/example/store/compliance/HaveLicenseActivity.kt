package com.example.store.compliance

import android.content.Context
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.store.LocaleHelper
import com.example.store.R

class HaveLicenseActivity : AppCompatActivity() {

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.wrapEnglishContext(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_have_license)

        findViewById<TextView>(R.id.complianceDisclaimerText)?.text =
            LocaleHelper.wrapContext(this).getString(R.string.compliance_ai_disclaimer)

        val detailsText = findViewById<TextView>(R.id.licenseDetailsText)
        detailsText.text = """
            Current license
            - Your record indicates that you currently have a valid license.

            License information
            - Review the current license details and ensure they remain valid for the business activity and location.

            License renewal
            - Check the renewal date and any upcoming expiry notice before the license lapses.

            How to renew the license
            - Submit the required renewal forms and supporting documents to the relevant local authority or portal.

            Renewal-related guidance
            - Keep the application, receipt, and any correspondence for proof and future inspection.
        """.trimIndent()
    }
}
