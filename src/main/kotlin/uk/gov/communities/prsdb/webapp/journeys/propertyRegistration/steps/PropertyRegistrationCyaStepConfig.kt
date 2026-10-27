package uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps

import org.springframework.context.MessageSource
import uk.gov.communities.prsdb.webapp.annotations.webAnnotations.JourneyFrameworkComponent
import uk.gov.communities.prsdb.webapp.config.managers.FeatureFlagManager
import uk.gov.communities.prsdb.webapp.constants.ASSOCIATED_PARTIES
import uk.gov.communities.prsdb.webapp.constants.PAYMENTS
import uk.gov.communities.prsdb.webapp.constants.enums.LicensingType
import uk.gov.communities.prsdb.webapp.constants.enums.OwnershipType
import uk.gov.communities.prsdb.webapp.constants.enums.PropertyType
import uk.gov.communities.prsdb.webapp.constants.enums.WhoProvidesRentalDetails
import uk.gov.communities.prsdb.webapp.exceptions.NotNullFormModelValueIsNullException.Companion.notNullValue
import uk.gov.communities.prsdb.webapp.journeys.Destination
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.PropertyRegistrationJourneyState
import uk.gov.communities.prsdb.webapp.journeys.shared.helpers.ComplianceDetailsHelper
import uk.gov.communities.prsdb.webapp.journeys.shared.helpers.LicensingDetailsHelper
import uk.gov.communities.prsdb.webapp.journeys.shared.helpers.OccupancyDetailsHelper
import uk.gov.communities.prsdb.webapp.journeys.shared.stepConfig.AbstractCheckYourAnswersStep
import uk.gov.communities.prsdb.webapp.journeys.shared.stepConfig.AbstractCheckYourAnswersStepConfig
import uk.gov.communities.prsdb.webapp.models.requestModels.formModels.HasJointLandlordsFormModel
import uk.gov.communities.prsdb.webapp.models.requestModels.formModels.OccupancyFormModel
import uk.gov.communities.prsdb.webapp.models.viewModels.summaryModels.SummaryListRowViewModel
import uk.gov.communities.prsdb.webapp.services.LocalCouncilService

