package uk.gov.communities.prsdb.webapp.journeys.propertyRegistration

import kotlinx.datetime.toJavaLocalDate
import uk.gov.communities.prsdb.webapp.annotations.webAnnotations.PrsdbWebComponent
import uk.gov.communities.prsdb.webapp.constants.enums.LicensingType
import uk.gov.communities.prsdb.webapp.constants.enums.PropertyType
import uk.gov.communities.prsdb.webapp.exceptions.NotNullFormModelValueIsNullException.Companion.notNullValue
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.states.GasSupplyOutcome
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps.HasElectricalCertMode
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps.HasEpcMode
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps.LicensingTypeMode
import uk.gov.communities.prsdb.webapp.models.dataModels.PropertyRegistrationDataModel
import uk.gov.communities.prsdb.webapp.models.requestModels.formModels.NewNumberOfPeopleFormModel
import uk.gov.communities.prsdb.webapp.models.requestModels.formModels.NumberOfBedroomsFormModel
import uk.gov.communities.prsdb.webapp.models.requestModels.formModels.NumberOfHouseholdsFormModel
import uk.gov.communities.prsdb.webapp.models.requestModels.formModels.OccupancyFormModel
import uk.gov.communities.prsdb.webapp.models.requestModels.formModels.OwnershipTypeFormModel
import uk.gov.communities.prsdb.webapp.models.requestModels.formModels.PropertyTypeFormModel
import uk.gov.communities.prsdb.webapp.services.EpcCertificateUrlProvider

@PrsdbWebComponent
class PropertyRegistrationDataModelFactory(
    private val epcCertificateUrlProvider: EpcCertificateUrlProvider,
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
        val jointLandlordsTask = state.ownershipAndLandlordsTask.jointLandlordsTask
        val jointLandlordEmails: List<String>? =
            jointLandlordsTask.inviteJointLandlordsTask.invitedJointLandlordEmailsMap
                ?.values
                ?.toList()
        val markedJointLandlord = jointLandlordsTask.hasJointLandlordsStep.formModel.hasJointLandlords == true

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
                when (state.gasSafetyTask.gasSafetyDetailsTask.gasSupplyOutcome) {
                    GasSupplyOutcome.HAS_SUPPLY -> true
                    GasSupplyOutcome.NO_SUPPLY -> false
                    GasSupplyOutcome.PROVIDE_LATER -> null
                    null ->
                        if (isDelegatedToLettingAgent) {
                            null
                        } else {
                            throw IllegalStateException(
                                "gasSupplyOutcome must be answered before registration unless it is delegated to a letting agent",
                            )
                        }
                },
            gasSafetyCertIssueDate =
                state.gasSafetyTask.gasSafetyDetailsTask
                    .getGasSafetyCertificateIssueDateIfReachable()
                    ?.toJavaLocalDate(),
            gasSafetyFileUploadIds = state.gasSafetyTask.gasSafetyDetailsTask.gasUploadIds,
            gasSafetyCertProvideLater =
                isDelegatedToLettingAgent ||
                    state.gasSafetyTask.gasSafetyDetailsTask.gasSupplyOutcome == GasSupplyOutcome.PROVIDE_LATER,
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
            isDelegatedToLettingAgent = isDelegatedToLettingAgent,
            correspondenceEmail =
                state.correspondenceTask.correspondenceEmailStep.formModel
                    .getEmailAddress { state.loggedInLandlordEmailAtStartOfJourney },
            correspondenceAddressModel = state.correspondenceTask.addressTask.getAddress(),
        )
    }
}
