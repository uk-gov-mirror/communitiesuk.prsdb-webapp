package uk.gov.communities.prsdb.webapp.helpers

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import net.datafaker.Faker
import uk.gov.communities.prsdb.webapp.constants.EIC_VALIDITY_YEARS
import uk.gov.communities.prsdb.webapp.constants.GAS_SAFETY_CERT_VALIDITY_MONTHS
import uk.gov.communities.prsdb.webapp.constants.INCOMPLETE_PROPERTY_AGE_WHEN_REMINDER_EMAIL_DUE_IN_DAYS
import uk.gov.communities.prsdb.webapp.constants.MAX_REG_NUM
import uk.gov.communities.prsdb.webapp.constants.MIN_REG_NUM
import uk.gov.communities.prsdb.webapp.constants.enums.BillsIncluded
import uk.gov.communities.prsdb.webapp.constants.enums.CertificateType
import uk.gov.communities.prsdb.webapp.constants.enums.CharityRegulator
import uk.gov.communities.prsdb.webapp.constants.enums.EpcExemptionReason
import uk.gov.communities.prsdb.webapp.constants.enums.FurnishedStatus
import uk.gov.communities.prsdb.webapp.constants.enums.GoverningBodyMemberType
import uk.gov.communities.prsdb.webapp.constants.enums.LandlordType
import uk.gov.communities.prsdb.webapp.constants.enums.LicensingType
import uk.gov.communities.prsdb.webapp.constants.enums.MeesExemptionReason
import uk.gov.communities.prsdb.webapp.constants.enums.OrganisationalLandlordUserRole
import uk.gov.communities.prsdb.webapp.constants.enums.OwnershipType
import uk.gov.communities.prsdb.webapp.constants.enums.OwnershipTypeBeforePdjb955
import uk.gov.communities.prsdb.webapp.constants.enums.PropertyType
import uk.gov.communities.prsdb.webapp.constants.enums.RentFrequency
import uk.gov.communities.prsdb.webapp.database.entity.Address
import uk.gov.communities.prsdb.webapp.models.dataModels.AddressDataModel
import java.math.BigDecimal
import java.math.RoundingMode
import java.sql.Date
import java.sql.Timestamp
import java.time.Instant
import java.time.LocalDate
import java.time.MonthDay
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import java.util.Locale
import java.util.Random
import java.util.UUID
import java.util.concurrent.TimeUnit

object NftDataFaker {
    private const val DEFAULT_SEED = 239L
    private const val MAX_ADDRESSES_PER_STREET = 30

    // Dates and instants are converted in a fixed zone, so seeding produces the same data whatever the machine's time zone
    private val SEEDING_ZONE: ZoneId = ZoneOffset.UTC

    private var seededRandom = Random(DEFAULT_SEED)
    private var faker = Faker(Locale.UK, seededRandom)
    private var scenarioRandom = Random(DEFAULT_SEED)

    /**
     * The reference "now" for all generated dates, pinned once per run so that a seeding run does not drift as it
     * executes. Dates stay relative to the run date, so expiry and reminder windows remain realistic.
     */
    private var runReference: Instant = Instant.now()

    /**
     * Resets all random sources so that a seeding run produces identical data for a given seed and reference date.
     * Must be called before seeding starts.
     */
    fun reset(
        seed: Long = DEFAULT_SEED,
        reference: Instant = Instant.now(),
    ) {
        seededRandom = Random(seed)
        faker = Faker(Locale.UK, seededRandom)
        scenarioRandom = Random(seed)
        runReference = reference
    }

    private fun referenceDate(): LocalDate = runReference.toSeedingDate()

    private fun Instant.toSeedingDate(): LocalDate = atZone(SEEDING_ZONE).toLocalDate()

    private fun LocalDate.toSeedingInstant(): Instant = atStartOfDay(SEEDING_ZONE).toInstant()

    fun generateBoolean(probabilityTrue: Double = 0.5): Boolean = faker.random().nextDouble() < probabilityTrue

    fun <T> pickOne(values: List<T>): T = values[seededRandom.nextInt(values.size)]

    fun <T> shuffle(values: List<T>): List<T> = values.shuffled(seededRandom)

    fun generatePropertyScenario(completed: Boolean? = null): PropertyScenario {
        val randomCompletion = scenarioRandom.nextDouble() < 0.9
        val isRegistrationComplete = completed ?: randomCompletion
        if (!isRegistrationComplete) {
            return PropertyScenario(
                isRegistrationComplete = false,
                isOccupied = false,
                hasLicence = false,
                licenseProvideLater = null,
                tenancyProvideLater = null,
            )
        }

        val isOccupied = scenarioRandom.nextDouble() < 0.8
        val hasLicence = scenarioRandom.nextDouble() < 0.2
        return PropertyScenario(
            isRegistrationComplete = true,
            isOccupied = isOccupied,
            hasLicence = hasLicence,
            licenseProvideLater = if (!hasLicence) scenarioRandom.nextDouble() < 0.4 else null,
            tenancyProvideLater = if (isOccupied) scenarioRandom.nextDouble() < 0.4 else null,
        )
    }

