package uk.gov.communities.prsdb.webapp.services

import uk.gov.communities.prsdb.webapp.annotations.webAnnotations.PrsdbWebService
import uk.gov.communities.prsdb.webapp.database.entity.PropertyOwnership
import uk.gov.communities.prsdb.webapp.models.viewModels.emailModels.CancelDelegationJointLandlordNotificationEmail
import uk.gov.communities.prsdb.webapp.models.viewModels.emailModels.CancelDelegationLandlordConfirmationEmail
import uk.gov.communities.prsdb.webapp.models.viewModels.emailModels.CancelDelegationLettingAgentNotificationEmail

@PrsdbWebService
class CancelLettingAgentDelegationEmailService(
    private val userToLandlordService: UserToLandlordService,
    private val absoluteUrlProvider: AbsoluteUrlProvider,
    private val landlordConfirmationEmailService: EmailNotificationService<CancelDelegationLandlordConfirmationEmail>,
    private val jointLandlordNotificationEmailService: EmailNotificationService<CancelDelegationJointLandlordNotificationEmail>,
    private val lettingAgentNotificationEmailService: EmailNotificationService<CancelDelegationLettingAgentNotificationEmail>,
) {
    fun sendCancellationEmails(propertyOwnership: PropertyOwnership) {
        val actingLandlord = userToLandlordService.getCurrentLandlordForUser()
        val lettingAgentEmail = propertyOwnership.lettingAgentAccess?.invitedEmail ?: return
        val propertyAddress = propertyOwnership.address.toMultiLineAddress()
        val propertyRecordUrl = absoluteUrlProvider.buildPropertyDetailsUri(propertyOwnership.id).toString()

        landlordConfirmationEmailService.sendEmail(
            actingLandlord.email,
            CancelDelegationLandlordConfirmationEmail(
                landlordName = actingLandlord.name,
                propertyAddress = propertyAddress,
                lettingAgentEmail = lettingAgentEmail,
                propertyRecordUrl = propertyRecordUrl,
            ),
        )

        propertyOwnership.otherLandlordsTo(actingLandlord).forEach { jointLandlord ->
            jointLandlordNotificationEmailService.sendEmail(
                jointLandlord.email,
                CancelDelegationJointLandlordNotificationEmail(
                    jointLandlordName = jointLandlord.name,
                    propertyAddress = propertyAddress,
                    lettingAgentEmail = lettingAgentEmail,
                    propertyRecordUrl = propertyRecordUrl,
                ),
            )
        }

        lettingAgentNotificationEmailService.sendEmail(
            lettingAgentEmail,
            CancelDelegationLettingAgentNotificationEmail(
                propertyAddress = propertyAddress,
                singleLineAddress = propertyOwnership.address.singleLineAddress,
            ),
        )
    }
}
