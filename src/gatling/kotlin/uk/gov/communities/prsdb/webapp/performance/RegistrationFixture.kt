package uk.gov.communities.prsdb.webapp.performance

data class RegistrationFixture(
    val subject: String,
    val address: String,
    val postcode: String,
    val building: String,
    val localCouncilId: Int,
    val uprn: Long,
)