    fun generatePaymentId(): String = scenarioRandom.nextLong().toULong().toString(36)

    fun generatePaymentAmount(): Int = scenarioRandom.nextInt(6500)

    fun generatePaymentReference(): String = faker.animal().name()

    fun generatePaymentCreated(registrationCreated: Timestamp): Timestamp = generateTimestampBefore(registrationCreated, 5)

    fun generateReminderEmailSent(): Boolean = scenarioRandom.nextDouble() < 0.25

    fun generateNumberLessThan(max: Int): Int = faker.random().nextInt(max)

    fun generateCreatedDate(after: Timestamp = Timestamp.from(referenceDate().minusYears(1).toSeedingInstant())): Timestamp =
        generateDateAfter(after)

    fun generateLastModifiedDate(createdDate: Timestamp): Timestamp? =
        if (generateBoolean(probabilityTrue = 0.6)) {
            generateDateAfter(createdDate)
        } else {
            null
        }

    fun generateDateOfBirth(): Date {
        val referenceDate = referenceDate()
        val earliestDateOfBirth = referenceDate.minusYears(120).toEpochDay()
        val latestDateOfBirth = referenceDate.minusYears(18).toEpochDay()
        return Date.valueOf(LocalDate.ofEpochDay(faker.random().nextLong(earliestDateOfBirth, latestDateOfBirth)))
    }

    fun generateRenewalDate(anniversary: MonthDay): Date = Date.valueOf(RenewalDateHelper.getRenewalDate(anniversary, referenceDate()))

    fun generateIncompletePropertyCreatedDate(
        landlordCreatedDate: Timestamp,
        reminderEmailSent: Boolean,
    ): Timestamp {
        val now = referenceDate()
        val earliestCreatedDate = landlordCreatedDate.coerceAtLeast(Timestamp.from(now.minusDays(28).toSeedingInstant()))
        return if (reminderEmailSent) {
            val latestCreatedDate =
                Timestamp
                    .from(now.minusDays(INCOMPLETE_PROPERTY_AGE_WHEN_REMINDER_EMAIL_DUE_IN_DAYS.toLong()).toSeedingInstant())
                    .coerceAtLeast(earliestCreatedDate)
            Timestamp.from(faker.timeAndDate().between(earliestCreatedDate.toInstant(), latestCreatedDate.toInstant()))
        } else {
            generateDateAfter(earliestCreatedDate)
        }
    }

    fun generateLastEmailReminderSentDate(createdDate: Timestamp): Timestamp =
        Timestamp.from(createdDate.toInstant().plus(INCOMPLETE_PROPERTY_AGE_WHEN_REMINDER_EMAIL_DUE_IN_DAYS.toLong(), ChronoUnit.DAYS))

    fun generateInvitationToken(): UUID = UUID.fromString(faker.internet().uuid())

    fun generateSubjectIdentifier(): String = "urn:fdc:gov.uk:2022:${faker.internet().uuid()}"

    fun generateEmail(name: String? = null): String =
        faker.internet().let { if (name != null) it.safeEmailAddress(generateUsername(name)) else it.safeEmailAddress() }

    fun generateName(): String {
        val middleName = if (generateBoolean(probabilityTrue = 0.05)) faker.name().firstName() else null
        return listOfNotNull(faker.name().firstName(), middleName, faker.name().lastName()).joinToString(" ")
    }

    fun generatePhoneNumber(): String =
        faker.phoneNumber().let { if (generateBoolean(probabilityTrue = 0.75)) it.cellPhone() else it.cellPhoneInternational() }

    fun generateRegistrationNumbers(count: Int): Set<Long> {
        val numbers = mutableSetOf<Long>()
        while (numbers.size < count) {
            numbers.add(faker.random().nextLong(MIN_REG_NUM, MAX_REG_NUM + 1))
        }
        return numbers
    }

    fun generateCoreDetailsForLandlords(landlordIds: List<Int>): List<CoreLandlordDetails> =
        landlordIds.map { id ->
            val landlordType = generateLandlordType()
            val subjectId = generateSubjectIdentifier()
            val createdDate = generateCreatedDate()
            val organisationDetails =
                if (landlordType == LandlordType.ORGANISATION) {
                    generateOrganisationLandlordDetails(generateOrganisationCategory(), subjectId, createdDate)
                } else {
                    null
                }
            CoreLandlordDetails(
                id = id.toLong(),
                subjectId = subjectId,
                createdDate = createdDate,
                landlordType = landlordType,
                organisationDetails = organisationDetails,
            )
        }

