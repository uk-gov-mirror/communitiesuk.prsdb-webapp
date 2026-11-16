package uk.gov.communities.prsdb.webapp.performance

import java.time.LocalDate
import java.time.ZoneId

enum class RegistrationVariant(
    val manual: Boolean,
    val occupied: Boolean,
    val licensing: String,
    val gas: String,
    val electrical: String,
    val epc: String,
) {
    VACANT_MANUAL_EXEMPT(true, false, "NO_LICENSING", "none", "missing", "exempt"),
    OCCUPIED_SELECTED_CERTIFICATES(false, true, "SELECTIVE_LICENCE", "upload", "eicr-upload", "valid"),
}

object ComplianceJourney {
    fun certificateNumber(
        fixture: RegistrationFixture,
        variant: RegistrationVariant,
    ): String {
        require(fixture.uprn < 1_000_000_000_000_000_000L) { "Fixture UPRN exceeds the local EPC contract" }
        return ((if (variant.manual) "1" else "0") + fixture.uprn.toString().padStart(18, '0') + "1").chunked(4).joinToString("-")
    }

    fun fields(
        step: String,
        variant: RegistrationVariant,
        fixture: RegistrationFixture?,
    ): Map<String, String>? {
        fun date(value: LocalDate) =
            mapOf("day" to value.dayOfMonth.toString(), "month" to value.monthValue.toString(), "year" to value.year.toString())
        val today = LocalDate.now(ZoneId.of("Europe/London"))
        return when (step) {
            "has-gas-supply" -> mapOf("hasGasSupply" to (variant.gas != "none").toString())
            "has-gas-safety" -> mapOf("hasCert" to (variant.gas != "missing").toString())
            "gas-safety-certificate-issue-date" ->
                date(today.minusMonths(1))
            "has-electrical-safety" ->
                mapOf("electricalCertType" to if (variant.electrical == "missing") "NO_CERTIFICATE" else "HAS_EICR")
            "eicr-expiry-date" -> date(today.plusYears(2))
            "check-epc-details" -> mapOf("matchedEpcIsCorrect" to "false")
            "has-epc" -> mapOf("hasCert" to (variant.epc == "valid").toString())
            "find-your-epc" ->
                mapOf(
                    "certificateNumber" to certificateNumber(requireNotNull(fixture) { "EPC lookup requires a fixture" }, variant),
                )
            "confirm-epc-details" -> mapOf("matchedEpcIsCorrect" to "true")
            "is-epc-required" -> mapOf("epcRequired" to (variant.epc != "exempt").toString())
            "epc-exemption" -> mapOf("exemptionReason" to "ANNUAL_USE_LESS_THAN_4_MONTHS")
            "electrical-safety-certificate-missing",
            "check-gas-safety-answers", "check-electrical-safety-answers", "check-epc-answers",
            "check-gas-safety-certificate-uploads", "check-eicr-uploads",
            -> emptyMap()
            else -> null
        }
    }
}
