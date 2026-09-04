package com.example.store

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.store.databinding.ActivityMainBinding
import java.util.Date
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.example.store.backup.BackupManagerImpl as BackupManager
import com.example.store.backup.AuthManager

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize BackupManager (auth + workmanager)
        // Initialize BackupManager (auth helper)
        BackupManager.init(this as ComponentActivity)
        AuthManager.init(this as ComponentActivity)

        // First-setup logic: show FirstSetupActivity if setup not complete and either no local data (fresh install)
        val prefs = getSharedPreferences("backup_prefs", MODE_PRIVATE)
        val setupComplete = prefs.getBoolean("setup_complete", false)
        if (!setupComplete) {
            // check local database for existing records
            lifecycleScope.launchWhenStarted {
                val db = com.example.store.data.local.DatabaseProvider.getDatabase(applicationContext)
                val existing = db.businessDao().getAllBusiness()
                if (existing.isEmpty()) {
                    // fresh install: show FirstSetupActivity
                    startActivity(Intent(this@MainActivity, com.example.store.backup.FirstSetupActivity::class.java))
                } else {
                    // existing user updating the app: offer to protect data (one-time)
                    androidx.appcompat.app.AlertDialog.Builder(this@MainActivity)
                        .setTitle("Protect your existing business data")
                        .setMessage("Sign in with Google to back up your current local data to the cloud. Your local data will not be changed.")
                        .setPositiveButton("Sign in & Back up") { _, _ ->
                            // launch sign-in. After sign-in, user can use BackupActivity to back up now.
                            AuthManager.startSignIn()
                        }
                        .setNegativeButton("Maybe later") { _, _ ->
                            // mark setup complete and continue
                            prefs.edit().putBoolean("setup_complete", true).apply()
                        }
                        .show()
                }
            }
        }

        binding.prod1.setOnClickListener {
            val intent = Intent(this, BusinessActivity::class.java)
            intent.putExtra("PRODUCT", "prod1")
            startActivity(intent)
        }

        binding.prod2.setOnClickListener {
            val intent = Intent(this, BusinessActivity::class.java)
            intent.putExtra("PRODUCT", "prod2")
            startActivity(intent)
        }

        binding.prod3.setOnClickListener {
            val intent = Intent(this, BusinessActivity::class.java)
            intent.putExtra("PRODUCT", "prod3")
            startActivity(intent)
        }
        binding.past.setOnClickListener {
            startActivity(Intent(this, CalendarActivity::class.java))
        }

        // Settings button opens Settings screen
        binding.settingsButton.setOnClickListener {
            startActivity(Intent(this, com.example.store.SettingsActivity::class.java))
        }
    }

    override fun onCreateOptionsMenu(menu: android.view.Menu?): Boolean {
        menuInflater.inflate(com.example.store.R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: android.view.MenuItem): Boolean {
        return when (item.itemId) {
            com.example.store.R.id.action_backup -> {
                // Open Backup & Restore screen
                startActivity(Intent(this, com.example.store.backup.BackupActivity::class.java))
                true
            }
            com.example.store.R.id.action_restore -> {
                // Also open Backup screen (restore is available there)
                startActivity(Intent(this, com.example.store.backup.BackupActivity::class.java))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}