    // Assumption: roughly 5% of seeded landlords are organisations. This is a rough estimate not backed by real-world
    // data, documented here as agreed, and can be adjusted if a more accurate ratio becomes available.
    fun generateLandlordType(): LandlordType =
        if (generateBoolean(
                probabilityTrue = 0.05,
            )
        ) {
            LandlordType.ORGANISATION
        } else {
            LandlordType.INDIVIDUAL
        }

    // A simplified, mutually-exclusive split of organisation shapes, chosen to give realistic variety without
    // modelling every combination the real registration journey allows (e.g. a charity that is also a company).
    enum class OrganisationCategory {
        COMPANY,
        CHARITY,
        TRUST,
        UNINCORPORATED,
    }

    fun generateOrganisationCategory(): OrganisationCategory {
        val randomDouble = faker.random().nextDouble()
        return when {
            // 40%
            randomDouble < 0.40 -> OrganisationCategory.COMPANY

            // 20%
            randomDouble < 0.60 -> OrganisationCategory.CHARITY

            // 10%
            randomDouble < 0.70 -> OrganisationCategory.TRUST

            // 30%
            else -> OrganisationCategory.UNINCORPORATED
        }
    }

    fun generateOrganisationLandlordDetails(
        category: OrganisationCategory,
        subjectId: String,
        createdDate: Timestamp,
    ): OrganisationLandlordDetails {
        val name = faker.company().name()
        val isCompany = category == OrganisationCategory.COMPANY
        val isTrust = category == OrganisationCategory.TRUST
        // Only companies are guaranteed not to be charities; the other categories may still be registered charities.
        val isCharity = category == OrganisationCategory.CHARITY || (!isCompany && generateBoolean(probabilityTrue = 0.15))

        val companyNumber = if (isCompany) faker.regexify("[0-9]{8}") else null

        val hasCharityRegistration = isCharity && generateBoolean(probabilityTrue = 0.8)
        val charityRegisteredWith =
            when {
                !isCharity -> {
                    null
                }

                hasCharityRegistration -> {
                    pickOne(
                        listOf(CharityRegulator.ENGLAND_AND_WALES, CharityRegulator.NORTHERN_IRELAND, CharityRegulator.SCOTLAND),
                    )
                }

                else -> {
                    CharityRegulator.NONE
                }
            }
        val charityNumber = if (hasCharityRegistration) faker.regexify("[0-9]{6,8}") else null

        val leadTrusteeName = if (isTrust) generateName() else null
        val leadTrusteeDateOfBirth = if (isTrust) generateDateOfBirth() else null
        val leadTrusteeEmail = if (isTrust) generateEmail(leadTrusteeName) else null
        val leadTrusteePhone = if (isTrust) generatePhoneNumber() else null

        val registrantName = generateName()
        val registrantEmail = generateEmail(registrantName)
        val mainContactName = generateName()

        val organisationalLandlordUsers = generateOrganisationalLandlordUsers(subjectId, registrantName, registrantEmail, createdDate)

        return OrganisationLandlordDetails(
            name = name,
            email = generateEmail(name),
            phoneNumber = generatePhoneNumber(),
            isCompany = isCompany,
            isCharity = isCharity,
            isTrust = isTrust,
            companyNumber = companyNumber,
            charityRegisteredWith = charityRegisteredWith,
            charityNumber = charityNumber,
            leadTrusteeName = leadTrusteeName,
            leadTrusteeDateOfBirth = leadTrusteeDateOfBirth,
            leadTrusteeEmail = leadTrusteeEmail,
            leadTrusteePhone = leadTrusteePhone,
            mainContactName = mainContactName,
            mainContactEmail = generateEmail(mainContactName),
            mainContactPhone = generatePhoneNumber(),
            registrantName = registrantName,
            registrantDateOfBirth = generateDateOfBirth(),
            registrantEmail = registrantEmail,
            registrantPhoneNumber = generatePhoneNumber(),
            // Per the real registration journey, only organisations without a company number need governing body
            // members (see LandlordRegistrationService.registerOrganisationLandlord).
            hasGoverningBody = !isCompany,
            users = organisationalLandlordUsers,
            pendingInvitations = generateOrganisationalLandlordInvitations(organisationalLandlordUsers),
        )
    }

