package com.example.store.compliance

import android.content.Context
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.store.LocaleHelper
import com.example.store.R

class FssaiActivity : AppCompatActivity() {

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.wrapEnglishContext(newBase))
    }

    private lateinit var statusText: TextView
    private lateinit var detailsText: TextView

    private val prefs by lazy {
        getSharedPreferences("profile_prefs", MODE_PRIVATE)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_fssai)

        findViewById<TextView>(R.id.complianceDisclaimerText)?.text =
            LocaleHelper.wrapContext(this).getString(R.string.compliance_ai_disclaimer)

        statusText = findViewById(R.id.fssaiStatusText)
        detailsText = findViewById(R.id.fssaiDetailsText)

        showFssaiInformation()
    }

    private fun showFssaiInformation() {

        val status = prefs.getString("fssai_status", "")
            ?.trim()
            .orEmpty()

        when (status) {

            "I have FSSAI" -> {
                showExistingFssai()
            }

            "I don't have FSSAI" -> {
                showFssaiAssessment()
            }

            "unknown",
            "I don't know" -> {
                showUnknownFssaiStatus()
            }

            else -> {
                showNoInformation()
            }
        }
    }

    private fun showExistingFssai() {

        statusText.text = "FSSAI registration/license"

        val number = prefs.getString("fssai_number", "")
            ?.trim()
            .orEmpty()

        val type = prefs.getString("fssai_type", "")
            ?.trim()
            .orEmpty()

        val expiry = prefs.getString("fssai_expiry", "")
            ?.trim()
            .orEmpty()

        detailsText.text = buildString {

            append(
                "Your saved information indicates that you currently have FSSAI registration/license."
            )

            append("\n\n")

            if (number.isNotEmpty()) {
                append("FSSAI number: $number\n")
            }

            if (type.isNotEmpty()) {
                append("Type/category: $type\n")
            }

            if (expiry.isNotEmpty()) {
                append("Validity/expiry: $expiry\n")
            }

            if (
                number.isEmpty() &&
                type.isEmpty() &&
                expiry.isEmpty()
            ) {
                append(
                    "No additional FSSAI details were saved."
                )
            }

            append("\n")

            append(
                "Keep your official FSSAI registration/license proof and current business details safely."
            )

            append("\n\n")

            append(
                "If your FSSAI status, business activity or other details change, update them in Add Information."
            )

            appendDisclaimer()
        }
    }

    private fun showFssaiAssessment() {

        statusText.text = "FSSAI requirements"

        val problems = mutableListOf<String>()
        val unknowns = mutableListOf<String>()

        when (
            prefs.getString("fssai_business_type", "")
                ?.trim()
                .orEmpty()
        ) {

            "I don't know",
            "" -> {
                unknowns.add(
                    buildUnknown(
                        title = "Business activity",
                        possibleOutcomes = """
                            • If the business is only retail sale of chicken/meat, the applicable FSSAI category may differ from a business that also slaughters, processes or supplies food.
                            • If slaughtering, processing, marinating or wholesale/supply is also carried out, additional requirements or a different category may apply.
                        """.trimIndent(),
                        action = "Confirm exactly what food activities are carried out at the premises and update Add Information."
                    )
                )
            }
        }

        when (
            prefs.getString("fssai_turnover", "")
                ?.trim()
                .orEmpty()
        ) {

            "Up to ₹1.5 crore" -> {
                // No problem shown.
            }

            "Above ₹1.5 crore to ₹50 crore" -> {
                // No problem shown.
            }

            "Above ₹50 crore" -> {
                // No problem shown.
            }

            "I don't know",
            "" -> {
                unknowns.add(
                    buildUnknown(
                        title = "Annual turnover",
                        possibleOutcomes = """
                            • Up to ₹1.5 crore: Registration may apply depending on the applicable Kind of Business.
                            • Above ₹1.5 crore up to ₹50 crore: State Licence may apply depending on the applicable Kind of Business.
                            • Above ₹50 crore: Central Licence may apply depending on the applicable Kind of Business.
                        """.trimIndent(),
                        action = "Check the business's actual annual turnover from reliable financial records and confirm the applicable category on FoSCoS."
                    )
                )
            }
        }

        when (
            prefs.getString("fssai_potable_water", "")
                ?.trim()
                .orEmpty()
        ) {

            "No" -> {
                problems.add(
                    buildProblem(
                        title = "Potable water",
                        problem = "Suitable potable water is not available for food handling/cleaning.",
                        why = "Water used in food handling and cleaning must be suitable for its intended use.",
                        solution = "Provide an appropriate potable water supply and ensure it is maintained hygienically.",
                        steps = """
                            1. Identify the water source used for food handling and cleaning.
                            2. Arrange a suitable potable water source.
                            3. Keep the storage/container system clean and protected from contamination.
                            4. Maintain relevant water-quality records or testing where applicable.
                        """.trimIndent()
                    )
                )
            }

            "I don't know",
            "" -> {
                unknowns.add(
                    buildUnknown(
                        title = "Potable water",
                        possibleOutcomes = """
                            • If the water used for food handling and cleaning is potable and suitable, this factor may be satisfactory.
                            • If it is not suitable, corrective action may be required before operating compliantly.
                        """.trimIndent(),
                        action = "Confirm the water source and its suitability for food handling and cleaning."
                    )
                )
            }
        }

        when (
            prefs.getString("fssai_food_surface", "")
                ?.trim()
                .orEmpty()
        ) {

            "No" -> {
                problems.add(
                    buildProblem(
                        title = "Food-contact/work surfaces",
                        problem = "Food-contact or work surfaces are reported as unsuitable or not adequately cleanable.",
                        why = "Food-contact areas should be suitable for hygienic food handling and capable of being effectively cleaned.",
                        solution = "Replace or improve unsuitable food-contact/work surfaces with appropriate cleanable, hygienic surfaces.",
                        steps = """
                            1. Identify surfaces that directly contact meat or are used for preparation.
                            2. Remove or repair damaged, porous or difficult-to-clean surfaces.
                            3. Install suitable food-contact/work surfaces.
                            4. Establish routine cleaning and sanitation.
                        """.trimIndent()
                    )
                )
            }

            "I don't know",
            "" -> {
                unknowns.add(
                    buildUnknown(
                        title = "Food-contact/work surfaces",
                        possibleOutcomes = """
                            • If the work surfaces are suitable, cleanable and maintained hygienically, this factor may be satisfactory.
                            • If they are porous, damaged, difficult to clean or otherwise unsuitable, corrective work may be required.
                        """.trimIndent(),
                        action = "Inspect the cutting, preparation and other food-contact surfaces and confirm their condition and cleanability."
                    )
                )
            }
        }
        when (
            prefs.getString("fssai_floor_wall", "")
                ?.trim()
                .orEmpty()
        ) {

            "No" -> {
                problems.add(
                    buildProblem(
                        title = "Floors/walls and relevant surfaces",
                        problem = "Floors, walls or relevant surfaces are reported as unsuitable or inadequately maintained.",
                        why = "Food premises need surfaces that can be maintained and cleaned effectively to support hygienic operations.",
                        solution = "Repair or replace unsuitable surfaces and maintain them in a cleanable condition.",
                        steps = """
                            1. Inspect floors, walls and other relevant surfaces.
                            2. Repair cracks, damage or areas that retain dirt or moisture.
                            3. Use suitable cleanable/impermeable finishes where required by the applicable standard.
                            4. Maintain a regular cleaning schedule.
                        """.trimIndent()
                    )
                )
            }

            "I don't know",
            "" -> {
                unknowns.add(
                    buildUnknown(
                        title = "Floors/walls and relevant surfaces",
                        possibleOutcomes = """
                            • If the relevant surfaces are suitably cleanable and maintained, this factor may be satisfactory.
                            • If surfaces are damaged, porous, difficult to clean or poorly maintained, corrective action may be required.
                        """.trimIndent(),
                        action = "Inspect the floors, walls and relevant surfaces used around food handling."
                    )
                )
            }
        }

        when (
            prefs.getString("fssai_waste_handling", "")
                ?.trim()
                .orEmpty()
        ) {

            "No" -> {
                problems.add(
                    buildProblem(
                        title = "Meat-waste handling",
                        problem = "A suitable meat-waste handling/disposal arrangement is not available.",
                        why = "Food waste and meat waste need to be handled so that they do not create contamination, pest or hygiene problems.",
                        solution = "Establish a suitable waste collection, storage and disposal arrangement in accordance with applicable local requirements.",
                        steps = """
                            1. Separate meat waste from usable food.
                            2. Use suitable covered containers where appropriate.
                            3. Prevent waste accumulation around food-handling areas.
                            4. Arrange disposal through the applicable authorised/local system.
                            5. Clean and sanitise waste-handling areas regularly.
                        """.trimIndent()
                    )
                )
            }

            "I don't know",
            "" -> {
                unknowns.add(
                    buildUnknown(
                        title = "Meat-waste handling",
                        possibleOutcomes = """
                            • If meat waste is properly collected, stored and disposed of without creating contamination or pest problems, this factor may be satisfactory.
                            • If waste is dumped, accumulated or handled unsafely, corrective action may be required.
                        """.trimIndent(),
                        action = "Inspect how meat waste is collected, stored and finally disposed of."
                    )
                )
            }
        }

        when (
            prefs.getString("fssai_drainage", "")
                ?.trim()
                .orEmpty()
        ) {

            "No" -> {
                problems.add(
                    buildProblem(
                        title = "Drainage/wastewater",
                        problem = "Suitable drainage or wastewater management is not available.",
                        why = "Poor drainage can cause standing water, contamination and hygiene problems in food premises.",
                        solution = "Provide or correct suitable drainage and wastewater management for the operation.",
                        steps = """
                            1. Identify all wastewater discharge points.
                            2. Remove blockages and standing-water problems.
                            3. Provide suitable drainage where required.
                            4. Ensure wastewater does not contaminate food-handling areas.
                            5. Maintain drains and cleaning arrangements.
                        """.trimIndent()
                    )
                )
            }

            "I don't know",
            "" -> {
                unknowns.add(
                    buildUnknown(
                        title = "Drainage/wastewater",
                        possibleOutcomes = """
                            • If wastewater is safely drained and does not create contamination or standing-water problems, this factor may be satisfactory.
                            • If wastewater accumulates, backs up or creates contamination risks, corrective work may be required.
                        """.trimIndent(),
                        action = "Inspect the drainage system during normal cleaning and food-handling operations."
                    )
                )
            }
        }

        when (
            prefs.getString("fssai_pest_control", "")
                ?.trim()
                .orEmpty()
        ) {

            "No" -> {
                problems.add(
                    buildProblem(
                        title = "Pest/insect protection",
                        problem = "Adequate pest or insect protection is not available.",
                        why = "Food premises should be protected from insects, pests and other contamination sources.",
                        solution = "Improve physical pest protection and establish appropriate pest-control practices.",
                        steps = """
                            1. Identify openings through which insects or pests can enter.
                            2. Install suitable screens/barriers where required.
                            3. Keep doors and openings managed to reduce pest entry.
                            4. Keep the premises clean and remove food/waste sources that attract pests.
                            5. Use appropriate pest-control measures where necessary.
                        """.trimIndent()
                    )
                )
            }

            "I don't know",
            "" -> {
                unknowns.add(
                    buildUnknown(
                        title = "Pest/insect protection",
                        possibleOutcomes = """
                            • If the premises are effectively protected from insects and pests, this factor may be satisfactory.
                            • If insects/pests can enter freely or contamination is occurring, corrective action may be required.
                        """.trimIndent(),
                        action = "Inspect doors, windows, vents, openings, waste areas and other possible pest-entry points."
                    )
                )
            }
        }

        val businessType =
            prefs.getString("fssai_business_type", "")
                ?.trim()
                .orEmpty()

        val slaughterValue =
            prefs.getString("fssai_slaughter_hygiene", "")
                ?.trim()
                .orEmpty()

        val slaughterRelevant =
            businessType == "Slaughter/dressing chicken at the premises" ||
                    businessType == "More than one"

        if (slaughterRelevant) {

            when (slaughterValue) {

                "No" -> {
                    problems.add(
                        buildProblem(
                            title = "Slaughter/dressing hygiene",
                            problem = "The slaughter/dressing arrangement is reported as not hygienically suitable.",
                            why = "Slaughter and dressing operations require appropriate hygienic arrangements to reduce contamination risks.",
                            solution = "Correct the slaughter/dressing area and operating practices according to the applicable requirements.",
                            steps = """
                                1. Separate slaughter/dressing activities from areas where contamination could affect finished food.
                                2. Ensure appropriate cleanable surfaces and equipment.
                                3. Provide suitable drainage and cleaning arrangements.
                                4. Control insects and other contamination sources.
                                5. Follow applicable slaughter/meat-handling hygiene requirements.
                            """.trimIndent()
                        )
                    )
                }

                "I don't know",
                "" -> {
                    unknowns.add(
                        buildUnknown(
                            title = "Slaughter/dressing hygiene",
                            possibleOutcomes = """
                                • If slaughter/dressing is carried out in a suitable hygienic arrangement, this factor may be satisfactory.
                                • If the arrangement does not meet applicable hygiene requirements, corrective action may be required.
                            """.trimIndent(),
                            action = "Confirm whether slaughter/dressing is performed at the premises and inspect the arrangement against the applicable requirements."
                        )
                    )
                }
            }
        }

        when (
            prefs.getString("fssai_storage_arrangement", "")
                ?.trim()
                .orEmpty()
        ) {

            "No" -> {
                problems.add(
                    buildProblem(
                        title = "Meat handling/storage arrangement",
                        problem = "The current meat handling/storage arrangement is reported as unsuitable.",
                        why = "Food must be handled and stored in a way that prevents contamination and maintains appropriate food safety conditions.",
                        solution = "Correct the storage and handling arrangement according to the actual operation and applicable requirements.",
                        steps = """
                            1. Identify where raw and prepared food are stored.
                            2. Separate food from contamination sources.
                            3. Prevent cross-contamination during handling.
                            4. Maintain suitable storage conditions for the particular operation.
                            5. Clean and maintain storage areas regularly.
                        """.trimIndent()
                    )
                )
            }

            "I don't know",
            "" -> {
                unknowns.add(
                    buildUnknown(
                        title = "Meat handling/storage",
                        possibleOutcomes = """
                            • If meat is handled and stored in a way that prevents contamination and is appropriate for the operation, this factor may be satisfactory.
                            • If storage or handling allows contamination or otherwise fails applicable requirements, corrective action may be required.
                        """.trimIndent(),
                        action = "Review how raw meat is received, handled, stored and protected from contamination."
                    )
                )
            }
        }
        when (
            prefs.getString("fssai_noc_status", "")
                ?.trim()
                .orEmpty()
        ) {

            "No/not available" -> {
                problems.add(
                    buildProblem(
                        title = "Local authority/NOC compliance",
                        problem = "The required local authority/NOC arrangement is reported as unavailable.",
                        why = "Food businesses may also need to comply with applicable local-body permissions or other requirements in addition to FSSAI.",
                        solution = "Confirm which local permissions/NOCs actually apply to the premises and obtain the required approvals before relying on the business as compliant.",
                        steps = """
                            1. Identify the local authority having jurisdiction over the premises.
                            2. Confirm which trade licence, NOC or other local permission applies to this business.
                            3. Collect the documents required by that authority.
                            4. Apply through the applicable official process.
                            5. Complete inspection/verification if required.
                            6. Keep the approval/acknowledgement safely.
                        """.trimIndent()
                    )
                )
            }

            "I don't know",
            "" -> {
                unknowns.add(
                    buildUnknown(
                        title = "Local authority/NOC",
                        possibleOutcomes = """
                            • If no additional local permission/NOC is applicable, this factor may not create an additional requirement.
                            • If a local permission/NOC is applicable and has not been obtained, additional compliance action may be required.
                        """.trimIndent(),
                        action = "Confirm with the appropriate local authority which permissions/NOCs apply to this premises and business activity."
                    )
                )
            }
        }

        val result = buildString {

            if (problems.isEmpty() && unknowns.isEmpty()) {

                append(
                    "No major FSSAI-related issues were identified from the information provided."
                )

                append("\n\n")

                append(
                    "The information provided appears generally favorable for the factors checked. Confirm the applicable FSSAI category and current official requirements before applying."
                )

            } else {

                if (problems.isNotEmpty()) {

                    append("Problems identified\n\n")

                    problems.forEachIndexed { index, problem ->

                        append(problem)

                        if (index < problems.lastIndex) {
                            append("\n\n")
                        }
                    }
                }

                if (unknowns.isNotEmpty()) {

                    if (problems.isNotEmpty()) {
                        append("\n\n")
                    }

                    append(
                        "Information that needs verification\n\n"
                    )

                    unknowns.forEachIndexed { index, unknown ->

                        append(unknown)

                        if (index < unknowns.lastIndex) {
                            append("\n\n")
                        }
                    }
                }

                append("\n\n")

                append(
                    "Next step: update any incorrect or unknown information in Add Information after verification."
                )
            }

            append("\n\n")

            append(
                "How to apply: If FSSAI registration/licence is required for your business, use the official FoSCoS process and confirm the applicable Kind of Business, category, documents and fee before submitting."
            )

            appendDisclaimer()
        }

        detailsText.text = result
    }

    private fun showUnknownFssaiStatus() {

        statusText.text = "FSSAI status is unknown"

        detailsText.text = buildString {

            append(
                "Your FSSAI status is currently marked as unknown."
            )

            append("\n\n")

            append(
                "Do not assume that an FSSAI registration/licence exists or that it is not required."
            )

            append("\n\n")

            append("What to check:\n\n")

            append(
                "1. Check your existing FSSAI registration/licence documents."
            )

            append("\n")

            append(
                "2. Check the official FSSAI/FoSCoS records where applicable."
            )

            append("\n")

            append(
                "3. Confirm the actual food business activity carried out at the premises."
            )

            append("\n")

            append(
                "4. Confirm the applicable turnover/category under the current rules."
            )

            append("\n\n")

            append(
                "After confirming the status, update it in Add Information."
            )

            appendDisclaimer()
        }
    }

    private fun showNoInformation() {

        statusText.text = "FSSAI information not provided"

        detailsText.text = buildString {

            append(
                "No FSSAI information has been saved yet."
            )

            append("\n\n")

            append(
                "Open Add Information and save your current FSSAI status. If you do not have FSSAI, complete the FSSAI information there so the app can identify possible issues and information that still needs verification."
            )

            appendDisclaimer()
        }
    }

    private fun buildProblem(
        title: String,
        problem: String,
        why: String,
        solution: String,
        steps: String
    ): String {

        return buildString {

            append(title)
            append("\n\n")

            append("Problem: ")
            append(problem)

            append("\n\n")

            append("Why it matters: ")
            append(why)

            append("\n\n")

            append("Solution: ")
            append(solution)

            append("\n\n")

            append("Steps:\n")
            append(steps)
        }
    }

    private fun buildUnknown(
        title: String,
        possibleOutcomes: String,
        action: String
    ): String {

        return buildString {

            append(title)
            append("\n\n")

            append("This information is unknown.\n\n")

            append("Possible outcomes:\n")
            append(possibleOutcomes)

            append("\n\n")

            append("What to do: ")
            append(action)
        }
    }

    private fun StringBuilder.appendDisclaimer() {

        append("\n\n")

        append(
            "Information disclaimer: This information is based on publicly available government/internet information and is intended as general guidance only. Requirements, procedures, documents, fees and applicable conditions can change and may differ depending on the business activity, premises and location. This is not official or legal advice. Confirm the current requirements with FSSAI/FoSCoS and the appropriate local government authority before relying on this information."
        )
    }
}