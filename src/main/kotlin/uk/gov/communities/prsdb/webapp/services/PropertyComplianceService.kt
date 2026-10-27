package uk.gov.communities.prsdb.webapp.services

import jakarta.persistence.EntityNotFoundException
import jakarta.transaction.Transactional
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException
import uk.gov.communities.prsdb.webapp.annotations.webAnnotations.PrsdbWebService
import uk.gov.communities.prsdb.webapp.constants.MAX_ENTRIES_IN_COMPLIANCE_ACTIONS_PAGE
import uk.gov.communities.prsdb.webapp.constants.PROVIDE_LATER_DEADLINE_DAYS
import uk.gov.communities.prsdb.webapp.constants.enums.CertificateType
import uk.gov.communities.prsdb.webapp.constants.enums.EpcExemptionReason
import uk.gov.communities.prsdb.webapp.constants.enums.MeesExemptionReason
import uk.gov.communities.prsdb.webapp.database.entity.Landlord
import uk.gov.communities.prsdb.webapp.database.entity.PropertyCompliance
import uk.gov.communities.prsdb.webapp.database.entity.PropertyOwnership
import uk.gov.communities.prsdb.webapp.database.repository.FileUploadRepository
import uk.gov.communities.prsdb.webapp.database.repository.PropertyComplianceRepository
import uk.gov.communities.prsdb.webapp.database.repository.PropertyOwnershipRepository
import uk.gov.communities.prsdb.webapp.exceptions.PrsdbWebException
import uk.gov.communities.prsdb.webapp.exceptions.UpdateConflictException
import uk.gov.communities.prsdb.webapp.models.dataModels.ComplianceStatusDataModel
import uk.gov.communities.prsdb.webapp.models.dataModels.RegistrationNumberDataModel
import uk.gov.communities.prsdb.webapp.models.viewModels.emailModels.ComplianceUpdateConfirmationEmail
import uk.gov.communities.prsdb.webapp.models.viewModels.emailModels.LettingAgentComplianceUpdateNotificationEmail
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@PrsdbWebService
class PropertyComplianceService(
    private val propertyComplianceRepository: PropertyComplianceRepository,
    private val propertyOwnershipRepository: PropertyOwnershipRepository,
    private val fileUploadRepository: FileUploadRepository,
    private val virusScanCallbackService: VirusScanCallbackService,
    private val complianceUpdateConfirmationSender: EmailNotificationService<ComplianceUpdateConfirmationEmail>,
    private val lettingAgentComplianceUpdateSender: EmailNotificationService<LettingAgentComplianceUpdateNotificationEmail>,
    private val absoluteUrlProvider: AbsoluteUrlProvider,
    private val userToLandlordService: UserToLandlordService,
    private val propertyOwnershipService: PropertyOwnershipService,
    private val currentUserService: CurrentUserService,
) {
    companion object {
        private val DATE_FORMATTER = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.UK)
    }

    fun getComplianceForPropertyOrNull(propertyOwnershipId: Long): PropertyCompliance? =
        propertyComplianceRepository.findByPropertyOwnership_Id(propertyOwnershipId)

    @Transactional
    fun saveRegistrationComplianceData(
        registrationNumberValue: Long,
        hasGasSupply: Boolean? = null,
        gasSafetyCertIssueDate: LocalDate? = null,
        gasSafetyFileUploadIds: List<Long> = listOf(),
        gasSafetyCertProvideLater: Boolean? = null,
        electricalSafetyFileUploadIds: List<Long> = listOf(),
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
    ) {
        val propertyOwnership =
            propertyOwnershipRepository.findByRegistrationNumber_Number(registrationNumberValue)
                ?: throw EntityNotFoundException("Property ownership not found for registration number $registrationNumberValue")

        val record =
            PropertyCompliance(propertyOwnership = propertyOwnership).apply {
                populateGasSafetyFields(
                    record = this,
                    hasGasSupply = hasGasSupply,
                    gasSafetyCertIssueDate = gasSafetyCertIssueDate,
                    gasSafetyFileUploadIds = gasSafetyFileUploadIds,
                    gasSafetyCertProvideLater = gasSafetyCertProvideLater,
                )
                populateElectricalSafetyFields(
                    record = this,
                    electricalSafetyFileUploadIds = electricalSafetyFileUploadIds,
                    electricalSafetyExpiryDate = electricalSafetyExpiryDate,
                    electricalCertType = electricalCertType,
                    electricalSafetyCertProvideLater = electricalSafetyCertProvideLater,
                )
                populateEpcFields(
                    record = this,
                    epcCertificateUrl = epcCertificateUrl,
                    epcExpiryDate = epcExpiryDate,
                    epcEnergyRating = epcEnergyRating,
                    tenancyStartedBeforeEpcExpiry = tenancyStartedBeforeEpcExpiry,
                    epcExemptionReason = epcExemptionReason,
                    epcMeesExemptionReason = epcMeesExemptionReason,
                    epcProvideLater = epcProvideLater,
                )
            }

        propertyComplianceRepository.save(record)

        updateFileUploadVirusScanningCallbacks(
            propertyOwnershipId = propertyOwnership.id,
            gasSafetyCertUploadIds = gasSafetyFileUploadIds,
            electricalSafetyCertUploadIds = electricalSafetyFileUploadIds,
            electricalCertType = electricalCertType,
        )
    }

    private fun populateGasSafetyFields(
        record: PropertyCompliance,
        hasGasSupply: Boolean?,
        gasSafetyCertIssueDate: LocalDate?,
        gasSafetyFileUploadIds: List<Long>,
        gasSafetyCertProvideLater: Boolean? = null,
    ) {
        record.hasGasSupply = hasGasSupply
        record.gasSafetyCertIssueDate = gasSafetyCertIssueDate
        record.gasSafetyCertProvideLater = gasSafetyCertProvideLater
        record.gasSafetyFileUploads =
            gasSafetyFileUploadIds
                .map { id -> fileUploadRepository.getReferenceById(id) }
                .toMutableList()
    }

    private fun populateElectricalSafetyFields(
        record: PropertyCompliance,
        electricalSafetyFileUploadIds: List<Long>,
        electricalSafetyExpiryDate: LocalDate?,
        electricalCertType: CertificateType?,
        electricalSafetyCertProvideLater: Boolean? = null,
    ) {
        record.electricalSafetyFileUploads =
            electricalSafetyFileUploadIds
                .map { id -> fileUploadRepository.getReferenceById(id) }
                .toMutableList()
        record.electricalSafetyExpiryDate = electricalSafetyExpiryDate
        record.electricalCertType = electricalCertType
        record.electricalSafetyCertProvideLater = electricalSafetyCertProvideLater
    }

    private fun populateEpcFields(
        record: PropertyCompliance,
        epcCertificateUrl: String?,
        epcExpiryDate: LocalDate?,
        epcEnergyRating: String?,
        tenancyStartedBeforeEpcExpiry: Boolean?,
        epcExemptionReason: EpcExemptionReason?,
        epcMeesExemptionReason: MeesExemptionReason?,
        epcProvideLater: Boolean? = null,
    ) {
        record.epcUrl = epcCertificateUrl
        record.epcExpiryDate = epcExpiryDate
        record.epcEnergyRating = epcEnergyRating
        record.tenancyStartedBeforeEpcExpiry = tenancyStartedBeforeEpcExpiry
        record.epcExemptionReason = epcExemptionReason
        record.epcMeesExemptionReason = epcMeesExemptionReason
        record.epcProvideLater = epcProvideLater
    }

    private fun updateFileUploadVirusScanningCallbacks(
        propertyOwnershipId: Long,
        gasSafetyCertUploadIds: List<Long> = emptyList(),
        electricalSafetyCertUploadIds: List<Long> = emptyList(),
        electricalCertType: CertificateType? = null,
    ) {
        gasSafetyCertUploadIds.forEach { uploadId ->
            virusScanCallbackService.updateCallbacksToOwner(uploadId, propertyOwnershipId, CertificateType.GasSafetyCert)
        }

        if (electricalSafetyCertUploadIds.isNotEmpty()) {
            requireNotNull(electricalCertType) { "electricalCertType must not be null when electrical safety uploads are present" }
        }

        electricalSafetyCertUploadIds.forEach { uploadId ->
            virusScanCallbackService.updateCallbacksToOwner(uploadId, propertyOwnershipId, electricalCertType!!)
        }
    }

    private fun getComplianceForProperty(propertyOwnershipId: Long): PropertyCompliance =
        getComplianceForPropertyOrNull(propertyOwnershipId)
            ?: throw EntityNotFoundException("No compliance record found for property ownership ID: $propertyOwnershipId")

    fun getNumberOfNonCompliantPropertiesForLandlord(landlord: Landlord) = getAllNonCompliantPropertiesForLandlord(landlord).size

    // TODO PDJB-2006: Remove when the PROPERTY_REGISTRATION_PHASE_THREE flag is removed
    fun getNonCompliantPropertiesForLandlord(
        landlord: Landlord,
        requestedPageIndex: Int,
    ): Page<ComplianceStatusDataModel> {
        val allNonCompliant = getAllNonCompliantPropertiesForLandlord(landlord)
        val pageRequest = PageRequest.of(requestedPageIndex, MAX_ENTRIES_IN_COMPLIANCE_ACTIONS_PAGE)
        val fromIndex = pageRequest.offset.toInt().coerceAtMost(allNonCompliant.size)
        val toIndex = (fromIndex + pageRequest.pageSize).coerceAtMost(allNonCompliant.size)
        return PageImpl(allNonCompliant.subList(fromIndex, toIndex), pageRequest, allNonCompliant.size.toLong())
    }

    private fun getAllNonCompliantPropertiesForLandlord(landlord: Landlord): List<ComplianceStatusDataModel> {
        val compliances =
            propertyComplianceRepository
                .findAllByPropertyOwnership_OwnershipLinks_Landlord_IdAndPropertyOwnership_IsActiveTrue(
                    landlord.id,
                )

        return compliances
            .map { ComplianceStatusDataModel.fromPropertyCompliance(it) }
            .filter { it.shouldShowOnComplianceActionsPage }
    }

    @Transactional
    fun updateGasSafety(
        propertyOwnershipId: Long,
        initialLastModifiedDate: Instant,
        hasGasSupply: Boolean,
        gasSafetyCertIssueDate: LocalDate? = null,
        gasSafetyCertUploadIds: List<Long> = listOf(),
    ) {
        val propertyCompliance = getComplianceForProperty(propertyOwnershipId)
        throwErrorIfLastModifiedDatesConflict(propertyCompliance, initialLastModifiedDate)

        propertyCompliance.apply {
            populateGasSafetyFields(
                record = this,
                hasGasSupply = hasGasSupply,
                gasSafetyCertIssueDate = gasSafetyCertIssueDate,
                gasSafetyFileUploadIds = gasSafetyCertUploadIds,
            )
        }

        propertyComplianceRepository.save(propertyCompliance)

        updateFileUploadVirusScanningCallbacks(
            propertyOwnershipId = propertyOwnershipId,
            gasSafetyCertUploadIds = gasSafetyCertUploadIds,
        )

        if (gasSafetyCertIssueDate != null) {
            sendCertificateUpdateEmail(
                propertyCompliance,
                isExpired = propertyCompliance.isGasSafetyCertExpired == true,
                certificateType = "gas safety certificate",
                certificateTypeLabel = "Gas safety certificate",
                expiryDate = propertyCompliance.gasSafetyCertExpiryDate,
            )
        }
    }

    @Transactional
    fun updateElectricalSafety(
        propertyOwnershipId: Long,
        initialLastModifiedDate: Instant,
        electricalCertType: CertificateType? = null,
        electricalSafetyExpiryDate: LocalDate? = null,
        electricalSafetyCertUploadIds: List<Long> = listOf(),
    ) {
        val propertyCompliance = getComplianceForProperty(propertyOwnershipId)
        throwErrorIfLastModifiedDatesConflict(propertyCompliance, initialLastModifiedDate)

        propertyCompliance.apply {
            populateElectricalSafetyFields(
                record = this,
                electricalSafetyFileUploadIds = electricalSafetyCertUploadIds,
                electricalSafetyExpiryDate = electricalSafetyExpiryDate,
                electricalCertType = electricalCertType,
            )
        }

        propertyComplianceRepository.save(propertyCompliance)

        updateFileUploadVirusScanningCallbacks(
            propertyOwnershipId = propertyOwnershipId,
            gasSafetyCertUploadIds = emptyList(),
            electricalSafetyCertUploadIds = electricalSafetyCertUploadIds,
            electricalCertType = electricalCertType,
        )

        if (electricalSafetyExpiryDate != null) {
            val certTypeAbbreviation = if (requireNotNull(electricalCertType) == CertificateType.Eic) "EIC" else "EICR"
            sendCertificateUpdateEmail(
                propertyCompliance,
                isExpired = propertyCompliance.isElectricalSafetyExpired == true,
                certificateType = "electrical safety certificate",
                certificateTypeLabel = "Electrical safety certificate ($certTypeAbbreviation)",
                expiryDate = propertyCompliance.electricalSafetyExpiryDate,
                lettingAgentCertificateTypeLabel = "Electrical safety certificate",
            )
        }
    }

    @Transactional
    fun updateEpc(
        propertyOwnershipId: Long,
        initialLastModifiedDate: Instant,
        epcCertificateUrl: String? = null,
        epcExpiryDate: LocalDate? = null,
        epcEnergyRating: String? = null,
        tenancyStartedBeforeEpcExpiry: Boolean? = null,
        epcExemptionReason: EpcExemptionReason? = null,
        epcMeesExemptionReason: MeesExemptionReason? = null,
    ) {
        val propertyCompliance = getComplianceForProperty(propertyOwnershipId)
        throwErrorIfLastModifiedDatesConflict(propertyCompliance, initialLastModifiedDate)

        propertyCompliance.apply {
            populateEpcFields(
                record = this,
                epcCertificateUrl = epcCertificateUrl,
                epcExpiryDate = epcExpiryDate,
                epcEnergyRating = epcEnergyRating,
                tenancyStartedBeforeEpcExpiry = tenancyStartedBeforeEpcExpiry,
                epcExemptionReason = epcExemptionReason,
                epcMeesExemptionReason = epcMeesExemptionReason,
            )
        }

        propertyComplianceRepository.save(propertyCompliance)

        if (epcCertificateUrl != null) {
            sendCertificateUpdateEmail(
                propertyCompliance,
                isExpired = propertyCompliance.isEpcExpired == true,
                certificateType = "energy performance certificate (EPC)",
                certificateTypeLabel = "Energy performance certificate (EPC)",
                expiryDate = propertyCompliance.epcExpiryDate,
                expiredOccupiedType = ComplianceUpdateConfirmationEmail.UpdateType.EXPIRED_EPC_OCCUPIED,
            )
        }
    }

    private fun sendCertificateUpdateEmail(
        propertyCompliance: PropertyCompliance,
        isExpired: Boolean,
        certificateType: String,
        certificateTypeLabel: String,
        expiryDate: LocalDate?,
        expiredOccupiedType: ComplianceUpdateConfirmationEmail.UpdateType =
            ComplianceUpdateConfirmationEmail.UpdateType.EXPIRED_CERTIFICATE_OCCUPIED,
        lettingAgentCertificateTypeLabel: String = certificateTypeLabel,
    ) {
        val isOccupied = propertyCompliance.propertyOwnership.isOccupied
        val updateType =
            if (!isExpired) {
                ComplianceUpdateConfirmationEmail.UpdateType.CERTIFICATE_ADDED
            } else if (isOccupied) {
                expiredOccupiedType
            } else {
                ComplianceUpdateConfirmationEmail.UpdateType.EXPIRED_CERTIFICATE_UNOCCUPIED
            }

        val formattedExpiryDate =
            if (updateType == ComplianceUpdateConfirmationEmail.UpdateType.CERTIFICATE_ADDED) {
                expiryDate?.format(DATE_FORMATTER)
            } else {
                null
            }
        val formattedDeadlineDate =
            if (updateType == ComplianceUpdateConfirmationEmail.UpdateType.EXPIRED_CERTIFICATE_OCCUPIED) {
                LocalDate.now().plusDays(PROVIDE_LATER_DEADLINE_DAYS.toLong()).format(DATE_FORMATTER)
            } else {
                null
            }

        val propertyOwnership = propertyCompliance.propertyOwnership

        val currentLandlord = userToLandlordService.getCurrentLandlordForUserOrNull()
        if (currentLandlord == null) {
            if (propertyOwnershipService.getCurrentUserIsAuthorizedToEditRecord(propertyOwnership.id)) {
                sendLettingAgentComplianceUpdateEmails(
                    propertyOwnership = propertyOwnership,
                    certificateType = certificateType,
                    certificateTypeLabel = lettingAgentCertificateTypeLabel,
                    expiryDate = expiryDate?.format(DATE_FORMATTER),
                )
                if (updateType != ComplianceUpdateConfirmationEmail.UpdateType.CERTIFICATE_ADDED) {
                    sendComplianceExpiryEmailsToLandlords(
                        propertyOwnership = propertyOwnership,
                        updateType = updateType,
                        certificateType = certificateType,
                        certificateTypeLabel = certificateTypeLabel,
                        expiryDate = formattedExpiryDate,
                        deadlineDate = formattedDeadlineDate,
                    )
                }
                return
            }
            throw ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "No acting landlord was found for the update to property ownership ${propertyOwnership.id}",
            )
        }

        propertyOwnership.landlords
            .singleOrNull { it.id == currentLandlord.id }
            ?: throw PrsdbWebException(
                "Current landlord ${currentLandlord.id} is not a landlord of property ${propertyOwnership.id}",
            )

        val currentUser = currentUserService.getRegisteredUserDetails()
        complianceUpdateConfirmationSender.sendEmail(
            currentUser.email,
            buildComplianceUpdateConfirmationEmail(
                recipientName = currentUser.name,
                propertyOwnership = propertyOwnership,
                updateType = updateType,
                certificateType = certificateType,
                certificateTypeLabel = certificateTypeLabel,
                expiryDate = formattedExpiryDate,
                deadlineDate = formattedDeadlineDate,
            ),
        )
        // TODO: PDJB-1274: Update emails to account for other users in the org

        val otherLandlords =
            propertyOwnership.landlords.filter { it.id != currentLandlord.id }
        otherLandlords.forEach { otherLandlord ->
            complianceUpdateConfirmationSender.sendEmailToLandlord(
                otherLandlord,
                buildComplianceUpdateConfirmationEmail(
                    recipientName = otherLandlord.name,
                    propertyOwnership = propertyOwnership,
                    updateType = updateType,
                    certificateType = certificateType,
                    certificateTypeLabel = certificateTypeLabel,
                    expiryDate = formattedExpiryDate,
                    deadlineDate = formattedDeadlineDate,
                    isJointLandlord = true,
                ),
            )
        }
    }

    private fun sendLettingAgentComplianceUpdateEmails(
        propertyOwnership: PropertyOwnership,
        certificateType: String,
        certificateTypeLabel: String,
        expiryDate: String?,
    ) {
        val registrationNumber =
            RegistrationNumberDataModel.fromRegistrationNumber(propertyOwnership.registrationNumber).toString()
        val propertyRecordUrl = absoluteUrlProvider.buildPropertyDetailsUri(propertyOwnership.id).toString()
        propertyOwnership.landlords.forEach { landlord ->
            lettingAgentComplianceUpdateSender.sendEmailToLandlord(
                landlord,
                LettingAgentComplianceUpdateNotificationEmail(
                    recipientName = landlord.name,
                    multiLineAddress = propertyOwnership.address.toMultiLineAddress(),
                    registrationNumber = registrationNumber,
                    certificateType = certificateType,
                    certificateTypeLabel = certificateTypeLabel,
                    expiryDate = expiryDate ?: "",
                    propertyRecordUrl = propertyRecordUrl,
                ),
            )
        }
    }

    private fun sendComplianceExpiryEmailsToLandlords(
        propertyOwnership: PropertyOwnership,
        updateType: ComplianceUpdateConfirmationEmail.UpdateType,
        certificateType: String,
        certificateTypeLabel: String,
        expiryDate: String?,
        deadlineDate: String?,
    ) {
        propertyOwnership.landlords.forEach { landlord ->
            complianceUpdateConfirmationSender.sendEmailToLandlord(
                landlord,
                buildComplianceUpdateConfirmationEmail(
                    recipientName = landlord.name,
                    propertyOwnership = propertyOwnership,
                    updateType = updateType,
                    certificateType = certificateType,
                    certificateTypeLabel = certificateTypeLabel,
                    expiryDate = expiryDate,
                    deadlineDate = deadlineDate,
                ),
            )
        }
    }

    private fun buildComplianceUpdateConfirmationEmail(
        recipientName: String,
        propertyOwnership: PropertyOwnership,
        updateType: ComplianceUpdateConfirmationEmail.UpdateType,
        certificateType: String,
        certificateTypeLabel: String,
        expiryDate: String?,
        deadlineDate: String?,
        isJointLandlord: Boolean = false,
    ) = ComplianceUpdateConfirmationEmail(
        landlordName = recipientName,
        multiLineAddress = propertyOwnership.address.toMultiLineAddress(),
        registrationNumber = RegistrationNumberDataModel.fromRegistrationNumber(propertyOwnership.registrationNumber),
        dashboardUrl = absoluteUrlProvider.buildLandlordDashboardUri(),
        newCertificateUrl = absoluteUrlProvider.buildComplianceInformationUri(propertyOwnership.id),
        complianceUpdateType = updateType,
        certificateType = certificateType,
        certificateTypeLabel = certificateTypeLabel,
        expiryDate = expiryDate,
        deadlineDate = deadlineDate,
        isJointLandlord = isJointLandlord,
    )

    private fun throwErrorIfLastModifiedDatesConflict(
        propertyCompliance: PropertyCompliance,
        initialLastModifiedDate: Instant,
    ) {
        if (propertyCompliance.getMostRecentlyUpdated() != initialLastModifiedDate) {
            throw UpdateConflictException(
                "The property compliance record has been updated since this update session started.",
            )
        }
    }
}
