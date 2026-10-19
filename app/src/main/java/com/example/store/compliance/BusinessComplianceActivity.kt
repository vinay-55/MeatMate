package com.example.store.compliance

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import com.example.store.LocaleHelper
import com.example.store.R

class BusinessComplianceActivity : AppCompatActivity() {

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.wrapEnglishContext(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_business_compliance)

        findViewById<android.widget.TextView>(R.id.complianceDisclaimerText)?.text =
            LocaleHelper.wrapContext(this).getString(R.string.compliance_ai_disclaimer)

        findViewById<Button>(R.id.addInformationButton).setOnClickListener {
            startActivity(Intent(this, DontLicenseActivity::class.java))
        }

        findViewById<Button>(R.id.fssaiButton).setOnClickListener {
            startActivity(Intent(this, FssaiActivity::class.java))
        }

        findViewById<Button>(R.id.licenseButton).setOnClickListener {
            handleLicenseClick()
        }

        findViewById<Button>(R.id.holidaysButton).setOnClickListener {
            // Holidays button intentionally unchanged.
        }
    }

    private fun handleLicenseClick() {
        val prefs = getSharedPreferences("profile_prefs", MODE_PRIVATE)
        val licenseStatus = prefs.getString("license_status", "")?.trim().orEmpty()

        when (licenseStatus) {
            "I have a license" -> {
                startActivity(Intent(this, HaveLicenseActivity::class.java))
            }

            "I don't have a license" -> {
                val requiredKeys = listOf(
                    "store_location",
                    "store_area",
                    "store_road",
                    "store_temple",
                    "store_school",
                    "khata_status",
                    "neighbor_noc",
                    "glazed_tiles",
                    "grease_trap",
                    "black_glass_screening"
                )

                val missingRequiredInfo = requiredKeys.any { key ->
                    prefs.getString(key, "")?.trim().isNullOrEmpty()
                }

                if (missingRequiredInfo) {
                    AlertDialog.Builder(this)
                        .setTitle("Information required")
                        .setMessage("Please complete all required information in Add Information before checking your license eligibility.")
                        .setPositiveButton("Go to Add Information") { _, _ ->
                            startActivity(Intent(this, DontLicenseActivity::class.java))
                        }
                        .show()
                    return
                }

                startActivity(Intent(this, HavemainlicenseActivity::class.java))
            }

            "I don't know" -> {
                AlertDialog.Builder(this)
                    .setTitle("License status unknown")
                    .setMessage("Your license status is unknown. This means the result could change depending on whether the business already has a valid license. Update the information in Add Information, then check the status again.")
                    .setPositiveButton("Go to Add Information") { _, _ ->
                        startActivity(Intent(this, DontLicenseActivity::class.java))
                    }
                    .show()
            }

            else -> {
                AlertDialog.Builder(this)
                    .setTitle("Information required")
                    .setMessage("Please select whether you currently have a valid license.")
                    .setPositiveButton("Go to Add Information") { _, _ ->
                        startActivity(Intent(this, DontLicenseActivity::class.java))
                    }
                    .show()
            }
        }
    }
}
