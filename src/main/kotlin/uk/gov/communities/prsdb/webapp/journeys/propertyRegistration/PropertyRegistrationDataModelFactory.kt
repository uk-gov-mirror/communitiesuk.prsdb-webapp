package uk.gov.communities.prsdb.webapp.journeys.propertyRegistration

import kotlinx.datetime.toJavaLocalDate
import uk.gov.communities.prsdb.webapp.annotations.webAnnotations.PrsdbWebComponent
import uk.gov.communities.prsdb.webapp.config.managers.FeatureFlagManager
import uk.gov.communities.prsdb.webapp.constants.ASSOCIATED_PARTIES
import uk.gov.communities.prsdb.webapp.constants.enums.LicensingType
import uk.gov.communities.prsdb.webapp.constants.enums.OwnershipType
import uk.gov.communities.prsdb.webapp.constants.enums.PropertyType
import uk.gov.communities.prsdb.webapp.exceptions.NotNullFormModelValueIsNullException.Companion.notNullValue
import uk.gov.communities.prsdb.webapp.helpers.converters.OwnershipTypeConverter
import uk.gov.communities.prsdb.webapp.journeys.UnrecoverableJourneyStateException
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.states.anyoneInOwnershipChain
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.states.getFreeholdersOrNull
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.states.getSuperiorLandlordsOrNull
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps.HasElectricalCertMode
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps.HasEpcMode
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps.HasGasSupplyMode
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps.LicensingTypeMode
import uk.gov.communities.prsdb.webapp.journeys.shared.YesOrNo
import uk.gov.communities.prsdb.webapp.models.dataModels.FreeholderDataModel
import uk.gov.communities.prsdb.webapp.models.dataModels.PropertyManagerDataModel
import uk.gov.communities.prsdb.webapp.models.dataModels.PropertyRegistrationDataModel
import uk.gov.communities.prsdb.webapp.models.dataModels.SuperiorLandlordDataModel
import uk.gov.communities.prsdb.webapp.models.requestModels.formModels.NewNumberOfPeopleFormModel
import uk.gov.communities.prsdb.webapp.models.requestModels.formModels.NumberOfBedroomsFormModel
import uk.gov.communities.prsdb.webapp.models.requestModels.formModels.NumberOfHouseholdsFormModel
import uk.gov.communities.prsdb.webapp.models.requestModels.formModels.OccupancyFormModel
import uk.gov.communities.prsdb.webapp.models.requestModels.formModels.OwnershipTypeFormModel
import uk.gov.communities.prsdb.webapp.models.requestModels.formModels.PropertyManagerDetailsFormModel
import uk.gov.communities.prsdb.webapp.models.requestModels.formModels.PropertyTypeFormModel
import uk.gov.communities.prsdb.webapp.services.EpcCertificateUrlProvider