    fun generateOrganisationalLandlordInvitations(
        users: List<OrganisationalLandlordUserDetails>,
    ): List<OrganisationalLandlordInvitationDetails> {
        val existingEmails = users.map { it.email }.toSet()
        val randomDouble = faker.random().nextDouble()
        val numberOfInvitations =
            when {
                // Roughly 60% of organisations have no pending invites; the rest have one to three depending on size.
                randomDouble < 0.60 -> 0

                randomDouble < 0.85 -> faker.random().nextInt(1, 2)

                else -> faker.random().nextInt(2, 4)
            }

        if (numberOfInvitations == 0) {
            return emptyList()
        }

        val invitations = mutableListOf<OrganisationalLandlordInvitationDetails>()
        val emailsUsed = existingEmails.toMutableSet()

        repeat(numberOfInvitations) {
            var email = generateEmail(generateName())
            while (email in emailsUsed) {
                email = generateEmail(generateName())
            }
            emailsUsed += email

            val role = generateOrganisationLandlordRole(probabilityOfAdmin = 0.8)

            invitations +=
                OrganisationalLandlordInvitationDetails(
                    invitedEmail = email,
                    role = role,
                    invitationExpiredEmailSent = generateBoolean(probabilityTrue = 0.05),
                    isHidden = generateBoolean(probabilityTrue = 0.1),
                )
        }

        return invitations
    }

    fun generateOrganisationLandlordRole(probabilityOfAdmin: Double = 0.8): OrganisationalLandlordUserRole =
        if (generateBoolean(probabilityTrue = probabilityOfAdmin)) {
            OrganisationalLandlordUserRole.ADMIN
        } else {
            OrganisationalLandlordUserRole.EDITOR
        }

    fun generateOrganisationalLandlordUsers(
        registrantSubjectId: String,
        registrantName: String,
        registrantEmail: String,
        orgCreatedDate: Timestamp,
    ): List<OrganisationalLandlordUserDetails> {
        val registrant =
            OrganisationalLandlordUserDetails(
                subjectId = registrantSubjectId,
                createdDate = orgCreatedDate,
                name = registrantName,
                email = registrantEmail,
                role = OrganisationalLandlordUserRole.ADMIN,
            )

        // Assumption: roughly half of seeded organisations have no additional users beyond the registrant, a third
        // have one or two, and the remainder have three to five. This is a rough estimate not backed by real-world
        // data, documented here as agreed, and can be adjusted if a more accurate ratio becomes available.
        val randomDouble = faker.random().nextDouble()

        val numOfExtraUsers =
            when {
                // 50%
                randomDouble < 0.50 -> 0

                // 35%
                randomDouble < 0.85 -> faker.random().nextInt(1, 2)

                // 15%
                else -> faker.random().nextInt(3, 5)
            }

        val extraUsers =
            (1..numOfExtraUsers).map {
                val name = generateName()
                val role = generateOrganisationLandlordRole(probabilityOfAdmin = 0.8)
                // Extra users are always freshly generated people, distinct from the registrant and main contact, so
                // that each has its own subject identifier and satisfies the organisational landlord user's unique
                // constraint on (organisation_landlord_id, subject_identifier).
                OrganisationalLandlordUserDetails(
                    subjectId = generateSubjectIdentifier(),
                    createdDate = generateCreatedDate(after = orgCreatedDate),
                    name = name,
                    email = generateEmail(name),
                    role = role,
                )
            }

        return listOf(registrant) + extraUsers
    }

    fun generateGoverningBodyMembers(hasLeadTrustee: Boolean): List<GoverningBodyMemberDetails> {
        val count = faker.random().nextInt(1, 3)
        return (1..count).map {
            val type =
                if (hasLeadTrustee) {
                    GoverningBodyMemberType.TRUSTEE
                } else {
                    pickOne(listOf(GoverningBodyMemberType.PARTNER, GoverningBodyMemberType.OTHER))
                }
            GoverningBodyMemberDetails(
                type = type,
                name = generateName(),
                dateOfBirth = generateDateOfBirth(),
            )
        }
    }

    fun generateNumberOfPropertiesForLandlord(): Int {
        val randomDouble = faker.random().nextDouble()

        return when {
            // 30%
            randomDouble < 0.30 -> 0

            // 31.5%
            randomDouble < 0.615 -> 1

            // 26.6%
            randomDouble < 0.881 -> faker.random().nextInt(2, 4)

            // 11.8%
            randomDouble < 0.999 -> faker.random().nextInt(5, 10)

            // 0.1%
            else -> faker.random().nextInt(10, 50)
        }
    }

    fun generateLicenceTypeAndNumber(): Pair<LicensingType, String> {
        val licenceType = faker.options().option(*LicensingType.licencedEntries.toTypedArray())
        val licenceNumber =
            when (licenceType) {
                LicensingType.SELECTIVE_LICENCE -> faker.regexify("SL-[A-Z0-9]{7}")
                LicensingType.HMO_MANDATORY_LICENCE -> faker.regexify("HMO-M-[A-Z0-9]{7}")
                else -> faker.regexify("HMO-A-[A-Z0-9]{7}")
            }
        return Pair(licenceType, licenceNumber)
    }

    fun generatePropertyAndOtherType(): Pair<PropertyType, String?> {
        val propertyType = faker.options().option(PropertyType::class.java)
        val otherPropertyType = if (propertyType == PropertyType.OTHER) faker.options().option(*otherPropertyTypes) else null
        return Pair(propertyType, otherPropertyType)
    }

