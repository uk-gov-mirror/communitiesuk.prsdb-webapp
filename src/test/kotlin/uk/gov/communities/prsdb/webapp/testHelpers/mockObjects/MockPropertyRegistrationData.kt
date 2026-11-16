package uk.gov.communities.prsdb.webapp.testHelpers.mockObjects

import uk.gov.communities.prsdb.webapp.constants.enums.CertificateType
import uk.gov.communities.prsdb.webapp.constants.enums.EpcExemptionReason
import uk.gov.communities.prsdb.webapp.constants.enums.FurnishedStatus
import uk.gov.communities.prsdb.webapp.constants.enums.LicensingType
import uk.gov.communities.prsdb.webapp.constants.enums.MeesExemptionReason
import uk.gov.communities.prsdb.webapp.constants.enums.OwnershipType
import uk.gov.communities.prsdb.webapp.constants.enums.PropertyType
import uk.gov.communities.prsdb.webapp.constants.enums.RentFrequency
import uk.gov.communities.prsdb.webapp.models.dataModels.AddressDataModel
import uk.gov.communities.prsdb.webapp.models.dataModels.FreeholderDataModel
import uk.gov.communities.prsdb.webapp.models.dataModels.PropertyManagerDataModel
import uk.gov.communities.prsdb.webapp.models.dataModels.PropertyRegistrationDataModel
import uk.gov.communities.prsdb.webapp.models.dataModels.SuperiorLandlordDataModel
import java.math.BigDecimal
import java.time.LocalDate

class MockPropertyRegistrationData {
    companion object {
        const val DEFAULT_CORRESPONDENCE_EMAIL = "correspondence@example.com"

        val DEFAULT_CORRESPONDENCE_ADDRESS_MODEL =
            AddressDataModel.fromManualAddressData("2 Correspondence Road", "Leeds", "LS1 1AA")

        fun createDefaultCorrespondenceAddress() = MockLandlordData.createAddress(DEFAULT_CORRESPONDENCE_ADDRESS_MODEL.singleLineAddress)

        fun createPropertyRegistrationDataModel(
            addressModel: AddressDataModel = AddressDataModel("1 Example Road, EG1 2AB"),
            propertyType: PropertyType = PropertyType.DETACHED_HOUSE,
            licenseType: LicensingType = LicensingType.NO_LICENSING,
            licenceNumber: String = "",
            ownershipType: OwnershipType = OwnershipType.SOLE_FREEHOLD,
            isOccupied: Boolean = false,
            numberOfHouseholds: Int = 0,
            numberOfPeople: Int = 0,
            numBedrooms: Int? = null,
            billsIncludedList: String? = null,
            customBillsIncluded: String? = null,
            furnishedStatus: FurnishedStatus? = null,
            rentFrequency: RentFrequency? = null,
            customRentFrequency: String? = null,
            rentAmount: BigDecimal? = null,
            customPropertyType: String? = null,
            jointLandlordEmails: List<String>? = null,
            lettingAgentEmail: String? = null,
            markedJointLandlord: Boolean = false,
            hasGasSupply: Boolean? = null,
            gasSafetyCertIssueDate: LocalDate? = null,
            gasSafetyFileUploadIds: List<Long> = emptyList(),
            gasSafetyCertProvideLater: Boolean = false,
            electricalSafetyFileUploadIds: List<Long> = emptyList(),
            eicrExpiryDate: LocalDate? = null,
            eicIssueDate: LocalDate? = null,
            // TODO PDJB-1679: Remove this when the EIC_ISSUE_DATE flag is removed
            eicExpiryDate: LocalDate? = null,
            electricalCertType: CertificateType? = null,
            electricalSafetyCertProvideLater: Boolean = false,
            epcCertificateUrl: String? = null,
            epcExpiryDate: LocalDate? = null,
            epcEnergyRating: String? = null,
            tenancyStartedBeforeEpcExpiry: Boolean? = null,
            epcExemptionReason: EpcExemptionReason? = null,
            epcMeesExemptionReason: MeesExemptionReason? = null,
            epcProvideLater: Boolean = false,
            licenseProvideLater: Boolean = false,
            tenancyProvideLater: Boolean = false,
            isDelegatedToLettingAgent: Boolean = false,
            correspondenceEmail: String = DEFAULT_CORRESPONDENCE_EMAIL,
            correspondenceAddressModel: AddressDataModel = DEFAULT_CORRESPONDENCE_ADDRESS_MODEL,
            freeholders: List<FreeholderDataModel>? = null,
            superiorLandlords: List<SuperiorLandlordDataModel> = emptyList(),
            propertyManager: PropertyManagerDataModel? = null,
        ) = PropertyRegistrationDataModel(
            addressModel = addressModel,
            propertyType = propertyType,
            licenseType = licenseType,
            licenceNumber = licenceNumber,
            ownershipType = ownershipType,
            isOccupied = isOccupied,
            numberOfHouseholds = numberOfHouseholds,
            numberOfPeople = numberOfPeople,
            numBedrooms = numBedrooms,
            billsIncludedList = billsIncludedList,
            customBillsIncluded = customBillsIncluded,
            furnishedStatus = furnishedStatus,
            rentFrequency = rentFrequency,
            customRentFrequency = customRentFrequency,
            rentAmount = rentAmount,
            customPropertyType = customPropertyType,
            jointLandlordEmails = jointLandlordEmails,
            lettingAgentEmail = lettingAgentEmail,
            markedJointLandlord = markedJointLandlord,
            hasGasSupply = hasGasSupply,
            gasSafetyCertIssueDate = gasSafetyCertIssueDate,
            gasSafetyFileUploadIds = gasSafetyFileUploadIds,
            gasSafetyCertProvideLater = gasSafetyCertProvideLater,
            electricalSafetyFileUploadIds = electricalSafetyFileUploadIds,
            eicrExpiryDate = eicrExpiryDate,
            eicIssueDate = eicIssueDate,
            // TODO PDJB-1679: Remove this when the EIC_ISSUE_DATE flag is removed
            eicExpiryDate = eicExpiryDate,
            electricalCertType = electricalCertType,
            electricalSafetyCertProvideLater = electricalSafetyCertProvideLater,
            epcCertificateUrl = epcCertificateUrl,
            epcExpiryDate = epcExpiryDate,
            epcEnergyRating = epcEnergyRating,
            tenancyStartedBeforeEpcExpiry = tenancyStartedBeforeEpcExpiry,
            epcExemptionReason = epcExemptionReason,
            epcMeesExemptionReason = epcMeesExemptionReason,
            epcProvideLater = epcProvideLater,
            licenseProvideLater = licenseProvideLater,
            tenancyProvideLater = tenancyProvideLater,
            isDelegatedToLettingAgent = isDelegatedToLettingAgent,
            correspondenceEmail = correspondenceEmail,
            correspondenceAddressModel = correspondenceAddressModel,
            freeholders = freeholders,
            superiorLandlords = superiorLandlords,
            propertyManager = propertyManager,
        )
    }
}
