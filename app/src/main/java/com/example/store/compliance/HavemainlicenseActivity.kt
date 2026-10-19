package com.example.store.compliance

import android.content.Context
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.store.LocaleHelper
import com.example.store.R

class HavemainlicenseActivity : AppCompatActivity() {

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.wrapEnglishContext(newBase))
    }

    private data class Issue(
        val order: Int,
        val title: String,
        val status: String,
        val problem: String,
        val whyItMatters: String,
        val recommendedAction: String,
        val steps: List<String>,
        val source: String
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_havemainlicense)

        findViewById<TextView>(R.id.complianceDisclaimerText)?.text =
            LocaleHelper.wrapContext(this).getString(R.string.compliance_ai_disclaimer)

        val statusText = findViewById<TextView>(R.id.statusText)
        val appMessageText = findViewById<TextView>(R.id.appMessageText)
        val probabilityText = findViewById<TextView>(R.id.probabilityText)
        val finesText = findViewById<TextView>(R.id.finesText)
        val recommendedActionText = findViewById<TextView>(R.id.recommendedActionText)

        val prefs = getSharedPreferences("profile_prefs", MODE_PRIVATE)
        val area = prefs.getString("store_area", "")?.trim().orEmpty()
        val road = prefs.getString("store_road", "")?.trim().orEmpty()
        val temple = prefs.getString("store_temple", "")?.trim().orEmpty()
        val school = prefs.getString("store_school", "")?.trim().orEmpty()
        val khataStatus = prefs.getString("khata_status", "")?.trim().orEmpty()
        val neighborNoc = prefs.getString("neighbor_noc", "")?.trim().orEmpty()
        val glazedTiles = prefs.getString("glazed_tiles", "")?.trim().orEmpty()
        val greaseTrap = prefs.getString("grease_trap", "")?.trim().orEmpty()
        val blackGlass = prefs.getString("black_glass_screening", "")?.trim().orEmpty()

        val issues = mutableListOf<Issue>()

        fun addIssue(
            title: String,
            status: String,
            problem: String,
            whyItMatters: String,
            recommendedAction: String,
            steps: List<String>,
            source: String,
            order: Int
        ) {
            issues.add(Issue(order, title, status, problem, whyItMatters, recommendedAction, steps, source))
        }

        if (area == "I don't know") {
            addIssue(
                title = "Area status unknown",
                status = "Verification required",
                problem = "The area category could affect the applicable local legal and licensing requirements.",
                whyItMatters = "The premises may fall under a Gram Panchayat or municipality, and the applicable local rule can differ by jurisdiction.",
                recommendedAction = "Confirm whether the premises is in a Gram Panchayat or municipality, and check the relevant local rules before applying.",
                steps = listOf(
                    "Confirm the exact local jurisdiction for the premises.",
                    "Check the applicable Panchayat or municipal rule.",
                    "Ask whether any zoning, setback, drainage or sanitation requirements apply.",
                    "Keep any written confirmation from the authority."
                ),
                source = "Relevant Gram Panchayat or municipal authority",
                order = 1
            )
        }

        if (road == "I don't know") {
            addIssue(
                title = "Road width unknown",
                status = "Verification required",
                problem = "Road width could affect the requirements for the premises.",
                whyItMatters = "Road-width conditions may affect local permissions, setbacks, frontage, access, drainage or other requirements depending on the applicable rule.",
                recommendedAction = "Measure the actual road width near the premises and confirm the current local requirement with the relevant authority.",
                steps = listOf(
                    "Measure the actual road width at or near the premises.",
                    "Confirm the applicable local rule with the relevant authority.",
                    "If the road is less than 40 ft, ask whether a different premises or local approval is required.",
                    "Keep any official measurement or confirmation document."
                ),
                source = "Relevant local authority / municipal or panchayat office",
                order = 2
            )
        }

        if (road == "Less than 40 ft") {
            val roadProblem = when (area) {
                "Gram Panchayat" -> "Road width may create a local Panchayat approval or premises-compatibility issue."
                "Municipality" -> "Road width may create a municipal/urban zoning or local permission issue."
                else -> "Road width may create a local permission or premises-compatibility issue."
            }
            val roadWhy = when (area) {
                "Gram Panchayat" -> "The exact requirement can depend on the local Panchayat rule, frontage, road margin, drainage, sanitation and the premises itself."
                "Municipality" -> "Urban/local zoning, frontage or road-width requirements may vary by authority and exact location."
                else -> "The requirement can depend on the local authority, premises and current rules."
            }
            addIssue(
                title = "Narrow road",
                status = "Requires verification",
                problem = roadProblem,
                whyItMatters = roadWhy,
                recommendedAction = "Confirm the applicable local rule, ask whether the premises can legally operate as a food business, and consider an alternative premises if the requirement cannot be met.",
                steps = listOf(
                    "Measure the actual road width at the premises.",
                    "Ask the relevant local authority whether the required road width or frontage applies.",
                    "Check whether a setback, drainage or sanitation condition also applies.",
                    "If the premises cannot satisfy the requirement, explore an alternative premises."
                ),
                source = "Relevant local authority / panchayat or municipal office",
                order = 3
            )
        }

        if (temple == "There is a temple within 50 m") {
            addIssue(
                title = "Temple proximity",
                status = "Requires verification",
                problem = "Temple proximity may create a location or licensing issue depending on the applicable local rule.",
                whyItMatters = "The exact effect depends on the actual distance, local authority rule and the premises details.",
                recommendedAction = "Verify the actual distance and obtain written clarification from the relevant authority. If the premises is not permitted, consider another suitable premises.",
                steps = listOf(
                    "Measure the actual distance from the temple to the premises.",
                    "Check the applicable local location rule and authority guidance.",
                    "Request written clarification if the distance is close to the threshold.",
                    "If the premises is not permitted, consider another location."
                ),
                source = "Relevant local authority / municipal or panchayat office",
                order = 4
            )
        } else if (temple == "I don't know") {
            addIssue(
                title = "Temple proximity unknown",
                status = "Verification required",
                problem = "Temple proximity could affect the requirements for the premises.",
                whyItMatters = "The location rule can depend on the actual distance and the relevant local authority requirement.",
                recommendedAction = "Measure the distance and confirm whether the local rule restricts or prohibits this location.",
                steps = listOf(
                    "Measure the actual distance to the nearest temple.",
                    "Check the applicable local rule and authority guidance.",
                    "Ask for written clarification where appropriate.",
                    "If the premises is not permitted, consider another location."
                ),
                source = "Relevant local authority / municipal or panchayat office",
                order = 4
            )
        }

        if (school == "There is a school within 100 m") {
            addIssue(
                title = "School proximity",
                status = "Requires verification",
                problem = "School proximity may create a location restriction or licensing issue depending on the applicable local rule.",
                whyItMatters = "The exact requirement can depend on the actual distance, local authority rule and the premises details.",
                recommendedAction = "Verify the actual distance and obtain written clarification from the relevant authority. If the premises is not permitted, consider another suitable premises.",
                steps = listOf(
                    "Measure the actual distance from the school to the premises.",
                    "Check the applicable local rule and authority guidance.",
                    "Ask for written clarification if necessary.",
                    "If the location is restricted, consider another premises."
                ),
                source = "Relevant local authority / municipal or panchayat office",
                order = 5
            )
        } else if (school == "I don't know") {
            addIssue(
                title = "School proximity unknown",
                status = "Verification required",
                problem = "School proximity could affect the requirements for the premises.",
                whyItMatters = "The rule can depend on the actual distance and the applicable local authority requirement.",
                recommendedAction = "Measure the distance and confirm the current requirement with the authority before relying on the location.",
                steps = listOf(
                    "Measure the actual distance to the nearest school.",
                    "Check the local rule and authority guidance.",
                    "Ask for written clarification where needed.",
                    "If the premises is restricted, consider an alternative location."
                ),
                source = "Relevant local authority / municipal or panchayat office",
                order = 5
            )
        }

        when (khataStatus) {
            "B-Khata" -> addIssue(
                title = "B-Khata / property record",
                status = "Requires verification",
                problem = "B-Khata / property-record status may create a property documentation issue for the proposed business.",
                whyItMatters = "The property record/status may affect permissions or licensing depending on the applicable local requirements.",
                recommendedAction = "Verify whether the property is eligible for the applicable Khata/e-Khata correction or conversion process and complete the official document check before relying on the property.",
                steps = listOf(
                    "Check the current property and Khata/e-Khata record.",
                    "Verify the property-tax and identification details.",
                    "Check whether B-to-A Khata, Single Plot Khata, e-Khata or another process is available.",
                    "Collect the required title/property/tax documents.",
                    "Apply through the proper authority or portal if eligible.",
                    "Complete document verification or inspection if required.",
                    "Pay only the official fee and keep the receipt or acknowledgement.",
                    "After the property record is corrected or regularised, re-check the business licence requirements."
                ),
                source = "Revenue / property record / Khata authority",
                order = 6
            )
            "No Khata" -> addIssue(
                title = "Property record not available",
                status = "Requires verification",
                problem = "The property record may not be in a suitable form for licensing or official verification.",
                whyItMatters = "The property may need an appropriate official record or regularisation before it can be relied on for licensing.",
                recommendedAction = "Check the property documents and official record status, and complete the applicable property-record regularisation before relying on the premises.",
                steps = listOf(
                    "Check the property-tax and identification documents.",
                    "Ask the authority what property record is required for this premises.",
                    "Determine whether a Khata/e-Khata or other regularisation is necessary.",
                    "Submit the required application and documents if needed."
                ),
                source = "Revenue / property record / Khata authority",
                order = 6
            )
            "I don't know" -> addIssue(
                title = "Khata status unknown",
                status = "Verification required",
                problem = "The property record status is not yet confirmed.",
                whyItMatters = "Property-record status may affect the legal suitability of the premises and the licensing process.",
                recommendedAction = "Check the current property and Khata/e-Khata records and confirm the status before relying on the premises.",
                steps = listOf(
                    "Check the current property record.",
                    "Review property-tax and land records.",
                    "Confirm the applicable document or regularisation process with the authority.",
                    "Keep the official confirmation for future review."
                ),
                source = "Revenue / property record / Khata authority",
                order = 6
            )
        }

        when (neighborNoc) {
            "Not available" -> addIssue(
                title = "Neighbour NOC not available",
                status = "Requires verification",
                problem = "A neighbour NOC may be required depending on the applicable local rule and authority.",
                whyItMatters = "The requirement can depend on the premises, activity and the local authority. If required, the consent/document may need to be obtained before application.",
                recommendedAction = "Verify whether the NOC is required, identify the correct person or authority, and obtain the document if the authority requires it.",
                steps = listOf(
                    "Ask the relevant authority whether an NOC is required.",
                    "Identify the required format and signatory.",
                    "Obtain the document if required.",
                    "Keep the signed document ready for the application."
                ),
                source = "Relevant local authority / municipal office",
                order = 7
            )
            "I don't know" -> addIssue(
                title = "Neighbour NOC requirement unknown",
                status = "Verification required",
                problem = "The need for a neighbour NOC is not yet confirmed.",
                whyItMatters = "The requirement may vary by location, authority and premises details.",
                recommendedAction = "Ask the relevant authority whether the NOC is required and what format is accepted before relying on the premises.",
                steps = listOf(
                    "Check with the authority whether an NOC is required.",
                    "Ask who must sign it and what format is acceptable.",
                    "Only obtain it if it is required."
                ),
                source = "Relevant local authority / municipal office",
                order = 7
            )
        }

        when (glazedTiles) {
            "Not available" -> addIssue(
                title = "Wall / tile surface",
                status = "Requires compliance action",
                problem = "A suitable washable, cleanable wall surface may not currently be in place.",
                whyItMatters = "Food hygiene and inspection requirements can require a suitable wall finish depending on the authority and premises.",
                recommendedAction = "Check the current hygiene requirement, install the compliant surface if required, and keep it ready for inspection.",
                steps = listOf(
                    "Confirm the applicable hygiene requirement with the authority.",
                    "Install a suitable washable or cleanable surface if required.",
                    "Keep the surface maintained and inspection-ready."
                ),
                source = "Food safety / health authority / local authority",
                order = 8
            )
            "I don't know" -> addIssue(
                title = "Wall / tile surface unknown",
                status = "Verification required",
                problem = "The wall-surface requirement is not yet confirmed.",
                whyItMatters = "The requirement can vary by premises and authority.",
                recommendedAction = "Confirm the applicable inspection requirement before making or upgrading the wall finish.",
                steps = listOf(
                    "Ask the authority what wall finish is required.",
                    "Check whether a washable or cleanable surface is required.",
                    "Proceed only after the requirement is confirmed."
                ),
                source = "Food safety / health authority / local authority",
                order = 8
            )
        }

        when (greaseTrap) {
            "Not available" -> addIssue(
                title = "Grease trap / drainage",
                status = "Requires compliance action",
                problem = "The required grease trap or drainage arrangement may be missing.",
                whyItMatters = "Wastewater and grease handling may create sanitation or drainage issues depending on the authority and premises.",
                recommendedAction = "Verify the current sanitation requirement, install the required system if needed, and maintain it before inspection.",
                steps = listOf(
                    "Confirm the current sanitation requirement.",
                    "Determine whether a grease trap or other arrangement is needed.",
                    "Install or upgrade the system if required.",
                    "Keep proof of the installation and maintenance records."
                ),
                source = "Sanitation / health authority / local authority",
                order = 9
            )
            "I don't know" -> addIssue(
                title = "Grease trap / drainage unknown",
                status = "Verification required",
                problem = "The grease-trap or drainage requirement is not yet confirmed.",
                whyItMatters = "The requirement depends on the applicable sanitation rule and premises.",
                recommendedAction = "Confirm the applicable requirement before installing or modifying drainage infrastructure.",
                steps = listOf(
                    "Ask the relevant authority what drainage or grease treatment is required.",
                    "Check whether a grease trap or other arrangement is needed.",
                    "Only proceed after the local requirement is confirmed."
                ),
                source = "Sanitation / health authority / local authority",
                order = 9
            )
        }

        when (blackGlass) {
            "Not available" -> addIssue(
                title = "Front screening / glass",
                status = "Requires verification",
                problem = "The required front screening or glass arrangement may not be in place.",
                whyItMatters = "Screening or visibility requirements can depend on the authority and exact premises.",
                recommendedAction = "Verify whether screening is required for the premises, obtain the current specification, and install the compliant solution if required.",
                steps = listOf(
                    "Confirm whether front screening is required for this premises.",
                    "Ask the authority for the current specification.",
                    "Install the compliant arrangement if required."
                ),
                source = "Health / licensing authority",
                order = 10
            )
            "I don't know" -> addIssue(
                title = "Front screening / glass unknown",
                status = "Verification required",
                problem = "The screening requirement is not yet confirmed.",
                whyItMatters = "Some authorities may require front screening while others may not depending on the premises and local rule.",
                recommendedAction = "Confirm the current requirement before installing or modifying front screening.",
                steps = listOf(
                    "Ask the authority whether screening is required.",
                    "Check the current specification.",
                    "Proceed only if the requirement is confirmed."
                ),
                source = "Health / licensing authority",
                order = 10
            )
        }

        val sortedIssues = issues.sortedBy { it.order }

        if (sortedIssues.isEmpty()) {
            statusText.text = "No major issues were identified from the information provided."
            appMessageText.text = "No major issues were identified from the information provided.\n\nVerify the current official requirements before applying, as requirements can vary by location and premises.\n\nImportant:\nThis information is general guidance based on publicly available internet and government information. Rules, procedures, fees, documents and local requirements can change and may differ depending on the location and premises.\n\nThis information may not be complete or accurate for every situation and should not be treated as legal or official approval guidance.\n\nBefore taking action or applying, confirm the current requirements with the relevant government/local authority or a qualified professional."
            probabilityText.text = ""
            finesText.text = ""
            recommendedActionText.text = ""
            return
        }

        val displayText = buildString {
            sortedIssues.forEachIndexed { index, issue ->
                append(index + 1).append(". ").append(issue.title).append("\n")
                append("Status: ").append(issue.status).append("\n")
                append("Problem: ").append(issue.problem).append("\n")
                append("Why it may matter: ").append(issue.whyItMatters).append("\n")
                append("What to do: ").append(issue.recommendedAction).append("\n")
                append("Steps:\n")
                issue.steps.forEachIndexed { stepIndex, step ->
                    append(stepIndex + 1).append(". ").append(step).append("\n")
                }
                append("Authority / document / verification: ").append(issue.source).append("\n\n")
            }
            append("Important:\n")
            append("This information is general guidance based on publicly available internet and government information. Rules, procedures, fees, documents and local requirements can change and may differ depending on the location and premises.\n\n")
            append("This information may not be complete or accurate for every situation and should not be treated as legal or official approval guidance.\n\n")
            append("Before taking action or applying, confirm the current requirements with the relevant government/local authority or a qualified professional.")
        }

        statusText.text = "Business compliance review"
        appMessageText.text = displayText
        probabilityText.text = "This assessment evaluates each available factor independently and aggregates all applicable problems and unknowns."
        finesText.text = ""
        recommendedActionText.text = "Resolve each issue with the relevant authority and keep supporting documents or written confirmations ready."
    }
}
