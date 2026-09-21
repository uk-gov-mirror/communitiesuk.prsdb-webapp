package uk.gov.communities.prsdb.webapp.services

import org.hibernate.SessionFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Profile
import uk.gov.communities.prsdb.webapp.annotations.taskAnnotations.PrsdbTaskService
import uk.gov.communities.prsdb.webapp.constants.GAS_SAFETY_CERT_VALIDITY_YEARS
import uk.gov.communities.prsdb.webapp.constants.enums.CertificateType
import uk.gov.communities.prsdb.webapp.constants.enums.RegistrationNumberType
import uk.gov.communities.prsdb.webapp.database.dao.NftDataSeederDao
import uk.gov.communities.prsdb.webapp.database.entity.Address
import uk.gov.communities.prsdb.webapp.database.repository.AddressRepository
import uk.gov.communities.prsdb.webapp.database.repository.LocalCouncilRepository
import uk.gov.communities.prsdb.webapp.helpers.CertificateFilenameHelper
import uk.gov.communities.prsdb.webapp.helpers.NftDataFaker
import uk.gov.communities.prsdb.webapp.helpers.NftDataFaker.CoreLandlordDetails
import uk.gov.communities.prsdb.webapp.helpers.extensions.PreparedStatementExtensions.Companion.setBigDecimalOrNull
import uk.gov.communities.prsdb.webapp.helpers.extensions.PreparedStatementExtensions.Companion.setBooleanOrNull
import uk.gov.communities.prsdb.webapp.helpers.extensions.PreparedStatementExtensions.Companion.setDateOrNull
import uk.gov.communities.prsdb.webapp.helpers.extensions.PreparedStatementExtensions.Companion.setIntOrNull
import uk.gov.communities.prsdb.webapp.helpers.extensions.PreparedStatementExtensions.Companion.setLongOrNull
import uk.gov.communities.prsdb.webapp.helpers.extensions.PreparedStatementExtensions.Companion.setStringOrNull
import uk.gov.communities.prsdb.webapp.models.dataModels.EpcDataModel
import java.sql.Connection
import java.sql.Date
import java.sql.PreparedStatement
import java.sql.Timestamp
import java.time.LocalDateTime
import kotlin.math.ceil
import kotlin.math.min

