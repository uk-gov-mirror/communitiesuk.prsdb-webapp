package uk.gov.communities.prsdb.webapp.performance

import java.security.MessageDigest

internal object CertificateChecks {
    const val GAS_CARD = "Gas safety certificate"
    const val ELECTRICAL_CARD = "Electrical safety certificate"
    const val EPC_CARD = "Energy performance certificate (EPC)"

    fun hiddenFields(
        names: List<String>,
        values: List<String>,
    ): Map<String, String> {
        require(names.size == values.size && names.distinct().size == names.size) { "Ambiguous or missing hidden fields" }
        val fields = names.zip(values).toMap()
        require(!fields["_csrf"].isNullOrBlank()) { "A non-empty correlated CSRF token is required before mutation" }
        return fields
    }

    fun isUploadCheck(step: String) = step in setOf("check-gas-safety-certificate-uploads", "check-eicr-uploads")

    // Mirrors TokenCookieService: one-use token scoped to the exact upload step path, always HttpOnly and Secure.
    fun isUploadCookie(
        header: String,
        uploadPath: String,
    ): Boolean {
        val parts = header.split(';').map { it.trim() }
        val attributes = parts.drop(1).map { it.substringBefore('=').lowercase() to it.substringAfter('=', "") }
        return parts.first().matches(Regex("file-upload-cookie=[A-Za-z0-9-]+")) &&
            attributes.any { it.first == "httponly" } && attributes.any { it.first == "secure" } &&
            attributes.filter { it.first == "path" }.map { it.second } == listOf(uploadPath)
    }

    fun sha256(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }

    fun persistedCompliance(
        cards: List<String>,
        variant: RegistrationVariant,
        fixture: RegistrationFixture?,
        only: String? = null,
    ): Boolean {
        fun card(title: String) =
            cards.singleOrNull { it.contains(title) }.orEmpty().replace(Regex("<[^>]+>"), " ").replace(Regex("\\s+"), " ")

        val titles = only?.let { listOf(it) } ?: listOf(GAS_CARD, ELECTRICAL_CARD, EPC_CARD)
        return titles.all { title ->
            val text = card(title)
            text.isNotBlank() && expectedCard(title, variant, fixture).all { text.contains(it, ignoreCase = true) }
        }
    }

    private fun expectedCard(
        title: String,
        variant: RegistrationVariant,
        fixture: RegistrationFixture?,
    ): List<String> {
        if (title == GAS_CARD) {
            return when (variant.gas) {
                "none" -> listOf("Does the property have a gas supply or any gas appliances?", "No")
                "upload" -> listOf("Valid", "performance-certificate.pdf")
                else -> listOf("Do you have a gas safety certificate for this property?", "No")
            }
        }
        if (title == ELECTRICAL_CARD) {
            return when {
                variant.electrical.endsWith("upload") ->
                    listOf("Valid", "performance-certificate.pdf", if (variant.electrical.startsWith("eicr")) "(EICR)" else "(EIC)")
                !variant.occupied -> listOf("Provide this later")
                else -> listOf("None")
            }
        }
        require(title == EPC_CARD) { "Unknown compliance card $title" }

        fun number() = ComplianceJourney.certificateNumber(requireNotNull(fixture) { "EPC card checks require a fixture" }, variant)
        return when (variant.epc) {
            "valid" -> listOf(number(), "Valid")
            "exempt" -> listOf("less than 4 months a year")
            else -> listOf("Do you have an EPC for this property?", "No")
        }
    }
}
