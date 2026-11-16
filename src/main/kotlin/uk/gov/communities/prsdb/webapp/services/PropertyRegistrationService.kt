package uk.gov.communities.prsdb.webapp.services

import jakarta.persistence.EntityExistsException
import jakarta.transaction.Transactional
import uk.gov.communities.prsdb.webapp.annotations.webAnnotations.PrsdbWebService
import uk.gov.communities.prsdb.webapp.config.managers.FeatureFlagManager
import uk.gov.communities.prsdb.webapp.constants.PAYMENTS
import uk.gov.communities.prsdb.webapp.constants.PROVIDE_LATER_DEADLINE_DAYS
import uk.gov.communities.prsdb.webapp.constants.enums.PaymentStatus
import uk.gov.communities.prsdb.webapp.database.entity.JointLandlordInvitation
import uk.gov.communities.prsdb.webapp.database.entity.Landlord
import uk.gov.communities.prsdb.webapp.database.entity.LettingAgentAccess
import uk.gov.communities.prsdb.webapp.database.entity.PropertyOwnership
import uk.gov.communities.prsdb.webapp.database.repository.PaymentRepository
import uk.gov.communities.prsdb.webapp.database.repository.PropertyOwnershipRepository
import uk.gov.communities.prsdb.webapp.helpers.DateTimeHelper
import uk.gov.communities.prsdb.webapp.helpers.EmailHelper.Companion.sendEmailAndLogFailures
import uk.gov.communities.prsdb.webapp.helpers.RenewalDateHelper
import uk.gov.communities.prsdb.webapp.helpers.TransactionHelper.Companion.runAfterTransactionCommits
import uk.gov.communities.prsdb.webapp.models.dataModels.PropertyRegistrationDataModel
import uk.gov.communities.prsdb.webapp.models.dataModels.RegistrationNumberDataModel
import uk.gov.communities.prsdb.webapp.models.viewModels.emailModels.PropertyRegistrationConfirmationEmail
import java.time.LocalDate
import java.time.MonthDay
import java.time.format.DateTimeFormatter
import java.util.Locale

