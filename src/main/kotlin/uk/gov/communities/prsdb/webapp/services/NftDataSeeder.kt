package uk.gov.communities.prsdb.webapp.services

import org.hibernate.SessionFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Profile
import uk.gov.communities.prsdb.webapp.annotations.taskAnnotations.PrsdbTaskService
import uk.gov.communities.prsdb.webapp.constants.enums.CertificateType
import uk.gov.communities.prsdb.webapp.constants.enums.LandlordType
import uk.gov.communities.prsdb.webapp.constants.enums.RegistrationNumberType
import uk.gov.communities.prsdb.webapp.database.dao.NftDataSeederDao
import uk.gov.communities.prsdb.webapp.database.entity.Address
import uk.gov.communities.prsdb.webapp.database.repository.LocalCouncilRepository
import uk.gov.communities.prsdb.webapp.helpers.CertificateFilenameHelper
import uk.gov.communities.prsdb.webapp.helpers.DateTimeHelper
import uk.gov.communities.prsdb.webapp.helpers.NftDataFaker
import uk.gov.communities.prsdb.webapp.helpers.NftDataFaker.CoreLandlordDetails
import uk.gov.communities.prsdb.webapp.helpers.NftDataFaker.PropertyScenario
import uk.gov.communities.prsdb.webapp.helpers.extensions.PreparedStatementExtensions.Companion.setBigDecimalOrNull
import uk.gov.communities.prsdb.webapp.helpers.extensions.PreparedStatementExtensions.Companion.setBooleanOrNull
import uk.gov.communities.prsdb.webapp.helpers.extensions.PreparedStatementExtensions.Companion.setDateOrNull
import uk.gov.communities.prsdb.webapp.helpers.extensions.PreparedStatementExtensions.Companion.setIntOrNull
import uk.gov.communities.prsdb.webapp.helpers.extensions.PreparedStatementExtensions.Companion.setLongOrNull
import uk.gov.communities.prsdb.webapp.helpers.extensions.PreparedStatementExtensions.Companion.setStringOrNull
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps.UploadEicStep
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps.UploadEicrStep
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps.UploadGasCertStep
import uk.gov.communities.prsdb.webapp.models.dataModels.EpcDataModel
import java.sql.Connection
import java.sql.Date
import java.sql.PreparedStatement
import java.sql.Timestamp
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.MonthDay
import java.time.ZoneOffset
import kotlin.math.min