@JourneyFrameworkComponent
class PropertyRegistrationCyaStepConfig(
    private val localCouncilService: LocalCouncilService,
    private val licensingHelper: LicensingDetailsHelper,
    private val occupancyDetailsHelper: OccupancyDetailsHelper,
    private val complianceDetailsHelper: ComplianceDetailsHelper,
    private val messageSource: MessageSource,
    private val featureFlagManager: FeatureFlagManager,
) : AbstractCheckYourAnswersStepConfig<PropertyRegistrationJourneyState>() {
    override fun chooseTemplate(state: PropertyRegistrationJourneyState): String = "forms/propertyRegistrationCheckAnswersForm"

    override fun getStepSpecificContent(state: PropertyRegistrationJourneyState): Map<String, Any?> =
        if (state.isDelegatedToLettingAgent()) {
            getDelegatedContent(state)
        } else {
            getNonDelegatedContent(state)
        }

    private fun getNonDelegatedContent(state: PropertyRegistrationJourneyState): Map<String, Any?> {
        val whoProvides =
            state.whoProvidesDetailsTask.whoProvidesRentalDetailsStep.formModelIfReachableOrNull
                ?.whoProvides
        val delegationContent = whoProvides?.let { getLettingAgentDelegationSummaryContent(state, it) } ?: emptyMap()
        return getContent(state, delegationContent) +
            mapOf(
                "showLettingAgentDelegationUnoccupiedPanel" to
                    (!state.occupied.formModel.notNullValue(OccupancyFormModel::occupied) && whoProvides == null),
            )
    }

    private fun getContent(
        state: PropertyRegistrationJourneyState,
        delegationContent: Map<String, Any?>,
    ): Map<String, Any?> {
        val isOccupied = state.occupied.formModel.notNullValue(OccupancyFormModel::occupied)
        val licensingDetails = getLicensingDetailsForState(state, isOccupied)
        val tenancyDetails = getTenancyDetails(state)
        val occupancyDetails = occupancyDetailsHelper.getOccupancySummaryList(state)
        val gasSafetyContent = complianceDetailsHelper.getGasSafetyCyaContent(state, state.gasSafetyTask)
        val electricalSafetyContent = complianceDetailsHelper.getElectricalSafetyCyaContent(state, state.electricalSafetyTask)
        val complianceContent =
            gasSafetyContent +
                electricalSafetyContent +
                complianceDetailsHelper.getEpcCyaContent(state, state.epcTask)
        return getBaseContent(state, occupancyDetails) +
            delegationContent +
            complianceContent +
            getContentSections(
                state,
                isOccupied,
                licensingDetails,
                tenancyDetails,
                occupancyDetails,
            )
    }

    private fun getDelegatedContent(state: PropertyRegistrationJourneyState): Map<String, Any?> {
        val isOccupied = state.occupied.formModel.notNullValue(OccupancyFormModel::occupied)
        val occupancyDetails = occupancyDetailsHelper.getOccupancySummaryList(state)
        val whoProvides =
            state.whoProvidesDetailsTask.whoProvidesRentalDetailsStep.formModelIfReachableOrNull
                ?.whoProvides
        return getBaseContent(state, occupancyDetails) +
            (whoProvides?.let { getLettingAgentDelegationSummaryContent(state, it) } ?: emptyMap()) +
            mapOf(
                "hideDelegatedSections" to true,
            ) +
            getContentSections(
                state,
                isOccupied,
                licensingDetails = emptyList(),
                tenancyDetails = emptyList(),
                occupancyDetails,
            )
    }

    private fun getBaseContent(
        state: PropertyRegistrationJourneyState,
        occupancyDetails: List<SummaryListRowViewModel>,
    ) = mapOf<String, Any?>(
        "submitButtonText" to getSubmitButtonText(),
        "warningTextKey" to "forms.checkPropertyAnswers.warning",
        "propertyName" to
            state.propertyDetailsTask.addressTask
                .getAddress()
                .singleLineAddress,
        "propertyDetails" to getPropertyDetailsSummaryList(state),
        "occupancyDetails" to occupancyDetails,
    )

    private fun getSubmitButtonText(): String =
        if (featureFlagManager.checkFeature(PAYMENTS)) {
            "forms.buttons.submitAndPay"
        } else {
            "forms.buttons.completeRegistration"
        }

    private fun getContentSections(
        state: PropertyRegistrationJourneyState,
        isOccupied: Boolean,
        licensingDetails: List<SummaryListRowViewModel>,
        tenancyDetails: List<SummaryListRowViewModel>,
        occupancyDetails: List<SummaryListRowViewModel>,
    ): Map<String, Any?> =
        mapOf(
            "aboutPropertyHeadingKey" to "forms.checkPropertyAnswers.aboutYourProperty.heading",
            "ownershipAndLandlordsHeadingKey" to "forms.checkPropertyAnswers.ownershipAndLandlords.heading",
            "ownershipAndLandlordsRows" to
                listOf(
                    getOwnershipTypeRow(state),
                    getJointLandLordsSummaryRow(state),
                ),
            "correspondenceRows" to getCorrespondenceRows(state),
            "rentedOutHeadingKey" to "forms.checkPropertyAnswers.rentedOut.heading",
            "rentedOutLicensingHeadingKey" to "forms.checkPropertyAnswers.rentedOut.licensing.heading",
            "rentedOutGasHeadingKey" to "checkGasSafety.heading",
            "rentedOutElectricalHeadingKey" to "checkElectricalSafety.heading",
            "rentedOutEpcHeadingKey" to "propertyCompliance.epcTask.checkEpcAnswers.heading",
            "rentedOutTenancyHeadingKey" to "forms.checkPropertyAnswers.tenancyDetails.heading",
            "rentedOutLicensingRows" to licensingDetails,
            "rentedOutTenancyRows" to tenancyDetails,
            "occupancyDetails" to occupancyDetails,
            "tenancyUnoccupiedBodyTextKey" to if (!isOccupied) "forms.checkPropertyAnswers.tenancyDetails.unoccupiedBodyText" else null,
        )

    private fun getCorrespondenceRows(state: PropertyRegistrationJourneyState): List<SummaryListRowViewModel> {
        val emailStep = state.correspondenceTask.correspondenceEmailStep
        val email = emailStep.formModel.getEmailAddress { state.loggedInLandlordEmailAtStartOfJourney }
        val addressTask = state.correspondenceTask.addressTask

        return listOf(
            SummaryListRowViewModel.forCheckYourAnswersPage(
                "forms.checkPropertyAnswers.correspondence.emailAddress",
                email,
                Destination.VisitableStep(emailStep, state.getCyaJourneyId(emailStep)),
            ),
            SummaryListRowViewModel.forCheckYourAnswersPage(
                "forms.checkPropertyAnswers.correspondence.postalAddress",
                addressTask.getAddress().toMultiLineAddress().split("\n"),
                Destination.VisitableStep(addressTask.lookupAddressStep, state.getCyaJourneyId(addressTask.lookupAddressStep)),
            ),
        )
    }

    private fun getTenancyDetails(state: PropertyRegistrationJourneyState) =
        occupancyDetailsHelper.getCheckYourAnswersSummaryList(
            state,
            messageSource,
            Destination.VisitableStep(
                state.tenancyDetailsTask.householdsAndTenantsTask.households,
                state.getCyaJourneyId(state.tenancyDetailsTask.householdsAndTenantsTask.provideTenancyDetailsLaterStep),
            ),
        )

    override fun resolveNextDestination(
        state: PropertyRegistrationJourneyState,
        defaultDestination: Destination,
    ): Destination = defaultDestination

    private fun hasJointLandlords(state: PropertyRegistrationJourneyState): Boolean =
        state.getJointLandlordsTask(featureFlagManager).hasJointLandlordsStep.formModel.notNullValue(
            HasJointLandlordsFormModel::hasJointLandlords,
        )

    private fun getJointLandLordsSummaryRow(state: PropertyRegistrationJourneyState): SummaryListRowViewModel {
        val jointLandlordsTask = state.getJointLandlordsTask(featureFlagManager)
        return if (hasJointLandlords(state)) {
            SummaryListRowViewModel.forCheckYourAnswersPage(
                "forms.checkPropertyAnswers.jointLandlordsDetails.jointLandlordInvitations",
                jointLandlordsTask.inviteJointLandlordsTask.invitedJointLandlords,
                Destination.VisitableStep(
                    jointLandlordsTask.inviteJointLandlordsTask.checkJointLandlordsStep,
                    state.getCyaJourneyId(jointLandlordsTask.inviteJointLandlordsTask.checkJointLandlordsStep),
                ),
            )
        } else {
            SummaryListRowViewModel.forCheckYourAnswersPage(
                "forms.checkPropertyAnswers.jointLandlordsDetails.areThereJointLandlords",
                "forms.checkPropertyAnswers.jointLandlordsDetails.noJointLandlords",
                Destination.VisitableStep(
                    jointLandlordsTask.hasJointLandlordsStep,
                    state.getCyaJourneyId(jointLandlordsTask.hasJointLandlordsStep),
                ),
            )
        }
    }

    private fun getPropertyDetailsSummaryList(state: PropertyRegistrationJourneyState) =
        getAddressRows(state) +
            getPropertyTypeRow(state) +
            getBedroomsRow(state)

    private fun getBedroomsRow(state: PropertyRegistrationJourneyState) =
        SummaryListRowViewModel.forCheckYourAnswersPage(
            "forms.checkPropertyAnswers.propertyDetails.bedrooms",
            state.bedrooms.formModel.numberOfBedrooms,
            Destination.VisitableStep(state.bedrooms, state.getCyaJourneyId(state.bedrooms)),
        )

    private fun getAddressRows(state: PropertyRegistrationJourneyState) =
        state.propertyDetailsTask.addressTask.getAddress().let { address ->
            listOf(
                SummaryListRowViewModel.forCheckYourAnswersPage(
                    "propertyDetails.propertyRecord.propertyDetails.address",
                    address.toMultiLineAddress().split("\n"),
                    Destination.VisitableStep(
                        state.propertyDetailsTask.addressTask.lookupAddressStep,
                        state.getCyaJourneyId(state.propertyDetailsTask.addressTask.lookupAddressStep),
                    ),
                ),
                SummaryListRowViewModel.forCheckYourAnswersPage(
                    "forms.checkPropertyAnswers.propertyDetails.localCouncil",
                    localCouncilService.retrieveLocalCouncilById(address.localCouncilId!!).name,
                    Destination.VisitableStep(
                        state.propertyDetailsTask.addressTask.localCouncilStep,
                        state.getCyaJourneyId(state.propertyDetailsTask.addressTask.localCouncilStep),
                    ),
                ),
            )
        }

    private fun getPropertyTypeRow(state: PropertyRegistrationJourneyState): SummaryListRowViewModel {
        val propertyTypeStep = state.propertyDetailsTask.propertyTypeStep
        val propertyType = propertyTypeStep.formModel.propertyType
        val customType = propertyTypeStep.formModel.customPropertyType
        return SummaryListRowViewModel.forCheckYourAnswersPage(
            "forms.checkPropertyAnswers.propertyDetails.type",
            if (propertyType == PropertyType.OTHER) listOf(propertyType, customType) else propertyType,
            Destination.VisitableStep(propertyTypeStep, state.getCyaJourneyId(propertyTypeStep)),
        )
    }

    private fun getOwnershipTypeRow(state: PropertyRegistrationJourneyState): SummaryListRowViewModel {
        // TODO PDJB-2014: replace with rows for the associated parties answers
        if (featureFlagManager.checkFeature(ASSOCIATED_PARTIES)) {
            return SummaryListRowViewModel.forCheckYourAnswersPage(
                "propertyDetails.propertyRecord.ownership.ownershipType",
                OwnershipType.FREEHOLD,
                actionUrl = null as String?,
            )
        }
        val ownershipTypeStep = state.ownershipAndLandlordsTaskBeforeAssociatedParties.ownershipTypeStep
        return SummaryListRowViewModel.forCheckYourAnswersPage(
            "propertyDetails.propertyRecord.ownership.ownershipType",
            ownershipTypeStep.formModel.ownershipType,
            Destination.VisitableStep(ownershipTypeStep, state.getCyaJourneyId(ownershipTypeStep)),
        )
    }

    private fun getLettingAgentDelegationSummaryContent(
        state: PropertyRegistrationJourneyState,
        whoProvides: WhoProvidesRentalDetails,
    ): Map<String, Any?> {
        val whoWillProvideMsgKey =
            when (whoProvides) {
                WhoProvidesRentalDetails.LANDLORD -> "forms.checkPropertyAnswers.lettingAgentDelegation.values.landlord.label"
                WhoProvidesRentalDetails.LETTING_AGENT -> "forms.checkPropertyAnswers.lettingAgentDelegation.values.lettingAgent.label"
            }

        val rows =
            mutableListOf(
                SummaryListRowViewModel.forCheckYourAnswersPage(
                    "forms.checkPropertyAnswers.lettingAgentDelegation.rows.whoWillProvide.label",
                    whoWillProvideMsgKey,
                    Destination.VisitableStep(
                        state.whoProvidesDetailsTask.whoProvidesRentalDetailsStep,
                        state.getCyaJourneyId(state.whoProvidesDetailsTask.whoProvidesRentalDetailsStep),
                    ),
                ),
            )

        if (whoProvides == WhoProvidesRentalDetails.LETTING_AGENT) {
            rows +=
                SummaryListRowViewModel.forCheckYourAnswersPage(
                    "forms.checkPropertyAnswers.lettingAgentDelegation.rows.agentEmail.label",
                    state.whoProvidesDetailsTask.lettingAgentEmailStep.formModel.emailAddress,
                    Destination.VisitableStep(
                        state.whoProvidesDetailsTask.lettingAgentEmailStep,
                        state.getCyaJourneyId(state.whoProvidesDetailsTask.lettingAgentEmailStep),
                    ),
                )
        }

        return mapOf(
            "lettingAgentDelegation" to rows,
            "lettingAgentDelegationBodyText" to (whoProvides == WhoProvidesRentalDetails.LETTING_AGENT),
        )
    }

    private fun getLicensingDetailsForState(
        state: PropertyRegistrationJourneyState,
        isOccupied: Boolean,
    ): List<SummaryListRowViewModel> {
        val licensingTask = state.licensingTask
        val licensingType = licensingTask.getLicensingType()

        if (licensingType == LicensingType.NO_LICENSING) {
            return listOf(
                SummaryListRowViewModel.forCheckYourAnswersPage(
                    "forms.checkPropertyAnswers.propertyDetails.licensingType",
                    "forms.checkPropertyAnswers.propertyDetails.noLicensingRequired",
                    Destination.VisitableStep(licensingTask.licensingTypeStep, state.getCyaJourneyId(licensingTask.licensingTypeStep)),
                ),
            )
        }

        if (!isOccupied && licensingType == LicensingType.PROVIDE_LATER) {
            return listOf(
                SummaryListRowViewModel.forCheckYourAnswersPage(
                    "forms.checkPropertyAnswers.propertyDetails.licensingType",
                    "forms.checkPropertyAnswers.propertyDetails.licensingProvideLaterUnoccupied",
                    Destination.VisitableStep(licensingTask.licensingTypeStep, state.getCyaJourneyId(licensingTask.licensingTypeStep)),
                ),
            )
        }

        return licensingHelper.getCheckYourAnswersSummaryList(state, licensingTask)
    }
}

@JourneyFrameworkComponent
final class PropertyRegistrationCyaStep(
    stepConfig: PropertyRegistrationCyaStepConfig,
) : AbstractCheckYourAnswersStep<PropertyRegistrationJourneyState>(stepConfig) {
    companion object {
        const val ROUTE_SEGMENT = "check-answers"
    }
}
