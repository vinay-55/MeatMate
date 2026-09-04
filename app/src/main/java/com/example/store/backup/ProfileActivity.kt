package com.example.store.backup

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.store.R

class ProfileActivity : AppCompatActivity() {

    private lateinit var btnSignIn: Button
    private lateinit var tvStatus: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        btnSignIn = findViewById(R.id.bu_signin)
        tvStatus = findViewById(R.id.bu_status)

        AuthManager.init(this)

        btnSignIn.setOnClickListener {
            AuthManager.startSignIn()
        }

        updateStatus()
    }

    private fun updateStatus() {
        val account = AuthManager.getSignedInAccount()
        tvStatus.text = if (account != null) {
            "Signed in: ${account.email}"
        } else {
            "Not signed in"
        }
    }

    override fun onResume() {
        super.onResume()
        updateStatus()
    }
}
