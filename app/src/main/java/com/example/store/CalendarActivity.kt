package com.example.store

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.store.databinding.ActivityCalendarBinding
import com.example.store.data.local.DatabaseProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CalendarActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCalendarBinding

    private lateinit var dao: com.example.store.data.local.BusinessDao

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityCalendarBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Database
        val database = DatabaseProvider.getDatabase(this)
        dao = database.businessDao()

        // Calendar date selection
        binding.calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->

            val actualMonth = month + 1

            val selectedDate = String.format(
                "%04d-%02d-%02d",
                year,
                actualMonth,
                dayOfMonth
            )

            checkDate(selectedDate)
        }
    }

    private fun checkDate(selectedDate: String) {

        lifecycleScope.launch(Dispatchers.IO) {

            val records = dao.getBusinessByDate(
                selectedDate
            )

            withContext(Dispatchers.Main) {

                if (records.isNotEmpty()) {

                    // Data exists → open PastMenuActivity

                    val intent = Intent(
                        this@CalendarActivity,
                        PastMenuActivity::class.java
                    )

                    intent.putExtra(
                        "DATE",
                        selectedDate
                    )

                    startActivity(intent)

                } else {

                    // No data
                    Toast.makeText(
                        this@CalendarActivity,
                        "No data for $selectedDate",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }
}