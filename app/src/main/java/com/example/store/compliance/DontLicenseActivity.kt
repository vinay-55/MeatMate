package com.example.store.compliance

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.RadioGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.store.BaseActivity
import com.example.store.LocaleHelper
import com.example.store.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

class DontLicenseActivity : BaseActivity() {

    private lateinit var enterAutoButton: Button
    private lateinit var storeLocationEdit: EditText

    private lateinit var licenseStatusRadioGroup: RadioGroup
    private lateinit var saveLicenseButton: Button

    private lateinit var fssaiStatusRadioGroup: RadioGroup
    private lateinit var saveFssaiButton: Button

    private lateinit var fssaiDetailsSection: View
    private lateinit var fssaiRequirementsSection: View

    private lateinit var fssaiNumberEdit: EditText
    private lateinit var fssaiTypeEdit: EditText
    private lateinit var fssaiExpiryEdit: EditText

    private lateinit var fssaiBusinessTypeRadioGroup: RadioGroup
    private lateinit var fssaiTurnoverRadioGroup: RadioGroup
    private lateinit var fssaiWaterRadioGroup: RadioGroup
    private lateinit var fssaiSurfaceRadioGroup: RadioGroup
    private lateinit var fssaiFloorRadioGroup: RadioGroup
    private lateinit var fssaiWasteRadioGroup: RadioGroup
    private lateinit var fssaiDrainageRadioGroup: RadioGroup
    private lateinit var fssaiPestRadioGroup: RadioGroup
    private lateinit var fssaiSlaughterRadioGroup: RadioGroup
    private lateinit var fssaiStorageRadioGroup: RadioGroup
    private lateinit var fssaiNocRadioGroup: RadioGroup

    private lateinit var businessInfoSection: View

    private lateinit var areaRadioGroup: RadioGroup
    private lateinit var roadRadioGroup: RadioGroup
    private lateinit var templeRadioGroup: RadioGroup
    private lateinit var schoolRadioGroup: RadioGroup
    private lateinit var khataRadioGroup: RadioGroup
    private lateinit var neighborNocRadioGroup: RadioGroup
    private lateinit var glazedTilesRadioGroup: RadioGroup
    private lateinit var greaseTrapRadioGroup: RadioGroup
    private lateinit var blackGlassRadioGroup: RadioGroup

    private lateinit var saveStoreButton: Button

    private val prefs by lazy {
        getSharedPreferences("profile_prefs", MODE_PRIVATE)
    }

    private val locationFetcher by lazy {
        AndroidLocationInformationFetcher(this)
    }

    private val locationManager: LocationManager by lazy {
        getSystemService(LocationManager::class.java) as LocationManager
    }

    private val locationHandler = Handler(Looper.getMainLooper())

    private var pendingLocationListener: LocationListener? = null
    private var locationTimeoutRunnable: Runnable? = null

    private val locationPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->

            val hasPermission =
                permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                        permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true

            if (hasPermission) {
                requestCurrentLocation()
            } else {
                enterAutoButton.isEnabled = true
                enterAutoButton.text = getString(R.string.enter_automatically)

                Toast.makeText(
                    this,
                    getString(R.string.location_permission_required),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dont_license)

        findViewById<android.widget.TextView>(R.id.complianceDisclaimerText)?.text =
            LocaleHelper.wrapContext(this).getString(R.string.compliance_ai_disclaimer)

        enterAutoButton = findViewById(R.id.enterAutoButton)
        storeLocationEdit = findViewById(R.id.storeLocationEdit)

        licenseStatusRadioGroup =
            findViewById(R.id.licenseStatusRadioGroup)

        saveLicenseButton =
            findViewById(R.id.saveLicenseButton)

        fssaiStatusRadioGroup =
            findViewById(R.id.fssaiStatusRadioGroup)

        saveFssaiButton =
            findViewById(R.id.saveFssaiButton)

        fssaiDetailsSection =
            findViewById(R.id.fssaiDetailsSection)

        fssaiRequirementsSection =
            findViewById(R.id.fssaiRequirementsSection)

        fssaiNumberEdit =
            findViewById(R.id.fssaiNumberEdit)

        fssaiTypeEdit =
            findViewById(R.id.fssaiTypeEdit)

        fssaiExpiryEdit =
            findViewById(R.id.fssaiExpiryEdit)

        fssaiBusinessTypeRadioGroup =
            findViewById(R.id.fssaiBusinessTypeRadioGroup)

        fssaiTurnoverRadioGroup =
            findViewById(R.id.fssaiTurnoverRadioGroup)

        fssaiWaterRadioGroup =
            findViewById(R.id.fssaiWaterRadioGroup)

        fssaiSurfaceRadioGroup =
            findViewById(R.id.fssaiSurfaceRadioGroup)

        fssaiFloorRadioGroup =
            findViewById(R.id.fssaiFloorRadioGroup)

        fssaiWasteRadioGroup =
            findViewById(R.id.fssaiWasteRadioGroup)

        fssaiDrainageRadioGroup =
            findViewById(R.id.fssaiDrainageRadioGroup)

        fssaiPestRadioGroup =
            findViewById(R.id.fssaiPestRadioGroup)

        fssaiSlaughterRadioGroup =
            findViewById(R.id.fssaiSlaughterRadioGroup)

        fssaiStorageRadioGroup =
            findViewById(R.id.fssaiStorageRadioGroup)

        fssaiNocRadioGroup =
            findViewById(R.id.fssaiNocRadioGroup)

        businessInfoSection =
            findViewById(R.id.businessInfoSection)

        areaRadioGroup =
            findViewById(R.id.areaRadioGroup)

        roadRadioGroup =
            findViewById(R.id.roadRadioGroup)

        templeRadioGroup =
            findViewById(R.id.templeRadioGroup)

        schoolRadioGroup =
            findViewById(R.id.schoolRadioGroup)

        khataRadioGroup =
            findViewById(R.id.khataRadioGroup)

        neighborNocRadioGroup =
            findViewById(R.id.neighborNocRadioGroup)

        glazedTilesRadioGroup =
            findViewById(R.id.glazedTilesRadioGroup)

        greaseTrapRadioGroup =
            findViewById(R.id.greaseTrapRadioGroup)

        blackGlassRadioGroup =
            findViewById(R.id.blackGlassRadioGroup)

        saveStoreButton =
            findViewById(R.id.saveStoreButton)

        loadSavedStoreData()

        setupListeners()

        updateLicenseSectionVisibility()
        updateFssaiDetailsVisibility()
        updateSaveButtons()
    }