    // TODO PDJB-2019: delete this once the legacy enum and column are gone.
    fun generateOwnershipTypeBeforePdjb955(): OwnershipTypeBeforePdjb955 = faker.options().option(OwnershipTypeBeforePdjb955::class.java)

    fun generateOwnershipType(): OwnershipType = faker.options().option(OwnershipType::class.java)

    // A property that requires freeholder records always gets at least one.
    fun generateFreeholders(): List<FreeholderDetails> = generateAssociatedParties(faker.random().nextInt(1, 3), ::FreeholderDetails)

    // A property can legitimately have no superior landlords on record, which models the landlord
    // answering "no" to HasAnySuperiorLandlordsStep.
    fun generateSuperiorLandlords(): List<SuperiorLandlordDetails> =
        generateAssociatedParties(faker.random().nextInt(0, 2), ::SuperiorLandlordDetails)

    // The landlord does not always know who the other party is, so some rows are seeded as
    // "no details known" placeholders. A named party is contactable by email or by post but never
    // both, because the journey asks for one contact type - see FreeholderContactTypeStep.
    private fun <T> generateAssociatedParties(
        count: Int,
        create: (name: String?, email: String?, hasAddress: Boolean) -> T,
    ): List<T> =
        (1..count).map {
            if (generateBoolean(probabilityTrue = 0.2)) {
                create(null, null, false)
            } else {
                val name = generateName()
                if (generateBoolean(probabilityTrue = 0.8)) {
                    create(name, generateEmail(name), false)
                } else {
                    create(name, null, true)
                }
            }
        }

    fun generateHasPropertyManager(): Boolean = generateBoolean(probabilityTrue = 0.2)

    fun generatePropertyManager(): PropertyManagerDetails {
        val name = generateName()
        return PropertyManagerDetails(name = name, email = generateEmail(name))
    }

    fun generateNumHouseholdsAndTenants(): Pair<Int, Int> {
        val numHouseholds = faker.random().nextInt(1, 10)
        val numTenants = faker.random().nextInt(numHouseholds, 10)
        return Pair(numHouseholds, numTenants)
    }

    fun generateNumBedrooms(): Int = faker.random().nextInt(1, 10)

    fun generateStandardAndCustomBillsIncluded(): Pair<String?, String?> {
        val includeBills = generateBoolean(probabilityTrue = 0.8)
        if (!includeBills) return Pair(null, null)

        val numStandardBillsIncluded = faker.random().nextInt(1, BillsIncluded.standardEntries.size)
        val standardBillsIncluded = faker.options().subset(numStandardBillsIncluded, *BillsIncluded.standardEntries.toTypedArray())

        val includeCustomBills = generateBoolean(probabilityTrue = 0.05)
        val customBillsIncluded =
            if (includeCustomBills) {
                standardBillsIncluded.add(BillsIncluded.SOMETHING_ELSE)
                val numCustomBillsIncluded = faker.random().nextInt(1, customBills.size)
                faker.options().subset(numCustomBillsIncluded, *customBills)
            } else {
                null
            }

        // subset() returns a HashSet, and enum hash codes are identity-based, so iteration order varies between JVM
        // runs. The stored order is not meaningful, so sort it to keep seeding reproducible.
        return Pair(
            standardBillsIncluded.sortedBy { it.ordinal }.joinToString(separator = ","),
            customBillsIncluded?.sorted()?.joinToString(separator = ","),
        )
    }

    fun generateFurnishedStatus(): FurnishedStatus = faker.options().option(FurnishedStatus::class.java)

    fun generateRentDetails(): RentDetails {
        val rentFrequency = faker.options().option(RentFrequency::class.java)
        val customRentFrequency = if (rentFrequency == RentFrequency.OTHER) faker.options().option(*customRentFrequencies) else null

        val rentAmount =
            when (rentFrequency) {
                RentFrequency.WEEKLY -> faker.random().nextDouble(50.0, 500.0)
                RentFrequency.FOUR_WEEKLY -> faker.random().nextDouble(200.0, 2000.0)
                else -> faker.random().nextDouble(200.0, 2000.0)
            }
        val formattedRentAmount = BigDecimal(rentAmount).setScale(2, RoundingMode.HALF_UP)

        return RentDetails(rentFrequency, customRentFrequency, formattedRentAmount)
    }

    fun generateETag(): String = faker.regexify("[a-f0-9]{32}")