@PrsdbWebComponent
class PropertyRegistrationDataModelFactory(
    private val epcCertificateUrlProvider: EpcCertificateUrlProvider,
    private val featureFlagManager: FeatureFlagManager,
) {
    fun fromJourneyState(state: PropertyRegistrationJourneyState): PropertyRegistrationDataModel {
        val isOccupied = state.occupied.formModel.notNullValue(OccupancyFormModel::occupied)
        val isDelegatedToLettingAgent = state.isDelegatedToLettingAgent()
        val lettingAgentEmail =
            if (isDelegatedToLettingAgent) {
                state.whoProvidesDetailsTask.lettingAgentEmailStep.formModel.emailAddress
            } else {
                null
            }
        val shouldRequireTenancyDetails = isOccupied && !state.provideTenancyDetailsLater && !isDelegatedToLettingAgent
        val billsIncludedDataModel = state.rentIncludesBillsTask.getBillsIncludedOrNull()
        val jointLandlordsTask = state.getJointLandlordsTask(featureFlagManager)
        val jointLandlordEmails: List<String>? =
            jointLandlordsTask.inviteJointLandlordsTask.invitedJointLandlordEmailsMap
                ?.values
                ?.toList()
        val markedJointLandlord = jointLandlordsTask.hasJointLandlordsStep.formModel.hasJointLandlords == true
        // TODO PDJB-2016: remove this with the flag, keeping the flag-on branches that depend on it.
        val isAssociatedPartiesEnabled = featureFlagManager.checkFeature(ASSOCIATED_PARTIES)
        val ownershipType =
            if (isAssociatedPartiesEnabled) {
                state.ownershipAndLandlordsTask.ownershipStructureTask.ownershipTypeTask.ownershipType
                    ?: throw UnrecoverableJourneyStateException(
                        state.journeyId,
                        "Property registration was submitted before the ownership type task was complete",
                    )
            } else {
                OwnershipTypeConverter.convert(
                    state.ownershipAndLandlordsTaskBeforeAssociatedParties.ownershipTypeStep.formModel
                        .notNullValue(OwnershipTypeFormModel::ownershipType),
                )
            }

        return PropertyRegistrationDataModel(
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
            ownershipType = ownershipType,
            freeholders = if (isAssociatedPartiesEnabled) getFreeholders(state, ownershipType) else null,
            superiorLandlords = if (isAssociatedPartiesEnabled) getSuperiorLandlords(state, ownershipType) else emptyList(),
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
                state.bedrooms.formModel
                    .notNullValue(NumberOfBedroomsFormModel::numberOfBedrooms)
                    .toInt(),
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
            lettingAgentEmail = lettingAgentEmail,
            markedJointLandlord = markedJointLandlord,
            hasGasSupply =
                when (state.gasSafetyTask.gasSafetyDetailsTask.hasGasSupplyStep.outcome) {
                    HasGasSupplyMode.HAS_SUPPLY -> {
                        true
                    }

                    HasGasSupplyMode.NO_SUPPLY -> {
                        false
                    }

                    HasGasSupplyMode.PROVIDE_LATER -> {
                        null
                    }

                    null -> {
                        if (isDelegatedToLettingAgent) {
                            null
                        } else {
                            throw IllegalStateException(
                                "hasGasSupply must be answered before registration unless it is delegated to a letting agent",
                            )
                        }
                    }
                },
            gasSafetyCertIssueDate =
                state.gasSafetyTask.gasSafetyDetailsTask
                    .getGasSafetyCertificateIssueDateIfReachable()
                    ?.toJavaLocalDate(),
            gasSafetyFileUploadIds = state.gasSafetyTask.gasSafetyDetailsTask.gasUploadIds,
            gasSafetyCertProvideLater =
                isDelegatedToLettingAgent ||
                    state.gasSafetyTask.gasSafetyDetailsTask.hasGasSupplyStep.outcome == HasGasSupplyMode.PROVIDE_LATER,
            electricalSafetyFileUploadIds = state.electricalSafetyTask.electricalSafetyDetailsTask.electricalUploadIds,
            eicrExpiryDate =
                state.electricalSafetyTask.electricalSafetyDetailsTask
                    .getEicrExpiryDateIfReachable()
                    ?.toJavaLocalDate(),
            eicIssueDate =
                state.electricalSafetyTask.electricalSafetyDetailsTask
                    .getEicIssueDateIfReachable()
                    ?.toJavaLocalDate(),
            // TODO PDJB-1679: Remove this when the EIC_ISSUE_DATE flag is removed
            eicExpiryDate =
                state.electricalSafetyTask.electricalSafetyDetailsTask
                    .getEicExpiryDateIfReachable()
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
            epcExpiryDate =
                state.epcTask.epcDetailsTask.acceptedEpcIfStillAccepted
                    ?.expiryDateAsJavaLocalDate,
            epcEnergyRating =
                state.epcTask.epcDetailsTask.acceptedEpcIfStillAccepted
                    ?.energyRating,
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
            isDelegatedToLettingAgent = isDelegatedToLettingAgent,
            correspondenceEmail =
                state.correspondenceTask.correspondenceEmailStep.formModel
                    .getEmailAddress { state.loggedInLandlordEmailAtStartOfJourney },
            correspondenceAddressModel = state.correspondenceTask.addressTask.getAddress(),
            propertyManager = if (isAssociatedPartiesEnabled) getPropertyManager(state) else null,
        )
    }

    private fun getPropertyManager(state: PropertyRegistrationJourneyState): PropertyManagerDataModel? {
        val propertyManagerTask = state.ownershipAndLandlordsTask.propertyManagerTask
        val usingPropertyManager =
            propertyManagerTask.propertyManagerStep.outcome
                ?: throw UnrecoverableJourneyStateException(
                    state.journeyId,
                    "Property registration was submitted before the property manager step was answered",
                )
        if (usingPropertyManager == YesOrNo.NO) return null

        val formModel = propertyManagerTask.propertyManagerDetailsStep.formModel
        return PropertyManagerDataModel(
            name = formModel.notNullValue(PropertyManagerDetailsFormModel::name),
            email = formModel.notNullValue(PropertyManagerDetailsFormModel::emailAddress),
        )
    }

    private fun getFreeholders(
        state: PropertyRegistrationJourneyState,
        ownershipType: OwnershipType,
    ): List<FreeholderDataModel>? {
        if (!ownershipType.requiresFreeholderRecords()) return null

        val freeholders =
            state.ownershipAndLandlordsTask.ownershipStructureTask.freeholdersTask
                .getFreeholdersOrNull()
                ?: throw UnrecoverableJourneyStateException(
                    state.journeyId,
                    "Property registration was submitted with an ownership type requiring freeholder records but no freeholders",
                )

        return freeholders.values.toList()
    }

    private fun getSuperiorLandlords(
        state: PropertyRegistrationJourneyState,
        ownershipType: OwnershipType,
    ): List<SuperiorLandlordDataModel> {
        if (!ownershipType.requiresSuperiorLandlordInfo()) return emptyList()

        val superiorLandlordsTask = state.ownershipAndLandlordsTask.ownershipStructureTask.superiorLandlordsTask
        val anyoneInOwnershipChain =
            superiorLandlordsTask.anyoneInOwnershipChain
                ?: throw UnrecoverableJourneyStateException(
                    state.journeyId,
                    "Property registration was submitted before the anyone in ownership chain step was answered",
                )
        if (!anyoneInOwnershipChain) return emptyList()

        val superiorLandlords =
            superiorLandlordsTask.getSuperiorLandlordsOrNull()
                ?: throw UnrecoverableJourneyStateException(
                    state.journeyId,
                    "Property registration was submitted with anyone in ownership chain answered yes but no superior landlords",
                )

        return superiorLandlords.values.toList()
    }
}
