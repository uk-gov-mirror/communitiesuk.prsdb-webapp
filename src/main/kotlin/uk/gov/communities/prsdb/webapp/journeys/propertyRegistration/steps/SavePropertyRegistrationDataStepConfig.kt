package uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps

import jakarta.persistence.EntityExistsException
import kotlinx.datetime.toJavaLocalDate
import uk.gov.communities.prsdb.webapp.annotations.webAnnotations.JourneyFrameworkComponent
import uk.gov.communities.prsdb.webapp.config.managers.FeatureFlagManager
import uk.gov.communities.prsdb.webapp.constants.PROPERTY_REGISTRATION_RESTRUCTURE_AND_SKIPPING
import uk.gov.communities.prsdb.webapp.constants.enums.LicensingType
import uk.gov.communities.prsdb.webapp.constants.enums.PropertyType
import uk.gov.communities.prsdb.webapp.exceptions.NotNullFormModelValueIsNullException.Companion.notNullValue
import uk.gov.communities.prsdb.webapp.journeys.AbstractInternalStepConfig
import uk.gov.communities.prsdb.webapp.journeys.Destination
import uk.gov.communities.prsdb.webapp.journeys.JourneyStep
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.PropertyRegistrationJourneyState
import uk.gov.communities.prsdb.webapp.journeys.shared.Complete
import uk.gov.communities.prsdb.webapp.journeys.shared.YesOrNo
import uk.gov.communities.prsdb.webapp.models.requestModels.formModels.NewNumberOfPeopleFormModel
import uk.gov.communities.prsdb.webapp.models.requestModels.formModels.NumberOfBedroomsFormModel
import uk.gov.communities.prsdb.webapp.models.requestModels.formModels.NumberOfHouseholdsFormModel
import uk.gov.communities.prsdb.webapp.models.requestModels.formModels.OccupancyFormModel
import uk.gov.communities.prsdb.webapp.models.requestModels.formModels.OwnershipTypeFormModel
import uk.gov.communities.prsdb.webapp.models.requestModels.formModels.PropertyTypeFormModel
import uk.gov.communities.prsdb.webapp.services.EpcCertificateUrlProvider
import uk.gov.communities.prsdb.webapp.services.PropertyRegistrationService

