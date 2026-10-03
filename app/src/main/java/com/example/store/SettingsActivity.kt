package com.example.store

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.example.store.backup.BackupActivity
import com.example.store.backup.ProfileActivity
import com.example.store.compliance.BusinessComplianceActivity

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        findViewById<View>(R.id.profile_item).setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }

        findViewById<View>(R.id.backup_item).setOnClickListener {
            startActivity(Intent(this, BackupActivity::class.java))
        }

        findViewById<View>(R.id.business_compliance_item).setOnClickListener {
            startActivity(Intent(this, BusinessComplianceActivity::class.java))
        }
    }
}