@PrsdbWebService
class PropertyRegistrationService(
    private val addressService: AddressService,
    private val licenseService: LicenseService,
    private val propertyOwnershipService: PropertyOwnershipService,
    private val propertyManagerService: PropertyManagerService,
    private val userToLandlordService: UserToLandlordService,
    private val absoluteUrlProvider: AbsoluteUrlProvider,
    private val confirmationEmailSender: EmailNotificationService<PropertyRegistrationConfirmationEmail>,
    private val propertyOwnershipRepository: PropertyOwnershipRepository,
    private val confirmationService: PropertyRegistrationConfirmationService,
    private val jointLandlordInvitationService: JointLandlordInvitationService,
    private val propertyComplianceService: PropertyComplianceService,
    private val lettingAgentAccessService: LettingAgentAccessService,
    private val delegateToLettingAgentEmailService: DelegateToLettingAgentEmailService,
    private val featureFlagManager: FeatureFlagManager,
    private val currentUserService: CurrentUserService,
    private val paymentRepository: PaymentRepository,
    private val activeInactiveService: ActiveInactiveService,
) {
    @Transactional
    fun registerProperty(registrationData: PropertyRegistrationDataModel): PropertyOwnership =
        with(registrationData) {
            val landlord = userToLandlordService.getCurrentLandlordForUser()
            val anniversary = landlord.anniversary ?: MonthDay.now(DateTimeHelper.UK_ZONE)

            val propertyOwnership =
                createPropertyOwnershipAndRelatedEntities(
                    registrationData,
                    landlord,
                    anniversary,
                    renewalDate ?: RenewalDateHelper.getRenewalDate(anniversary),
                )

            activeInactiveService.reactivateLandlordIfInactive(landlord)

            landlord.setAnniversaryIfAbsent(anniversary)

            val lettingAgentInvitation =
                lettingAgentEmail?.let { email -> lettingAgentAccessService.createInvitation(propertyOwnership, email) }

            val jointLandlordInvitations =
                if (jointLandlordEmails.isNullOrEmpty()) {
                    emptyList()
                } else {
                    jointLandlordInvitationService.createInvitations(jointLandlordEmails, propertyOwnership, landlord)
                }

            propertyComplianceService.saveRegistrationComplianceData(
                propertyOwnership.registrationNumber.number,
                hasGasSupply,
                gasSafetyCertIssueDate,
                gasSafetyFileUploadIds,
                gasSafetyCertProvideLater = gasSafetyCertProvideLater,
                electricalSafetyFileUploadIds,
                eicrExpiryDate,
                electricalCertType,
                electricalSafetyCertProvideLater = electricalSafetyCertProvideLater,
                epcCertificateUrl,
                epcExpiryDate,
                epcEnergyRating,
                tenancyStartedBeforeEpcExpiry,
                epcExemptionReason,
                epcMeesExemptionReason,
                epcProvideLater = epcProvideLater,
                eicIssueDate = eicIssueDate,
                // TODO PDJB-1679: Remove this when the EIC_ISSUE_DATE flag is removed
                eicExpiryDate = eicExpiryDate,
            )

            runAfterTransactionCommits {
                confirmationService.setLastPrnRegisteredThisSession(propertyOwnership.registrationNumber.number)
                sendRegistrationEmails(registrationData, landlord, propertyOwnership, lettingAgentInvitation, jointLandlordInvitations)
            }

            propertyOwnership
        }

    private fun createPropertyOwnershipAndRelatedEntities(
        registrationData: PropertyRegistrationDataModel,
        registeringLandlord: Landlord,
        anniversary: MonthDay,
        renewalDate: LocalDate,
    ): PropertyOwnership =
        with(registrationData) {
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

            val propertyManagerEntity =
                propertyManager?.let { propertyManagerService.createPropertyManager(it.name, it.email) }

            propertyOwnershipService.createPropertyOwnership(
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
                registeringLandlord = registeringLandlord,
                anniversary = anniversary,
                renewalDate = renewalDate,
                propertyBuildType = propertyType,
                customPropertyType = customPropertyType,
                markedJointLandlord = markedJointLandlord,
                tenancyProvideLater = tenancyProvideLater,
                address = address,
                license = license,
                licenseProvideLater = licenseProvideLater,
                correspondenceEmail = correspondenceEmail,
                correspondenceAddressModel = correspondenceAddressModel,
                freeholders = freeholders,
                superiorLandlords = superiorLandlords,
                propertyManager = propertyManagerEntity,
            )
        }

    private fun sendRegistrationEmails(
        registrationData: PropertyRegistrationDataModel,
        landlord: Landlord,
        propertyOwnership: PropertyOwnership,
        lettingAgentInvitation: LettingAgentAccess?,
        jointLandlordInvitations: List<JointLandlordInvitation>,
    ) {
        sendEmailAndLogFailures("the property registration confirmation email for property ownership ${propertyOwnership.id}") {
            sendConfirmationEmail(registrationData, landlord, propertyOwnership)
        }

        lettingAgentInvitation?.let { invitation ->
            sendEmailAndLogFailures("the letting agent invitation email for property ownership ${propertyOwnership.id}") {
                sendLettingAgentInvitationEmail(invitation, landlord)
            }
        }

        jointLandlordInvitations.forEach { invitation ->
            sendEmailAndLogFailures("the joint landlord invitation email for property ownership ${propertyOwnership.id}") {
                jointLandlordInvitationService.sendInvitationEmail(invitation)
            }
        }

        if (jointLandlordInvitations.isNotEmpty()) {
            sendEmailAndLogFailures("the joint landlord invitation confirmation emails for property ownership ${propertyOwnership.id}") {
                jointLandlordInvitationService.sendInvitationConfirmationEmails(
                    jointLandlordInvitations.map { it.invitedEmail },
                    propertyOwnership,
                    landlord,
                )
            }
        }
    }

    private fun sendConfirmationEmail(
        registrationData: PropertyRegistrationDataModel,
        landlord: Landlord,
        propertyOwnership: PropertyOwnership,
    ) = with(registrationData) {
        // TODO: PDJB-1274: Update emails to account for org landlord (check which org email address to use, currently registrant)
        confirmationEmailSender.sendEmail(
            currentUserService.getCurrentEmail(),
            PropertyRegistrationConfirmationEmail(
                RegistrationNumberDataModel
                    .fromRegistrationNumber(propertyOwnership.registrationNumber)
                    .toString(),
                propertyOwnership.address.toMultiLineAddress(),
                absoluteUrlProvider.buildLandlordDashboardUri().toString(),
                propertyOwnership.isOccupied,
                jointLandlordEmails,
                isDelegatedToLettingAgent,
                licenseProvideLater = licenseProvideLater,
                gasSafetyCertProvideLater = gasSafetyCertProvideLater,
                electricalSafetyCertProvideLater = electricalSafetyCertProvideLater,
                epcProvideLater = epcProvideLater,
                tenancyProvideLater = tenancyProvideLater,
                // TODO PDJB-1734: Always get the payment details when the PAYMENTS flag is removed
                paymentDetails = if (featureFlagManager.checkFeature(PAYMENTS)) getPaymentDetails(propertyOwnership) else null,
            ),
        )
    }

    private fun getPaymentDetails(propertyOwnership: PropertyOwnership): PropertyRegistrationConfirmationEmail.PaymentDetails {
        val payment =
            checkNotNull(
                paymentRepository.findFirstByAssociatedPropertyAndStatusOrderByPaymentCreatedAtDesc(
                    propertyOwnership,
                    PaymentStatus.SUCCEEDED,
                ),
            ) { "No succeeded payment found for property ownership ${propertyOwnership.id}" }

        return PropertyRegistrationConfirmationEmail.PaymentDetails(
            amountInPence = payment.amountInPence,
            paymentDate = payment.quoteDate,
            renewalDate = propertyOwnership.renewalDate,
        )
    }

    private fun sendLettingAgentInvitationEmail(
        invitation: LettingAgentAccess,
        landlord: Landlord,
    ) {
        val deadlineDate =
            LocalDate.now().plusDays(PROVIDE_LATER_DEADLINE_DAYS.toLong()).format(
                DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.UK),
            )
        delegateToLettingAgentEmailService.sendDelegationEmailToLettingAgent(
            invitation.propertyOwnership,
            landlord.name,
            invitation.invitedEmail,
            deadlineDate,
            invitationToken = invitation.token,
        )
    }
}