    fun generatePropertyComplianceData(createdDateTimestamp: Timestamp): PropertyComplianceData {
        val createdDate = Date.valueOf(createdDateTimestamp.toInstant().toSeedingDate())

        val hasGasSupply = generateBoolean(probabilityTrue = 0.9)

        val gasCertificateSafetyMissing = generateBoolean(probabilityTrue = 0.1)
        val gasSafetyIssueDate =
            if (!gasCertificateSafetyMissing) {
                generateDateBefore(createdDate, (GAS_SAFETY_CERT_VALIDITY_MONTHS / 12.0 * 365 * 1.5).toLong())
            } else {
                null
            }

        val eicrMissing = generateBoolean(probabilityTrue = 0.1)

        val electricalSafetyExpiryDate =
            if (!eicrMissing) {
                val expiryDate =
                    faker.timeAndDate().between(
                        createdDate.toLocalDate().minusYears(5).toSeedingInstant(),
                        createdDate.toLocalDate().plusYears(10).toSeedingInstant(),
                    )
                Date.valueOf(expiryDate.toSeedingDate())
            } else {
                null
            }

        val hasEpcExemption = generateBoolean(probabilityTrue = 0.05)
        val epcExemptionReason = if (hasEpcExemption) faker.options().option(EpcExemptionReason::class.java) else null

        val epcMissing = !hasEpcExemption && generateBoolean(probabilityTrue = 0.01)
        val epcNumber =
            if (!hasEpcExemption && !epcMissing) {
                faker.options().option(*epcNumbers)
            } else {
                null
            }

        val epcExpiryDate =
            if (epcNumber != null) {
                val expiryDate =
                    faker.timeAndDate().between(
                        createdDate.toLocalDate().minusYears(5).toSeedingInstant(),
                        createdDate.toLocalDate().plusYears(10).toSeedingInstant(),
                    )
                Date.valueOf(expiryDate.toSeedingDate())
            } else {
                null
            }

        val epcExpired = epcExpiryDate?.before(createdDate) == true
        val tenancyStartedBeforeEpcExpiry =
            if (epcExpired) {
                generateBoolean(probabilityTrue = 0.6)
            } else {
                null
            }

        val epcHasAcceptableRating = epcNumber != null && generateBoolean(probabilityTrue = 0.9)
        val epcEnergyRating =
            if (epcNumber != null) {
                if (epcHasAcceptableRating) {
                    faker.options().option("A", "B", "C", "D", "E")
                } else {
                    faker.options().option("F", "G")
                }
            } else {
                null
            }

        val epcNeedsMeesExemption = (!epcExpired || tenancyStartedBeforeEpcExpiry == true) && !epcHasAcceptableRating
        val epcMeesExemptionReason =
            if (epcNeedsMeesExemption && generateBoolean(probabilityTrue = 0.8)) {
                faker.options().option(MeesExemptionReason::class.java)
            } else {
                null
            }

        val electricalCertType =
            if (electricalSafetyExpiryDate != null) {
                faker.options().option(CertificateType.Eicr, CertificateType.Eic)
            } else {
                null
            }

        // EICs store an issue date rather than an expiry date
        val eicIssueDate =
            if (electricalCertType == CertificateType.Eic) {
                electricalSafetyExpiryDate?.let { Date.valueOf(it.toLocalDate().minusYears(EIC_VALIDITY_YEARS.toLong())) }
            } else {
                null
            }
        val eicrExpiryDate = if (electricalCertType == CertificateType.Eic) null else electricalSafetyExpiryDate

        return PropertyComplianceData(
            hasGasSupply = hasGasSupply,
            gasSafetyCertIssueDate = gasSafetyIssueDate,
            eicrExpiryDate = eicrExpiryDate,
            eicIssueDate = eicIssueDate,
            electricalCertType = electricalCertType,
            epcNumber = epcNumber,
            epcExpiryDate = epcExpiryDate,
            tenancyStartedBeforeEpcExpiry = tenancyStartedBeforeEpcExpiry,
            epcEnergyRating = epcEnergyRating,
            epcExemptionReason = epcExemptionReason,
            epcMeesExemptionReason = epcMeesExemptionReason,
        )
    }

    fun isGasSafetyCertificateCurrent(issueDate: Date?): Boolean =
        issueDate?.let {
            !CertificateExpiryHelper.isGasIssueDateExpired(
                it.toLocalDate(),
                isGas14MonthExpiryEnabled = true,
                currentDate = referenceDate(),
            )
        } == true

    fun isElectricalSafetyCertificateCurrent(
        expiryDate: Date?,
        certType: CertificateType?,
        asOf: Timestamp,
    ): Boolean {
        if (expiryDate == null) return false
        val asOfDate = asOf.toInstant().toSeedingDate()
        return when (certType) {
            CertificateType.Eic -> !CertificateExpiryHelper.isEicExpiryDateExpired(expiryDate.toLocalDate(), asOfDate)
            CertificateType.Eicr -> !CertificateExpiryHelper.isEicrExpiryDateExpired(expiryDate.toLocalDate(), asOfDate)
            else -> throw IllegalArgumentException("Unexpected electrical certificate type: $certType")
        }
    }

