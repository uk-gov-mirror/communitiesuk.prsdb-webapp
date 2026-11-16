package uk.gov.communities.prsdb.webapp.models.dataModels

import uk.gov.communities.prsdb.webapp.constants.enums.CertificateType
import uk.gov.communities.prsdb.webapp.constants.enums.EpcExemptionReason
import uk.gov.communities.prsdb.webapp.constants.enums.FurnishedStatus
import uk.gov.communities.prsdb.webapp.constants.enums.LicensingType
import uk.gov.communities.prsdb.webapp.constants.enums.MeesExemptionReason
import uk.gov.communities.prsdb.webapp.constants.enums.OwnershipType
import uk.gov.communities.prsdb.webapp.constants.enums.PropertyType
import uk.gov.communities.prsdb.webapp.constants.enums.RentFrequency
import java.math.BigDecimal
import java.time.LocalDate

data class PropertyRegistrationDataModel(
    val addressModel: AddressDataModel,
    val propertyType: PropertyType,
    val licenseType: LicensingType,
    val licenceNumber: String,
    val ownershipType: OwnershipType,
    val isOccupied: Boolean,
    val numberOfHouseholds: Int,
    val numberOfPeople: Int,
    val numBedrooms: Int?,
    val billsIncludedList: String?,
    val customBillsIncluded: String?,
    val furnishedStatus: FurnishedStatus?,
    val rentFrequency: RentFrequency?,
    val customRentFrequency: String?,
    val rentAmount: BigDecimal?,
    val customPropertyType: String?,
    val jointLandlordEmails: List<String>?,
    val lettingAgentEmail: String?,
    val markedJointLandlord: Boolean,
    val hasGasSupply: Boolean?,
    val gasSafetyCertIssueDate: LocalDate?,
    val gasSafetyFileUploadIds: List<Long>,
    val gasSafetyCertProvideLater: Boolean,
    val electricalSafetyFileUploadIds: List<Long>,
    val eicrExpiryDate: LocalDate?,
    val eicIssueDate: LocalDate?,
    // TODO PDJB-1679: Remove this when the EIC_ISSUE_DATE flag is removed
    val eicExpiryDate: LocalDate?,
    val electricalCertType: CertificateType?,
    val electricalSafetyCertProvideLater: Boolean,
    val epcCertificateUrl: String?,
    val epcExpiryDate: LocalDate?,
    val epcEnergyRating: String?,
    val tenancyStartedBeforeEpcExpiry: Boolean?,
    val epcExemptionReason: EpcExemptionReason?,
    val epcMeesExemptionReason: MeesExemptionReason?,
    val epcProvideLater: Boolean,
    val licenseProvideLater: Boolean,
    val tenancyProvideLater: Boolean,
    val isDelegatedToLettingAgent: Boolean,
    val correspondenceEmail: String,
    val correspondenceAddressModel: AddressDataModel,
    val renewalDate: LocalDate? = null,
    val freeholders: List<FreeholderDataModel>? = null,
    val superiorLandlords: List<SuperiorLandlordDataModel> = emptyList(),
    val propertyManager: PropertyManagerDataModel? = null,
)
