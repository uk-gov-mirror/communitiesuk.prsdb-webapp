package uk.gov.communities.prsdb.webapp.helpers

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.mock.web.MockHttpSession
import tools.jackson.databind.ObjectMapper
import uk.gov.communities.prsdb.webapp.constants.EIC_VALIDITY_YEARS
import uk.gov.communities.prsdb.webapp.constants.enums.CertificateType
import uk.gov.communities.prsdb.webapp.constants.enums.OrganisationalLandlordUserRole
import uk.gov.communities.prsdb.webapp.database.entity.Address
import uk.gov.communities.prsdb.webapp.database.entity.LocalCouncil
import uk.gov.communities.prsdb.webapp.journeys.JourneyStateDelegateProvider
import uk.gov.communities.prsdb.webapp.journeys.JourneyStatePersistenceService
import uk.gov.communities.prsdb.webapp.journeys.JourneyStateService
import uk.gov.communities.prsdb.webapp.models.dataModels.AddressDataModel
import java.sql.Date
import java.sql.Timestamp
import java.time.Instant
import java.time.LocalDate
import java.time.MonthDay
import java.util.TimeZone
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class NftDataFakerTests {
    @ParameterizedTest
    @ValueSource(strings = ["Performance House", "The \"Quoted\" House\\Annexe\nFloor 1"])
    fun `generated incomplete state restores full selected address through journey delegates`(buildingName: String) {
        val address =
            Address(
                AddressDataModel(
                    singleLineAddress = "$buildingName, 1 Performance Road, London, EG1 1EG",
                    uprn = 900000000001,
                    buildingName = buildingName,
                    buildingNumber = "1",
                    streetName = "Performance Road",
                    townName = "London",
                    postcode = "EG1 1EG",
                ),
                LocalCouncil(2, "Test council", "1234"),
            )
        val serializedState = NftDataFaker.generateIncompletePropertyJourneyState(address)
        val persistenceService = mock<JourneyStatePersistenceService>()
        whenever(persistenceService.retrieveJourneyStateData("seeded-state"))
            .thenReturn(ObjectMapper().readValue(serializedState, Any::class.java))
        val stateService = JourneyStateService(MockHttpSession(), mock(), persistenceService)
        stateService.setJourneyId("seeded-state")
        val delegateProvider = JourneyStateDelegateProvider(stateService)
        val state =
            object {
                val cachedAddresses: List<AddressDataModel>? by delegateProvider.nullableDelegate("cachedAddresses")
                val isAddressAlreadyRegistered: Boolean? by delegateProvider.nullableDelegate("isAddressAlreadyRegistered")
            }

        assertEquals(listOf(AddressDataModel.fromAddress(address)), state.cachedAddresses)
        assertEquals(false, state.isAddressAlreadyRegistered)
        assertEquals(
            mapOf("address" to address.singleLineAddress),
            stateService.getSubmittedStepData()["select-address"],
        )
    }

    @Test
    fun `same seed produces the same scenario sequence`() {
        NftDataFaker.reset(seed = TEST_SEED)
        val first = List(SAMPLE_SIZE) { NftDataFaker.generatePropertyScenario() }

        NftDataFaker.reset(seed = TEST_SEED)
        val second = List(SAMPLE_SIZE) { NftDataFaker.generatePropertyScenario() }

        assertEquals(first, second)
    }

    @Test
    fun `different seeds produce different scenario sequences`() {
        NftDataFaker.reset(seed = TEST_SEED)
        val first = List(SAMPLE_SIZE) { NftDataFaker.generatePropertyScenario() }

        NftDataFaker.reset(seed = TEST_SEED + 1)
        val second = List(SAMPLE_SIZE) { NftDataFaker.generatePropertyScenario() }

        assertNotEquals(first, second)
    }

    @Test
    fun `scenario sample contains each supported property state`() {
        NftDataFaker.reset(seed = TEST_SEED)
        val scenarios = List(SAMPLE_SIZE) { NftDataFaker.generatePropertyScenario() }

        assertTrue(scenarios.any { !it.isRegistrationComplete })
        assertTrue(scenarios.any { it.isRegistrationComplete && it.isOccupied })
        assertTrue(scenarios.any { it.isRegistrationComplete && !it.isOccupied })
        assertTrue(scenarios.any { it.isRegistrationComplete && it.hasLicence })
        assertTrue(scenarios.any { it.isRegistrationComplete && !it.hasLicence && it.licenseProvideLater == true })
        assertTrue(scenarios.any { it.isRegistrationComplete && it.isOccupied && it.tenancyProvideLater == true })
    }

    @Test
    fun `scenario states preserve property invariants`() {
        NftDataFaker.reset(seed = TEST_SEED)
        val scenarios = List(SAMPLE_SIZE) { NftDataFaker.generatePropertyScenario() }

        scenarios.forEach {
            if (!it.isRegistrationComplete) {
                assertEquals(false, it.isOccupied)
                assertEquals(false, it.hasLicence)
            }
            if (!it.isOccupied) {
                assertEquals(null, it.tenancyProvideLater)
            }
            if (it.hasLicence) {
                assertEquals(null, it.licenseProvideLater)
            }
        }
    }

    @Test
    fun `same seed produces the same faker-generated values`() {
        NftDataFaker.reset(seed = TEST_SEED)
        val first = generateFakerSample()

        NftDataFaker.reset(seed = TEST_SEED)
        val second = generateFakerSample()

        assertEquals(first, second)
    }

    @Test
    fun `different seeds produce different faker-generated values`() {
        NftDataFaker.reset(seed = TEST_SEED)
        val first = generateFakerSample()

        NftDataFaker.reset(seed = TEST_SEED + 1)
        val second = generateFakerSample()

        assertNotEquals(first, second)
    }

    @Test
    fun `same seed and reference produce the same generated dates`() {
        NftDataFaker.reset(seed = TEST_SEED, reference = FIXED_REFERENCE)
        val first = List(FAKER_SAMPLE_SIZE) { NftDataFaker.generateCreatedDate().toString() }

        NftDataFaker.reset(seed = TEST_SEED, reference = FIXED_REFERENCE)
        val second = List(FAKER_SAMPLE_SIZE) { NftDataFaker.generateCreatedDate().toString() }

        assertEquals(first, second)
    }

    @Test
    fun `generateDateOfBirth returns dates between 18 and 120 years before the reference date`() {
        val datesOfBirth = generateDatesOfBirth()

        val referenceDate = LocalDate.of(2025, 1, 1)
        assertTrue(datesOfBirth.all { it >= referenceDate.minusYears(120) && it < referenceDate.minusYears(18) })
    }

    @Test
    fun `generateDateOfBirth returns the same dates whatever the default time zone`() {
        val datesOfBirthInUtcPlus14 = withDefaultTimeZone("Pacific/Kiritimati") { generateDatesOfBirth() }
        val datesOfBirthInUtcMinus12 = withDefaultTimeZone("Etc/GMT+12") { generateDatesOfBirth() }

        assertEquals(datesOfBirthInUtcPlus14, datesOfBirthInUtcMinus12)
    }

    @Test
    fun `generateLastEmailReminderSentDate returns the same instant whatever the default time zone`() {
        val createdDateBeforeClocksGoBack = Timestamp.from(Instant.parse("2025-10-20T12:00:00Z"))

        val ukReminderDate =
            withDefaultTimeZone("Europe/London") { NftDataFaker.generateLastEmailReminderSentDate(createdDateBeforeClocksGoBack) }
        val utcReminderDate = withDefaultTimeZone("UTC") { NftDataFaker.generateLastEmailReminderSentDate(createdDateBeforeClocksGoBack) }

        assertEquals(utcReminderDate, ukReminderDate)
    }

    @Test
    fun `generatePropertyComplianceData stores EIC issue dates and EICR expiry dates`() {
        NftDataFaker.reset(seed = TEST_SEED, reference = FIXED_REFERENCE)
        val sample = List(FAKER_SAMPLE_SIZE) { NftDataFaker.generatePropertyComplianceData(Timestamp.from(FIXED_REFERENCE)) }

        sample.forEach {
            when (it.electricalCertType) {
                CertificateType.Eic -> {
                    assertTrue(it.eicIssueDate != null && it.eicrExpiryDate == null)
                    assertEquals(
                        it.eicIssueDate!!.toLocalDate().plusYears(EIC_VALIDITY_YEARS.toLong()),
                        it.electricalSafetyExpiryDate!!.toLocalDate(),
                    )
                }

                CertificateType.Eicr -> assertTrue(it.eicIssueDate == null && it.eicrExpiryDate != null)
                else -> assertTrue(it.eicIssueDate == null && it.eicrExpiryDate == null)
            }
        }
        assertTrue(sample.any { it.electricalCertType == CertificateType.Eic })
        assertTrue(sample.any { it.electricalCertType == CertificateType.Eicr })
    }

    @Test
    fun `generateRenewalDate returns next year's anniversary when this year's has passed`() {
        // Arrange
        NftDataFaker.reset(seed = TEST_SEED, reference = MIDYEAR_REFERENCE)

        // Act
        val renewalDate = NftDataFaker.generateRenewalDate(MonthDay.of(3, 10))

        // Assert
        assertEquals(LocalDate.of(2026, 3, 10), renewalDate.toLocalDate())
    }

    @Test
    fun `generateRenewalDate returns this year's anniversary when it is still to come`() {
        // Arrange
        NftDataFaker.reset(seed = TEST_SEED, reference = MIDYEAR_REFERENCE)

        // Act
        val renewalDate = NftDataFaker.generateRenewalDate(MonthDay.of(9, 1))

        // Assert
        assertEquals(LocalDate.of(2025, 9, 1), renewalDate.toLocalDate())
    }

    @Test
    fun `isGasSafetyCertificateCurrent returns true on the expiry date 14 months after the issue date`() {
        NftDataFaker.reset(seed = TEST_SEED, reference = MIDYEAR_REFERENCE)

        assertTrue(NftDataFaker.isGasSafetyCertificateCurrent(Date.valueOf(LocalDate.of(2024, 4, 15))))
    }

    @Test
    fun `isGasSafetyCertificateCurrent returns false the day after the expiry date 14 months after the issue date`() {
        NftDataFaker.reset(seed = TEST_SEED, reference = MIDYEAR_REFERENCE)

        assertFalse(NftDataFaker.isGasSafetyCertificateCurrent(Date.valueOf(LocalDate.of(2024, 4, 14))))
    }

    @Test
    fun `isElectricalSafetyCertificateCurrent returns true on the expiry date`() {
        listOf(CertificateType.Eic, CertificateType.Eicr).forEach { certType ->
            assertTrue(
                NftDataFaker.isElectricalSafetyCertificateCurrent(
                    Date.valueOf(LocalDate.of(2025, 6, 15)),
                    certType,
                    Timestamp.from(MIDYEAR_REFERENCE),
                ),
            )
        }
    }

    @Test
    fun `isElectricalSafetyCertificateCurrent returns true the day before the expiry date`() {
        listOf(CertificateType.Eic, CertificateType.Eicr).forEach { certType ->
            assertTrue(
                NftDataFaker.isElectricalSafetyCertificateCurrent(
                    Date.valueOf(LocalDate.of(2025, 6, 16)),
                    certType,
                    Timestamp.from(MIDYEAR_REFERENCE),
                ),
            )
        }
    }

    @Test
    fun `isElectricalSafetyCertificateCurrent throws when the cert type is missing`() {
        assertThrows<IllegalArgumentException> {
            NftDataFaker.isElectricalSafetyCertificateCurrent(
                Date.valueOf(LocalDate.of(2025, 6, 16)),
                null,
                Timestamp.from(MIDYEAR_REFERENCE),
            )
        }
    }

    @Test
    fun `generateOrganisationalLandlordUsers always makes the first (registrant) user the admin`() {
        NftDataFaker.reset(seed = TEST_SEED)
        val orgCreatedDate = Timestamp.from(FIXED_REFERENCE)

        repeat(SAMPLE_SIZE / 100) {
            val users =
                NftDataFaker.generateOrganisationalLandlordUsers(
                    registrantSubjectId = "registrant-subject-id",
                    registrantName = "Registrant Name",
                    registrantEmail = "registrant@example.com",
                    orgCreatedDate = orgCreatedDate,
                )

            assertTrue(users.isNotEmpty())
            val registrant = users.first()
            assertEquals("registrant-subject-id", registrant.subjectId)
            assertEquals("Registrant Name", registrant.name)
            assertEquals("registrant@example.com", registrant.email)
            assertEquals(orgCreatedDate, registrant.createdDate)
            assertEquals(OrganisationalLandlordUserRole.ADMIN, registrant.role)
        }
    }

    @Test
    fun `generateOrganisationalLandlordUsers sample contains organisations with and without extra users, and both roles`() {
        NftDataFaker.reset(seed = TEST_SEED)
        val orgCreatedDate = Timestamp.from(FIXED_REFERENCE)

        val samples =
            List(SAMPLE_SIZE / 100) {
                NftDataFaker.generateOrganisationalLandlordUsers(
                    registrantSubjectId = "registrant-subject-id-$it",
                    registrantName = "Registrant Name",
                    registrantEmail = "registrant@example.com",
                    orgCreatedDate = orgCreatedDate,
                )
            }

        assertTrue(samples.any { it.size == 1 }, "Expected at least one organisation with no extra users")
        assertTrue(samples.any { it.size > 1 }, "Expected at least one organisation with extra users")
        assertTrue(
            samples.flatMap { it.drop(1) }.any { it.role == OrganisationalLandlordUserRole.EDITOR },
            "Expected at least one extra user with the EDITOR role",
        )
    }

    @Test
    fun `generated freeholders are either named with one contact method or have no details at all`() {
        NftDataFaker.reset(seed = TEST_SEED)

        val freeholders = List(SAMPLE_SIZE / 100) { NftDataFaker.generateFreeholders() }.flatten()

        assertPartiesAreNamedWithOneContactMethodOrHaveNoDetails(
            freeholders,
            { it.name },
            { it.email },
            { it.hasAddress },
        )
    }

    @Test
    fun `generated superior landlords are either named with one contact method or have no details at all`() {
        NftDataFaker.reset(seed = TEST_SEED)

        val superiorLandlords = List(SAMPLE_SIZE / 100) { NftDataFaker.generateSuperiorLandlords() }.flatten()

        assertPartiesAreNamedWithOneContactMethodOrHaveNoDetails(
            superiorLandlords,
            { it.name },
            { it.email },
            { it.hasAddress },
        )
    }

    private fun <T> assertPartiesAreNamedWithOneContactMethodOrHaveNoDetails(
        parties: List<T>,
        name: (T) -> String?,
        email: (T) -> String?,
        hasAddress: (T) -> Boolean,
    ) {
        parties.forEach { party ->
            if (name(party) == null) {
                assertFalse(
                    email(party) != null || hasAddress(party),
                    "Expected a party with no name to have no email or address",
                )
            } else {
                assertNotEquals(
                    email(party) != null,
                    hasAddress(party),
                    "Expected a named party to have exactly one of an email and an address",
                )
            }
        }

        assertTrue(parties.any { name(it) == null }, "Expected at least one party with no details")
        assertTrue(parties.any { email(it) != null }, "Expected at least one party contactable by email")
        assertTrue(parties.any { hasAddress(it) }, "Expected at least one party contactable by post")
    }

    private fun generateFakerSample(): List<String> =
        List(FAKER_SAMPLE_SIZE) {
            listOf(
                NftDataFaker.generateNumberOfPropertiesForLandlord(),
                NftDataFaker.generateNumberLessThan(1000),
                NftDataFaker.generateBoolean(),
                NftDataFaker.generateInvitationToken(),
                NftDataFaker.pickOne(listOf(1, 2, 3, 4, 5)),
                NftDataFaker.shuffle(listOf(1, 2, 3, 4, 5)),
                NftDataFaker.generateStandardAndCustomBillsIncluded(),
            ).joinToString("|")
        }

    private fun generateDatesOfBirth(): List<LocalDate> {
        NftDataFaker.reset(seed = TEST_SEED, reference = FIXED_REFERENCE)
        return List(FAKER_SAMPLE_SIZE) { NftDataFaker.generateDateOfBirth().toLocalDate() }
    }

    private fun <T> withDefaultTimeZone(
        timeZoneId: String,
        block: () -> T,
    ): T {
        val originalTimeZone = TimeZone.getDefault()
        try {
            TimeZone.setDefault(TimeZone.getTimeZone(timeZoneId))
            return block()
        } finally {
            TimeZone.setDefault(originalTimeZone)
        }
    }

    companion object {
        private const val TEST_SEED = 239L
        private const val SAMPLE_SIZE = 10_000
        private const val FAKER_SAMPLE_SIZE = 500
        private val FIXED_REFERENCE: Instant = Instant.parse("2025-01-01T00:00:00Z")
        private val MIDYEAR_REFERENCE: Instant = Instant.parse("2025-06-15T12:00:00Z")
    }
}