    fun generateJourneyId(): String = faker.regexify("[a-z0-9]{7}")

    fun generateIncompletePropertyJourneyState(address: Address): String {
        val propertyType = generatePropertyAndOtherType()
        return buildJsonObject {
            putJsonObject("journeyData") {
                putJsonObject("lookup-address") {
                    put("houseNameOrNumber", address.buildingNumber ?: address.buildingName.orEmpty())
                    put("postcode", address.postcode)
                }
                putJsonObject("select-address") {
                    put("address", address.singleLineAddress)
                }
                putJsonObject("property-type") {
                    put("propertyType", propertyType.first.name)
                    put("customPropertyType", propertyType.second.orEmpty())
                }
            }
            put("cachedAddresses", Json.encodeToString(listOf(AddressDataModel.fromAddress(address))))
            put("isAddressAlreadyRegistered", Json.encodeToString(false))
        }.toString()
    }

    private val otherPropertyTypes = arrayOf("Bungalow", "Maisonette", "Studio", "Loft", "Cottage")

    private val customBills = arrayOf("Security System", "Pool Maintenance", "Gym Membership", "Parking Fees", "Waste Disposal")

    private val customRentFrequencies = arrayOf("Fortnightly", "Quarterly", "Yearly")

    /**
     * Deliberately fictional street and town names. Generated addresses must never coincide with a real address, so
     * these are paired with the reserved ZZ postcode area (see [generateFakePostcode]).
     */
    private val fakeStreetNames =
        listOf(
            "Fictional",
            "Imaginary",
            "Invented",
            "Pretend",
            "Notional",
            "Hypothetical",
            "Sample",
            "Example",
            "Placeholder",
            "Specimen",
            "Mockingbird",
            "Phantom",
            "Mirage",
            "Chimera",
            "Folly",
            "Whimsy",
            "Fable",
            "Legend",
            "Myth",
            "Parable",
            "Riddle",
            "Rumour",
            "Daydream",
            "Reverie",
            "Foxglove",
            "Bramble",
            "Thistle",
            "Hawthorn",
            "Willow",
            "Alder",
            "Juniper",
            "Larkspur",
            "Kestrel",
            "Heron",
            "Otter",
            "Badger",
            "Marten",
            "Pipit",
            "Curlew",
            "Redshank",
        )

    private val fakeStreetTypes =
        listOf("Road", "Street", "Way", "Avenue", "Close", "Lane", "Drive", "Gardens", "Crescent", "Grove", "Rise", "View")

    private val fakeTownNames =
        listOf(
            "Testerton",
            "Fakenham Parva",
            "Mockbury",
            "Sampleford",
            "Dummerton",
            "Stubbington Magna",
            "Placeholder Green",
            "Exampleside",
            "Notreal Heath",
            "Pretendwick",
            "Fictionbury",
            "Imagineley",
        )

    private val fakePostcodeLetters = ('A'..'Z').filterNot { it in "CIKMOV" }

    /**
     * ZZ is not a real UK postcode area — it is reserved for fictional and test addresses — so a generated postcode
     * can never collide with a genuine one while still being format-valid.
     */
    fun generateFakePostcode(): String {
        val outwardDigits = seededRandom.nextInt(1, 100)
        val inwardDigit = seededRandom.nextInt(0, 10)
        val firstLetter = fakePostcodeLetters[seededRandom.nextInt(fakePostcodeLetters.size)]
        val secondLetter = fakePostcodeLetters[seededRandom.nextInt(fakePostcodeLetters.size)]
        return "ZZ$outwardDigits $inwardDigit$firstLetter$secondLetter"
    }

    fun generateFakeStreet(): FakeStreet =
        FakeStreet(
            streetName = "${pickOne(fakeStreetNames)} ${pickOne(fakeStreetTypes)}",
            townName = pickOne(fakeTownNames),
            postcode = generateFakePostcode(),
        )

    /**
     * Real postcodes cover several addresses, so addresses are generated in streets rather than individually. This
     * keeps postcode lookups representative of production, where a search returns a cluster of results.
     */
    fun generateNumberOfAddressesOnStreet(): Int = seededRandom.nextInt(1, MAX_ADDRESSES_PER_STREET + 1)

    fun buildSingleLineAddress(
        buildingNumber: Int,
        street: FakeStreet,
    ): String = "$buildingNumber ${street.streetName}, ${street.townName}, ${street.postcode}"

    data class FakeStreet(
        val streetName: String,
        val townName: String,
        val postcode: String,
    )

    private val epcNumbers =
        arrayOf(
            "0000-0000-0000-1050-2867",
            "0000-0000-0000-0554-8410",
            "0000-0000-0000-0000-8410",
            "0000-0000-0000-0892-1563",
            "0000-0000-0000-0961-0832",
            "0000-0000-0000-0438-7749",
        )

