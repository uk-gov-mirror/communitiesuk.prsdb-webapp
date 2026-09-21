package uk.gov.communities.prsdb.webapp.services

import jakarta.persistence.EntityExistsException
import jakarta.transaction.Transactional
import uk.gov.communities.prsdb.webapp.annotations.webAnnotations.PrsdbWebService
import uk.gov.communities.prsdb.webapp.constants.PROVIDE_LATER_DEADLINE_DAYS
import uk.gov.communities.prsdb.webapp.constants.enums.CertificateType
import uk.gov.communities.prsdb.webapp.constants.enums.EpcExemptionReason
import uk.gov.communities.prsdb.webapp.constants.enums.FurnishedStatus
import uk.gov.communities.prsdb.webapp.constants.enums.LicensingType
import uk.gov.communities.prsdb.webapp.constants.enums.MeesExemptionReason
import uk.gov.communities.prsdb.webapp.constants.enums.OwnershipType
import uk.gov.communities.prsdb.webapp.constants.enums.PropertyType
import uk.gov.communities.prsdb.webapp.constants.enums.RentFrequency
import uk.gov.communities.prsdb.webapp.database.entity.Landlord
import uk.gov.communities.prsdb.webapp.database.entity.PropertyOwnership
import uk.gov.communities.prsdb.webapp.database.repository.PropertyOwnershipRepository
import uk.gov.communities.prsdb.webapp.models.dataModels.AddressDataModel
import uk.gov.communities.prsdb.webapp.models.dataModels.RegistrationNumberDataModel
import uk.gov.communities.prsdb.webapp.models.viewModels.emailModels.PropertyRegistrationConfirmationEmail
import java.math.BigDecimal
import java.time.LocalDate
import java.time.MonthDay
import java.time.format.DateTimeFormatter
import java.util.Locale

