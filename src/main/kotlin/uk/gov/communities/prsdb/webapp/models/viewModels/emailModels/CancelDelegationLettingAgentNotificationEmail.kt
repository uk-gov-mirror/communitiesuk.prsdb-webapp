package uk.gov.communities.prsdb.webapp.models.viewModels.emailModels

data class CancelDelegationLettingAgentNotificationEmail(
    val propertyAddress: String,
    val singleLineAddress: String,
) : EmailTemplateModel {
    private val propertyAddressKey = "property address"
    private val singleLineAddressKey = "single line address"

    override val template = EmailTemplate.LETTING_AGENT_CANCEL_DELEGATION_LETTING_AGENT_NOTIFICATION_EMAIL

    override fun toHashMap(): HashMap<String, String> =
        hashMapOf(
            propertyAddressKey to propertyAddress,
            singleLineAddressKey to singleLineAddress,
        )
}