    private fun generateDateAfter(date: Timestamp): Timestamp = Timestamp.from(faker.timeAndDate().between(date.toInstant(), runReference))

    private fun generateDateBefore(
        date: Date,
        maxDaysAgo: Long,
    ): Date = Date.valueOf(faker.timeAndDate().past(maxDaysAgo, TimeUnit.DAYS, date.toLocalDate().toSeedingInstant()).toSeedingDate())

    private fun generateTimestampBefore(
        date: Timestamp,
        maxDaysAgo: Long,
    ): Timestamp = Timestamp.from(faker.timeAndDate().past(maxDaysAgo, TimeUnit.DAYS, date.toInstant()))

    private fun generateUsername(name: String): String {
        val nameParts = name.split(" ")
        val firstName = nameParts.first().lowercase()
        val lastName = nameParts.last().lowercase()

        return when (faker.random().nextInt(0, 10)) {
            0 -> "$firstName.$lastName"
            1 -> "$firstName$lastName"
            2 -> "$firstName${faker.random().nextInt(1, 999)}"
            3 -> "$firstName.$lastName${faker.random().nextInt(1, 99)}"
            4 -> "$lastName-$firstName"
            5 -> "${firstName.first()}$lastName"
            6 -> "$firstName${lastName.first()}"
            7 -> "$firstName-$lastName"
            8 -> "${firstName}_$lastName"
            9 -> "$lastName$firstName"
            else -> "${firstName.first()}_$lastName"
        }
    }

    data class CoreLandlordDetails(
        val id: Long,
        val subjectId: String,
        val createdDate: Timestamp,
        val landlordType: LandlordType = LandlordType.INDIVIDUAL,
        val organisationDetails: OrganisationLandlordDetails? = null,
    )

    data class OrganisationLandlordDetails(
        val name: String,
        val email: String,
        val phoneNumber: String,
        val isCompany: Boolean,
        val isCharity: Boolean,
        val isTrust: Boolean,
        val companyNumber: String?,
        val charityRegisteredWith: CharityRegulator?,
        val charityNumber: String?,
        val leadTrusteeName: String?,
        val leadTrusteeDateOfBirth: Date?,
        val leadTrusteeEmail: String?,
        val leadTrusteePhone: String?,
        val mainContactName: String,
        val mainContactEmail: String,
        val mainContactPhone: String,
        val registrantName: String,
        val registrantDateOfBirth: Date,
        val registrantEmail: String,
        val registrantPhoneNumber: String,
        val hasGoverningBody: Boolean,
        val users: List<OrganisationalLandlordUserDetails>,
        val pendingInvitations: List<OrganisationalLandlordInvitationDetails>,
    )

    data class OrganisationalLandlordUserDetails(
        val subjectId: String,
        val createdDate: Timestamp,
        val name: String,
        val email: String,
        val role: OrganisationalLandlordUserRole,
    )

    data class OrganisationalLandlordInvitationDetails(
        val invitedEmail: String,
        val role: OrganisationalLandlordUserRole,
        val invitationExpiredEmailSent: Boolean = false,
        val isHidden: Boolean = false,
    )

    data class GoverningBodyMemberDetails(
        val type: GoverningBodyMemberType,
        val name: String,
        val dateOfBirth: Date,
    )

    // A null name, email and address together mean "no details known".
    data class FreeholderDetails(
        val name: String?,
        val email: String?,
        val hasAddress: Boolean,
    )

    // A null name, email and address together mean "no details known".
    data class SuperiorLandlordDetails(
        val name: String?,
        val email: String?,
        val hasAddress: Boolean,
    )

    data class PropertyManagerDetails(
        val name: String,
        val email: String,
    )

    data class PropertyScenario(
        val isRegistrationComplete: Boolean,
        val isOccupied: Boolean,
        val hasLicence: Boolean,
        val licenseProvideLater: Boolean?,
        val tenancyProvideLater: Boolean?,
    )

    data class RentDetails(
        val rentFrequency: RentFrequency,
        val customRentFrequency: String?,
        val rentAmount: BigDecimal,
    )

    data class PropertyComplianceData(
        val hasGasSupply: Boolean,
        val gasSafetyCertIssueDate: Date?,
        val eicrExpiryDate: Date?,
        val eicIssueDate: Date?,
        val electricalCertType: CertificateType?,
        val epcNumber: String?,
        val epcExpiryDate: Date?,
        val tenancyStartedBeforeEpcExpiry: Boolean?,
        val epcEnergyRating: String?,
        val epcExemptionReason: EpcExemptionReason?,
        val epcMeesExemptionReason: MeesExemptionReason?,
    ) {
        val electricalSafetyExpiryDate: Date?
            get() = eicIssueDate?.let { Date.valueOf(it.toLocalDate().plusYears(EIC_VALIDITY_YEARS.toLong())) } ?: eicrExpiryDate
    }
}
