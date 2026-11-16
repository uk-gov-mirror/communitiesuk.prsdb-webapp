package uk.gov.communities.prsdb.webapp.journeys.propertyDeactivation.stepConfig

import uk.gov.communities.prsdb.webapp.annotations.webAnnotations.JourneyFrameworkComponent
import uk.gov.communities.prsdb.webapp.controllers.PropertyDetailsController
import uk.gov.communities.prsdb.webapp.helpers.EmailHelper
import uk.gov.communities.prsdb.webapp.journeys.AbstractRequestableStepConfig
import uk.gov.communities.prsdb.webapp.journeys.Destination
import uk.gov.communities.prsdb.webapp.journeys.JourneyStep.RequestableStep
import uk.gov.communities.prsdb.webapp.journeys.propertyDeactivation.PropertyDeactivationJourneyState
import uk.gov.communities.prsdb.webapp.journeys.shared.Complete
import uk.gov.communities.prsdb.webapp.models.dataModels.RegistrationNumberDataModel
import uk.gov.communities.prsdb.webapp.models.requestModels.formModels.NoInputFormModel
import uk.gov.communities.prsdb.webapp.models.viewModels.emailModels.PropertyDeactivationConfirmationEmail
import uk.gov.communities.prsdb.webapp.services.AbsoluteUrlProvider
import uk.gov.communities.prsdb.webapp.services.ActiveInactiveService
import uk.gov.communities.prsdb.webapp.services.CurrentUserService
import uk.gov.communities.prsdb.webapp.services.EmailNotificationService
import uk.gov.communities.prsdb.webapp.services.PropertyOwnershipService
import uk.gov.communities.prsdb.webapp.services.UserToLandlordService

@JourneyFrameworkComponent
class ConfirmDeactivationStepConfig(
    private val propertyOwnershipService: PropertyOwnershipService,
    private val activeInactiveService: ActiveInactiveService,
    private val userToLandlordService: UserToLandlordService,
    private val currentUserService: CurrentUserService,
    private val absoluteUrlProvider: AbsoluteUrlProvider,
    private val confirmationEmailSender: EmailNotificationService<PropertyDeactivationConfirmationEmail>,
) : AbstractRequestableStepConfig<Complete, NoInputFormModel, PropertyDeactivationJourneyState>() {
    override val formModelClass = NoInputFormModel::class

    override fun getStepSpecificContent(state: PropertyDeactivationJourneyState) =
        mapOf(
            "cancelLinkUrl" to PropertyDetailsController.getPropertyDetailsPath(state.propertyOwnershipId),
            "showLandlordAccountSection" to isCurrentLandlordsLastActiveProperty(state.propertyOwnershipId),
        )

    override fun chooseTemplate(state: PropertyDeactivationJourneyState) = "forms/confirmPropertyDeactivationForm"

    override fun mode(state: PropertyDeactivationJourneyState): Complete? = getFormModelFromStateOrNull(state)?.let { Complete.COMPLETE }

    override fun afterStepDataIsAdded(state: PropertyDeactivationJourneyState) {
        val propertyOwnership = propertyOwnershipService.getPropertyOwnership(state.propertyOwnershipId)
        val landlordWillBeDeactivated = isCurrentLandlordsLastActiveProperty(state.propertyOwnershipId)
        val currentUser = currentUserService.getRegisteredUserDetails()

        activeInactiveService.deactivateProperty(state.propertyOwnershipId)

        // TODO PDJB-1274: Update emails to account for other users in the org
        EmailHelper.sendEmailAndLogFailures("the property deactivation confirmation email for property ownership ${propertyOwnership.id}") {
            confirmationEmailSender.sendEmail(
                currentUser.email,
                PropertyDeactivationConfirmationEmail(
                    recipientName = currentUser.name,
                    multiLineAddress = propertyOwnership.address.toMultiLineAddress(),
                    registrationNumber =
                        RegistrationNumberDataModel.fromRegistrationNumber(propertyOwnership.registrationNumber).toString(),
                    landlordDeactivated = landlordWillBeDeactivated,
                    dashboardUrl = absoluteUrlProvider.buildLandlordDashboardUri().toString(),
                ),
            )
        }
    }

    override fun resolveNextDestination(
        state: PropertyDeactivationJourneyState,
        defaultDestination: Destination,
    ): Destination {
        // Clean up journey state from session after all business logic completes
        state.deleteJourney()
        return defaultDestination
    }

    private fun isCurrentLandlordsLastActiveProperty(propertyOwnershipId: Long): Boolean {
        val landlord = userToLandlordService.getCurrentLandlordForUser()
        return !activeInactiveService.hasOtherActiveProperties(landlord.id, propertyOwnershipId)
    }
}

@JourneyFrameworkComponent
final class ConfirmDeactivationStep(
    stepConfig: ConfirmDeactivationStepConfig,
) : RequestableStep<Complete, NoInputFormModel, PropertyDeactivationJourneyState>(stepConfig) {
    companion object {
        const val ROUTE_SEGMENT = "confirm"
    }
}