    private fun setupListeners() {

        licenseStatusRadioGroup.setOnCheckedChangeListener { _, _ ->
            updateLicenseSectionVisibility()
            updateSaveButtons()
        }

        fssaiStatusRadioGroup.setOnCheckedChangeListener { _, _ ->
            updateFssaiDetailsVisibility()
            updateSaveButtons()
        }

        enterAutoButton.setOnClickListener {
            showStoreLocationConfirmation()
        }

        val radioListener =
            RadioGroup.OnCheckedChangeListener { _, _ ->
                updateSaveButtons()
            }

        areaRadioGroup.setOnCheckedChangeListener(radioListener)
        roadRadioGroup.setOnCheckedChangeListener(radioListener)
        templeRadioGroup.setOnCheckedChangeListener(radioListener)
        schoolRadioGroup.setOnCheckedChangeListener(radioListener)
        khataRadioGroup.setOnCheckedChangeListener(radioListener)
        neighborNocRadioGroup.setOnCheckedChangeListener(radioListener)
        glazedTilesRadioGroup.setOnCheckedChangeListener(radioListener)
        greaseTrapRadioGroup.setOnCheckedChangeListener(radioListener)
        blackGlassRadioGroup.setOnCheckedChangeListener(radioListener)

        fssaiBusinessTypeRadioGroup.setOnCheckedChangeListener(radioListener)
        fssaiTurnoverRadioGroup.setOnCheckedChangeListener(radioListener)
        fssaiWaterRadioGroup.setOnCheckedChangeListener(radioListener)
        fssaiSurfaceRadioGroup.setOnCheckedChangeListener(radioListener)
        fssaiFloorRadioGroup.setOnCheckedChangeListener(radioListener)
        fssaiWasteRadioGroup.setOnCheckedChangeListener(radioListener)
        fssaiDrainageRadioGroup.setOnCheckedChangeListener(radioListener)
        fssaiPestRadioGroup.setOnCheckedChangeListener(radioListener)
        fssaiSlaughterRadioGroup.setOnCheckedChangeListener(radioListener)
        fssaiStorageRadioGroup.setOnCheckedChangeListener(radioListener)
        fssaiNocRadioGroup.setOnCheckedChangeListener(radioListener)

        storeLocationEdit.setOnKeyListener { _, _, _ ->
            updateSaveButtons()
            false
        }

        fssaiNumberEdit.setOnKeyListener { _, _, _ ->
            updateSaveButtons()
            false
        }

        fssaiTypeEdit.setOnKeyListener { _, _, _ ->
            updateSaveButtons()
            false
        }

        fssaiExpiryEdit.setOnKeyListener { _, _, _ ->
            updateSaveButtons()
            false
        }

        saveLicenseButton.setOnClickListener {
            saveLicenseData()
        }

        saveFssaiButton.setOnClickListener {
            saveFssaiData()
        }

        saveStoreButton.setOnClickListener {
            saveBusinessData()
        }
    }
    private fun loadSavedStoreData() {

        storeLocationEdit.setText(
            prefs.getString("store_location", "")
        )

        when (prefs.getString("license_status", "")) {
            "I have a license" ->
                licenseStatusRadioGroup.check(R.id.rbLicenseHave)

            "I don't have a license" ->
                licenseStatusRadioGroup.check(R.id.rbLicenseNo)

            "I don't know" ->
                licenseStatusRadioGroup.check(R.id.rbLicenseUnknown)
        }

        when (prefs.getString("fssai_status", "")) {
            "I have FSSAI" ->
                fssaiStatusRadioGroup.check(R.id.rbFssaiHave)

            "I don't have FSSAI" ->
                fssaiStatusRadioGroup.check(R.id.rbFssaiNo)

            "unknown",
            "I don't know" ->
                fssaiStatusRadioGroup.check(R.id.rbFssaiUnknown)
        }

        fssaiNumberEdit.setText(
            prefs.getString("fssai_number", "")
        )

        fssaiTypeEdit.setText(
            prefs.getString("fssai_type", "")
        )

        fssaiExpiryEdit.setText(
            prefs.getString("fssai_expiry", "")
        )

        when (prefs.getString("fssai_business_type", "")) {
            "Retail sale of chicken/meat" ->
                fssaiBusinessTypeRadioGroup.check(R.id.rbFssaiRetail)

            "Slaughter/dressing chicken at the premises" ->
                fssaiBusinessTypeRadioGroup.check(R.id.rbFssaiSlaughter)

            "Processing/marinating/other preparation" ->
                fssaiBusinessTypeRadioGroup.check(R.id.rbFssaiProcessing)

            "Wholesale/supply" ->
                fssaiBusinessTypeRadioGroup.check(R.id.rbFssaiWholesale)

            "More than one" ->
                fssaiBusinessTypeRadioGroup.check(R.id.rbFssaiMultiple)

            "I don't know" ->
                fssaiBusinessTypeRadioGroup.check(R.id.rbFssaiBusinessUnknown)
        }

        when (prefs.getString("fssai_turnover", "")) {
            "Up to ₹1.5 crore" ->
                fssaiTurnoverRadioGroup.check(R.id.rbFssaiTurnover1)

            "Above ₹1.5 crore to ₹50 crore" ->
                fssaiTurnoverRadioGroup.check(R.id.rbFssaiTurnover2)

            "Above ₹50 crore" ->
                fssaiTurnoverRadioGroup.check(R.id.rbFssaiTurnover3)

            "I don't know" ->
                fssaiTurnoverRadioGroup.check(R.id.rbFssaiTurnoverUnknown)
        }

        when (prefs.getString("fssai_potable_water", "")) {
            "Yes" ->
                fssaiWaterRadioGroup.check(R.id.rbFssaiWaterYes)

            "No" ->
                fssaiWaterRadioGroup.check(R.id.rbFssaiWaterNo)

            "I don't know" ->
                fssaiWaterRadioGroup.check(R.id.rbFssaiWaterUnknown)
        }

        when (prefs.getString("fssai_food_surface", "")) {
            "Yes" ->
                fssaiSurfaceRadioGroup.check(R.id.rbFssaiSurfaceYes)

            "No" ->
                fssaiSurfaceRadioGroup.check(R.id.rbFssaiSurfaceNo)

            "I don't know" ->
                fssaiSurfaceRadioGroup.check(R.id.rbFssaiSurfaceUnknown)
        }

        when (prefs.getString("fssai_floor_wall", "")) {
            "Yes" ->
                fssaiFloorRadioGroup.check(R.id.rbFssaiFloorYes)

            "No" ->
                fssaiFloorRadioGroup.check(R.id.rbFssaiFloorNo)

            "I don't know" ->
                fssaiFloorRadioGroup.check(R.id.rbFssaiFloorUnknown)
        }

        when (prefs.getString("fssai_waste_handling", "")) {
            "Yes" ->
                fssaiWasteRadioGroup.check(R.id.rbFssaiWasteYes)

            "No" ->
                fssaiWasteRadioGroup.check(R.id.rbFssaiWasteNo)

            "I don't know" ->
                fssaiWasteRadioGroup.check(R.id.rbFssaiWasteUnknown)
        }

        when (prefs.getString("fssai_drainage", "")) {
            "Yes" ->
                fssaiDrainageRadioGroup.check(R.id.rbFssaiDrainageYes)

            "No" ->
                fssaiDrainageRadioGroup.check(R.id.rbFssaiDrainageNo)

            "I don't know" ->
                fssaiDrainageRadioGroup.check(R.id.rbFssaiDrainageUnknown)
        }

        when (prefs.getString("fssai_pest_control", "")) {
            "Yes" ->
                fssaiPestRadioGroup.check(R.id.rbFssaiPestYes)

            "No" ->
                fssaiPestRadioGroup.check(R.id.rbFssaiPestNo)

            "I don't know" ->
                fssaiPestRadioGroup.check(R.id.rbFssaiPestUnknown)
        }

        when (prefs.getString("fssai_slaughter_hygiene", "")) {
            "Yes" ->
                fssaiSlaughterRadioGroup.check(R.id.rbFssaiSlaughterYes)

            "No" ->
                fssaiSlaughterRadioGroup.check(R.id.rbFssaiSlaughterNo)

            "I don't know" ->
                fssaiSlaughterRadioGroup.check(R.id.rbFssaiSlaughterUnknown)

            "Not applicable" ->
                fssaiSlaughterRadioGroup.check(R.id.rbFssaiSlaughterN_a)
        }

        when (prefs.getString("fssai_storage_arrangement", "")) {
            "Yes" ->
                fssaiStorageRadioGroup.check(R.id.rbFssaiStorageYes)

            "No" ->
                fssaiStorageRadioGroup.check(R.id.rbFssaiStorageNo)

            "I don't know" ->
                fssaiStorageRadioGroup.check(R.id.rbFssaiStorageUnknown)
        }

        when (prefs.getString("fssai_noc_status", "")) {
            "Yes/available" ->
                fssaiNocRadioGroup.check(R.id.rbFssaiNocYes)

            "No/not available" ->
                fssaiNocRadioGroup.check(R.id.rbFssaiNocNo)

            "I don't know" ->
                fssaiNocRadioGroup.check(R.id.rbFssaiNocUnknown)

            "Not applicable/if appropriate" ->
                fssaiNocRadioGroup.check(R.id.rbFssaiNocNa)
        }

        when (prefs.getString("store_area", "")) {
            "Gram Panchayat" ->
                areaRadioGroup.check(R.id.rbAreaPanchayat)

            "Municipality" ->
                areaRadioGroup.check(R.id.rbAreaMunicipality)

            "I don't know" ->
                areaRadioGroup.check(R.id.rbAreaUnknown)
        }

        when (prefs.getString("store_road", "")) {
            "Greater than 40 ft" ->
                roadRadioGroup.check(R.id.rbRoadGreater)

            "Less than 40 ft" ->
                roadRadioGroup.check(R.id.rbRoadLess)

            "I don't know" ->
                roadRadioGroup.check(R.id.rbRoadUnknown)
        }

        when (prefs.getString("store_temple", "")) {
            "There is a temple within 50 m" ->
                templeRadioGroup.check(R.id.rbTempleYes)

            "There is no temple within 50 m" ->
                templeRadioGroup.check(R.id.rbTempleNo)

            "I don't know" ->
                templeRadioGroup.check(R.id.rbTempleUnknown)
        }

        when (prefs.getString("store_school", "")) {
            "There is a school within 100 m" ->
                schoolRadioGroup.check(R.id.rbSchoolYes)

            "There is no school within 100 m" ->
                schoolRadioGroup.check(R.id.rbSchoolNo)

            "I don't know" ->
                schoolRadioGroup.check(R.id.rbSchoolUnknown)
        }

        when (prefs.getString("khata_status", "")) {
            "A-Khata" ->
                khataRadioGroup.check(R.id.rbKhataA)

            "B-Khata" ->
                khataRadioGroup.check(R.id.rbKhataB)

            "No Khata" ->
                khataRadioGroup.check(R.id.rbKhataNo)

            "I don't know" ->
                khataRadioGroup.check(R.id.rbKhataUnknown)
        }

        when (prefs.getString("neighbor_noc", "")) {
            "Available" ->
                neighborNocRadioGroup.check(R.id.rbNeighborYes)

            "Not available" ->
                neighborNocRadioGroup.check(R.id.rbNeighborNo)

            "I don't know" ->
                neighborNocRadioGroup.check(R.id.rbNeighborUnknown)
        }

        when (prefs.getString("glazed_tiles", "")) {
            "Available as required" ->
                glazedTilesRadioGroup.check(R.id.rbGlazedYes)

            "Not available" ->
                glazedTilesRadioGroup.check(R.id.rbGlazedNo)

            "I don't know" ->
                glazedTilesRadioGroup.check(R.id.rbGlazedUnknown)
        }

        when (prefs.getString("grease_trap", "")) {
            "Available" ->
                greaseTrapRadioGroup.check(R.id.rbGreaseYes)

            "Not available" ->
                greaseTrapRadioGroup.check(R.id.rbGreaseNo)

            "I don't know" ->
                greaseTrapRadioGroup.check(R.id.rbGreaseUnknown)
        }

        when (prefs.getString("black_glass_screening", "")) {
            "Available" ->
                blackGlassRadioGroup.check(R.id.rbBlackGlassYes)

            "Not available" ->
                blackGlassRadioGroup.check(R.id.rbBlackGlassNo)

            "I don't know" ->
                blackGlassRadioGroup.check(R.id.rbBlackGlassUnknown)
        }
    }

