package uk.gov.communities.prsdb.webapp.services

import org.hibernate.SessionFactory
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.TestPropertySource
import uk.gov.communities.prsdb.webapp.TestcontainersConfiguration
import uk.gov.communities.prsdb.webapp.database.repository.IndividualLandlordRepository
import uk.gov.communities.prsdb.webapp.database.repository.LandlordIncompletePropertiesRepository
import uk.gov.communities.prsdb.webapp.database.repository.LandlordRepository
import uk.gov.communities.prsdb.webapp.database.repository.LocalCouncilRepository
import uk.gov.communities.prsdb.webapp.database.repository.OrganisationGoverningBodyMemberRepository
import uk.gov.communities.prsdb.webapp.database.repository.OrganisationLandlordRepository
import uk.gov.communities.prsdb.webapp.database.repository.OrganisationalLandlordUserRepository
import uk.gov.communities.prsdb.webapp.database.repository.PropertyOwnershipRepository
import uk.gov.communities.prsdb.webapp.testHelpers.IntegrationTestHelper

// This test seeds a small but non-trivial volume of data, so it exercises the same batching, address-generation and
// organisation-landlord branching logic as a real NFT run, just at a scale that completes quickly on a test database.
//
// Deliberately does NOT activate the "nft-data-seeder" profile: NftDataSeedingTaskApplicationRunner (which is only
// active under that profile) calls exitProcess() once seeding finishes, which would kill the test JVM before JUnit
// could run any @Test methods. Instead, NftDataSeeder is constructed directly with test-scale values, bypassing the
// task-runner profile entirely.
@Import(TestcontainersConfiguration::class)
@SpringBootTest
@ActiveProfiles("web-server-deactivated", "local")
@TestPropertySource(
    properties = [
        "EMAILNOTIFICATIONS_APIKEY=test",
        "OS_API_KEY=test",
    ],
)
class NftDataSeederTests(
    @Autowired private val sessionFactory: SessionFactory,
    @Autowired private val localCouncilRepository: LocalCouncilRepository,
    @Autowired private val jdbcTemplate: JdbcTemplate,
    @Autowired private val landlordRepository: LandlordRepository,
    @Autowired private val individualLandlordRepository: IndividualLandlordRepository,
    @Autowired private val organisationLandlordRepository: OrganisationLandlordRepository,
    @Autowired private val organisationalLandlordUserRepository: OrganisationalLandlordUserRepository,
    @Autowired private val organisationGoverningBodyMemberRepository: OrganisationGoverningBodyMemberRepository,
    @Autowired private val propertyOwnershipRepository: PropertyOwnershipRepository,
    @Autowired private val incompletePropertiesRepository: LandlordIncompletePropertiesRepository,
) {
    private val numOfLandlords = 200
    private val numOfProperties = 300

    private fun newSeeder(
        landlords: Int = numOfLandlords,
        properties: Int = numOfProperties,
        batchSize: Int = 25,
        generatedAddresses: Int = 2000,
    ) = NftDataSeeder(
        sessionFactory = sessionFactory,
        localCouncilRepository = localCouncilRepository,
        epcCertificateBaseUrl = "http://localhost",
        numOfSystemOperators = 2,
        numOfLcUsers = 2,
        numOfLandlords = landlords,
        numOfProperties = properties,
        batchSize = batchSize,
        randomSeed = 239L,
        // A blank referenceDate resolves to Instant.now(), which makes seeding non-reproducible: the
        // random-call sequence for date generation depends on the exact instant, so any two runs (even with
        // the same random seed) can diverge. Use a fixed date so "seedDatabase is deterministic" actually
        // tests what it claims to test.
        referenceDate = "2024-01-01",
        numOfGeneratedAddresses = generatedAddresses,
    )

    @BeforeEach
    fun setUp() {
        IntegrationTestHelper.resetDatabase(jdbcTemplate)
    }

    @Test
    fun `seedDatabase seeds the configured number of landlords and properties without error`() {
        newSeeder().seedDatabase()

        assertEquals(numOfLandlords.toLong(), landlordRepository.count())
        assertEquals(numOfProperties.toLong(), propertyOwnershipRepository.count() + incompletePropertiesRepository.count())
    }

    @Test
    fun `seedDatabase seeds populated property compliance and certificate upload metadata`() {
        newSeeder().seedDatabase()

        val populatedComplianceCount =
            jdbcTemplate.queryForObject(
                """
                SELECT count(*)
                FROM property_compliance
                WHERE has_gas_supply IS NOT NULL
                   OR electrical_safety_expiry_date IS NOT NULL
                   OR epc_url IS NOT NULL
                   OR epc_exemption_reason IS NOT NULL
                """.trimIndent(),
                Long::class.java,
            )

        assertTrue(populatedComplianceCount!! > 0, "Expected populated property compliance records")
        assertTrue(countRows("file_upload") > 0, "Expected certificate file upload metadata")
        assertTrue(countRows("gas_safety_uploads") > 0, "Expected gas safety certificate relationships")
        assertTrue(countRows("electrical_safety_uploads") > 0, "Expected electrical certificate relationships")
    }

    @Test
    fun `seedDatabase replenishes property addresses when fewer than one replenishment remain`() {
        val properties = 150

        newSeeder(
            landlords = 120,
            properties = properties,
            batchSize = 20,
            generatedAddresses = 180,
        ).seedDatabase()

        assertEquals(
            properties.toLong(),
            propertyOwnershipRepository.count() + incompletePropertiesRepository.count(),
        )
    }

    @Test
    fun `seedDatabase reuses property addresses for correspondence without reserving extras`() {
        val properties = 80

        newSeeder(
            landlords = 120,
            properties = properties,
            batchSize = 10,
            generatedAddresses = properties,
        ).seedDatabase()

        assertEquals(
            properties.toLong(),
            propertyOwnershipRepository.count() + incompletePropertiesRepository.count(),
        )
        assertEquals(
            propertyOwnershipRepository.count(),
            jdbcTemplate.queryForObject(
                """
                SELECT count(*)
                FROM property_ownership po
                JOIN address a ON a.id = po.correspondence_address_id
                WHERE po.correspondence_email IS NOT NULL
                  AND po.correspondence_address_id = po.address_id
                """.trimIndent(),
                Long::class.java,
            ),
        )
    }

    @Test
    fun `seedDatabase fails clearly when addresses are exhausted`() {
        val exception =
            assertThrows(IllegalStateException::class.java) {
                newSeeder(
                    landlords = 20,
                    properties = 80,
                    batchSize = 10,
                    generatedAddresses = 30,
                ).seedDatabase()
            }

        assertTrue(exception.message!!.contains("not enough addresses"))
    }

    @Test
    fun `seedDatabase seeds both individual and organisation landlords`() {
        newSeeder().seedDatabase()

        val individualCount = individualLandlordRepository.count()
        val organisationCount = organisationLandlordRepository.count()

        assertEquals(numOfLandlords.toLong(), individualCount + organisationCount)
        assertTrue(individualCount > 0, "Expected at least one individual landlord to be seeded")
        assertTrue(organisationCount > 0, "Expected at least one organisation landlord to be seeded")
    }

    @Test
    fun `seedDatabase gives every organisation landlord exactly one organisational landlord user`() {
        newSeeder().seedDatabase()

        val organisationLandlords = organisationLandlordRepository.findAll()
        assertTrue(organisationLandlords.isNotEmpty(), "Expected at least one organisation landlord to be seeded")

        organisationLandlords.forEach { organisationLandlord ->
            val users = organisationalLandlordUserRepository.findByOrganisationalLandlord(organisationLandlord)
            assertEquals(1, users.size) {
                "Expected exactly one organisational landlord user for organisation landlord ${organisationLandlord.id}"
            }
        }
    }

    @Test
    fun `seedDatabase only gives non-company organisations governing body members`() {
        newSeeder().seedDatabase()

        val organisationLandlords = organisationLandlordRepository.findAll()
        assertTrue(organisationLandlords.isNotEmpty(), "Expected at least one organisation landlord to be seeded")
        assertTrue(
            organisationLandlords.any { it.isCompany },
            "Expected at least one company organisation landlord to be seeded",
        )
        assertTrue(
            organisationLandlords.any { !it.isCompany },
            "Expected at least one non-company organisation landlord to be seeded",
        )

        organisationLandlords.forEach { organisationLandlord ->
            val memberCount =
                organisationGoverningBodyMemberRepository
                    .findAll()
                    .count { it.organisationalLandlord.id == organisationLandlord.id }

            if (organisationLandlord.isCompany) {
                assertEquals(0, memberCount) {
                    "Expected company organisation landlord ${organisationLandlord.id} to have no governing body members"
                }
            } else {
                assertTrue(memberCount in 1..3) {
                    "Expected non-company organisation landlord ${organisationLandlord.id} to have 1-3 governing " +
                        "body members, but had $memberCount"
                }
            }
        }
    }

    @Test
    fun `seedDatabase is deterministic for a given random seed`() {
        newSeeder().seedDatabase()
        val firstRunSnapshot = captureSeedSnapshot()

        IntegrationTestHelper.resetDatabase(jdbcTemplate)

        newSeeder().seedDatabase()
        val secondRunSnapshot = captureSeedSnapshot()

        assertEquals(firstRunSnapshot, secondRunSnapshot)
    }

    private fun countRows(tableName: String): Long =
        jdbcTemplate.queryForObject(
            "SELECT count(*) FROM $tableName",
            Long::class.java,
        )!!

    private fun captureSeedSnapshot() =
        SeedSnapshot(
            landlords =
                jdbcTemplate.queryForList(
                    """
                    SELECT rn.number, l.landlord_type, l.organisation_landlord_name,
                           l.organisation_is_company, l.organisation_is_charity, l.organisation_is_trust
                    FROM landlord l
                    JOIN registration_number rn ON rn.id = l.registration_number_id
                    ORDER BY rn.number
                    """.trimIndent(),
                ),
            properties =
                jdbcTemplate.queryForList(
                    """
                    SELECT po.id, a.uprn, po.ownership_type, po.property_build_type, po.is_occupied,
                           po.license_provide_later, po.tenancy_provide_later, po.current_num_households,
                           po.current_num_tenants, po.num_bedrooms
                    FROM property_ownership po
                    JOIN address a ON a.id = po.address_id
                    ORDER BY po.id
                    """.trimIndent(),
                ),
            incompleteProperties =
                jdbcTemplate.queryForList(
                    """
                    SELECT lip.user_id, sjs.journey_id, sjs.serialized_state, sjs.reminder_email_sent_id
                    FROM landlord_incomplete_properties lip
                    JOIN saved_journey_state sjs ON sjs.id = lip.saved_journey_state_id
                    ORDER BY sjs.id
                    """.trimIndent(),
                ),
            compliance =
                jdbcTemplate.queryForList(
                    """
                    SELECT property_ownership_id, gas_safety_cert_issue_date, has_gas_supply,
                           electrical_safety_expiry_date, electrical_cert_type, epc_url, epc_expiry_date,
                           tenancy_started_before_epc_expiry, epc_energy_rating, epc_exemption_reason,
                           epc_mees_exemption_reason
                    FROM property_compliance
                    ORDER BY property_ownership_id
                    """.trimIndent(),
                ),
            uploads =
                jdbcTemplate.queryForList(
                    """
                    SELECT fu.object_key, fu.e_tag, g.property_compliance_id AS gas_compliance_id,
                           e.property_compliance_id AS electrical_compliance_id
                    FROM file_upload fu
                    LEFT JOIN gas_safety_uploads g ON g.gas_safety_file_uploads_id = fu.id
                    LEFT JOIN electrical_safety_uploads e ON e.electrical_safety_file_uploads_id = fu.id
                    ORDER BY fu.object_key
                    """.trimIndent(),
                ),
        )

    private data class SeedSnapshot(
        val landlords: List<Map<String, Any>>,
        val properties: List<Map<String, Any>>,
        val incompleteProperties: List<Map<String, Any>>,
        val compliance: List<Map<String, Any>>,
        val uploads: List<Map<String, Any>>,
    )
}
