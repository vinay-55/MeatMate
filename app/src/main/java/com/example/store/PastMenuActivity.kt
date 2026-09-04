package com.example.store

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.store.data.local.BusinessDao
import com.example.store.databinding.ActivityPastMenuBinding
import com.example.store.data.local.DatabaseProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PastMenuActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPastMenuBinding

    private lateinit var dao: BusinessDao

    private lateinit var selectedDate: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityPastMenuBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Get date from CalendarActivity
        selectedDate = intent.getStringExtra("DATE") ?: ""

        // Show selected date
        binding.date.text = selectedDate

        // Get Room database
        val database = DatabaseProvider.getDatabase(this)
        dao = database.businessDao()

        // prod1
        binding.pastProd1.setOnClickListener {
            openProduct("prod1")
        }

        // prod2
        binding.pastProd2.setOnClickListener {
            openProduct("prod2")
        }

        // prod3
        binding.pastProd3.setOnClickListener {
            openProduct("prod3")
        }
    }

    private fun openProduct(product: String) {

        lifecycleScope.launch(Dispatchers.IO) {

            val hasData = if (product == "prod3") {

                val records1 =
                    dao.getBusinessByDateAndProduct(
                        date = selectedDate,
                        product = "prod3.1"
                    )

                val records2 =
                    dao.getBusinessByDateAndProduct(
                        date = selectedDate,
                        product = "prod3.2"
                    )

                records1.isNotEmpty() ||
                        records2.isNotEmpty()

            } else {

                val records =
                    dao.getBusinessByDateAndProduct(
                        date = selectedDate,
                        product = product
                    )

                records.isNotEmpty()
            }

            withContext(Dispatchers.Main) {

                if (!hasData) {

                    Toast.makeText(
                        this@PastMenuActivity,
                        "No data for $product on $selectedDate",
                        Toast.LENGTH_SHORT
                    ).show()

                } else {

                    val intent =
                        Intent(
                            this@PastMenuActivity,
                            PastBusinessActivity::class.java
                        )

                    intent.putExtra(
                        "DATE",
                        selectedDate
                    )

                    intent.putExtra(
                        "PRODUCT",
                        product
                    )

                    startActivity(intent)
                }
            }
        }
    }
}