@PrsdbWebService
class PropertyRegistrationService(
    private val addressService: AddressService,
    private val licenseService: LicenseService,
    private val propertyOwnershipService: PropertyOwnershipService,
    private val userToLandlordService: UserToLandlordService,
    private val absoluteUrlProvider: AbsoluteUrlProvider,
    private val confirmationEmailSender: EmailNotificationService<PropertyRegistrationConfirmationEmail>,
    private val propertyOwnershipRepository: PropertyOwnershipRepository,
    private val confirmationService: PropertyRegistrationConfirmationService,
    private val jointLandlordInvitationService: JointLandlordInvitationService,
    private val propertyComplianceService: PropertyComplianceService,
    private val lettingAgentAccessService: LettingAgentAccessService,
    private val delegateToLettingAgentEmailService: DelegateToLettingAgentEmailService,
) {
    @Transactional
    fun registerProperty(
        addressModel: AddressDataModel,
        propertyType: PropertyType,
        licenseType: LicensingType,
        licenceNumber: String,
        ownershipType: OwnershipType,
        isOccupied: Boolean,
        numberOfHouseholds: Int,
        numberOfPeople: Int,
        numBedrooms: Int?,
        billsIncludedList: String?,
        customBillsIncluded: String?,
        furnishedStatus: FurnishedStatus?,
        rentFrequency: RentFrequency?,
        customRentFrequency: String?,
        rentAmount: BigDecimal?,
        customPropertyType: String?,
        jointLandlordEmails: List<String>? = null,
        lettingAgentEmail: String? = null,
        markedJointLandlord: Boolean = false,
        hasGasSupply: Boolean? = null,
        gasSafetyCertIssueDate: LocalDate? = null,
        gasSafetyFileUploadIds: List<Long> = emptyList(),
        gasSafetyCertProvideLater: Boolean? = null,
        electricalSafetyFileUploadIds: List<Long> = emptyList(),
        electricalSafetyExpiryDate: LocalDate? = null,
        electricalCertType: CertificateType? = null,
        electricalSafetyCertProvideLater: Boolean? = null,
        epcCertificateUrl: String? = null,
        epcExpiryDate: LocalDate? = null,
        epcEnergyRating: String? = null,
        tenancyStartedBeforeEpcExpiry: Boolean? = null,
        epcExemptionReason: EpcExemptionReason? = null,
        epcMeesExemptionReason: MeesExemptionReason? = null,
        epcProvideLater: Boolean? = null,
        licenseProvideLater: Boolean = false,
        tenancyProvideLater: Boolean? = null,
        isDelegatedToLettingAgent: Boolean = false,
        correspondenceEmail: String? = null,
        correspondenceAddressModel: AddressDataModel? = null,
    ) {
        val landlord = userToLandlordService.getCurrentLandlordForUser()

        val propertyOwnership =
            createPropertyOwnershipAndRelatedEntities(
                addressModel,
                propertyType,
                licenseType,
                licenceNumber,
                ownershipType,
                isOccupied,
                numberOfHouseholds,
                numberOfPeople,
                numBedrooms,
                billsIncludedList,
                customBillsIncluded,
                furnishedStatus,
                rentFrequency,
                customRentFrequency,
                rentAmount,
                customPropertyType,
                markedJointLandlord,
                tenancyProvideLater,
                mutableSetOf(landlord),
                licenseProvideLater = licenseProvideLater,
                correspondenceEmail = correspondenceEmail,
                correspondenceAddressModel = correspondenceAddressModel,
            )

        landlord.setAnniversaryIfAbsent(MonthDay.from(propertyOwnership.registrationDate))

        if (lettingAgentEmail != null) {
            val invitation = lettingAgentAccessService.createInvitation(propertyOwnership, lettingAgentEmail)
            val deadlineDate =
                LocalDate.now().plusDays(PROVIDE_LATER_DEADLINE_DAYS.toLong()).format(
                    DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.UK),
                )
            delegateToLettingAgentEmailService.sendDelegationEmailToLettingAgent(
                propertyOwnership,
                landlord.name,
                lettingAgentEmail,
                deadlineDate,
                invitationToken = invitation.token,
            )
        }

        propertyComplianceService.saveRegistrationComplianceData(
            propertyOwnership.registrationNumber.number,
            hasGasSupply,
            gasSafetyCertIssueDate,
            gasSafetyFileUploadIds,
            gasSafetyCertProvideLater = gasSafetyCertProvideLater,
            electricalSafetyFileUploadIds,
            electricalSafetyExpiryDate,
            electricalCertType,
            electricalSafetyCertProvideLater = electricalSafetyCertProvideLater,
            epcCertificateUrl,
            epcExpiryDate,
            epcEnergyRating,
            tenancyStartedBeforeEpcExpiry,
            epcExemptionReason,
            epcMeesExemptionReason,
            epcProvideLater = epcProvideLater,
        )

        confirmationService.setLastPrnRegisteredThisSession(propertyOwnership.registrationNumber.number)

        sendConfirmationEmails(landlord, propertyOwnership, addressModel, jointLandlordEmails, isDelegatedToLettingAgent)
    }

    private fun createPropertyOwnershipAndRelatedEntities(
        addressModel: AddressDataModel,
        propertyType: PropertyType,
        licenseType: LicensingType,
        licenceNumber: String,
        ownershipType: OwnershipType,
        isOccupied: Boolean,
        numberOfHouseholds: Int,
        numberOfPeople: Int,
        numBedrooms: Int?,
        billsIncludedList: String?,
        customBillsIncluded: String?,
        furnishedStatus: FurnishedStatus?,
        rentFrequency: RentFrequency?,
        customRentFrequency: String?,
        rentAmount: BigDecimal?,
        customPropertyType: String?,
        markedJointLandlord: Boolean,
        tenancyProvideLater: Boolean?,
        landlords: MutableSet<Landlord>,
        licenseProvideLater: Boolean = false,
        correspondenceEmail: String?,
        correspondenceAddressModel: AddressDataModel?,
    ): PropertyOwnership {
        if (addressModel.uprn != null && propertyOwnershipRepository.existsByIsActiveTrueAndAddress_Uprn(addressModel.uprn)) {
            throw EntityExistsException("Address already registered")
        }

        val address = addressService.findOrCreateAddress(addressModel)

        val license =
            if (LicenseService.licenceShouldBeStored(licenseType)) {
                licenseService.createLicense(licenseType, licenceNumber)
            } else {
                null
            }

        return propertyOwnershipService.createPropertyOwnership(
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
            landlords = landlords,
            propertyBuildType = propertyType,
            customPropertyType = customPropertyType,
            markedJointLandlord = markedJointLandlord,
            tenancyProvideLater = tenancyProvideLater,
            address = address,
            license = license,
            licenseProvideLater = licenseProvideLater,
            correspondenceEmail = correspondenceEmail,
            correspondenceAddressModel = correspondenceAddressModel,
        )
    }

    private fun sendConfirmationEmails(
        landlord: Landlord,
        propertyOwnership: PropertyOwnership,
        addressModel: AddressDataModel,
        jointLandlordEmails: List<String>?,
        isDelegatedToLettingAgent: Boolean,
    ) {
        // TODO: PDJB-1274: Update emails to account for org landlord (check which org email address to use, currently registrant)
        confirmationEmailSender.sendEmail(
            landlord.email,
            PropertyRegistrationConfirmationEmail(
                RegistrationNumberDataModel
                    .fromRegistrationNumber(propertyOwnership.registrationNumber)
                    .toString(),
                addressModel.singleLineAddress,
                absoluteUrlProvider.buildLandlordDashboardUri().toString(),
                propertyOwnership.isOccupied,
                jointLandlordEmails,
                isDelegatedToLettingAgent,
            ),
        )

        if (!jointLandlordEmails.isNullOrEmpty()) {
            jointLandlordInvitationService.sendInvitationEmails(jointLandlordEmails, propertyOwnership, landlord)
        }
    }
}