@JourneyFrameworkComponent
class SavePropertyRegistrationDataStepConfig(
    private val propertyRegistrationService: PropertyRegistrationService,
    private val epcCertificateUrlProvider: EpcCertificateUrlProvider,
    private val featureFlagManager: FeatureFlagManager,
) : AbstractInternalStepConfig<Complete, PropertyRegistrationJourneyState>() {
    override fun mode(state: PropertyRegistrationJourneyState): Complete = Complete.COMPLETE

    override fun afterStepIsReached(state: PropertyRegistrationJourneyState) {
        try {
            registerProperty(state)
        } catch (_: EntityExistsException) {
            state.propertyDetailsTask.addressTask.isAddressAlreadyRegistered = true
            return
        }
    }

    override fun resolveNextDestination(
        state: PropertyRegistrationJourneyState,
        defaultDestination: Destination,
    ): Destination =
        if (state.propertyDetailsTask.addressTask.isAddressAlreadyRegistered == true) {
            Destination(state.propertyDetailsTask.addressTask.alreadyRegisteredStep)
        } else {
            state.deleteJourney()
            defaultDestination
        }

    private fun registerProperty(state: PropertyRegistrationJourneyState) {
        val isOccupied = state.occupied.formModel.notNullValue(OccupancyFormModel::occupied)
        val isRestructured = featureFlagManager.checkFeature(PROPERTY_REGISTRATION_RESTRUCTURE_AND_SKIPPING)
        // TODO PDJB-1391: when a letting agent provides the rented-out details the landlord provides no licensing,
        //  tenancy or compliance details, so those tasks are skipped. Persist placeholder "provide later" values
        //  until the real delegated-details flow is implemented.
        val isDelegatedToLettingAgent = state.isDelegatedToLettingAgent(featureFlagManager)
        // TODO PDJB-1587: when isDelegatedToLettingAgent is true, persist the letting agent delegation details
        //  (e.g. the letting agent's email address from the WhoProvidesDetailsTask).
        val shouldRequireTenancyDetails = isOccupied && !state.provideTenancyDetailsLater && !isDelegatedToLettingAgent
        val billsIncludedDataModel = state.rentIncludesBillsTask.getBillsIncludedOrNull()
        val jointLandlordsTask = state.ownershipAndLandlordsTask.jointLandlordsTask
        val jointLandlordEmails: List<String>? =
            jointLandlordsTask.inviteJointLandlordsTask.invitedJointLandlordEmailsMap
                ?.values
                ?.toList()
        val markedJointLandlord = jointLandlordsTask.hasJointLandlordsStep.formModel.hasJointLandlords == true

        propertyRegistrationService.registerProperty(
            addressModel = state.propertyDetailsTask.addressTask.getAddress(),
            propertyType =
                state.propertyDetailsTask.propertyTypeStep.formModel
                    .notNullValue(PropertyTypeFormModel::propertyType),
            customPropertyType =
                if (state.propertyDetailsTask.propertyTypeStep.formModel.propertyType == PropertyType.OTHER) {
                    state.propertyDetailsTask.propertyTypeStep.formModel.customPropertyType
                } else {
                    null
                },
            licenseType = if (isDelegatedToLettingAgent) LicensingType.PROVIDE_LATER else state.licensingTask.getLicensingType(),
            licenceNumber = state.licensingTask.getLicenceNumberOrNull() ?: "",
            ownershipType =
                state.ownershipAndLandlordsTask.ownershipTypeStep.formModel
                    .notNullValue(OwnershipTypeFormModel::ownershipType),
            isOccupied = isOccupied,
            numberOfHouseholds =
                if (shouldRequireTenancyDetails) {
                    state.householdsAndTenantsTask.households.formModel
                        .notNullValue(NumberOfHouseholdsFormModel::numberOfHouseholds)
                        .toInt()
                } else {
                    0
                },
            numberOfPeople =
                if (shouldRequireTenancyDetails) {
                    state.householdsAndTenantsTask.tenants.formModel
                        .notNullValue(NewNumberOfPeopleFormModel::numberOfPeople)
                        .toInt()
                } else {
                    0
                },
            numBedrooms =
                if (isRestructured || shouldRequireTenancyDetails) {
                    state.bedrooms.formModel
                        .notNullValue(NumberOfBedroomsFormModel::numberOfBedrooms)
                        .toInt()
                } else {
                    null
                },
            billsIncludedList = if (shouldRequireTenancyDetails) billsIncludedDataModel?.standardBillsIncludedListAsString else null,
            customBillsIncluded = if (shouldRequireTenancyDetails) billsIncludedDataModel?.customBillsIncluded else null,
            furnishedStatus = if (shouldRequireTenancyDetails) state.furnishedStatus.formModel.furnishedStatus else null,
            rentFrequency =
                if (shouldRequireTenancyDetails) {
                    state.rentFrequencyAndAmountTask.rentFrequency.formModel.rentFrequency
                } else {
                    null
                },
            customRentFrequency =
                if (shouldRequireTenancyDetails) {
                    state.rentFrequencyAndAmountTask.getCustomRentFrequencyIfSelected()
                } else {
                    null
                },
            rentAmount =
                if (shouldRequireTenancyDetails) {
                    state.rentFrequencyAndAmountTask.rentAmount.formModel.rentAmount
                        .toBigDecimal()
                } else {
                    null
                },
            jointLandlordEmails = jointLandlordEmails,
            markedJointLandlord = markedJointLandlord,
            hasGasSupply = state.gasSafetyTask.gasSafetyDetailsTask.hasGasSupplyStep.outcome == YesOrNo.YES,
            gasSafetyCertIssueDate =
                state.gasSafetyTask.gasSafetyDetailsTask
                    .getGasSafetyCertificateIssueDateIfReachable()
                    ?.toJavaLocalDate(),
            gasSafetyFileUploadIds = state.gasSafetyTask.gasSafetyDetailsTask.gasUploadIds,
            gasSafetyCertProvideLater =
                isDelegatedToLettingAgent ||
                    state.gasSafetyTask.gasSafetyDetailsTask.hasGasCertStep.outcome == HasGasCertMode.PROVIDE_THIS_LATER,
            electricalSafetyFileUploadIds = state.electricalSafetyTask.electricalSafetyDetailsTask.electricalUploadIds,
            electricalSafetyExpiryDate =
                state.electricalSafetyTask.electricalSafetyDetailsTask
                    .getElectricalCertificateExpiryDateIfReachable()
                    ?.toJavaLocalDate(),
            electricalCertType =
                state.electricalSafetyTask.electricalSafetyDetailsTask
                    .mapElectricalCertificateTypeToGlobalCertificateType(),
            electricalSafetyCertProvideLater =
                isDelegatedToLettingAgent ||
                    state.electricalSafetyTask.electricalSafetyDetailsTask
                        .hasElectricalCertStep.outcome == HasElectricalCertMode.PROVIDE_THIS_LATER,
            epcCertificateUrl =
                state.epcTask.epcDetailsTask.acceptedEpcIfStillAccepted?.let {
                    epcCertificateUrlProvider.getEpcCertificateUrl(it.certificateNumber)
                },
            epcExpiryDate = state.epcTask.epcDetailsTask.acceptedEpcIfStillAccepted?.expiryDateAsJavaLocalDate,
            epcEnergyRating = state.epcTask.epcDetailsTask.acceptedEpcIfStillAccepted?.energyRating,
            tenancyStartedBeforeEpcExpiry =
                state.epcTask.epcDetailsTask.epcInDateAtStartOfTenancyCheckStep
                    .formModelIfReachableOrNull
                    ?.tenancyStartedBeforeExpiry,
            epcExemptionReason =
                state.epcTask.epcDetailsTask.epcExemptionStep
                    .formModelIfReachableOrNull
                    ?.exemptionReason,
            epcMeesExemptionReason =
                state.epcTask.epcDetailsTask.meesExemptionStep
                    .formModelIfReachableOrNull
                    ?.exemptionReason,
            epcProvideLater =
                isDelegatedToLettingAgent || state.epcTask.epcDetailsTask.hasEpcStep.outcome == HasEpcMode.PROVIDE_LATER,
            licenseProvideLater =
                isDelegatedToLettingAgent ||
                    state.licensingTask.licensingTypeStep.outcome == LicensingTypeMode.PROVIDE_LATER,
            tenancyProvideLater = isDelegatedToLettingAgent || state.provideTenancyDetailsLater,
        )
    }
}

@JourneyFrameworkComponent
class SavePropertyRegistrationDataStep(
    stepConfig: SavePropertyRegistrationDataStepConfig,
) : JourneyStep.InternalStep<Complete, PropertyRegistrationJourneyState>(stepConfig)
