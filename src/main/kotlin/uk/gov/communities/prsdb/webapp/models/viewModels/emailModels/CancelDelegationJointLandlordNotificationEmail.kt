package uk.gov.communities.prsdb.webapp.models.viewModels.emailModels

data class CancelDelegationJointLandlordNotificationEmail(
    val jointLandlordName: String,
    val propertyAddress: String,
    val lettingAgentEmail: String,
    val propertyRecordUrl: String,
) : EmailTemplateModel {
    private val jointLandlordNameKey = "joint landlord name"
    private val propertyAddressKey = "property address"
    private val lettingAgentEmailKey = "letting agent email"
    private val propertyRecordUrlKey = "property record url"

    override val template = EmailTemplate.LETTING_AGENT_CANCEL_DELEGATION_JOINT_LANDLORD_NOTIFICATION_EMAIL

    override fun toHashMap(): HashMap<String, String> =
        hashMapOf(
            jointLandlordNameKey to jointLandlordName,
            propertyAddressKey to propertyAddress,
            lettingAgentEmailKey to lettingAgentEmail,
            propertyRecordUrlKey to propertyRecordUrl,
        )
}
