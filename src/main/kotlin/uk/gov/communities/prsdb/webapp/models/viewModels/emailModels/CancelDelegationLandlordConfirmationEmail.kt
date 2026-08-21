package uk.gov.communities.prsdb.webapp.models.viewModels.emailModels

data class CancelDelegationLandlordConfirmationEmail(
    val landlordName: String,
    val propertyAddress: String,
    val lettingAgentEmail: String,
    val propertyRecordUrl: String,
) : EmailTemplateModel {
    private val landlordNameKey = "landlord name"
    private val propertyAddressKey = "property address"
    private val lettingAgentEmailKey = "letting agent email"
    private val propertyRecordUrlKey = "property record url"

    override val template = EmailTemplate.LETTING_AGENT_CANCEL_DELEGATION_LANDLORD_CONFIRMATION_EMAIL

    override fun toHashMap(): HashMap<String, String> =
        hashMapOf(
            landlordNameKey to landlordName,
            propertyAddressKey to propertyAddress,
            lettingAgentEmailKey to lettingAgentEmail,
            propertyRecordUrlKey to propertyRecordUrl,
        )
}