    private fun updateLicenseSectionVisibility() {
        val licenseStatus = selectedLicenseStatus()

        businessInfoSection.visibility =
            if (licenseStatus == "I don't have a license") {
                View.VISIBLE
            } else {
                View.GONE
            }
    }

    private fun updateFssaiDetailsVisibility() {
        val fssaiStatus = selectedFssaiStatus()

        fssaiDetailsSection.visibility =
            if (fssaiStatus == "I have FSSAI") {
                View.VISIBLE
            } else {
                View.GONE
            }

        fssaiRequirementsSection.visibility =
            if (fssaiStatus == "I don't have FSSAI") {
                View.VISIBLE
            } else {
                View.GONE
            }
    }

    private fun selectedLicenseStatus(): String {
        return when (licenseStatusRadioGroup.checkedRadioButtonId) {
            R.id.rbLicenseHave ->
                "I have a license"

            R.id.rbLicenseNo ->
                "I don't have a license"

            R.id.rbLicenseUnknown ->
                "I don't know"

            else -> ""
        }
    }

    private fun selectedFssaiStatus(): String {
        return when (fssaiStatusRadioGroup.checkedRadioButtonId) {
            R.id.rbFssaiHave ->
                "I have FSSAI"

            R.id.rbFssaiNo ->
                "I don't have FSSAI"

            R.id.rbFssaiUnknown ->
                "unknown"

            else -> ""
        }
    }
    private fun radioSelectedValue(
        radioGroup: RadioGroup,
        mapping: Map<Int, String>
    ): String {
        val selectedId = radioGroup.checkedRadioButtonId
        return mapping[selectedId].orEmpty()
    }

