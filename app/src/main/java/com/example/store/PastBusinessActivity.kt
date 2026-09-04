package com.example.store

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.store.data.local.BusinessDao
import com.example.store.data.local.BusinessEntity
import com.example.store.databinding.ActivityPastBusinessBinding
import com.example.store.databinding.ItemPastBusinessRowBinding
import com.example.store.data.local.DatabaseProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class PastBusinessActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPastBusinessBinding
    private lateinit var dao: BusinessDao

    private lateinit var selectedDate: String
    private lateinit var selectedProduct: String

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding =
            ActivityPastBusinessBinding.inflate(layoutInflater)

        setContentView(binding.root)

        selectedDate =
            intent.getStringExtra("DATE") ?: ""

        selectedProduct =
            intent.getStringExtra("PRODUCT") ?: ""

        binding.date.text =
            selectedDate

        try {

            val date =
                LocalDate.parse(
                    selectedDate,
                    DateTimeFormatter.ofPattern(
                        "yyyy-MM-dd"
                    )
                )

            binding.day.text =
                date.format(
                    DateTimeFormatter.ofPattern(
                        "EEEE"
                    )
                )

        } catch (e: Exception) {

            binding.day.text = ""
        }

        when (selectedProduct) {

            "prod1" -> {

                binding.product.text =
                    "Broiler (ಬ್ರಾಯ್ಲರ್)"

                setupSingleScroll()
            }

            "prod2" -> {

                binding.product.text =
                    "Farm (ಮೊಟ್ಟೆ ಕೋಳಿ)"

                setupSingleScroll()
            }

            "prod3" -> {

                binding.product.text =
                    "Ready Chicken"

                setupProd3Scrolls()
            }

            else -> {

                binding.product.text = ""
            }
        }

        val database =
            DatabaseProvider.getDatabase(this)

        dao =
            database.businessDao()

        loadData()
    }

    private fun setupSingleScroll() {

        binding.scrollView2.visibility =
            View.VISIBLE

        binding.scrollView3.visibility =
            View.GONE

        binding.totalWeight.visibility =
            View.VISIBLE

        binding.totalCash.visibility =
            View.VISIBLE

        binding.totalWeight2.visibility =
            View.GONE

        binding.totalCash2.visibility =
            View.GONE
    }

    private fun setupProd3Scrolls() {

        binding.scrollView2.visibility =
            View.VISIBLE

        binding.scrollView3.visibility =
            View.VISIBLE

        binding.totalWeight.visibility =
            View.VISIBLE

        binding.totalCash.visibility =
            View.GONE

        binding.totalWeight2.visibility =
            View.VISIBLE

        binding.totalCash2.visibility =
            View.VISIBLE

        binding.scrollView2.layoutParams =
            binding.scrollView2.layoutParams.apply {
                height = 0
            }

        binding.scrollView3.layoutParams =
            binding.scrollView3.layoutParams.apply {
                height = 0
            }

        val params1 =
            binding.scrollView2.layoutParams
                    as LinearLayout.LayoutParams

        params1.weight = 1f

        binding.scrollView2.layoutParams =
            params1

        val params2 =
            binding.scrollView3.layoutParams
                    as LinearLayout.LayoutParams

        params2.weight = 1f

        binding.scrollView3.layoutParams =
            params2
    }

    private fun loadData() {

        lifecycleScope.launch(
            Dispatchers.IO
        ) {

            if (selectedProduct == "prod3") {

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

                withContext(
                    Dispatchers.Main
                ) {

                    binding.rowContainer
                        .removeAllViews()

                    binding.rowContainer2
                        .removeAllViews()

                    if (records2.isNotEmpty()) {

                        binding.price.text =
                            "Price: ${
                                String.format(
                                    "%.2f",
                                    records2.last().price
                                )
                            }"

                    } else if (
                        records1.isNotEmpty()
                    ) {

                        binding.price.text =
                            "Price: ${
                                String.format(
                                    "%.2f",
                                    records1.last().price
                                )
                            }"

                    } else {

                        binding.price.text =
                            "No data"
                    }

                    for (record in records1) {

                        addRow(
                            record,
                            1,
                            "prod3.1"
                        )
                    }

                    for (record in records2) {

                        addRow(
                            record,
                            2,
                            "prod3.2"
                        )
                    }

                    updateSummary(
                        records1,
                        1
                    )

                    updateSummary(
                        records2,
                        2
                    )

                    scrollToBottom(1)

                    scrollToBottom(2)
                }

            } else {

                val records =
                    dao.getBusinessByDateAndProduct(
                        date = selectedDate,
                        product = selectedProduct
                    )

                withContext(
                    Dispatchers.Main
                ) {

                    binding.rowContainer
                        .removeAllViews()

                    for (record in records) {

                        addRow(
                            record,
                            1,
                            selectedProduct
                        )
                    }

                    if (records.isNotEmpty()) {

                        binding.price.text =
                            "Price: ${
                                String.format(
                                    "%.2f",
                                    records.last().price
                                )
                            }"

                    } else {

                        binding.price.text =
                            "No data"
                    }

                    updateSummary(
                        records,
                        1
                    )

                    scrollToBottom(1)
                }
            }
        }
    }

    private fun addRow(
        record: BusinessEntity,
        target: Int,
        databaseProduct: String
    ) {

        val container =
            if (target == 1) {
                binding.rowContainer
            } else {
                binding.rowContainer2
            }

        val rowBinding =
            ItemPastBusinessRowBinding.inflate(
                LayoutInflater.from(this),
                container,
                false
            )

        container.addView(
            rowBinding.root
        )

        rowBinding.slNo.text =
            record.slno?.toString() ?: ""

        rowBinding.weight.text =
            String.format(
                "%.2f",
                record.weight
            )

        rowBinding.price.text =
            String.format(
                "%.2f",
                record.price
            )

        if (
            databaseProduct == "prod3.1"
        ) {

            rowBinding.price.text =
                ""
            rowBinding.totalprice.text=""

        } else {

            rowBinding.totalprice.text =
                String.format(
                    "%.2f",
                    record.total
                )
            rowBinding.price.text=String.format(
                "%.2f",
                record.price
            )
        }

        rowBinding.more.setOnClickListener {
        }
    }

    private fun updateSummary(
        records: List<BusinessEntity>,
        target: Int
    ) {

        var totalWeight = 0.0
        var totalCash = 0.0

        for (record in records) {

            totalWeight +=
                record.weight

            totalCash +=
                record.total
        }

        if (target == 1) {

            binding.totalWeight.text =
                "Total Weight: ${
                    String.format(
                        "%.2f",
                        totalWeight
                    )
                }"

            if (selectedProduct != "prod3") {

                binding.totalCash.text =
                    "Total Cash: ${
                        String.format(
                            "%.2f",
                            totalCash
                        )
                    }"
            }

        } else {

            binding.totalWeight2.text =
                "Total Weight 2: ${
                    String.format(
                        "%.2f",
                        totalWeight
                    )
                }"

            binding.totalCash2.text =
                "Total Cash 2: ${
                    String.format(
                        "%.2f",
                        totalCash
                    )
                }"
        }
    }

    private fun scrollToBottom(
        target: Int
    ) {

        val scrollView =
            if (target == 1) {
                binding.scrollView2
            } else {
                binding.scrollView3
            }

        scrollView.post {

            scrollView.fullScroll(
                View.FOCUS_DOWN
            )
        }
    }
}