@PrsdbTaskService
@Profile("nft-data-seeder")
class NftDataSeeder(
    private val sessionFactory: SessionFactory,
    private val localCouncilRepository: LocalCouncilRepository,
    private val addressRepository: AddressRepository,
    @Value("\${epc.certificate-base-url}")
    private val epcCertificateBaseUrl: String,
) {
    private lateinit var nftDataSeederDao: NftDataSeederDao

    private val registrationNumberGenerator = RegistrationNumberGenerator()
    private val landlordAddressGenerator = AddressGenerator()
    private val propertyOwnershipAddressGenerator = AddressGenerator(restrictToAvailable = true)
    private val incompletePropertyAddressGenerator = AddressGenerator(restrictToAvailable = true)

    fun seedDatabase() {
        val statelessSession = sessionFactory.openStatelessSession()
        statelessSession.use { session ->
            val transaction = session.beginTransaction()
            try {
                session.doWork { connection: Connection ->
                    nftDataSeederDao = NftDataSeederDao(session, connection)
                    seedSystemOperatorData()
                    seedLocalCouncilData()
                    seedLandlordData()
                    nftDataSeederDao.updateIdSequences()
                }
                transaction.commit()
            } catch (e: Exception) {
                transaction.rollback()
                throw e
            }
        }
    }

    private fun seedSystemOperatorData() {
        log("Starting to seed system operator data")

        val prsdbUserStmt = nftDataSeederDao.preparePrsdbUserStatement()
        val systemOperatorStmt = nftDataSeederDao.prepareSystemOperatorStatement()

        try {
            repeat(NUM_OF_SYSTEM_OPERATORS) { addSystemOperatorToBatch(prsdbUserStmt, systemOperatorStmt) }

            prsdbUserStmt.executeBatch()
            systemOperatorStmt.executeBatch()

            log("Seeded $NUM_OF_SYSTEM_OPERATORS system operators")
        } finally {
            prsdbUserStmt.close()
            systemOperatorStmt.close()
        }

        log("Finished seeding system operator data")
    }

    private fun seedLocalCouncilData() {
        log("Starting to seed local council data")

        val prsdbUserStmt = nftDataSeederDao.preparePrsdbUserStatement()
        val localCouncilUserStmt = nftDataSeederDao.prepareLocalCouncilUserStatement()
        val localCouncilInvitationStmt = nftDataSeederDao.prepareLocalCouncilInvitationStatement()

        try {
            val localCouncilIds = localCouncilRepository.findAllId()

            repeat(NUM_OF_LC_USERS) {
                val isManager = NftDataFaker.generateBoolean(probabilityTrue = 0.2)
                val localCouncilId = localCouncilIds.random()

                val hasUserRegistered = NftDataFaker.generateBoolean(probabilityTrue = 0.75)
                if (hasUserRegistered) {
                    addLcUserToBatch(prsdbUserStmt, localCouncilUserStmt, isManager, localCouncilId)
                } else {
                    addLcInvitationToBatch(localCouncilInvitationStmt, isManager, localCouncilId)
                }
            }

            prsdbUserStmt.executeBatch()
            localCouncilUserStmt.executeBatch()
            localCouncilInvitationStmt.executeBatch()

            log("Seeded $NUM_OF_LC_USERS local council users/invitations")
        } finally {
            prsdbUserStmt.close()
            localCouncilUserStmt.close()
            localCouncilInvitationStmt.close()
        }

        log("Finished seeding local council data")
    }

    private fun seedLandlordData() {
        log("Starting to seed landlord data")

        val prsdbUserStmt = nftDataSeederDao.preparePrsdbUserStatement()
        val registrationNumberStmt = nftDataSeederDao.prepareRegistrationNumberStatement()
        val landlordStmt = nftDataSeederDao.prepareLandlordStatement()

        val licenceStmt = nftDataSeederDao.prepareLicenceStatement()
        val propertyOwnershipStmt = nftDataSeederDao.preparePropertyOwnershipStatement()
        val landlordMembershipStmt = nftDataSeederDao.prepareLandlordshipMembersStatement()

        val fileUploadStmt = nftDataSeederDao.prepareFileUploadStatement()
        val gasSafetyFileUploadsStmt = nftDataSeederDao.prepareGasSafetyFileUploadsStatement()
        val electricalSafetyFileUploadsStmt = nftDataSeederDao.prepareElectricalSafetyFileUploadsStatement()
        val propertyComplianceStmt = nftDataSeederDao.preparePropertyComplianceStatement()

        val reminderEmailSentStmt = nftDataSeederDao.prepareReminderEmailSentStatement()
        val savedJourneyStateStmt = nftDataSeederDao.prepareSavedJourneyStateStatement()
        val incompletePropertyStmt = nftDataSeederDao.prepareLandlordIncompletePropertyStatement()

        try {
            var registrationNumbersAdded = 0

            var licencesAdded = 0
            var propertyOwnershipsAdded = 0

            var fileUploadsAdded = 0
            var complianceRecordsAdded = 0

            var reminderEmailsAdded = 0
            var incompletePropertiesAdded = 0

            fun propertyRegistrationsAdded() = propertyOwnershipsAdded + incompletePropertiesAdded

            val numOfLandlordBatches = ceil(NUM_OF_LANDLORDS.toFloat() / BATCH_SIZE).toInt()
            for (landlordBatchNum in 1..numOfLandlordBatches) {
                val landlordIdRange = ((landlordBatchNum - 1) * BATCH_SIZE + 1)..min(landlordBatchNum * BATCH_SIZE, NUM_OF_LANDLORDS)
                val coreDetailsForLandlords = NftDataFaker.generateCoreDetailsForLandlords(landlordIdRange.toList())

                coreDetailsForLandlords.forEach {
                    addLandlordToBatch(
                        prsdbUserStmt,
                        registrationNumberStmt,
                        landlordStmt,
                        it,
                        registrationNumberId = (++registrationNumbersAdded).toLong(),
                    )
                }

                prsdbUserStmt.executeBatch()
                registrationNumberStmt.executeBatch()
                landlordStmt.executeBatch()
                registrationNumberGenerator.forgetUsedValues()
                landlordAddressGenerator.forgetUsedValues()

                log("Seeded ${landlordIdRange.last} landlords")

                coreDetailsForLandlords.forEach { landlord ->
                    val numOfPropertiesLeft = NUM_OF_PROPERTIES - propertyRegistrationsAdded()
                    val numOfPropertiesForLandlord = NftDataFaker.generateNumberOfPropertiesForLandlord().coerceAtMost(numOfPropertiesLeft)

                    repeat(numOfPropertiesForLandlord) {
                        val isRegistrationComplete = NftDataFaker.generateBoolean(probabilityTrue = 0.9)
                        if (isRegistrationComplete) {
                            val isOccupied = NftDataFaker.generateBoolean(probabilityTrue = 0.8)
                            val hasLicence = NftDataFaker.generateBoolean(probabilityTrue = 0.2)
                            val propertyOwnershipId = (++propertyOwnershipsAdded).toLong()

                            val propertyOwnershipCreatedDate =
                                addPropertyOwnershipToBatchReturningCreatedDate(
                                    registrationNumberStmt,
                                    propertyOwnershipStmt,
                                    landlordMembershipStmt,
                                    licenceStmt,
                                    isOccupied,
                                    registrationNumberId = (++registrationNumbersAdded).toLong(),
                                    licenceIdIfHasLicence = if (hasLicence) (++licencesAdded).toLong() else null,
                                    propertyOwnershipId,
                                    landlord,
                                    licenseProvideLater = if (!hasLicence) NftDataFaker.generateBoolean(probabilityTrue = 0.4) else null,
                                    tenancyProvideLater = if (isOccupied) NftDataFaker.generateBoolean(probabilityTrue = 0.4) else null,
                                )

                            val complianceId = (++complianceRecordsAdded).toLong()
                            fileUploadsAdded =
                                addPropertyComplianceToBatchReturningUpdatedFileUploadsAdded(
                                    fileUploadStmt,
                                    gasSafetyFileUploadsStmt,
                                    electricalSafetyFileUploadsStmt,
                                    propertyComplianceStmt,
                                    complianceId,
                                    propertyOwnershipId,
                                    propertyOwnershipCreatedDate,
                                    fileUploadsAdded,
                                )
                        } else {
                            val hasReminderEmailBeenSent = NftDataFaker.generateBoolean(probabilityTrue = 0.25)
                            addIncompletePropertyToBatch(
                                reminderEmailSentStmt,
                                savedJourneyStateStmt,
                                incompletePropertyStmt,
                                reminderEmailSentIdIfSent = if (hasReminderEmailBeenSent) (++reminderEmailsAdded).toLong() else null,
                                savedJourneyStateId = (++incompletePropertiesAdded).toLong(),
                                landlord,
                            )
                        }

                        if (propertyOwnershipsAdded % BATCH_SIZE == 0 || propertyRegistrationsAdded() == NUM_OF_PROPERTIES) {
                            registrationNumberStmt.executeBatch()
                            licenceStmt.executeBatch()
                            propertyOwnershipStmt.executeBatch()
                            landlordMembershipStmt.executeBatch()
                            registrationNumberGenerator.forgetUsedValues()
                            propertyOwnershipAddressGenerator.forgetUsedValues()

                            fileUploadStmt.executeBatch()
                            propertyComplianceStmt.executeBatch()
                            gasSafetyFileUploadsStmt.executeBatch()
                            electricalSafetyFileUploadsStmt.executeBatch()
                        }
                        if (incompletePropertiesAdded % BATCH_SIZE == 0 || propertyRegistrationsAdded() == NUM_OF_PROPERTIES) {
                            reminderEmailSentStmt.executeBatch()
                            savedJourneyStateStmt.executeBatch()
                            incompletePropertyStmt.executeBatch()
                            incompletePropertyAddressGenerator.forgetUsedValues()
                        }

                        if (propertyRegistrationsAdded() % BATCH_SIZE == 0 || propertyRegistrationsAdded() == NUM_OF_PROPERTIES) {
                            log("Seeded ${propertyRegistrationsAdded()} property ownerships/incomplete properties")
                        }
                    }
                }
            }
        } finally {
            prsdbUserStmt.close()
            registrationNumberStmt.close()
            landlordStmt.close()

            licenceStmt.close()
            propertyOwnershipStmt.close()
            landlordMembershipStmt.close()

            fileUploadStmt.close()
            gasSafetyFileUploadsStmt.close()
            electricalSafetyFileUploadsStmt.close()
            propertyComplianceStmt.close()

            reminderEmailSentStmt.close()
            savedJourneyStateStmt.close()
            incompletePropertyStmt.close()
        }

        log("Finished seeding landlord data")
    }

    private fun addSystemOperatorToBatch(
        prsdbUserStmt: PreparedStatement,
        systemOperatorStmt: PreparedStatement,
    ) {
        val subjectIdentifier = NftDataFaker.generateSubjectIdentifier()
        val createdDate = NftDataFaker.generateCreatedDate()

        prsdbUserStmt.setString(1, subjectIdentifier)
        prsdbUserStmt.setTimestamp(2, createdDate)
        prsdbUserStmt.addBatch()

        systemOperatorStmt.setTimestamp(1, createdDate)
        systemOperatorStmt.setTimestamp(2, NftDataFaker.generateLastModifiedDate(createdDate))
        systemOperatorStmt.setString(3, subjectIdentifier)
        systemOperatorStmt.addBatch()
    }

    private fun addLcUserToBatch(
        prsdbUserStmt: PreparedStatement,
        localCouncilUserStmt: PreparedStatement,
        isManager: Boolean,
        localCouncilId: Int,
    ) {
        val subjectIdentifier = NftDataFaker.generateSubjectIdentifier()
        val createdDate = NftDataFaker.generateCreatedDate()

        prsdbUserStmt.setString(1, subjectIdentifier)
        prsdbUserStmt.setTimestamp(2, createdDate)
        prsdbUserStmt.addBatch()

        val name = NftDataFaker.generateName()

        localCouncilUserStmt.setTimestamp(1, createdDate)
        localCouncilUserStmt.setTimestamp(2, NftDataFaker.generateLastModifiedDate(createdDate))
        localCouncilUserStmt.setString(3, subjectIdentifier)
        localCouncilUserStmt.setBoolean(4, isManager)
        localCouncilUserStmt.setString(5, name)
        localCouncilUserStmt.setString(6, NftDataFaker.generateEmail(name))
        localCouncilUserStmt.setInt(7, localCouncilId)
        localCouncilUserStmt.addBatch()
    }

    private fun addLcInvitationToBatch(
        localCouncilInvitationStmt: PreparedStatement,
        isManager: Boolean,
        localCouncilId: Int,
    ) {
        localCouncilInvitationStmt.setTimestamp(1, NftDataFaker.generateCreatedDate())
        localCouncilInvitationStmt.setObject(2, NftDataFaker.generateInvitationToken())
        localCouncilInvitationStmt.setString(3, NftDataFaker.generateEmail())
        localCouncilInvitationStmt.setBoolean(4, isManager)
        localCouncilInvitationStmt.setInt(5, localCouncilId)
        localCouncilInvitationStmt.addBatch()
    }

    private fun addLandlordToBatch(
        prsdbUserStmt: PreparedStatement,
        registrationNumberStmt: PreparedStatement,
        landlordStmt: PreparedStatement,
        coreDetails: CoreLandlordDetails,
        registrationNumberId: Long,
    ) {
        prsdbUserStmt.setString(1, coreDetails.subjectId)
        prsdbUserStmt.setTimestamp(2, coreDetails.createdDate)
        prsdbUserStmt.addBatch()

        registrationNumberStmt.setLong(1, registrationNumberId)
        registrationNumberStmt.setTimestamp(2, coreDetails.createdDate)
        registrationNumberStmt.setLong(3, registrationNumberGenerator.next())
        registrationNumberStmt.setInt(4, RegistrationNumberType.LANDLORD.ordinal)
        registrationNumberStmt.addBatch()

        val name = NftDataFaker.generateName()
        val isVerified = NftDataFaker.generateBoolean(probabilityTrue = 0.8)

        landlordStmt.setLong(1, coreDetails.id)
        landlordStmt.setTimestamp(2, coreDetails.createdDate)
        landlordStmt.setTimestamp(3, NftDataFaker.generateLastModifiedDate(coreDetails.createdDate))
        landlordStmt.setString(4, coreDetails.subjectId)
        landlordStmt.setString(5, name)
        landlordStmt.setString(6, NftDataFaker.generateEmail(name))
        landlordStmt.setString(7, NftDataFaker.generatePhoneNumber())
        landlordStmt.setLong(8, landlordAddressGenerator.next().id)
        landlordStmt.setDate(9, NftDataFaker.generateDateOfBirth())
        landlordStmt.setLong(10, registrationNumberId)
        landlordStmt.setBoolean(11, isVerified)
        landlordStmt.addBatch()
    }

    private fun addPropertyOwnershipToBatchReturningCreatedDate(
        registrationNumberStmt: PreparedStatement,
        propertyOwnershipStmt: PreparedStatement,
        membershipStmt: PreparedStatement,
        licenceStmt: PreparedStatement,
        isOccupied: Boolean,
        registrationNumberId: Long,
        licenceIdIfHasLicence: Long?,
        propertyOwnershipId: Long,
        landlordDetails: CoreLandlordDetails,
        licenseProvideLater: Boolean?,
        tenancyProvideLater: Boolean?,
    ): Timestamp {
        val createdDate = NftDataFaker.generateCreatedDate(after = landlordDetails.createdDate)

        registrationNumberStmt.setLong(1, registrationNumberId)
        registrationNumberStmt.setTimestamp(2, createdDate)
        registrationNumberStmt.setLong(3, registrationNumberGenerator.next())
        registrationNumberStmt.setInt(4, RegistrationNumberType.PROPERTY.ordinal)
        registrationNumberStmt.addBatch()

        if (licenceIdIfHasLicence != null) {
            val licenceTypeAndNumber = NftDataFaker.generateLicenceTypeAndNumber()

            licenceStmt.setLong(1, licenceIdIfHasLicence)
            licenceStmt.setTimestamp(2, createdDate)
            licenceStmt.setTimestamp(3, NftDataFaker.generateLastModifiedDate(createdDate))
            licenceStmt.setInt(4, licenceTypeAndNumber.first.ordinal)
            licenceStmt.setString(5, licenceTypeAndNumber.second)
            licenceStmt.addBatch()
        }

        // A "provide tenancy details later" property has no tenancy details yet, mirroring the real app where the details
        // are cleared/absent until the landlord provides them. Only generate details when occupied and not provide-later.
        val hasTenancyDetails = isOccupied && tenancyProvideLater != true
        val numHouseholdsAndTenants = if (hasTenancyDetails) NftDataFaker.generateNumHouseholdsAndTenants() else Pair(0, 0)
        val numBedrooms = if (isOccupied) NftDataFaker.generateNumBedrooms() else null
        val standardAndCustomBillsIncluded = if (hasTenancyDetails) NftDataFaker.generateStandardAndCustomBillsIncluded() else null
        val furnishedStatus = if (hasTenancyDetails) NftDataFaker.generateFurnishedStatus() else null
        val rentDetails = if (hasTenancyDetails) NftDataFaker.generateRentDetails() else null

        propertyOwnershipStmt.setLong(1, propertyOwnershipId)
        propertyOwnershipStmt.setTimestamp(2, createdDate)
        propertyOwnershipStmt.setTimestamp(3, NftDataFaker.generateLastModifiedDate(createdDate))
        propertyOwnershipStmt.setInt(4, NftDataFaker.generateOwnershipType().ordinal)
        propertyOwnershipStmt.setInt(5, numHouseholdsAndTenants.first)
        propertyOwnershipStmt.setInt(6, numHouseholdsAndTenants.second)
        propertyOwnershipStmt.setLong(7, registrationNumberId)
        propertyOwnershipStmt.setLongOrNull(8, licenceIdIfHasLicence)
        propertyOwnershipStmt.setInt(9, NftDataFaker.generatePropertyAndOtherType().first.ordinal)
        propertyOwnershipStmt.setLong(10, propertyOwnershipAddressGenerator.next().id)
        propertyOwnershipStmt.setIntOrNull(11, numBedrooms)
        propertyOwnershipStmt.setStringOrNull(12, standardAndCustomBillsIncluded?.first)
        propertyOwnershipStmt.setStringOrNull(13, standardAndCustomBillsIncluded?.second)
        propertyOwnershipStmt.setIntOrNull(14, furnishedStatus?.ordinal)
        propertyOwnershipStmt.setIntOrNull(15, rentDetails?.rentFrequency?.ordinal)
        propertyOwnershipStmt.setStringOrNull(16, rentDetails?.customRentFrequency)
        propertyOwnershipStmt.setBigDecimalOrNull(17, rentDetails?.rentAmount)
        propertyOwnershipStmt.setBoolean(18, isOccupied)
        propertyOwnershipStmt.setBooleanOrNull(19, licenseProvideLater)
        propertyOwnershipStmt.setBooleanOrNull(20, tenancyProvideLater)
        propertyOwnershipStmt.setString(21, NftDataFaker.generateEmail())
        propertyOwnershipStmt.setLong(22, propertyOwnershipAddressGenerator.next().id)
        propertyOwnershipStmt.addBatch()

        membershipStmt.setLong(1, landlordDetails.id)
        membershipStmt.setLong(2, propertyOwnershipId)
        membershipStmt.setTimestamp(3, createdDate)
        membershipStmt.addBatch()

        return createdDate
    }

    private fun addIncompletePropertyToBatch(
        reminderEmailSentStmt: PreparedStatement,
        savedJourneyStateStmt: PreparedStatement,
        incompletePropertiesStmt: PreparedStatement,
        reminderEmailSentIdIfSent: Long?,
        savedJourneyStateId: Long,
        landlordDetails: CoreLandlordDetails,
    ) {
        val reminderEmailSent = reminderEmailSentIdIfSent != null
        val createdDate = NftDataFaker.generateIncompletePropertyCreatedDate(landlordDetails.createdDate, reminderEmailSent)

        if (reminderEmailSent) {
            reminderEmailSentStmt.setLong(1, reminderEmailSentIdIfSent)
            reminderEmailSentStmt.setTimestamp(2, NftDataFaker.generateLastEmailReminderSentDate(createdDate))
            reminderEmailSentStmt.addBatch()
        }

        val address = incompletePropertyAddressGenerator.next()

        savedJourneyStateStmt.setLong(1, savedJourneyStateId)
        savedJourneyStateStmt.setTimestamp(2, createdDate)
        savedJourneyStateStmt.setTimestamp(3, NftDataFaker.generateLastModifiedDate(createdDate))
        savedJourneyStateStmt.setString(4, NftDataFaker.generateJourneyId())
        savedJourneyStateStmt.setString(5, NftDataFaker.generateIncompletePropertyJourneyState(address))
        savedJourneyStateStmt.setString(6, landlordDetails.subjectId)
        savedJourneyStateStmt.setLongOrNull(7, reminderEmailSentIdIfSent)
        savedJourneyStateStmt.addBatch()

        incompletePropertiesStmt.setString(1, landlordDetails.subjectId)
        incompletePropertiesStmt.setLong(2, savedJourneyStateId)
        incompletePropertiesStmt.addBatch()
    }

    private fun addPropertyComplianceToBatchReturningUpdatedFileUploadsAdded(
        fileUploadStmt: PreparedStatement,
        gasSafetyFileUploadsStmt: PreparedStatement,
        electricalSafetyFileUploadsStmt: PreparedStatement,
        propertyComplianceStmt: PreparedStatement,
        complianceId: Long,
        propertyOwnershipId: Long,
        propertyOwnershipCreatedDate: Timestamp,
        currentFileUploadCount: Int,
    ): Int {
        val createdDate = NftDataFaker.generateCreatedDate(after = propertyOwnershipCreatedDate)
        val complianceData = NftDataFaker.generatePropertyComplianceData(createdDate)

        var updatedFileUploadCount = currentFileUploadCount

        if (complianceData.gasSafetyCertIssueDate?.after(
                Date.valueOf(
                    java.time.LocalDate
                        .now()
                        .minusYears(GAS_SAFETY_CERT_VALIDITY_YEARS.toLong()),
                ),
            ) ==
            true
        ) {
            val gasSafetyUploadId = (++updatedFileUploadCount).toLong()
            addFileUploadToBatch(
                fileUploadStmt,
                propertyOwnershipId,
                createdDate,
                CertificateType.GasSafetyCert,
                gasSafetyUploadId,
            )
            gasSafetyFileUploadsStmt.setLong(1, complianceId)
            gasSafetyFileUploadsStmt.setLong(2, gasSafetyUploadId)
            gasSafetyFileUploadsStmt.addBatch()
        }
        if (complianceData.electricalSafetyExpiryDate?.after(createdDate) == true) {
            val eicrUploadId = (++updatedFileUploadCount).toLong()
            addFileUploadToBatch(
                fileUploadStmt,
                propertyOwnershipId,
                createdDate,
                complianceData.electricalCertType!!,
                eicrUploadId,
            )
            electricalSafetyFileUploadsStmt.setLong(1, complianceId)
            electricalSafetyFileUploadsStmt.setLong(2, eicrUploadId)
            electricalSafetyFileUploadsStmt.addBatch()
        }

        propertyComplianceStmt.setLong(1, complianceId)
        propertyComplianceStmt.setTimestamp(2, createdDate)
        propertyComplianceStmt.setTimestamp(3, NftDataFaker.generateLastModifiedDate(createdDate))
        propertyComplianceStmt.setLong(4, propertyOwnershipId)
        propertyComplianceStmt.setDateOrNull(5, complianceData.gasSafetyCertIssueDate)
        propertyComplianceStmt.setBooleanOrNull(6, complianceData.hasGasSupply)
        propertyComplianceStmt.setDateOrNull(7, complianceData.electricalSafetyExpiryDate)
        propertyComplianceStmt.setIntOrNull(8, complianceData.electricalCertType?.ordinal)
        propertyComplianceStmt.setStringOrNull(
            9,
            complianceData.epcNumber?.let {
                "$epcCertificateBaseUrl/${EpcDataModel.parseCertificateNumberOrNull(it)}"
            },
        )
        propertyComplianceStmt.setDateOrNull(10, complianceData.epcExpiryDate)
        propertyComplianceStmt.setBooleanOrNull(11, complianceData.tenancyStartedBeforeEpcExpiry)
        propertyComplianceStmt.setStringOrNull(12, complianceData.epcEnergyRating)
        propertyComplianceStmt.setIntOrNull(13, complianceData.epcExemptionReason?.ordinal)
        propertyComplianceStmt.setIntOrNull(14, complianceData.epcMeesExemptionReason?.ordinal)
        propertyComplianceStmt.addBatch()

        return updatedFileUploadCount
    }

    // TODO PDJB-239: Upload files to S3
    private fun addFileUploadToBatch(
        fileUploadStmt: PreparedStatement,
        propertyOwnershipId: Long,
        createdDate: Timestamp,
        certificateType: CertificateType,
        fileUploadId: Long,
    ) {
        val stepName = CertificateFilenameHelper.getUploadStepName(certificateType)
        val fakeJourneyId = "seed-$propertyOwnershipId"
        val fakeMemberId = "member-$fileUploadId"

        fileUploadStmt.setLong(1, fileUploadId)
        fileUploadStmt.setTimestamp(2, createdDate)
        fileUploadStmt.setTimestamp(3, NftDataFaker.generateLastModifiedDate(createdDate))
        fileUploadStmt.setString(4, CertificateFilenameHelper.getCertFilename(fakeJourneyId, stepName, fakeMemberId))
        fileUploadStmt.setString(5, NftDataFaker.generateETag())
        fileUploadStmt.setString(6, "fake-certificate.png")
        fileUploadStmt.addBatch()
    }

    private fun log(message: String) {
        println("${LocalDateTime.now()} $message")
    }

    companion object {
        const val NUM_OF_SYSTEM_OPERATORS = 150
        const val NUM_OF_LC_USERS = 3000
        const val NUM_OF_LANDLORDS = 2820000
        const val NUM_OF_PROPERTIES = 4700000
        const val BATCH_SIZE = 10000
    }

    private abstract class Generator<T> {
        protected var values = emptyList<T>()
        private var index = 0

        abstract fun replenishValues()

        fun next(): T {
            while (index == values.size) {
                replenishValues()
            }
            return values[index++]
        }

        fun forgetUsedValues() {
            values = values.drop(index)
            index = 0
        }
    }

    private inner class RegistrationNumberGenerator : Generator<Long>() {
        override fun replenishValues() {
            val valueSet = values.toSet()
            val potentialNewNumbers = NftDataFaker.generateRegistrationNumbers(BATCH_SIZE * 5)
            val alreadyUsedNumbers = nftDataSeederDao.findRegistrationNumbersIn(potentialNewNumbers).toSet()
            val newNumbers = potentialNewNumbers - alreadyUsedNumbers
            values = (valueSet + newNumbers).toList()
        }
    }

    private val addressCount by lazy { addressRepository.count().toInt() }

    private inner class AddressGenerator(
        private val restrictToAvailable: Boolean = false,
    ) : Generator<Address>() {
        private val replenishmentSize = BATCH_SIZE * 5

        override fun replenishValues() {
            val valueSet = values.toSet()
            val newAddresses =
                nftDataSeederDao.findAddresses(
                    limit = replenishmentSize,
                    offset = NftDataFaker.generateNumberLessThan(addressCount - replenishmentSize),
                    restrictToAvailable,
                )
            values = (valueSet + newAddresses.shuffled()).toList()
        }
    }
}