    private fun updateSaveButtons() {

        /*
         * Each Save button is independent.
         *
         * License button:
         * enabled when a license status is selected.
         */
        saveLicenseButton.isEnabled =
            selectedLicenseStatus().isNotEmpty()

        /*
         * FSSAI button:
         *
         * If "I have FSSAI" is selected, the existing FSSAI
         * details are optional and can be saved.
         *
         * If "I don't have FSSAI" is selected, all assessment
         * questions must have an answer, including "I don't know".
         *
         * If "I don't know" is selected, no extra questions
         * are required.
         */
        val fssaiStatus = selectedFssaiStatus()

        val fssaiComplete =
            when (fssaiStatus) {

                "I have FSSAI" -> true

                "unknown" -> true

                "I don't have FSSAI" -> {

                    val bt =
                        radioSelectedValue(
                            fssaiBusinessTypeRadioGroup,
                            mapOf(
                                R.id.rbFssaiRetail to
                                        "Retail sale of chicken/meat",

                                R.id.rbFssaiSlaughter to
                                        "Slaughter/dressing chicken at the premises",

                                R.id.rbFssaiProcessing to
                                        "Processing/marinating/other preparation",

                                R.id.rbFssaiWholesale to
                                        "Wholesale/supply",

                                R.id.rbFssaiMultiple to
                                        "More than one",

                                R.id.rbFssaiBusinessUnknown to
                                        "I don't know"
                            )
                        ).isNotEmpty()

                    val turnover =
                        radioSelectedValue(
                            fssaiTurnoverRadioGroup,
                            mapOf(
                                R.id.rbFssaiTurnover1 to
                                        "Up to ₹1.5 crore",

                                R.id.rbFssaiTurnover2 to
                                        "Above ₹1.5 crore to ₹50 crore",

                                R.id.rbFssaiTurnover3 to
                                        "Above ₹50 crore",

                                R.id.rbFssaiTurnoverUnknown to
                                        "I don't know"
                            )
                        ).isNotEmpty()

                    val water =
                        radioSelectedValue(
                            fssaiWaterRadioGroup,
                            mapOf(
                                R.id.rbFssaiWaterYes to "Yes",
                                R.id.rbFssaiWaterNo to "No",
                                R.id.rbFssaiWaterUnknown to "I don't know"
                            )
                        ).isNotEmpty()

                    val surfaces =
                        radioSelectedValue(
                            fssaiSurfaceRadioGroup,
                            mapOf(
                                R.id.rbFssaiSurfaceYes to "Yes",
                                R.id.rbFssaiSurfaceNo to "No",
                                R.id.rbFssaiSurfaceUnknown to "I don't know"
                            )
                        ).isNotEmpty()

                    val floors =
                        radioSelectedValue(
                            fssaiFloorRadioGroup,
                            mapOf(
                                R.id.rbFssaiFloorYes to "Yes",
                                R.id.rbFssaiFloorNo to "No",
                                R.id.rbFssaiFloorUnknown to "I don't know"
                            )
                        ).isNotEmpty()

                    val waste =
                        radioSelectedValue(
                            fssaiWasteRadioGroup,
                            mapOf(
                                R.id.rbFssaiWasteYes to "Yes",
                                R.id.rbFssaiWasteNo to "No",
                                R.id.rbFssaiWasteUnknown to "I don't know"
                            )
                        ).isNotEmpty()

                    val drainage =
                        radioSelectedValue(
                            fssaiDrainageRadioGroup,
                            mapOf(
                                R.id.rbFssaiDrainageYes to "Yes",
                                R.id.rbFssaiDrainageNo to "No",
                                R.id.rbFssaiDrainageUnknown to "I don't know"
                            )
                        ).isNotEmpty()

                    val pest =
                        radioSelectedValue(
                            fssaiPestRadioGroup,
                            mapOf(
                                R.id.rbFssaiPestYes to "Yes",
                                R.id.rbFssaiPestNo to "No",
                                R.id.rbFssaiPestUnknown to "I don't know"
                            )
                        ).isNotEmpty()

                    val slaughter =
                        radioSelectedValue(
                            fssaiSlaughterRadioGroup,
                            mapOf(
                                R.id.rbFssaiSlaughterYes to "Yes",
                                R.id.rbFssaiSlaughterNo to "No",
                                R.id.rbFssaiSlaughterUnknown to "I don't know",
                                R.id.rbFssaiSlaughterN_a to "Not applicable"
                            )
                        ).isNotEmpty()

                    val storage =
                        radioSelectedValue(
                            fssaiStorageRadioGroup,
                            mapOf(
                                R.id.rbFssaiStorageYes to "Yes",
                                R.id.rbFssaiStorageNo to "No",
                                R.id.rbFssaiStorageUnknown to "I don't know"
                            )
                        ).isNotEmpty()

                    val noc =
                        radioSelectedValue(
                            fssaiNocRadioGroup,
                            mapOf(
                                R.id.rbFssaiNocYes to "Yes/available",
                                R.id.rbFssaiNocNo to "No/not available",
                                R.id.rbFssaiNocUnknown to "I don't know",
                                R.id.rbFssaiNocNa to
                                        "Not applicable/if appropriate"
                            )
                        ).isNotEmpty()

                    bt &&
                            turnover &&
                            water &&
                            surfaces &&
                            floors &&
                            waste &&
                            drainage &&
                            pest &&
                            slaughter &&
                            storage &&
                            noc
                }

                else -> false
            }

        saveFssaiButton.isEnabled =
            fssaiStatus.isNotEmpty() && fssaiComplete

        /*
         * Business Save button is independent from FSSAI.
         */
        val licenseStatus = selectedLicenseStatus()

        val businessComplete =
            if (licenseStatus == "I don't have a license") {

                val locationFilled =
                    storeLocationEdit.text.toString()
                        .trim()
                        .isNotEmpty()

                val areaFilled =
                    radioSelectedValue(
                        areaRadioGroup,
                        mapOf(
                            R.id.rbAreaPanchayat to "Gram Panchayat",
                            R.id.rbAreaMunicipality to "Municipality",
                            R.id.rbAreaUnknown to "I don't know"
                        )
                    ).isNotEmpty()

                val roadFilled =
                    radioSelectedValue(
                        roadRadioGroup,
                        mapOf(
                            R.id.rbRoadGreater to "Greater than 40 ft",
                            R.id.rbRoadLess to "Less than 40 ft",
                            R.id.rbRoadUnknown to "I don't know"
                        )
                    ).isNotEmpty()

                val templeFilled =
                    radioSelectedValue(
                        templeRadioGroup,
                        mapOf(
                            R.id.rbTempleYes to
                                    "There is a temple within 50 m",

                            R.id.rbTempleNo to
                                    "There is no temple within 50 m",

                            R.id.rbTempleUnknown to
                                    "I don't know"
                        )
                    ).isNotEmpty()

                val schoolFilled =
                    radioSelectedValue(
                        schoolRadioGroup,
                        mapOf(
                            R.id.rbSchoolYes to
                                    "There is a school within 100 m",

                            R.id.rbSchoolNo to
                                    "There is no school within 100 m",

                            R.id.rbSchoolUnknown to
                                    "I don't know"
                        )
                    ).isNotEmpty()

                val khataFilled =
                    radioSelectedValue(
                        khataRadioGroup,
                        mapOf(
                            R.id.rbKhataA to "A-Khata",
                            R.id.rbKhataB to "B-Khata",
                            R.id.rbKhataNo to "No Khata",
                            R.id.rbKhataUnknown to "I don't know"
                        )
                    ).isNotEmpty()

                val nocFilled =
                    radioSelectedValue(
                        neighborNocRadioGroup,
                        mapOf(
                            R.id.rbNeighborYes to "Available",
                            R.id.rbNeighborNo to "Not available",
                            R.id.rbNeighborUnknown to "I don't know"
                        )
                    ).isNotEmpty()

                val glazedFilled =
                    radioSelectedValue(
                        glazedTilesRadioGroup,
                        mapOf(
                            R.id.rbGlazedYes to "Available as required",
                            R.id.rbGlazedNo to "Not available",
                            R.id.rbGlazedUnknown to "I don't know"
                        )
                    ).isNotEmpty()

                val greaseFilled =
                    radioSelectedValue(
                        greaseTrapRadioGroup,
                        mapOf(
                            R.id.rbGreaseYes to "Available",
                            R.id.rbGreaseNo to "Not available",
                            R.id.rbGreaseUnknown to "I don't know"
                        )
                    ).isNotEmpty()

                val blackGlassFilled =
                    radioSelectedValue(
                        blackGlassRadioGroup,
                        mapOf(
                            R.id.rbBlackGlassYes to "Available",
                            R.id.rbBlackGlassNo to "Not available",
                            R.id.rbBlackGlassUnknown to "I don't know"
                        )
                    ).isNotEmpty()

                locationFilled &&
                        areaFilled &&
                        roadFilled &&
                        templeFilled &&
                        schoolFilled &&
                        khataFilled &&
                        nocFilled &&
                        glazedFilled &&
                        greaseFilled &&
                        blackGlassFilled

            } else {
                /*
                 * If the user has a license or doesn't know,
                 * business information is not required here.
                 */
                true
            }

        saveStoreButton.isEnabled =
            licenseStatus.isNotEmpty() && businessComplete
    }

