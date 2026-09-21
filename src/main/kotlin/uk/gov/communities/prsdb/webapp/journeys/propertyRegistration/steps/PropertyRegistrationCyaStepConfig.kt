package uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps

import org.springframework.context.MessageSource
import uk.gov.communities.prsdb.webapp.annotations.webAnnotations.JourneyFrameworkComponent
import uk.gov.communities.prsdb.webapp.config.managers.FeatureFlagManager
import uk.gov.communities.prsdb.webapp.constants.CORRESPONDENCE_ADDRESS
import uk.gov.communities.prsdb.webapp.constants.DELEGATE_TO_LETTING_AGENT
import uk.gov.communities.prsdb.webapp.constants.PROPERTY_REGISTRATION_RESTRUCTURE_AND_SKIPPING
import uk.gov.communities.prsdb.webapp.constants.enums.LicensingType
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
    // TODO PDJB-1340: Remove the legacy template branch when PROPERTY_REGISTRATION_RESTRUCTURE_AND_SKIPPING is removed.
    override fun chooseTemplate(state: PropertyRegistrationJourneyState): String =
        if (featureFlagManager.checkFeature(PROPERTY_REGISTRATION_RESTRUCTURE_AND_SKIPPING)) {
            "forms/restructureAndSkipping/propertyRegistrationCheckAnswersForm"
        } else {
            "forms/restructureAndSkipping/propertyRegistrationCheckAnswersFormLegacy"
        }

    override fun getStepSpecificContent(state: PropertyRegistrationJourneyState): Map<String, Any?> {
        val isRestructureAndSkippingEnabled =
            featureFlagManager.checkFeature(PROPERTY_REGISTRATION_RESTRUCTURE_AND_SKIPPING)
        if (!isRestructureAndSkippingEnabled) {
            return getLegacyContent(state)
        }

        val isLettingAgentEnabled = featureFlagManager.checkFeature(DELEGATE_TO_LETTING_AGENT)
        if (!isLettingAgentEnabled) {
            return getRestructuredContent(state)
        }

        return if (state.isDelegatedToLettingAgent(featureFlagManager)) {
            getDelegatedRestructuredContent(state)
        } else {
            getLettingAgentRestructuredContent(state)
        }
    }

    private fun getLegacyContent(state: PropertyRegistrationJourneyState): Map<String, Any?> {
        val isOccupied = state.occupied.formModel.notNullValue(OccupancyFormModel::occupied)
        return getBaseContent(
            state = state,
            submitButtonText = "forms.buttons.completeRegistration",
            warningTextKey = "forms.warning",
            insetText = true,
            propertyDetails = getPropertyDetailsSummaryList(state),
            licensingDetails = getLicensingDetailsForState(state, isOccupied, false),
            occupancyDetails = null,
            tenancyDetails = getTenancyDetails(state),
        ) + getComplianceContent(state)
    }

    private fun getRestructuredContent(state: PropertyRegistrationJourneyState): Map<String, Any?> {
        return getRestructuredContent(state, emptyMap())
    }

    private fun getLettingAgentRestructuredContent(state: PropertyRegistrationJourneyState): Map<String, Any?> {
        val whoProvides = state.whoProvidesDetailsTask.whoProvidesRentalDetailsStep.formModelIfReachableOrNull?.whoProvides
        val delegationContent = whoProvides?.let { getLettingAgentDelegationSummaryContent(state, it) } ?: emptyMap()
        return getRestructuredContent(state, delegationContent) +
            mapOf(
                "showLettingAgentDelegationUnoccupiedPanel" to
                    (!state.occupied.formModel.notNullValue(OccupancyFormModel::occupied) && whoProvides == null),
            )
    }

    private fun getRestructuredContent(
        state: PropertyRegistrationJourneyState,
        delegationContent: Map<String, Any?>,
    ): Map<String, Any?> {
        val isOccupied = state.occupied.formModel.notNullValue(OccupancyFormModel::occupied)
        val licensingDetails = getLicensingDetailsForState(state, isOccupied, true)
        val tenancyDetails = getRestructuredTenancyDetails(state)
        val occupancyDetails = occupancyDetailsHelper.getRestructuredOccupancySummaryList(state)
        val gasSafetyContent = complianceDetailsHelper.getGasSafetyCyaContent(state, state.gasSafetyTask)
        val electricalSafetyContent = complianceDetailsHelper.getElectricalSafetyCyaContent(state, state.electricalSafetyTask)
        val complianceContent =
            gasSafetyContent +
                electricalSafetyContent +
                complianceDetailsHelper.getEpcCyaContent(state, state.epcTask)
        return getRestructuredBaseContent(state, licensingDetails, tenancyDetails, occupancyDetails) +
            delegationContent +
            complianceContent +
            getRestructuredContentSections(
                state,
                isOccupied,
                licensingDetails,
                tenancyDetails,
                occupancyDetails,
            )
    }

    private fun getDelegatedRestructuredContent(state: PropertyRegistrationJourneyState): Map<String, Any?> {
        val isOccupied = state.occupied.formModel.notNullValue(OccupancyFormModel::occupied)
        val occupancyDetails = occupancyDetailsHelper.getRestructuredOccupancySummaryList(state)
        val whoProvides =
            state.whoProvidesDetailsTask.whoProvidesRentalDetailsStep.formModelIfReachableOrNull?.whoProvides
        return getBaseContent(
            state = state,
            submitButtonText = "forms.buttons.completeRegistration",
            warningTextKey = "forms.checkPropertyAnswers.warning",
            insetText = false,
            propertyDetails = getRestructuredPropertyDetailsSummaryList(state),
            licensingDetails = emptyList(),
            occupancyDetails = occupancyDetails,
            tenancyDetails = emptyList(),
        ) +
            (whoProvides?.let { getLettingAgentDelegationSummaryContent(state, it) } ?: emptyMap()) +
            mapOf(
                "hideDelegatedSections" to true,
            ) +
            getRestructuredContentSections(
                state,
                isOccupied,
                licensingDetails = emptyList(),
                tenancyDetails = emptyList(),
                occupancyDetails,
            )
    }

    private fun getBaseContent(
        state: PropertyRegistrationJourneyState,
        submitButtonText: String,
        warningTextKey: String,
        insetText: Boolean,
        propertyDetails: List<SummaryListRowViewModel>,
        licensingDetails: List<SummaryListRowViewModel>,
        occupancyDetails: List<SummaryListRowViewModel>?,
        tenancyDetails: List<SummaryListRowViewModel>,
    ) = mapOf<String, Any?>(
        "title" to "registerProperty.title",
        "submitButtonText" to submitButtonText,
        "warningTextKey" to warningTextKey,
        "insetText" to insetText,
        "propertyName" to state.propertyDetailsTask.addressTask.getAddress().singleLineAddress,
        "propertyDetails" to propertyDetails,
        "licensingDetails" to licensingDetails,
        "occupancyDetails" to occupancyDetails,
        "jointLandlordsDetails" to getJointLandLordsSummaryRow(state),
        "tenancyDetails" to tenancyDetails,
    )

    private fun getRestructuredBaseContent(
        state: PropertyRegistrationJourneyState,
        licensingDetails: List<SummaryListRowViewModel>,
        tenancyDetails: List<SummaryListRowViewModel>,
        occupancyDetails: List<SummaryListRowViewModel>,
    ) = getBaseContent(
        state,
        "forms.buttons.completeRegistration",
        "forms.checkPropertyAnswers.warning",
        false,
        getRestructuredPropertyDetailsSummaryList(state),
        licensingDetails,
        occupancyDetails,
        tenancyDetails,
    )

    private fun getComplianceContent(state: PropertyRegistrationJourneyState) =
        complianceDetailsHelper.getGasSafetyCyaContent(state, state.gasSafetyTask) +
            complianceDetailsHelper.getElectricalSafetyCyaContent(state, state.electricalSafetyTask) +
            complianceDetailsHelper.getEpcCyaContent(state, state.epcTask)

    private fun getRestructuredContentSections(
        state: PropertyRegistrationJourneyState,
        isOccupied: Boolean,
        licensingDetails: List<SummaryListRowViewModel>,
        tenancyDetails: List<SummaryListRowViewModel>,
        occupancyDetails: List<SummaryListRowViewModel>,
    ): Map<String, Any?> {
        return mapOf(
            "aboutPropertyHeadingKey" to "forms.checkPropertyAnswers.aboutYourProperty.heading",
            "ownershipAndLandlordsHeadingKey" to "forms.checkPropertyAnswers.ownershipAndLandlords.heading",
            "ownershipAndLandlordsRows" to
                listOf(
                    getOwnershipTypeRow(state, "propertyDetails.propertyRecord.ownership.ownershipType"),
                    getJointLandLordsSummaryRow(state, "forms.checkPropertyAnswers.jointLandlordsDetails.jointLandlordInvitations"),
                ),
            "correspondenceRows" to
                if (featureFlagManager.checkFeature(CORRESPONDENCE_ADDRESS)) getCorrespondenceRows(state) else emptyList(),
            "rentedOutHeadingKey" to "forms.checkPropertyAnswers.rentedOut.heading",
            "rentedOutLicensingHeadingKey" to "forms.checkPropertyAnswers.rentedOut.licensing.heading",
            "rentedOutGasHeadingKey" to "checkGasSafety.heading",
            "rentedOutElectricalHeadingKey" to "checkElectricalSafety.heading",
            "rentedOutEpcHeadingKey" to "propertyCompliance.epcTask.checkEpcAnswers.heading",
            "rentedOutTenancyHeadingKey" to "forms.checkPropertyAnswers.tenancyDetails.restructureAndSkipping.heading",
            "rentedOutLicensingRows" to licensingDetails,
            "rentedOutTenancyRows" to tenancyDetails,
            "occupancyDetails" to occupancyDetails,
            "tenancyUnoccupiedBodyTextKey" to if (!isOccupied) "forms.checkPropertyAnswers.tenancyDetails.unoccupiedBodyText" else null,
        )
    }

    private fun getCorrespondenceRows(state: PropertyRegistrationJourneyState): List<SummaryListRowViewModel> {
        val emailStep = state.correspondenceTask.correspondenceEmailStep
        val email = emailStep.formModel.getEmailAddress { checkNotNull(state.loggedInLandlordEmail) }
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

    private fun getTenancyDetails(state: PropertyRegistrationJourneyState): List<SummaryListRowViewModel> =
        occupancyDetailsHelper.getCheckYourAnswersSummaryList(state, messageSource)

    private fun getRestructuredTenancyDetails(state: PropertyRegistrationJourneyState) =
        occupancyDetailsHelper.getRestructuredCheckYourAnswersSummaryList(
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
        state.ownershipAndLandlordsTask.jointLandlordsTask.hasJointLandlordsStep.formModel.notNullValue(
            HasJointLandlordsFormModel::hasJointLandlords,
        )

    private fun getJointLandLordsSummaryRow(
        state: PropertyRegistrationJourneyState,
        invitationsHeadingKey: String = "forms.checkPropertyAnswers.jointLandlordsDetails.invitations",
    ): SummaryListRowViewModel {
        val jointLandlordsTask = state.ownershipAndLandlordsTask.jointLandlordsTask
        return if (hasJointLandlords(state)) {
            SummaryListRowViewModel.forCheckYourAnswersPage(
                invitationsHeadingKey,
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
        getAddressRows(
            state,
            "forms.checkPropertyAnswers.propertyDetails.address",
            // TODO PDJB-1340: Remove this legacy address format when the restructure and skipping flag is removed.
            multiLineAddress = false,
        ) +
            getPropertyTypeRow(state) +
            getOwnershipTypeRow(state, "forms.checkPropertyAnswers.propertyDetails.ownership")

    private fun getRestructuredPropertyDetailsSummaryList(state: PropertyRegistrationJourneyState) =
        getAddressRows(
            state,
            "propertyDetails.propertyRecord.propertyDetails.address",
            multiLineAddress = true,
        ) +
            getPropertyTypeRow(state) +
            getBedroomsRow(state)

    private fun getBedroomsRow(state: PropertyRegistrationJourneyState) =
        SummaryListRowViewModel.forCheckYourAnswersPage(
            "forms.checkPropertyAnswers.propertyDetails.bedrooms",
            state.bedrooms.formModel.numberOfBedrooms,
            Destination.VisitableStep(state.bedrooms, state.getCyaJourneyId(state.bedrooms)),
        )

    private fun getAddressRows(
        state: PropertyRegistrationJourneyState,
        addressHeadingKey: String,
        multiLineAddress: Boolean,
    ) = state.propertyDetailsTask.addressTask.getAddress().let { address ->
        listOf(
            SummaryListRowViewModel.forCheckYourAnswersPage(
                addressHeadingKey,
                if (multiLineAddress) address.toMultiLineAddress().split("\n") else address.singleLineAddress,
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

    private fun getOwnershipTypeRow(
        state: PropertyRegistrationJourneyState,
        ownershipHeadingKey: String,
    ): SummaryListRowViewModel {
        val ownershipTypeStep = state.ownershipAndLandlordsTask.ownershipTypeStep
        return SummaryListRowViewModel.forCheckYourAnswersPage(
            ownershipHeadingKey,
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
        isSkippingEnabled: Boolean,
    ): List<SummaryListRowViewModel> {
        val licensingTask = state.licensingTask
        val licensingType = licensingTask.getLicensingType()

        if (isSkippingEnabled && licensingType == LicensingType.NO_LICENSING) {
            return listOf(
                SummaryListRowViewModel.forCheckYourAnswersPage(
                    "forms.checkPropertyAnswers.propertyDetails.licensingType",
                    "forms.checkPropertyAnswers.propertyDetails.noLicensing.restructureAndSkipping",
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