@PrsdbTaskService
@Profile("nft-data-seeder")
class NftDataSeeder(
    private val sessionFactory: SessionFactory,
    private val localCouncilRepository: LocalCouncilRepository,
    @Value("\${epc.certificate-base-url}")
    private val epcCertificateBaseUrl: String,
    @Value("\${nft-seed.system-operators}")
    private val numOfSystemOperators: Int,
    @Value("\${nft-seed.local-council-users}")
    private val numOfLcUsers: Int,
    @Value("\${nft-seed.landlords}")
    private val numOfLandlords: Int,
    @Value("\${nft-seed.properties}")
    private val numOfProperties: Int,
    @Value("\${nft-seed.batch-size}")
    private val batchSize: Int,
    @Value("\${nft-seed.random-seed}")
    private val randomSeed: Long,
    @Value("\${nft-seed.reference-date:}")
    private val referenceDate: String,
    @Value("\${nft-seed.generated-addresses}")
    private val numOfGeneratedAddresses: Int,
    @Value("\${nft-seed.minimum-free-addresses:0}")
    private val minimumFreeAddresses: Int = 0,
    @Value("\${nft-seed.incomplete-properties:0}")
    private val numOfIncompleteProperties: Int = 0,
) {
    private lateinit var nftDataSeederDao: NftDataSeederDao
    private lateinit var startingIds: Map<String, Long>
    private var eligibleAddressCount = 0
    private var remainingAvailableAddressCount = 0

    private val registrationNumberGenerator = RegistrationNumberGenerator()

    // Addresses for people and organisations rather than for the property itself. Unrestricted, so it
    // draws from every eligible address and does not consume the pool of addresses still available to
    // be registered as a property.
    private val contactAddressGenerator = AddressGenerator()
    private val propertyAddressGenerator = PropertyAddressGenerator()

    fun seedDatabase() {
        require(batchSize > 0) { "nft-seed.batch-size must be positive" }
        require(
            listOf(
                numOfSystemOperators,
                numOfLcUsers,
                numOfLandlords,
                numOfProperties,
                numOfIncompleteProperties,
                numOfGeneratedAddresses,
                minimumFreeAddresses,
            )
                .all { it >= 0 },
        ) { "NFT seed counts and minimum-free-addresses must not be negative" }
        val totalProperties = Math.addExact(numOfProperties, numOfIncompleteProperties)
        NftDataFaker.reset(seed = randomSeed, reference = resolveReferenceInstant())
        val statelessSession = sessionFactory.openStatelessSession()
        statelessSession.use { session ->
            val transaction = session.beginTransaction()
            try {
                session.doWork { connection: Connection ->
                    nftDataSeederDao = NftDataSeederDao(session, connection)
                    startingIds = nftDataSeederDao.findStartingIds()
                    nftDataSeederDao.updateIdSequences()
                    seedGeneratedAddressData()
                    nftDataSeederDao.snapshotIncompleteAddressAllocations()
                    eligibleAddressCount = nftDataSeederDao.countAvailableAddresses(restrictToAvailable = false)
                    remainingAvailableAddressCount = nftDataSeederDao.countAvailableAddresses(restrictToAvailable = true)
                    check(totalProperties.toLong() + minimumFreeAddresses <= remainingAvailableAddressCount) {
                        "not enough addresses to seed $totalProperties properties: " +
                            "only $remainingAvailableAddressCount addresses are available " +
                            "and $minimumFreeAddresses free addresses must remain. " +
                            "Reduce the NFT seed property count or add more generated addresses."
                    }
                    seedSystemOperatorData()
                    seedLocalCouncilData()
                    seedLandlordData()
                    nftDataSeederDao.updateIdSequences()
                    log("Finished seeding: $remainingAvailableAddressCount unallocated addresses remain for subsequent journeys")
                }
                transaction.commit()
            } catch (e: Exception) {
                transaction.rollback()
                throw e
            }
        }
    }

    /**
     * Generated dates are relative to this instant. Leaving `nft-seed.reference-date` blank uses the current time,
     * which keeps data realistic; setting it to an ISO date (e.g. 2026-01-01) makes a run fully reproducible.
     */
    private fun resolveReferenceInstant(): Instant =
        if (referenceDate.isBlank()) {
            Instant.now()
        } else {
            LocalDate.parse(referenceDate).atStartOfDay(ZoneOffset.UTC).toInstant()
        }

    /**
     * Generates fictional addresses so the seeder can run without the real NGD address data, which requires an OS API
     * key and a large download. Deployed NFT leaves `nft-seed.generated-addresses` at 0 and uses the NGD data instead.
     */
    private fun seedGeneratedAddressData() {
        if (numOfGeneratedAddresses <= 0) return

        log("Starting to seed generated address data")

        val addressStmt = nftDataSeederDao.prepareAddressStatement()

        try {
            val localCouncilIds = localCouncilRepository.findAllId()
            check(localCouncilIds.isNotEmpty()) { "Cannot generate addresses: no local councils exist." }

            var addressesAdded = 0
            var uprn = GENERATED_UPRN_BASE + nftDataSeederDao.countAddresses()

            while (addressesAdded < numOfGeneratedAddresses) {
                val generatedStreet = NftDataFaker.generateFakeStreet()
                val street = generatedStreet.copy(streetName = "${generatedStreet.streetName} ${uprn - GENERATED_UPRN_BASE}")
                val localCouncilId = NftDataFaker.pickOne(localCouncilIds)
                val addressesOnStreet =
                    min(NftDataFaker.generateNumberOfAddressesOnStreet(), numOfGeneratedAddresses - addressesAdded)

                for (buildingNumber in 1..addressesOnStreet) {
                    addAddressToBatch(addressStmt, uprn++, buildingNumber, street, localCouncilId)
                    addressesAdded++

                    if (addressesAdded % batchSize == 0) {
                        addressStmt.executeBatch()
                        log("Seeded $addressesAdded addresses")
                    }
                }
            }

            addressStmt.executeBatch()

            log("Finished seeding generated address data: $addressesAdded addresses in total")
        } finally {
            addressStmt.close()
        }
    }

    private fun addAddressToBatch(
        addressStmt: PreparedStatement,
        uprn: Long,
        buildingNumber: Int,
        street: NftDataFaker.FakeStreet,
        localCouncilId: Int,
    ) {
        val createdDate = NftDataFaker.generateCreatedDate()
        addressStmt.setTimestamp(1, createdDate)
        addressStmt.setTimestamp(2, NftDataFaker.generateLastModifiedDate(createdDate))
        addressStmt.setLong(3, uprn)
        addressStmt.setString(4, NftDataFaker.buildSingleLineAddress(buildingNumber, street))
        addressStmt.setString(5, buildingNumber.toString())
        addressStmt.setString(6, street.streetName)
        addressStmt.setString(7, street.townName)
        addressStmt.setString(8, street.postcode)
        addressStmt.setInt(9, localCouncilId)
        addressStmt.addBatch()
    }

    private fun seedSystemOperatorData() {
        log("Starting to seed system operator data")

        val prsdbUserStmt = nftDataSeederDao.preparePrsdbUserStatement()
        val systemOperatorStmt = nftDataSeederDao.prepareSystemOperatorStatement()

        try {
            repeat(numOfSystemOperators) { addSystemOperatorToBatch(prsdbUserStmt, systemOperatorStmt) }

            prsdbUserStmt.executeBatch()
            systemOperatorStmt.executeBatch()

            log("Seeded $numOfSystemOperators system operators")
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

            repeat(numOfLcUsers) {
                val isManager = NftDataFaker.generateBoolean(probabilityTrue = 0.2)
                val localCouncilId = NftDataFaker.pickOne(localCouncilIds)

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

            log("Seeded $numOfLcUsers local council users/invitations")
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
        val individualLandlordStmt = nftDataSeederDao.prepareIndividualLandlordStatement()
        val organisationLandlordStmt = nftDataSeederDao.prepareOrganisationLandlordStatement()
        val organisationalLandlordUserStmt = nftDataSeederDao.prepareOrganisationalLandlordUserStatement()
        val organisationalLandlordInvitationStmt = nftDataSeederDao.prepareOrganisationalLandlordInvitationStatement()
        val organisationGoverningBodyMemberStmt = nftDataSeederDao.prepareOrganisationGoverningBodyMemberStatement()

        val licenceStmt = nftDataSeederDao.prepareLicenceStatement()
        val propertyManagerStmt = nftDataSeederDao.preparePropertyManagerStatement()
        val propertyOwnershipStmt = nftDataSeederDao.preparePropertyOwnershipStatement()
        val freeholderStmt = nftDataSeederDao.prepareFreeholderStatement()
        val superiorLandlordStmt = nftDataSeederDao.prepareSuperiorLandlordStatement()
        val ownershipPaymentStmt = nftDataSeederDao.prepareOwnedPropertyPaymentStatement()
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
            var orgUsersAdded = 0

            var licencesAdded = 0
            var propertyManagersAdded = 0
            var propertyOwnershipsAdded = 0
            var fileUploadsAdded = 0
            var complianceRecordsAdded = 0

            var reminderEmailsAdded = 0
            var incompletePropertiesAdded = 0

            fun propertyRegistrationsAdded() = propertyOwnershipsAdded + incompletePropertiesAdded
            val totalProperties = numOfProperties + numOfIncompleteProperties
            var landlordsAdded = 0

            while (
                if (numOfLandlords == 0) propertyRegistrationsAdded() < totalProperties else landlordsAdded < numOfLandlords
            ) {
                val landlordsInBatch = if (numOfLandlords == 0) batchSize else min(batchSize, numOfLandlords - landlordsAdded)
                val landlordIdRange = (landlordsAdded + 1)..Math.addExact(landlordsAdded, landlordsInBatch)
                val landlordsToSeed =
                    generateLandlordsToSeed(
                        landlordIdRange.toList(),
                        remainingCompleted = numOfProperties - propertyOwnershipsAdded,
                        remainingIncomplete = numOfIncompleteProperties - incompletePropertiesAdded,
                    )
                landlordsAdded += landlordsInBatch

                landlordsToSeed.forEach {
                    addLandlordToBatch(
                        prsdbUserStmt,
                        registrationNumberStmt,
                        individualLandlordStmt,
                        organisationLandlordStmt,
                        organisationalLandlordUserStmt,
                        organisationalLandlordInvitationStmt,
                        organisationGoverningBodyMemberStmt,
                        it.details,
                        it.anniversary,
                        hasRegisteredProperty = it.properties.any { property -> property.scenario.isRegistrationComplete },
                        registrationNumberId = startingIds.getValue("registration_number") + ++registrationNumbersAdded,
                    )

                    orgUsersAdded += it.details.organisationDetails
                        ?.users
                        ?.size ?: 0
                }

                prsdbUserStmt.executeBatch()
                registrationNumberStmt.executeBatch()
                individualLandlordStmt.executeBatch()
                organisationLandlordStmt.executeBatch()
                organisationalLandlordUserStmt.executeBatch()
                organisationalLandlordInvitationStmt.executeBatch()
                organisationGoverningBodyMemberStmt.executeBatch()
                registrationNumberGenerator.forgetUsedValues()
                contactAddressGenerator.forgetUsedValues()

                log("Seeded ${landlordIdRange.last} landlords and $orgUsersAdded organisational landlord users")

                landlordsToSeed.forEach { landlord ->
                    landlord.properties.forEach { property ->
                        val scenario = property.scenario
                        if (scenario.isRegistrationComplete) {
                            val propertyOwnershipId = startingIds.getValue("property_ownership") + ++propertyOwnershipsAdded
                            val registrationDate = checkNotNull(property.registrationDate)

                            addPropertyOwnershipToBatch(
                                registrationNumberStmt,
                                propertyManagerStmt,
                                propertyOwnershipStmt,
                                freeholderStmt,
                                superiorLandlordStmt,
                                ownershipPaymentStmt,
                                landlordMembershipStmt,
                                licenceStmt,
                                scenario.isOccupied,
                                registrationNumberId = startingIds.getValue("registration_number") + ++registrationNumbersAdded,
                                licenceIdIfHasLicence =
                                    if (scenario.hasLicence) startingIds.getValue("license") + ++licencesAdded else null,
                                propertyManagerIdIfManaged =
                                    if (NftDataFaker.generateHasPropertyManager()) {
                                        startingIds.getValue("property_manager") + ++propertyManagersAdded
                                    } else {
                                        null
                                    },
                                propertyOwnershipId,
                                registrationDate,
                                landlord.details,
                                landlordAnniversary = checkNotNull(landlord.anniversary),
                                licenseProvideLater = scenario.licenseProvideLater,
                                tenancyProvideLater = scenario.tenancyProvideLater,
                            )
                            val complianceId = startingIds.getValue("property_compliance") + ++complianceRecordsAdded
                            fileUploadsAdded =
                                addPropertyComplianceToBatchReturningUpdatedFileUploadsAdded(
                                    fileUploadStmt,
                                    gasSafetyFileUploadsStmt,
                                    electricalSafetyFileUploadsStmt,
                                    propertyComplianceStmt,
                                    complianceId,
                                    propertyOwnershipId,
                                    registrationDate,
                                    fileUploadsAdded,
                                )
                        } else {
                            val hasReminderEmailBeenSent = NftDataFaker.generateReminderEmailSent()
                            addIncompletePropertyToBatch(
                                reminderEmailSentStmt,
                                savedJourneyStateStmt,
                                incompletePropertyStmt,
                                reminderEmailSentIdIfSent =
                                    if (hasReminderEmailBeenSent) {
                                        startingIds.getValue("reminder_email_sent") + ++reminderEmailsAdded
                                    } else {
                                        null
                                    },
                                savedJourneyStateId = startingIds.getValue("saved_journey_state") + ++incompletePropertiesAdded,
                                landlord.details,
                            )
                        }

                        if (
                            (scenario.isRegistrationComplete && propertyOwnershipsAdded % batchSize == 0) ||
                            propertyRegistrationsAdded() == totalProperties
                        ) {
                            registrationNumberStmt.executeBatch()
                            licenceStmt.executeBatch()
                            propertyManagerStmt.executeBatch()
                            propertyOwnershipStmt.executeBatch()
                            freeholderStmt.executeBatch()
                            superiorLandlordStmt.executeBatch()
                            ownershipPaymentStmt.executeBatch()
                            landlordMembershipStmt.executeBatch()
                            registrationNumberGenerator.forgetUsedValues()
                            // Associated party addresses are drawn here too, so bound this generator's
                            // retained values with the property batches rather than the landlord ones.
                            contactAddressGenerator.forgetUsedValues()
                            propertyAddressGenerator.forgetUsedValues()
                            fileUploadStmt.executeBatch()
                            propertyComplianceStmt.executeBatch()
                            gasSafetyFileUploadsStmt.executeBatch()
                            electricalSafetyFileUploadsStmt.executeBatch()
                        }
                        if (
                            (!scenario.isRegistrationComplete && incompletePropertiesAdded % batchSize == 0) ||
                            propertyRegistrationsAdded() == totalProperties
                        ) {
                            reminderEmailSentStmt.executeBatch()
                            savedJourneyStateStmt.executeBatch()
                            incompletePropertyStmt.executeBatch()
                            propertyAddressGenerator.forgetUsedValues()
                        }

                        if (propertyRegistrationsAdded() % batchSize == 0 || propertyRegistrationsAdded() == totalProperties) {
                            log("Seeded ${propertyRegistrationsAdded()} property ownerships/incomplete properties")
                        }
                    }
                }
            }

            // The per-iteration flushes above only fire on exact batch boundaries or when the property target is
            // reached. If the landlords run out first, the trailing partial batches would never be sent.
            registrationNumberStmt.executeBatch()
            licenceStmt.executeBatch()
            propertyManagerStmt.executeBatch()
            propertyOwnershipStmt.executeBatch()
            freeholderStmt.executeBatch()
            superiorLandlordStmt.executeBatch()
            ownershipPaymentStmt.executeBatch()
            landlordMembershipStmt.executeBatch()
            fileUploadStmt.executeBatch()
            propertyComplianceStmt.executeBatch()
            gasSafetyFileUploadsStmt.executeBatch()
            electricalSafetyFileUploadsStmt.executeBatch()
            reminderEmailSentStmt.executeBatch()
            savedJourneyStateStmt.executeBatch()
            incompletePropertyStmt.executeBatch()

            check(propertyOwnershipsAdded == numOfProperties && incompletePropertiesAdded == numOfIncompleteProperties) {
                "Requested $numOfProperties completed and $numOfIncompleteProperties incomplete properties " +
                    "but only generated $propertyOwnershipsAdded completed and $incompletePropertiesAdded incomplete. " +
                    "Increase the landlord count or reduce the property count."
            }
            log(
                "Finished seeding $landlordsAdded landlords: $propertyOwnershipsAdded completed " +
                    "and $incompletePropertiesAdded incomplete property registrations",
            )
        } finally {
            prsdbUserStmt.close()
            registrationNumberStmt.close()
            individualLandlordStmt.close()
            organisationLandlordStmt.close()
            organisationalLandlordUserStmt.close()
            organisationalLandlordInvitationStmt.close()
            organisationGoverningBodyMemberStmt.close()

            licenceStmt.close()
            propertyManagerStmt.close()
            propertyOwnershipStmt.close()
            freeholderStmt.close()
            superiorLandlordStmt.close()
            ownershipPaymentStmt.close()
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
        individualLandlordStmt: PreparedStatement,
        organisationLandlordStmt: PreparedStatement,
        organisationalLandlordUserStmt: PreparedStatement,
        organisationalLandlordInvitationStmt: PreparedStatement,
        organisationGoverningBodyMemberStmt: PreparedStatement,
        coreDetails: CoreLandlordDetails,
        anniversary: MonthDay?,
        hasRegisteredProperty: Boolean,
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

        when (coreDetails.landlordType) {
            LandlordType.INDIVIDUAL -> {
                addIndividualLandlordToBatch(individualLandlordStmt, coreDetails, anniversary, hasRegisteredProperty, registrationNumberId)
            }

            LandlordType.ORGANISATION -> {
                addOrganisationLandlordToBatch(
                    prsdbUserStmt,
                    organisationLandlordStmt,
                    organisationalLandlordUserStmt,
                    organisationalLandlordInvitationStmt,
                    organisationGoverningBodyMemberStmt,
                    coreDetails,
                    anniversary,
                    hasRegisteredProperty,
                    registrationNumberId,
                )
            }
        }
    }

    private fun addIndividualLandlordToBatch(
        landlordStmt: PreparedStatement,
        coreDetails: CoreLandlordDetails,
        anniversary: MonthDay?,
        hasRegisteredProperty: Boolean,
        registrationNumberId: Long,
    ) {
        val name = NftDataFaker.generateName()
        val isVerified = NftDataFaker.generateBoolean(probabilityTrue = 0.8)

        landlordStmt.setLong(1, coreDetails.id)
        landlordStmt.setTimestamp(2, coreDetails.createdDate)
        landlordStmt.setTimestamp(3, NftDataFaker.generateLastModifiedDate(coreDetails.createdDate))
        landlordStmt.setString(4, coreDetails.subjectId)
        landlordStmt.setString(5, name)
        landlordStmt.setString(6, NftDataFaker.generateEmail(name))
        landlordStmt.setString(7, NftDataFaker.generatePhoneNumber())
        landlordStmt.setLong(8, contactAddressGenerator.next().id)
        landlordStmt.setDate(9, NftDataFaker.generateDateOfBirth())
        landlordStmt.setLong(10, registrationNumberId)
        landlordStmt.setBoolean(11, isVerified)
        landlordStmt.setIntOrNull(12, anniversary?.dayOfMonth)
        landlordStmt.setIntOrNull(13, anniversary?.monthValue)
        landlordStmt.setBoolean(14, hasRegisteredProperty)
        landlordStmt.addBatch()
    }

    private fun addOrganisationLandlordToBatch(
        prsdbUserStmt: PreparedStatement,
        organisationLandlordStmt: PreparedStatement,
        organisationalLandlordUserStmt: PreparedStatement,
        organisationalLandlordInvitationStmt: PreparedStatement,
        organisationGoverningBodyMemberStmt: PreparedStatement,
        coreDetails: CoreLandlordDetails,
        anniversary: MonthDay?,
        hasRegisteredProperty: Boolean,
        registrationNumberId: Long,
    ) {
        val details =
            requireNotNull(coreDetails.organisationDetails) {
                "Organisation landlords must have organisation details generated for them"
            }

        organisationLandlordStmt.setLong(1, coreDetails.id)
        organisationLandlordStmt.setTimestamp(2, coreDetails.createdDate)
        organisationLandlordStmt.setTimestamp(3, NftDataFaker.generateLastModifiedDate(coreDetails.createdDate))
        organisationLandlordStmt.setLong(4, registrationNumberId)
        organisationLandlordStmt.setString(5, details.name)
        organisationLandlordStmt.setLong(6, contactAddressGenerator.next().id)
        organisationLandlordStmt.setString(7, details.email)
        organisationLandlordStmt.setString(8, details.phoneNumber)
        organisationLandlordStmt.setBoolean(9, details.isCompany)
        organisationLandlordStmt.setBoolean(10, details.isCharity)
        organisationLandlordStmt.setBoolean(11, details.isTrust)
        organisationLandlordStmt.setStringOrNull(12, details.companyNumber)
        organisationLandlordStmt.setIntOrNull(13, details.charityRegisteredWith?.ordinal)
        organisationLandlordStmt.setStringOrNull(14, details.charityNumber)
        organisationLandlordStmt.setStringOrNull(15, details.leadTrusteeName)
        organisationLandlordStmt.setDateOrNull(16, details.leadTrusteeDateOfBirth)
        organisationLandlordStmt.setStringOrNull(17, details.leadTrusteeEmail)
        organisationLandlordStmt.setStringOrNull(18, details.leadTrusteePhone)
        organisationLandlordStmt.setLongOrNull(19, if (details.isTrust) contactAddressGenerator.next().id else null)
        organisationLandlordStmt.setString(20, details.mainContactName)
        organisationLandlordStmt.setString(21, details.mainContactEmail)
        organisationLandlordStmt.setString(22, details.mainContactPhone)
        organisationLandlordStmt.setString(23, details.registrantName)
        organisationLandlordStmt.setDate(24, details.registrantDateOfBirth)
        organisationLandlordStmt.setString(25, details.registrantEmail)
        organisationLandlordStmt.setString(26, details.registrantPhoneNumber)
        organisationLandlordStmt.setIntOrNull(27, anniversary?.dayOfMonth)
        organisationLandlordStmt.setIntOrNull(28, anniversary?.monthValue)
        organisationLandlordStmt.setBoolean(29, hasRegisteredProperty)
        organisationLandlordStmt.addBatch()

        details.users.forEachIndexed { index, user ->
            // The registrant (index 0) already has a prsdb_user row added by addLandlordToBatch; only extra
            // organisational landlord users need their own.
            if (index > 0) {
                prsdbUserStmt.setString(1, user.subjectId)
                prsdbUserStmt.setTimestamp(2, user.createdDate)
                prsdbUserStmt.addBatch()
            }

            organisationalLandlordUserStmt.setTimestamp(1, user.createdDate)
            organisationalLandlordUserStmt.setLong(2, coreDetails.id)
            organisationalLandlordUserStmt.setString(3, user.subjectId)
            organisationalLandlordUserStmt.setString(4, user.name)
            organisationalLandlordUserStmt.setString(5, user.email)
            organisationalLandlordUserStmt.setInt(6, user.role.ordinal)
            organisationalLandlordUserStmt.addBatch()
        }

        details.pendingInvitations.forEach { invitation ->
            organisationalLandlordInvitationStmt.setTimestamp(1, coreDetails.createdDate)
            organisationalLandlordInvitationStmt.setTimestamp(2, NftDataFaker.generateLastModifiedDate(coreDetails.createdDate))
            organisationalLandlordInvitationStmt.setObject(3, NftDataFaker.generateInvitationToken())
            organisationalLandlordInvitationStmt.setString(4, invitation.invitedEmail)
            organisationalLandlordInvitationStmt.setLong(5, coreDetails.id)
            organisationalLandlordInvitationStmt.setInt(6, invitation.role.ordinal)
            organisationalLandlordInvitationStmt.setBoolean(7, invitation.invitationExpiredEmailSent)
            organisationalLandlordInvitationStmt.setBoolean(8, invitation.isHidden)
            organisationalLandlordInvitationStmt.addBatch()
        }

        if (details.hasGoverningBody) {
            NftDataFaker.generateGoverningBodyMembers(hasLeadTrustee = details.isTrust).forEach { member ->
                organisationGoverningBodyMemberStmt.setTimestamp(1, coreDetails.createdDate)
                organisationGoverningBodyMemberStmt.setTimestamp(2, NftDataFaker.generateLastModifiedDate(coreDetails.createdDate))
                organisationGoverningBodyMemberStmt.setLong(3, coreDetails.id)
                organisationGoverningBodyMemberStmt.setInt(4, member.type.ordinal)
                organisationGoverningBodyMemberStmt.setString(5, member.name)
                organisationGoverningBodyMemberStmt.setDate(6, member.dateOfBirth)
                organisationGoverningBodyMemberStmt.setLong(7, contactAddressGenerator.next().id)
                organisationGoverningBodyMemberStmt.addBatch()
            }
        }
    }

    private fun generateLandlordsToSeed(
        landlordIds: List<Int>,
        remainingCompleted: Int,
        remainingIncomplete: Int,
    ): List<LandlordToSeed> {
        var completedLeft = remainingCompleted
        var incompleteLeft = remainingIncomplete
        return NftDataFaker.generateCoreDetailsForLandlords(landlordIds).map { generatedLandlord ->
            val landlord = generatedLandlord.copy(id = startingIds.getValue("landlord") + generatedLandlord.id)
            val numOfPropertiesForLandlord =
                NftDataFaker.generateNumberOfPropertiesForLandlord().coerceAtMost(completedLeft + incompleteLeft)
            val properties =
                List(numOfPropertiesForLandlord) {
                    val completed =
                        when {
                            completedLeft == 0 -> false
                            incompleteLeft == 0 -> true
                            else -> null
                        }
                    generatePropertyToSeed(landlord, completed).also {
                        if (it.scenario.isRegistrationComplete) completedLeft-- else incompleteLeft--
                    }
                }
            LandlordToSeed(landlord, properties)
        }
    }

    private fun generatePropertyToSeed(
        landlord: CoreLandlordDetails,
        completed: Boolean?,
    ): PropertyToSeed {
        val scenario = NftDataFaker.generatePropertyScenario(completed)
        val registrationDate =
            if (scenario.isRegistrationComplete) NftDataFaker.generateCreatedDate(after = landlord.createdDate) else null
        return PropertyToSeed(scenario, registrationDate)
    }

    private fun addPropertyOwnershipToBatch(
        registrationNumberStmt: PreparedStatement,
        propertyManagerStmt: PreparedStatement,
        propertyOwnershipStmt: PreparedStatement,
        freeholderStmt: PreparedStatement,
        superiorLandlordStmt: PreparedStatement,
        paymentStmt: PreparedStatement,
        membershipStmt: PreparedStatement,
        licenceStmt: PreparedStatement,
        isOccupied: Boolean,
        registrationNumberId: Long,
        licenceIdIfHasLicence: Long?,
        propertyManagerIdIfManaged: Long?,
        propertyOwnershipId: Long,
        createdDate: Timestamp,
        landlordDetails: CoreLandlordDetails,
        landlordAnniversary: MonthDay,
        licenseProvideLater: Boolean?,
        tenancyProvideLater: Boolean?,
    ) {
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

        if (propertyManagerIdIfManaged != null) {
            val propertyManager = NftDataFaker.generatePropertyManager()

            propertyManagerStmt.setLong(1, propertyManagerIdIfManaged)
            propertyManagerStmt.setTimestamp(2, createdDate)
            propertyManagerStmt.setTimestamp(3, NftDataFaker.generateLastModifiedDate(createdDate))
            propertyManagerStmt.setString(4, propertyManager.name)
            propertyManagerStmt.setString(5, propertyManager.email)
            propertyManagerStmt.addBatch()
        }

        // A "provide tenancy details later" property has no tenancy details yet, mirroring the real app where the details
        // are cleared/absent until the landlord provides them. Only generate details when occupied and not provide-later.
        val hasTenancyDetails = isOccupied && tenancyProvideLater != true
        val numHouseholdsAndTenants = if (hasTenancyDetails) NftDataFaker.generateNumHouseholdsAndTenants() else Pair(0, 0)
        val numBedrooms = if (isOccupied) NftDataFaker.generateNumBedrooms() else null
        val standardAndCustomBillsIncluded = if (hasTenancyDetails) NftDataFaker.generateStandardAndCustomBillsIncluded() else null
        val furnishedStatus = if (hasTenancyDetails) NftDataFaker.generateFurnishedStatus() else null
        val rentDetails = if (hasTenancyDetails) NftDataFaker.generateRentDetails() else null
        val renewalDate = NftDataFaker.generateRenewalDate(landlordAnniversary)

        propertyOwnershipStmt.setLong(1, propertyOwnershipId)
        propertyOwnershipStmt.setTimestamp(2, createdDate)
        propertyOwnershipStmt.setTimestamp(3, NftDataFaker.generateLastModifiedDate(createdDate))
        val ownershipType = NftDataFaker.generateOwnershipType()
        propertyOwnershipStmt.setInt(4, ownershipType.ordinal)
        propertyOwnershipStmt.setInt(5, numHouseholdsAndTenants.first)
        propertyOwnershipStmt.setInt(6, numHouseholdsAndTenants.second)
        propertyOwnershipStmt.setLong(7, registrationNumberId)
        propertyOwnershipStmt.setLongOrNull(8, licenceIdIfHasLicence)
        propertyOwnershipStmt.setInt(9, NftDataFaker.generatePropertyAndOtherType().first.ordinal)
        val propertyAddress = propertyAddressGenerator.next()
        propertyOwnershipStmt.setLong(10, propertyAddress.id)
        remainingAvailableAddressCount--
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
        propertyOwnershipStmt.setLong(22, propertyAddress.id)
        propertyOwnershipStmt.setDate(23, renewalDate)
        propertyOwnershipStmt.setInt(24, NftDataFaker.generateOwnershipTypeBeforePdjb955().ordinal)
        propertyOwnershipStmt.setLongOrNull(25, propertyManagerIdIfManaged)
        propertyOwnershipStmt.addBatch()

        if (ownershipType.requiresFreeholderRecords()) {
            NftDataFaker.generateFreeholders().forEach { freeholder ->
                addFreeholderToBatch(freeholderStmt, createdDate, propertyOwnershipId, freeholder)
            }
        }

        if (ownershipType.requiresSuperiorLandlordInfo()) {
            NftDataFaker.generateSuperiorLandlords().forEach { superiorLandlord ->
                addSuperiorLandlordToBatch(superiorLandlordStmt, createdDate, propertyOwnershipId, superiorLandlord)
            }
        }

        val paymentCreatedAt = NftDataFaker.generatePaymentCreated(createdDate)
        paymentStmt.setString(1, NftDataFaker.generatePaymentId())
        paymentStmt.setTimestamp(2, createdDate)
        paymentStmt.setTimestamp(3, NftDataFaker.generateLastModifiedDate(createdDate))
        paymentStmt.setInt(4, NftDataFaker.generatePaymentAmount())
        paymentStmt.setString(5, NftDataFaker.generatePaymentReference())
        paymentStmt.setTimestamp(6, paymentCreatedAt)
        paymentStmt.setDate(7, Date.valueOf(paymentCreatedAt.toInstant().atZone(DateTimeHelper.UK_ZONE).toLocalDate()))
        paymentStmt.setDate(8, renewalDate)
        paymentStmt.setString(9, landlordDetails.subjectId)
        paymentStmt.setLong(10, propertyOwnershipId)
        paymentStmt.addBatch()

        membershipStmt.setLong(1, landlordDetails.id)
        membershipStmt.setLong(2, propertyOwnershipId)
        membershipStmt.setTimestamp(3, createdDate)
        membershipStmt.addBatch()
    }

    private fun addFreeholderToBatch(
        freeholderStmt: PreparedStatement,
        createdDate: Timestamp,
        propertyOwnershipId: Long,
        freeholder: NftDataFaker.FreeholderDetails,
    ) {
        freeholderStmt.setTimestamp(1, createdDate)
        freeholderStmt.setLong(2, propertyOwnershipId)
        freeholderStmt.setStringOrNull(3, freeholder.name)
        freeholderStmt.setStringOrNull(4, freeholder.email)
        freeholderStmt.setLongOrNull(5, if (freeholder.hasAddress) contactAddressGenerator.next().id else null)
        freeholderStmt.addBatch()
    }

    private fun addSuperiorLandlordToBatch(
        superiorLandlordStmt: PreparedStatement,
        createdDate: Timestamp,
        propertyOwnershipId: Long,
        superiorLandlord: NftDataFaker.SuperiorLandlordDetails,
    ) {
        superiorLandlordStmt.setTimestamp(1, createdDate)
        superiorLandlordStmt.setLong(2, propertyOwnershipId)
        superiorLandlordStmt.setStringOrNull(3, superiorLandlord.name)
        superiorLandlordStmt.setStringOrNull(4, superiorLandlord.email)
        superiorLandlordStmt.setLongOrNull(5, if (superiorLandlord.hasAddress) contactAddressGenerator.next().id else null)
        superiorLandlordStmt.addBatch()
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

        val address = propertyAddressGenerator.next()
        remainingAvailableAddressCount--

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

        if (NftDataFaker.isGasSafetyCertificateCurrent(complianceData.gasSafetyCertIssueDate)) {
            val gasSafetyUploadId = startingIds.getValue("file_upload") + ++updatedFileUploadCount
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
        if (
            NftDataFaker.isElectricalSafetyCertificateCurrent(
                complianceData.electricalSafetyExpiryDate,
                complianceData.electricalCertType,
                createdDate,
            )
        ) {
            val electricalSafetyUploadId = startingIds.getValue("file_upload") + ++updatedFileUploadCount
            addFileUploadToBatch(
                fileUploadStmt,
                propertyOwnershipId,
                createdDate,
                complianceData.electricalCertType!!,
                electricalSafetyUploadId,
            )
            electricalSafetyFileUploadsStmt.setLong(1, complianceId)
            electricalSafetyFileUploadsStmt.setLong(2, electricalSafetyUploadId)
            electricalSafetyFileUploadsStmt.addBatch()
        }

        propertyComplianceStmt.setLong(1, complianceId)
        propertyComplianceStmt.setTimestamp(2, createdDate)
        propertyComplianceStmt.setTimestamp(3, NftDataFaker.generateLastModifiedDate(createdDate))
        propertyComplianceStmt.setLong(4, propertyOwnershipId)
        propertyComplianceStmt.setDateOrNull(5, complianceData.gasSafetyCertIssueDate)
        propertyComplianceStmt.setBooleanOrNull(6, complianceData.hasGasSupply)
        propertyComplianceStmt.setDateOrNull(7, complianceData.eicrExpiryDate)
        propertyComplianceStmt.setDateOrNull(8, complianceData.eicIssueDate)
        propertyComplianceStmt.setIntOrNull(9, complianceData.electricalCertType?.ordinal)
        propertyComplianceStmt.setStringOrNull(
            10,
            complianceData.epcNumber?.let {
                "$epcCertificateBaseUrl/${EpcDataModel.parseCertificateNumberOrNull(it)}"
            },
        )
        propertyComplianceStmt.setDateOrNull(11, complianceData.epcExpiryDate)
        propertyComplianceStmt.setBooleanOrNull(12, complianceData.tenancyStartedBeforeEpcExpiry)
        propertyComplianceStmt.setStringOrNull(13, complianceData.epcEnergyRating)
        propertyComplianceStmt.setIntOrNull(14, complianceData.epcExemptionReason?.ordinal)
        propertyComplianceStmt.setIntOrNull(15, complianceData.epcMeesExemptionReason?.ordinal)
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
        val stepName = getUploadStepName(certificateType)
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

    private fun getUploadStepName(certificateType: CertificateType): String =
        when (certificateType) {
            CertificateType.GasSafetyCert -> UploadGasCertStep.ROUTE_SEGMENT
            CertificateType.Eic -> UploadEicStep.ROUTE_SEGMENT
            CertificateType.Eicr -> UploadEicrStep.ROUTE_SEGMENT
        }

    private fun log(message: String) {
        println("${LocalDateTime.now()} $message")
    }

    private data class PropertyToSeed(
        val scenario: PropertyScenario,
        val registrationDate: Timestamp?,
    )

    private data class LandlordToSeed(
        val details: CoreLandlordDetails,
        val properties: List<PropertyToSeed>,
    ) {
        val anniversary: MonthDay? =
            properties
                .mapNotNull { it.registrationDate }
                .minOrNull()
                ?.let { MonthDay.from(it.toInstant().atZone(DateTimeHelper.UK_ZONE)) }
    }

    private abstract class Generator<T> {
        protected var values = emptyList<T>()
        private var index = 0

        abstract fun replenishValues()

        fun next(): T {
            while (index == values.size) {
                val sizeBeforeReplenish = values.size
                replenishValues()
                check(values.size > sizeBeforeReplenish) {
                    "${this::class.simpleName} could not supply any new values. The configured seed volumes are too " +
                        "large for the available source data (e.g. not enough addresses in the database)."
                }
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
            val potentialNewNumbers = NftDataFaker.generateRegistrationNumbers(batchSize * 5)
            val alreadyUsedNumbers = nftDataSeederDao.findRegistrationNumbersIn(potentialNewNumbers).toSet()
            val newNumbers = potentialNewNumbers - alreadyUsedNumbers
            values = (valueSet + newNumbers).toList()
        }
    }

    private companion object {
        /**
         * Generated UPRNs start well above the real NGD range so they can never collide with genuine UPRNs or with
         * the small UPRNs used by the local development data.
         */
        const val GENERATED_UPRN_BASE = 900_000_000_000L
    }

    // Both registration types share a keyset cursor, so flushing either batch cannot make an address reusable.
    private inner class PropertyAddressGenerator : Generator<Address>() {
        private var lastAddressId = 0L

        override fun replenishValues() {
            val addresses = nftDataSeederDao.findAvailablePropertyAddresses(batchSize * 5, lastAddressId)
            if (addresses.isNotEmpty()) {
                lastAddressId = addresses.maxOf { it.id }
                values = values + NftDataFaker.shuffle(addresses)
            }
        }
    }

    private inner class AddressGenerator : Generator<Address>() {
        private val replenishmentSize = batchSize * 5

        override fun replenishValues() {
            val valueSet = values.toSet()
            val usedAddressIds = values.map { it.id }.toSet()
            val availableAddressCount = eligibleAddressCount
            val firstOffset =
                NftDataFaker.generateNumberLessThan(
                    (availableAddressCount - replenishmentSize).coerceAtLeast(1),
                )
            var offset = firstOffset
            var triedFromStart = firstOffset == 0
            var newAddresses = emptyList<Address>()
            while (newAddresses.isEmpty() && offset <= availableAddressCount) {
                newAddresses =
                    nftDataSeederDao
                        .findAddresses(
                            limit = replenishmentSize,
                            offset = offset,
                        ).filterNot { it.id in usedAddressIds }
                offset =
                    if (!triedFromStart) {
                        triedFromStart = true
                        0
                    } else {
                        offset + replenishmentSize
                    }
            }
            values = (valueSet + NftDataFaker.shuffle(newAddresses)).toList()
        }
    }
}