    private fun saveLicenseData() {

        val selectedStatus = selectedLicenseStatus()

        if (selectedStatus.isEmpty()) {
            Toast.makeText(
                this,
                getString(R.string.prompt_select_license_status),
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        prefs.edit()
            .putString("license_status", selectedStatus)
            .apply()

        Toast.makeText(
            this,
            getString(R.string.license_info_saved),
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun saveFssaiData() {

        val selectedStatus = selectedFssaiStatus()

        if (selectedStatus.isEmpty()) {
            Toast.makeText(
                this,
                getString(R.string.prompt_select_fssai_status),
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        if (
            selectedStatus == "I don't have FSSAI" &&
            !isFssaiRequirementsComplete()
        ) {
            Toast.makeText(
                this,
                getString(R.string.prompt_complete_fssai_info),
                Toast.LENGTH_SHORT
            ).show()

            updateSaveButtons()
            return
        }

        val editor = prefs.edit()

        editor.putString(
            "fssai_status",
            selectedStatus
        )

        if (selectedStatus == "I have FSSAI") {

            editor.putString(
                "fssai_number",
                fssaiNumberEdit.text.toString().trim()
            )

            editor.putString(
                "fssai_type",
                fssaiTypeEdit.text.toString().trim()
            )

            editor.putString(
                "fssai_expiry",
                fssaiExpiryEdit.text.toString().trim()
            )

            /*
             * Existing assessment answers are not deleted here.
             * This protects previously saved information.
             */

        } else if (selectedStatus == "I don't have FSSAI") {

            saveFssaiRequirementValues(editor)

        } else {
            /*
             * "I don't know"
             *
             * Preserve previously entered FSSAI details and
             * requirements instead of deleting user data.
             */
        }

        editor.apply()

        Toast.makeText(
            this,
            getString(R.string.fssai_info_saved),
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun isFssaiRequirementsComplete(): Boolean {

        val groups = listOf(
            fssaiBusinessTypeRadioGroup,
            fssaiTurnoverRadioGroup,
            fssaiWaterRadioGroup,
            fssaiSurfaceRadioGroup,
            fssaiFloorRadioGroup,
            fssaiWasteRadioGroup,
            fssaiDrainageRadioGroup,
            fssaiPestRadioGroup,
            fssaiSlaughterRadioGroup,
            fssaiStorageRadioGroup,
            fssaiNocRadioGroup
        )

        return groups.all {
            it.checkedRadioButtonId != -1
        }
    }
    private fun saveFssaiRequirementValues(
        editor: android.content.SharedPreferences.Editor
    ) {

        editor.putString(
            "fssai_business_type",
            radioSelectedValue(
                fssaiBusinessTypeRadioGroup,
                mapOf(
                    R.id.rbFssaiRetail to
                            "Retail sale of chicken/meat",

                    R.id.rbFssaiSlaughter to
                            "Slaughter/dressing chicken at the premises",

                    R.id.rbFssaiProcessing to
                            "Processing/marinating/other preparation",

                    R.id.rbFssaiWholesale to
                            "Wholesale/supply",

                    R.id.rbFssaiMultiple to
                            "More than one",

                    R.id.rbFssaiBusinessUnknown to
                            "I don't know"
                )
            )
        )

        editor.putString(
            "fssai_turnover",
            radioSelectedValue(
                fssaiTurnoverRadioGroup,
                mapOf(
                    R.id.rbFssaiTurnover1 to
                            "Up to ₹1.5 crore",

                    R.id.rbFssaiTurnover2 to
                            "Above ₹1.5 crore to ₹50 crore",

                    R.id.rbFssaiTurnover3 to
                            "Above ₹50 crore",

                    R.id.rbFssaiTurnoverUnknown to
                            "I don't know"
                )
            )
        )

        editor.putString(
            "fssai_potable_water",
            radioSelectedValue(
                fssaiWaterRadioGroup,
                mapOf(
                    R.id.rbFssaiWaterYes to "Yes",
                    R.id.rbFssaiWaterNo to "No",
                    R.id.rbFssaiWaterUnknown to "I don't know"
                )
            )
        )

        editor.putString(
            "fssai_food_surface",
            radioSelectedValue(
                fssaiSurfaceRadioGroup,
                mapOf(
                    R.id.rbFssaiSurfaceYes to "Yes",
                    R.id.rbFssaiSurfaceNo to "No",
                    R.id.rbFssaiSurfaceUnknown to "I don't know"
                )
            )
        )

        editor.putString(
            "fssai_floor_wall",
            radioSelectedValue(
                fssaiFloorRadioGroup,
                mapOf(
                    R.id.rbFssaiFloorYes to "Yes",
                    R.id.rbFssaiFloorNo to "No",
                    R.id.rbFssaiFloorUnknown to "I don't know"
                )
            )
        )

        editor.putString(
            "fssai_waste_handling",
            radioSelectedValue(
                fssaiWasteRadioGroup,
                mapOf(
                    R.id.rbFssaiWasteYes to "Yes",
                    R.id.rbFssaiWasteNo to "No",
                    R.id.rbFssaiWasteUnknown to "I don't know"
                )
            )
        )

        editor.putString(
            "fssai_drainage",
            radioSelectedValue(
                fssaiDrainageRadioGroup,
                mapOf(
                    R.id.rbFssaiDrainageYes to "Yes",
                    R.id.rbFssaiDrainageNo to "No",
                    R.id.rbFssaiDrainageUnknown to "I don't know"
                )
            )
        )

        editor.putString(
            "fssai_pest_control",
            radioSelectedValue(
                fssaiPestRadioGroup,
                mapOf(
                    R.id.rbFssaiPestYes to "Yes",
                    R.id.rbFssaiPestNo to "No",
                    R.id.rbFssaiPestUnknown to "I don't know"
                )
            )
        )

        editor.putString(
            "fssai_slaughter_hygiene",
            radioSelectedValue(
                fssaiSlaughterRadioGroup,
                mapOf(
                    R.id.rbFssaiSlaughterYes to "Yes",
                    R.id.rbFssaiSlaughterNo to "No",
                    R.id.rbFssaiSlaughterUnknown to "I don't know",
                    R.id.rbFssaiSlaughterN_a to "Not applicable"
                )
            )
        )

        editor.putString(
            "fssai_storage_arrangement",
            radioSelectedValue(
                fssaiStorageRadioGroup,
                mapOf(
                    R.id.rbFssaiStorageYes to "Yes",
                    R.id.rbFssaiStorageNo to "No",
                    R.id.rbFssaiStorageUnknown to "I don't know"
                )
            )
        )

        editor.putString(
            "fssai_noc_status",
            radioSelectedValue(
                fssaiNocRadioGroup,
                mapOf(
                    R.id.rbFssaiNocYes to "Yes/available",
                    R.id.rbFssaiNocNo to "No/not available",
                    R.id.rbFssaiNocUnknown to "I don't know",
                    R.id.rbFssaiNocNa to
                            "Not applicable/if appropriate"
                )
            )
        )
    }

    private fun saveBusinessData() {

        val licenseStatus = selectedLicenseStatus()

        if (licenseStatus.isEmpty()) {
            Toast.makeText(
                this,
                getString(R.string.prompt_select_license_status),
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        /*
         * Business/location information is relevant to the
         * new-license assessment.
         *
         * If the user already has a license or doesn't know
         * their license status, we do not force this section.
         */
        if (licenseStatus != "I don't have a license") {
            Toast.makeText(
                this,
                getString(R.string.business_info_saved),
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val location =
            storeLocationEdit.text.toString().trim()

        val area =
            radioSelectedValue(
                areaRadioGroup,
                mapOf(
                    R.id.rbAreaPanchayat to "Gram Panchayat",
                    R.id.rbAreaMunicipality to "Municipality",
                    R.id.rbAreaUnknown to "I don't know"
                )
            )

        val road =
            radioSelectedValue(
                roadRadioGroup,
                mapOf(
                    R.id.rbRoadGreater to "Greater than 40 ft",
                    R.id.rbRoadLess to "Less than 40 ft",
                    R.id.rbRoadUnknown to "I don't know"
                )
            )

        val temple =
            radioSelectedValue(
                templeRadioGroup,
                mapOf(
                    R.id.rbTempleYes to
                            "There is a temple within 50 m",

                    R.id.rbTempleNo to
                            "There is no temple within 50 m",

                    R.id.rbTempleUnknown to
                            "I don't know"
                )
            )

        val school =
            radioSelectedValue(
                schoolRadioGroup,
                mapOf(
                    R.id.rbSchoolYes to
                            "There is a school within 100 m",

                    R.id.rbSchoolNo to
                            "There is no school within 100 m",

                    R.id.rbSchoolUnknown to
                            "I don't know"
                )
            )

        val khata =
            radioSelectedValue(
                khataRadioGroup,
                mapOf(
                    R.id.rbKhataA to "A-Khata",
                    R.id.rbKhataB to "B-Khata",
                    R.id.rbKhataNo to "No Khata",
                    R.id.rbKhataUnknown to "I don't know"
                )
            )

        val neighbor =
            radioSelectedValue(
                neighborNocRadioGroup,
                mapOf(
                    R.id.rbNeighborYes to "Available",
                    R.id.rbNeighborNo to "Not available",
                    R.id.rbNeighborUnknown to "I don't know"
                )
            )

        val glazedTiles =
            radioSelectedValue(
                glazedTilesRadioGroup,
                mapOf(
                    R.id.rbGlazedYes to "Available as required",
                    R.id.rbGlazedNo to "Not available",
                    R.id.rbGlazedUnknown to "I don't know"
                )
            )

        val grease =
            radioSelectedValue(
                greaseTrapRadioGroup,
                mapOf(
                    R.id.rbGreaseYes to "Available",
                    R.id.rbGreaseNo to "Not available",
                    R.id.rbGreaseUnknown to "I don't know"
                )
            )

        val blackGlass =
            radioSelectedValue(
                blackGlassRadioGroup,
                mapOf(
                    R.id.rbBlackGlassYes to "Available",
                    R.id.rbBlackGlassNo to "Not available",
                    R.id.rbBlackGlassUnknown to "I don't know"
                )
            )

        if (
            location.isEmpty() ||
            area.isEmpty() ||
            road.isEmpty() ||
            temple.isEmpty() ||
            school.isEmpty() ||
            khata.isEmpty() ||
            neighbor.isEmpty() ||
            glazedTiles.isEmpty() ||
            grease.isEmpty() ||
            blackGlass.isEmpty()
        ) {
            Toast.makeText(
                this,
                getString(R.string.prompt_complete_business_info),
                Toast.LENGTH_SHORT
            ).show()

            updateSaveButtons()
            return
        }

        prefs.edit()
            .putString("store_location", location)
            .putString("store_area", area)
            .putString("store_road", road)
            .putString("store_temple", temple)
            .putString("store_school", school)
            .putString("khata_status", khata)
            .putString("neighbor_noc", neighbor)
            .putString("glazed_tiles", glazedTiles)
            .putString("grease_trap", grease)
            .putString("black_glass_screening", blackGlass)
            .apply()

        Toast.makeText(
            this,
            getString(R.string.business_info_saved),
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun showStoreLocationConfirmation() {

        AlertDialog.Builder(this)
            .setTitle("Make sure you are in your store location.")
            .setPositiveButton("I’m in") { _, _ ->
                startAutomaticEntry()
            }
            .setNegativeButton("I’m not") { _, _ ->
                // Nothing to do.
            }
            .show()
    }

    private fun startAutomaticEntry() {

        if (!hasLocationPermission()) {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
            return
        }

        requestCurrentLocation()
    }

    private fun hasLocationPermission(): Boolean {

        val fineLocation =
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            )

        val coarseLocation =
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )

        return fineLocation ==
                android.content.pm.PackageManager.PERMISSION_GRANTED ||
                coarseLocation ==
                android.content.pm.PackageManager.PERMISSION_GRANTED
    }

    @SuppressLint("MissingPermission")
    private fun requestCurrentLocation() {

        val gpsEnabled =
            locationManager.isProviderEnabled(
                LocationManager.GPS_PROVIDER
            )

        val networkEnabled =
            locationManager.isProviderEnabled(
                LocationManager.NETWORK_PROVIDER
            )

        if (!gpsEnabled && !networkEnabled) {
            Toast.makeText(
                this,
                "Please turn on location services.",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val provider =
            if (gpsEnabled) {
                LocationManager.GPS_PROVIDER
            } else {
                LocationManager.NETWORK_PROVIDER
            }

        enterAutoButton.isEnabled = false
        enterAutoButton.text = "Getting location..."

        val lastKnownLocation =
            locationManager.getLastKnownLocation(provider)

        if (lastKnownLocation != null) {
            handleLocationResult(lastKnownLocation)
            return
        }

        val listener = object : LocationListener {

            override fun onLocationChanged(location: Location) {
                handleLocationResult(location)
            }
        }

        pendingLocationListener = listener

        locationTimeoutRunnable = Runnable {

            pendingLocationListener?.let {
                locationManager.removeUpdates(it)
            }

            pendingLocationListener = null

            enterAutoButton.isEnabled = true
            enterAutoButton.text = getString(R.string.enter_automatically)

            Toast.makeText(
                this,
                getString(R.string.unable_to_get_location),
                Toast.LENGTH_SHORT
            ).show()
        }

        locationHandler.postDelayed(
            locationTimeoutRunnable!!,
            20_000
        )

        locationManager.requestSingleUpdate(
            provider,
            listener,
            Looper.getMainLooper()
        )
    }

    private fun handleLocationResult(location: Location) {

        pendingLocationListener?.let {
            locationManager.removeUpdates(it)
        }

        pendingLocationListener = null

        locationTimeoutRunnable?.let {
            locationHandler.removeCallbacks(it)
        }

        lifecycleScope.launch {

            enterAutoButton.text =
                "Finding store details..."

            val coordinates =
                BusinessLocationCoordinates(
                    latitude = location.latitude,
                    longitude = location.longitude,
                    accuracyMeters = location.accuracy.toDouble()
                )

            val fetched: FetchedLocationInfo? =
                withContext(Dispatchers.IO) {
                    try {
                        locationFetcher.fetchInfo(coordinates)
                    } catch (_: Exception) {
                        null
                    }
                }

            val fullAddress =
                withContext(Dispatchers.IO) {
                    getFullAddress(
                        location.latitude,
                        location.longitude
                    )
                }

            if (!fullAddress.isNullOrBlank()) {

                storeLocationEdit.setText(fullAddress)

            } else if (fetched != null) {

                val fallback =
                    fetched.areaOrLocality.takeIf {
                        it.isNotBlank() &&
                                it != "Not available"
                    }

                if (!fallback.isNullOrBlank()) {
                    storeLocationEdit.setText(fallback)
                }
            }

            if (fetched != null) {

                val localAuthority =
                    fetched.localAuthority ?: ""

                val municipality =
                    fetched.municipality ?: ""

                val panchayat =
                    localAuthority.contains(
                        "panchayat",
                        ignoreCase = true
                    ) ||
                            municipality.contains(
                                "panchayat",
                                ignoreCase = true
                            )

                if (panchayat) {

                    areaRadioGroup.check(
                        R.id.rbAreaPanchayat
                    )

                } else if (
                    municipality.isNotBlank() &&
                    municipality != "Not available"
                ) {

                    areaRadioGroup.check(
                        R.id.rbAreaMunicipality
                    )
                }
            }

            updateSaveButtons()

            enterAutoButton.isEnabled = true
            enterAutoButton.text = getString(R.string.enter_automatically)

            Toast.makeText(
                this@DontLicenseActivity,
                "Automatic entry completed where information was available.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun getFullAddress(
        latitude: Double,
        longitude: Double
    ): String? {

        return try {

            if (!Geocoder.isPresent()) {
                return null
            }

            val geocoder =
                Geocoder(
                    this,
                    Locale.getDefault()
                )

            val addresses =
                geocoder.getFromLocation(
                    latitude,
                    longitude,
                    1
                ) ?: return null

            if (addresses.isEmpty()) {
                return null
            }

            val address = addresses[0]
            val parts = mutableListOf<String>()

            for (
            index in 0..address.maxAddressLineIndex
            ) {

                val line =
                    address
                        .getAddressLine(index)
                        ?.trim()

                if (
                    !line.isNullOrBlank() &&
                    !parts.contains(line)
                ) {
                    parts.add(line)
                }
            }

            if (parts.isNotEmpty()) {

                parts.joinToString(", ")

            } else {

                buildAddressFromComponents(address)
            }

        } catch (_: Exception) {
            null
        }
    }

    private fun buildAddressFromComponents(
        address: Address
    ): String? {

        val parts = mutableListOf<String>()

        fun add(value: String?) {

            if (
                !value.isNullOrBlank() &&
                !parts.contains(value)
            ) {
                parts.add(value)
            }
        }

        add(address.subThoroughfare)
        add(address.thoroughfare)
        add(address.subLocality)
        add(address.locality)
        add(address.subAdminArea)
        add(address.adminArea)
        add(address.postalCode)
        add(address.countryName)

        return if (parts.isEmpty()) {
            null
        } else {
            parts.joinToString(", ")
        }
    }

    override fun onDestroy() {

        pendingLocationListener?.let {
            locationManager.removeUpdates(it)
        }

        locationTimeoutRunnable?.let {
            locationHandler.removeCallbacks(it)
        }

        super.onDestroy()
    }
}