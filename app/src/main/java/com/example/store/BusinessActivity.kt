package com.example.store
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.store.data.local.BusinessDao
import com.example.store.data.local.BusinessEntity
import com.example.store.databinding.ActivityBusinessBinding
import com.example.store.databinding.ItemBusinessRowBinding
import com.example.store.data.local.DatabaseProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter

class BusinessActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBusinessBinding
    private lateinit var dao: BusinessDao
    private lateinit var product: String

    private val rowIds =
        mutableMapOf<ItemBusinessRowBinding, Int>()

    private val rowProducts =
        mutableMapOf<ItemBusinessRowBinding, String>()

    private val midnightHandler =
        Handler(Looper.getMainLooper())

    private val midnightRunnable =
        object : Runnable {

            @RequiresApi(Build.VERSION_CODES.O)
            override fun run() {
                changeToNewDay()
                scheduleNextMidnight()
            }
        }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding =
            ActivityBusinessBinding.inflate(layoutInflater)

        setContentView(binding.root)

        product =
            intent.getStringExtra("PRODUCT") ?: ""

        when (product) {

            "prod1" -> {
                binding.productName.text =
                    "Broiler (ಬ್ರಾಯ್ಲರ್)"
            }

            "prod2" -> {
                binding.productName.text =
                    "Farm (ಮೊಟ್ಟೆ ಕೋಳಿ)"
            }

            "prod3" -> {
                binding.productName.text =
                    "Ready Chicken"

                binding.totalWeight2.visibility =
                    View.VISIBLE
            }

            else -> {
                binding.productName.text = ""
            }
        }

        if (product == "prod3") {

            binding.scrollView3.visibility =
                View.VISIBLE
        }

        binding.totalCash.visibility =
            if (product == "prod1" ||
                product == "prod2"
            ) {
                View.VISIBLE
            } else {
                View.GONE
            }

        binding.totalCash2.visibility =
            if (product == "prod3") {
                View.VISIBLE
            } else {
                View.GONE
            }

        val database =
            DatabaseProvider.getDatabase(this)

        dao =
            database.businessDao()

        setTodayDate()


        loadSavedData()

        scheduleNextMidnight()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun setTodayDate() {

        val today =
            LocalDate.now()

        binding.date.text =
            today.format(
                DateTimeFormatter.ofPattern(
                    "yyyy-MM-dd"
                )
            )

        binding.day.text =
            today.format(
                DateTimeFormatter.ofPattern(
                    "EEEE"
                )
            )
    }



    private fun loadSavedData() {

        lifecycleScope.launch(
            Dispatchers.IO
        ) {

            val date =
                binding.date.text.toString()

            if (product == "prod3") {

                val records1 =
                    dao.getBusinessByDateAndProduct(
                        date = date,
                        product = "prod3.1"
                    )

                val records2 =
                    dao.getBusinessByDateAndProduct(
                        date = date,
                        product = "prod3.2"
                    )

                withContext(
                    Dispatchers.Main
                ) {

                    if (records2.isNotEmpty()) {

                        binding.price.setText(
                            records2.last().price.toString()
                        )

                    } else if (
                        records1.isNotEmpty()
                    ) {

                        binding.price.setText(
                            records1.last().price.toString()
                        )
                    }

                    if (records1.isEmpty()) {

                        addNewRow(
                            1,
                            "prod3.1"
                        )

                    } else {

                        for (record in records1) {

                            addSavedRow(
                                record,
                                1,
                                "prod3.1"
                            )
                        }
                    }

                    if (records2.isEmpty()) {

                        addNewRow(
                            2,
                            "prod3.2"
                        )

                    } else {

                        for (record in records2) {

                            addSavedRow(
                                record,
                                2,
                                "prod3.2"
                            )
                        }
                    }

                    updateTotalWeightAndCash(1)
                    updateTotalWeightAndCash(2)

                    scrollToBottom(1)
                    scrollToBottom(2)
                }

            } else {

                val records =
                    dao.getBusinessByDateAndProduct(
                        date = date,
                        product = product
                    )

                withContext(
                    Dispatchers.Main
                ) {

                    if (records.isEmpty()) {

                        addNewRow(
                            1,
                            product
                        )

                    } else {

                        binding.price.setText(
                            records.last().price.toString()
                        )

                        for (record in records) {

                            addSavedRow(
                                record,
                                1,
                                product
                            )
                        }
                    }

                    updateTotalWeightAndCash(1)

                    scrollToBottom(1)
                }
            }
        }
    }

    private fun updateTotalWeightAndCash(
        target: Int
    ) {

        val container =
            if (target == 1) {
                binding.rowContainer
            } else {
                binding.rowContainer2
            }

        var totalWeight = 0.0
        var totalCash = 0.0

        for (i in 0 until container.childCount) {

            val rowView =
                container.getChildAt(i)

            val rowBinding =
                rowIds.keys.firstOrNull {
                    it.root == rowView
                }

            if (rowBinding != null) {

                val weight =
                    rowBinding.weight.text
                        .toString()
                        .toDoubleOrNull()

                if (weight != null) {

                    totalWeight += weight
                }

                val rowTotal =
                    rowBinding.totalprice.text
                        .toString()
                        .toDoubleOrNull()

                if (rowTotal != null) {

                    totalCash += rowTotal
                }
            }
        }

        if (target == 1) {

            binding.totalWeight.text =
                "Total Weight: ${
                    String.format(
                        "%.2f",
                        totalWeight
                    )
                }"

            if (product != "prod3") {

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

    @RequiresApi(Build.VERSION_CODES.O)
    private fun scheduleNextMidnight() {

        val now =
            LocalDateTime.now()

        val tomorrow =
            now.toLocalDate()
                .plusDays(1)

        val nextMidnight =
            LocalDateTime.of(
                tomorrow,
                LocalTime.MIDNIGHT
            )

        val delay =
            Duration.between(
                now,
                nextMidnight
            ).toMillis()

        midnightHandler.postDelayed(
            midnightRunnable,
            delay
        )
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun changeToNewDay() {

        setTodayDate()

        binding.price.text.clear()

        binding.totalWeight.text =
            "Total Weight: 0.00"

        binding.totalCash.text =
            "Total Cash: 0.00"

        binding.totalWeight2.text =
            "Total Weight 2: 0.00"

        binding.totalCash2.text =
            "Total Cash 2: 0.00"

        binding.rowContainer
            .removeAllViews()

        binding.rowContainer2
            .removeAllViews()

        rowIds.clear()

        rowProducts.clear()

        if (product == "prod3") {

            addNewRow(
                1,
                "prod3.1"
            )

            addNewRow(
                2,
                "prod3.2"
            )

        } else {

            addNewRow(
                1,
                product
            )
        }
    }

    private fun addNewRow(
        target: Int,
        databaseProduct: String
    ) {

        val container =
            if (target == 1) {
                binding.rowContainer
            } else {
                binding.rowContainer2
            }

        val lastRow =
            getLastRow(
                container
            )

        lastRow?.plus?.visibility =
            View.GONE

        val rowBinding =
            ItemBusinessRowBinding.inflate(
                LayoutInflater.from(this),
                container,
                false
            )

        container.addView(
            rowBinding.root
        )

        rowIds[rowBinding] = 0

        rowProducts[rowBinding] =
            databaseProduct

        rowBinding.plus.visibility =
            View.VISIBLE

        setupRow(
            rowBinding
        )

        updateTotalWeightAndCash(
            target
        )
    }

    private fun addSavedRow(
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

        val lastRow =
            getLastRow(
                container
            )

        lastRow?.plus?.visibility =
            View.GONE

        val rowBinding =
            ItemBusinessRowBinding.inflate(
                LayoutInflater.from(this),
                container,
                false
            )

        container.addView(
            rowBinding.root
        )

        rowIds[rowBinding] =
            record.id

        rowProducts[rowBinding] =
            databaseProduct

        rowBinding.slNo.setText(
            record.slno?.toString() ?: ""
        )

        rowBinding.weight.setText(
            record.weight.toString()
        )

        if (
            databaseProduct ==
            "prod3.1"
        ) {

            rowBinding.totalprice.text =
                ""

        } else {

            rowBinding.totalprice.text =
                String.format(
                    "%.2f",
                    record.total
                )
        }

        rowBinding.plus.visibility =
            View.VISIBLE

        setupRow(
            rowBinding
        )
    }

    private fun getLastRow(
        container: android.widget.LinearLayout
    ): ItemBusinessRowBinding? {

        if (
            container.childCount == 0
        ) {
            return null
        }

        val lastView =
            container.getChildAt(
                container.childCount - 1
            )

        return rowIds.keys.firstOrNull {
            it.root == lastView
        }
    }

    private fun deleteRow(
        rowBinding: ItemBusinessRowBinding
    ) {

        val databaseId =
            rowIds[rowBinding] ?: 0

        val target =
            if (
                binding.rowContainer
                    .indexOfChild(
                        rowBinding.root
                    ) != -1
            ) {
                1
            } else {
                2
            }

        lifecycleScope.launch(
            Dispatchers.IO
        ) {

            if (databaseId != 0) {

                dao.deleteBusiness(
                    databaseId
                )
            // schedule backup after deletion
            //com.example.store.backup.BackupManager.requestBackup(applicationContext)
            }

            withContext(
            Dispatchers.Main
            ) {

                rowIds.remove(
                    rowBinding
                )

                rowProducts.remove(
                    rowBinding
                )

                if (
                    binding.rowContainer
                        .indexOfChild(
                            rowBinding.root
                        ) != -1
                ) {

                    binding.rowContainer
                        .removeView(
                            rowBinding.root
                        )
                }

                if (
                    binding.rowContainer2
                        .indexOfChild(
                            rowBinding.root
                        ) != -1
                ) {

                    binding.rowContainer2
                        .removeView(
                            rowBinding.root
                        )
                }

                if (
                    product == "prod3" &&
                    target == 2
                ) {

                    if (
                        binding.rowContainer2
                            .childCount == 0
                    ) {

                        addNewRow(
                            2,
                            "prod3.2"
                        )
                    }

                } else {

                    if (
                        binding.rowContainer
                            .childCount == 0
                    ) {

                        addNewRow(
                            1,
                            product
                        )
                    }
                }

                updateTotalWeightAndCash(
                    target
                )
            }
        }
    }

    private fun showDeleteConfirmation(
        rowBinding: ItemBusinessRowBinding
    ) {

        AlertDialog.Builder(this)
            .setTitle("Delete row?")
            .setMessage(
                "Are you sure you want to delete this row?"
            )
            .setPositiveButton("Yes") { _, _ ->

                deleteRow(
                    rowBinding
                )
            }
            .setNegativeButton(
                "No",
                null
            )
            .show()
    }

    private fun showRowMenu(
        rowBinding: ItemBusinessRowBinding
    ) {

        val popupMenu =
            android.widget.PopupMenu(
                this,
                rowBinding.more
            )

        popupMenu.menu.add(
            "Delete"
        )

        popupMenu.setOnMenuItemClickListener {

            if (
                it.title == "Delete"
            ) {

                showDeleteConfirmation(
                    rowBinding
                )
            }

            true
        }

        popupMenu.show()
    }
    private fun scrollToRow(
        rowBinding: ItemBusinessRowBinding
    ) {

        val scrollView =
            if (
                binding.rowContainer
                    .indexOfChild(
                        rowBinding.root
                    ) != -1
            ) {
                binding.scrollView2
            } else {
                binding.scrollView3
            }

        scrollView.postDelayed({

            val insets =
                ViewCompat.getRootWindowInsets(
                    binding.root
                )

            val imeInsets =
                insets?.getInsets(
                    WindowInsetsCompat.Type.ime()
                )

            val keyboardHeight =
                imeInsets?.bottom ?: 0

            if (keyboardHeight == 0) {
                return@postDelayed
            }

            val rowLocation =
                IntArray(2)

            rowBinding.root
                .getLocationOnScreen(
                    rowLocation
                )

            val rowBottom =
                rowLocation[1] +
                        rowBinding.root.height

            val rootLocation =
                IntArray(2)

            binding.root
                .getLocationOnScreen(
                    rootLocation
                )

            val keyboardTop =
                rootLocation[1] +
                        binding.root.height -
                        keyboardHeight

            val margin =
                (
                        24 *
                                resources.displayMetrics.density
                        ).toInt()

            val allowedBottom =
                keyboardTop - margin

            if (
                rowBottom >
                allowedBottom
            ) {

                scrollView.smoothScrollBy(
                    0,
                    rowBottom -
                            allowedBottom
                )
            }

        }, 300)
    }

    private fun setupRow(
        rowBinding: ItemBusinessRowBinding
    ) {

        rowBinding.plus.setOnClickListener {

            val databaseProduct =
                rowProducts[rowBinding]
                    ?: product

            val target =
                if (
                    binding.rowContainer
                        .indexOfChild(
                            rowBinding.root
                        ) != -1
                ) {
                    1
                } else {
                    2
                }

            addNewRow(
                target,
                databaseProduct
            )

            scrollToBottom(
                target
            )
        }

        rowBinding.more.setOnClickListener {

            showRowMenu(
                rowBinding
            )
        }

        rowBinding.slNo.setOnFocusChangeListener {
                _,
                hasFocus ->

            if (hasFocus) {

                scrollToRow(
                    rowBinding
                )
            }
        }

        rowBinding.weight.setOnFocusChangeListener {
                _,
                hasFocus ->

            if (hasFocus) {

                scrollToRow(
                    rowBinding
                )
            }
        }

        rowBinding.slNo.addTextChangedListener(
            object : TextWatcher {

                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) {
                }

                override fun onTextChanged(
                    s: CharSequence?,
                    start: Int,
                    before: Int,
                    count: Int
                ) {

                    saveCurrentRow(
                        rowBinding
                    )
                }

                override fun afterTextChanged(
                    s: Editable?
                ) {
                }
            }
        )

        rowBinding.weight.addTextChangedListener(
            object : TextWatcher {

                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) {
                }

                override fun onTextChanged(
                    s: CharSequence?,
                    start: Int,
                    before: Int,
                    count: Int
                ) {

                    saveCurrentRow(
                        rowBinding
                    )

                    val target =
                        if (
                            binding.rowContainer
                                .indexOfChild(
                                    rowBinding.root
                                ) != -1
                        ) {
                            1
                        } else {
                            2
                        }

                    updateTotalWeightAndCash(
                        target
                    )
                }

                override fun afterTextChanged(
                    s: Editable?
                ) {
                }
            }
        )
    }

    private fun saveCurrentRow(
        rowBinding: ItemBusinessRowBinding
    ) {

        val slNo =
            rowBinding.slNo.text
                .toString()
                .toIntOrNull()

        val weight =
            rowBinding.weight.text
                .toString()
                .toDoubleOrNull()

        val price =
            binding.price.text
                .toString()
                .toDoubleOrNull()

        val databaseProduct =
            rowProducts[rowBinding]
                ?: product

        if (
            weight == null ||
            price == null
        ) {

            rowBinding.totalprice.text =
                ""

            return
        }

        val total =
            weight * price

        if (
            databaseProduct ==
            "prod3.1"
        ) {

            rowBinding.totalprice.text =
                ""

        } else {

            rowBinding.totalprice.text =
                String.format(
                    "%.2f",
                    total
                )
        }

        saveOrUpdateRow(
            rowBinding,
            slNo,
            weight,
            price,
            total
        )
    }

    private fun saveOrUpdateRow(
        rowBinding: ItemBusinessRowBinding,
        slNo: Int?,
        weight: Double,
        price: Double,
        total: Double
    ) {

        lifecycleScope.launch(
            Dispatchers.IO
        ) {

            val databaseId =
                rowIds[rowBinding] ?: 0

            val databaseProduct =
                rowProducts[rowBinding]
                    ?: product

            val date =
                binding.date.text
                    .toString()

            val business =
                BusinessEntity(

                    id =
                        databaseId,

                    slno =
                        slNo,

                    product =
                        databaseProduct,

                    date =
                        date,

                    price =
                        price,

                    weight =
                        weight,

                    total =
                        total
                )

            if (databaseId == 0) {

                val newId =
                    dao.insertBusiness(
                        business
                    )

                withContext(
                    Dispatchers.Main
                ) {

                    rowIds[rowBinding] =
                        newId.toInt()
                }

                // schedule backup after insert
                //com.example.store.backup.BackupManager.requestBackup(applicationContext)

            } else {

                dao.updateBusiness(
                    business
                )
                // schedule backup after update
               // com.example.store.backup.BackupManager.requestBackup(applicationContext)
            }
        }
    }

    override fun onDestroy() {

        midnightHandler.removeCallbacks(
            midnightRunnable
        )

        super.onDestroy()
    }
}