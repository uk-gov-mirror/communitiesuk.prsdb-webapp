package uk.gov.communities.prsdb.webapp.models.viewModels.emailModels

data class PropertyDeactivationConfirmationEmail(
    val recipientName: String,
    val multiLineAddress: String,
    val registrationNumber: String,
    val landlordDeactivated: Boolean,
    val dashboardUrl: String,
) : EmailTemplateModel {
    private val recipientNameKey = "recipient name"
    private val addressKey = "property address"
    private val registrationNumberKey = "prn"
    private val landlordDeactivatedKey = "landlord deactivated"
    private val dashboardUrlKey = "dashboard url"

    override val template = EmailTemplate.PROPERTY_DEACTIVATION_CONFIRMATION

    override fun toHashMap(): HashMap<String, String> =
        hashMapOf(
            recipientNameKey to recipientName,
            addressKey to multiLineAddress,
            registrationNumberKey to registrationNumber,
            landlordDeactivatedKey to landlordDeactivated.toNotifyYesNo(),
            dashboardUrlKey to dashboardUrl,
        )
}
