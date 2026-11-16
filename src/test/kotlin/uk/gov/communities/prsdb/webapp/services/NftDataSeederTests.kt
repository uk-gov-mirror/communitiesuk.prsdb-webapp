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
import org.springframework.core.io.ClassPathResource
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.queryForList
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.TestPropertySource
import uk.gov.communities.prsdb.webapp.TestcontainersConfiguration
import uk.gov.communities.prsdb.webapp.constants.enums.OrganisationalLandlordUserRole
import uk.gov.communities.prsdb.webapp.constants.enums.OwnershipType
import uk.gov.communities.prsdb.webapp.database.dao.NftDataSeederDao
import uk.gov.communities.prsdb.webapp.database.repository.IndividualLandlordRepository
import uk.gov.communities.prsdb.webapp.database.repository.LandlordIncompletePropertiesRepository
import uk.gov.communities.prsdb.webapp.database.repository.LandlordRepository
import uk.gov.communities.prsdb.webapp.database.repository.LocalCouncilRepository
import uk.gov.communities.prsdb.webapp.database.repository.OrganisationGoverningBodyMemberRepository
import uk.gov.communities.prsdb.webapp.database.repository.OrganisationLandlordRepository
import uk.gov.communities.prsdb.webapp.database.repository.OrganisationalLandlordUserRepository
import uk.gov.communities.prsdb.webapp.database.repository.PropertyOwnershipRepository
import uk.gov.communities.prsdb.webapp.database.repository.PrsdbUserRepository
import uk.gov.communities.prsdb.webapp.helpers.NftDataFaker
import uk.gov.communities.prsdb.webapp.testHelpers.IntegrationTestHelper
import java.util.TimeZone
import kotlin.test.assertNotNull

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
    @Autowired private val prsdbUserRepository: PrsdbUserRepository,
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
        randomSeed: Long = 239L,
        minimumFreeAddresses: Int = 0,
        incompleteProperties: Int = properties / 10,
    ) = NftDataSeeder(
        sessionFactory = sessionFactory,
        localCouncilRepository = localCouncilRepository,
        epcCertificateBaseUrl = "http://localhost",
        numOfSystemOperators = 2,
        numOfLcUsers = 2,
        numOfLandlords = landlords,
        numOfProperties = properties - incompleteProperties,
        batchSize = batchSize,
        randomSeed = randomSeed,
        // A blank referenceDate resolves to Instant.now(), which makes seeding non-reproducible: the
        // random-call sequence for date generation depends on the exact instant, so any two runs (even with
        // the same random seed) can diverge. Use a fixed date so "seedDatabase is deterministic" actually
        // tests what it claims to test.
        referenceDate = "2024-01-01",
        numOfGeneratedAddresses = generatedAddresses,
        minimumFreeAddresses = minimumFreeAddresses,
        numOfIncompleteProperties = incompleteProperties,
    )

    @BeforeEach
    fun setUp() {
        IntegrationTestHelper.resetDatabase(jdbcTemplate)
    }

    @Test
    fun `seeder collision checks use custom planning without leaking after commit or rollback`() {
        sessionFactory.openStatelessSession().use { session ->
            session.doWork { connection ->
                connection.createStatement().use { it.execute("SET plan_cache_mode = force_generic_plan") }
            }
            try {
                for (commit in listOf(true, false)) {
                    val transaction = session.beginTransaction()
                    session.doWork { connection ->
                        NftDataSeederDao(session, connection)
                        connection.createStatement().use { statement ->
                            statement.executeQuery("SHOW plan_cache_mode").use { result ->
                                result.next()
                                assertEquals("force_custom_plan", result.getString(1))
                            }
                        }
                    }
                    if (commit) transaction.commit() else transaction.rollback()
                    session.doWork { connection ->
                        connection.createStatement().use { statement ->
                            statement.executeQuery("SHOW plan_cache_mode").use { result ->
                                result.next()
                                assertEquals("force_generic_plan", result.getString(1))
                            }
                        }
                    }
                }
            } finally {
                session.doWork { connection ->
                    connection.createStatement().use { it.execute("RESET plan_cache_mode") }
                }
            }
        }
    }

    @Test
    fun `seedDatabase seeds the configured number of landlords and properties without error`() {
        newSeeder(minimumFreeAddresses = 100).seedDatabase()

        assertEquals(numOfLandlords.toLong(), landlordRepository.count())
        assertEquals(numOfProperties.toLong(), propertyOwnershipRepository.count() + incompletePropertiesRepository.count())
    }

    @Test
    fun `seedDatabase produces exact completed and incomplete property counts`() {
        newSeeder(properties = 300, incompleteProperties = 30).seedDatabase()

        assertEquals(270L, propertyOwnershipRepository.count())
        assertEquals(30L, incompletePropertiesRepository.count())
    }

    @Test
    fun `seedDatabase derives the landlord population from portfolios when no landlord count is supplied`() {
        newSeeder(landlords = 0, properties = 120, incompleteProperties = 20).seedDatabase()

        assertEquals(100L, propertyOwnershipRepository.count())
        assertEquals(20L, incompletePropertiesRepository.count())
        assertTrue(landlordRepository.count() > 0)
        assertTrue(
            jdbcTemplate.queryForObject(
                """
                SELECT count(*) FROM landlord l
                WHERE NOT EXISTS (SELECT 1 FROM ownership_link ol WHERE ol.landlord_id = l.id)
                AND NOT EXISTS (
                    SELECT 1 FROM saved_journey_state sjs
                    WHERE sjs.subject_identifier = l.individual_subject_identifier
                )
                AND l.landlord_type = 0
                """.trimIndent(),
                Long::class.java,
            )!! > 0,
        )
    }

    @Test
    fun `seedDatabase can generate only completed properties`() {
        newSeeder(properties = 100, incompleteProperties = 0).seedDatabase()

        assertEquals(100L, propertyOwnershipRepository.count())
        assertEquals(0L, incompletePropertiesRepository.count())
    }

    @Test
    fun `seedDatabase can generate only incomplete properties`() {
        newSeeder(properties = 100, incompleteProperties = 100).seedDatabase()

        assertEquals(0L, propertyOwnershipRepository.count())
        assertEquals(100L, incompletePropertiesRepository.count())
    }

    @Test
    fun `seedDatabase preserves the NFT fixture and adds the configured volume`() {
        jdbcTemplate.execute(
            """
            INSERT INTO address (id, created_date, uprn, single_line_address, postcode, local_council_id)
            VALUES (1, current_timestamp, 1, '1 Test Street', 'TE1 1ST', 1)
            """.trimIndent(),
        )
        jdbcTemplate.execute(
            """
            INSERT INTO prsdb_user (id, created_date) VALUES ('fixture-landlord', current_timestamp);
            INSERT INTO registration_number (id, created_date, number, type)
            VALUES (2147484000, current_timestamp, 210000000001, 1);
            INSERT INTO landlord
                (id, registration_number_id, individual_address_id, created_date, individual_email,
                 is_active, individual_name, individual_phone_number, individual_subject_identifier,
                 individual_date_of_birth, individual_country_of_residence, individual_is_verified,
                 individual_has_accepted_privacy_notice)
            VALUES (2147484000, 2147484000, 1, current_timestamp, 'fixture@example.com',
                    true, 'Fixture Landlord', '07777777777', 'fixture-landlord',
                    '1980-01-01', 'England or Wales', true, true);
            INSERT INTO system_operator (id, created_date, subject_identifier)
            VALUES (1, current_timestamp, 'fixture-landlord');
            SELECT setval(pg_get_serial_sequence('address', 'id'), 1);
            """.trimIndent(),
        )
        jdbcTemplate.execute(
            ClassPathResource("data-nft-performance.sql").inputStream.bufferedReader().use { it.readText() },
        )
        val before = captureSeedSnapshot()
        val landlordCount = landlordRepository.count()
        val propertyCount = propertyOwnershipRepository.count() + incompletePropertiesRepository.count()

        newSeeder().seedDatabase()

        assertEquals(landlordCount + numOfLandlords, landlordRepository.count())
        assertEquals(
            propertyCount + numOfProperties,
            propertyOwnershipRepository.count() + incompletePropertiesRepository.count(),
        )
        val after = captureSeedSnapshot()
        assertTrue(after.landlords.containsAll(before.landlords))
        assertTrue(after.properties.containsAll(before.properties))
        assertEquals(
            0L,
            jdbcTemplate.queryForObject(
                "SELECT count(*) FROM property_ownership WHERE address_id BETWEEN 9100000001 AND 9100000005",
                Long::class.java,
            ),
        )
    }

    @Test
    fun `seedDatabase appends records with existing compliance and saved journeys without changing them`() {
        newSeeder().seedDatabase()
        val before = captureSeedSnapshot()
        val propertyCount = propertyOwnershipRepository.count() + incompletePropertiesRepository.count()

        newSeeder(randomSeed = 240L).seedDatabase()

        assertEquals(2L * numOfLandlords, landlordRepository.count())
        assertEquals(
            propertyCount + numOfProperties,
            propertyOwnershipRepository.count() + incompletePropertiesRepository.count(),
        )
        val after = captureSeedSnapshot()
        assertTrue(after.properties.containsAll(before.properties))
        assertTrue(after.incompleteProperties.containsAll(before.incompleteProperties))
        assertTrue(after.compliance.containsAll(before.compliance))
        assertTrue(after.uploads.containsAll(before.uploads))
    }

    @Test
    fun `seedDatabase fails rather than consuming reserved lookup addresses`() {
        jdbcTemplate.execute(
            ClassPathResource("data-nft-performance.sql").inputStream.bufferedReader().use { it.readText() },
        )

        val exception =
            assertThrows(IllegalStateException::class.java) {
                newSeeder(generatedAddresses = 0).seedDatabase()
            }

        assertTrue(exception.message!!.contains("not enough addresses"))
        assertEquals(5L, countRows("address"))
        assertEquals(0L, landlordRepository.count())
    }

    @Test
    fun `seedDatabase rejects insufficient free address headroom and rolls back generated addresses`() {
        val exception =
            assertThrows(IllegalStateException::class.java) {
                newSeeder(generatedAddresses = numOfProperties + 10, minimumFreeAddresses = 20).seedDatabase()
            }

        assertTrue(exception.message!!.contains("20 free addresses"))
        assertEquals(0L, countRows("address"))
        assertEquals(0L, landlordRepository.count())
    }

    @Test
    fun `seedDatabase fails instead of accepting fewer properties than requested`() {
        val exception =
            assertThrows(IllegalStateException::class.java) {
                newSeeder(landlords = 1, properties = 1000).seedDatabase()
            }

        assertTrue(exception.message!!.contains("only generated"))
        assertEquals(0L, countRows("address"))
        assertEquals(0L, countRows("system_operator"))
    }

    @Test
    fun `seedDatabase does not rewind sequences already ahead of the data`() {
        newSeeder().seedDatabase()
        jdbcTemplate.execute("SELECT setval(pg_get_serial_sequence('landlord', 'id'), 70000)")

        newSeeder(randomSeed = 240L).seedDatabase()

        assertEquals(
            70001L,
            jdbcTemplate.queryForObject("SELECT nextval(pg_get_serial_sequence('landlord', 'id'))", Long::class.java),
        )
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
                   OR eicr_expiry_date IS NOT NULL
                   OR eic_issue_date IS NOT NULL
                   OR epc_url IS NOT NULL
                   OR epc_exemption_reason IS NOT NULL
                """.trimIndent(),
                Long::class.java,
            )

        assertTrue(populatedComplianceCount!! > 0, "Expected populated property compliance records")
        assertEquals(
            0L,
            jdbcTemplate.queryForObject(
                """
                SELECT count(*)
                FROM property_compliance
                WHERE (electrical_cert_type = 1 AND eicr_expiry_date IS NOT NULL)
                   OR (electrical_cert_type = 0 AND eic_issue_date IS NOT NULL)
                """.trimIndent(),
                Long::class.java,
            ),
            "Expected EIC records to store only an issue date and EICR records to store only an expiry date",
        )
        assertTrue(countRows("file_upload") > 0, "Expected certificate file upload metadata")
        assertTrue(countRows("gas_safety_uploads") > 0, "Expected gas safety certificate relationships")
        assertTrue(countRows("electrical_safety_uploads") > 0, "Expected electrical certificate relationships")
    }

    @Test
    fun `seedDatabase allocates distinct addresses across completed and incomplete batches and appended runs`() {
        repeat(2) { run ->
            newSeeder(
                landlords = 0,
                properties = 120,
                incompleteProperties = 60,
                batchSize = 5,
                generatedAddresses = if (run == 0) 260 else 0,
                randomSeed = 239L + run,
                minimumFreeAddresses = 20,
            ).seedDatabase()

            val allocations =
                jdbcTemplate.queryForList(
                    """
                    SELECT a.single_line_address AS address
                    FROM property_ownership p JOIN address a ON a.id = p.address_id
                    UNION ALL
                    SELECT s.serialized_state::jsonb #>> '{journeyData,select-address,address}' AS address
                    FROM saved_journey_state s
                    JOIN landlord_incomplete_properties i ON i.saved_journey_state_id = s.id
                    """.trimIndent(),
                    String::class.java,
                )
            assertEquals((run + 1) * 120, allocations.size)
            assertEquals(allocations.size, allocations.toSet().size, "Every completed and incomplete allocation must be distinct")
        }
    }

    @Test
    fun `seedDatabase keeps generated address selections unambiguous alongside an earlier synthetic street`() {
        NftDataFaker.reset(seed = 239L)
        val street = NftDataFaker.generateFakeStreet()
        jdbcTemplate.update(
            """
            INSERT INTO address (created_date, uprn, single_line_address, postcode, building_number, street_name, local_council_id)
            VALUES (current_timestamp, 1, ?, ?, '1', ?, 1)
            """.trimIndent(),
            NftDataFaker.buildSingleLineAddress(1, street),
            street.postcode,
            street.streetName,
        )
        newSeeder(landlords = 0, properties = 70, incompleteProperties = 35, generatedAddresses = 80, batchSize = 5)
            .seedDatabase()

        assertEquals(
            0,
            jdbcTemplate.queryForObject(
                """
                SELECT count(*) FROM (
                    SELECT single_line_address FROM address
                    GROUP BY single_line_address HAVING count(*) > 1
                ) ambiguous_addresses
                """.trimIndent(),
                Int::class.java,
            ),
        )
    }

    @Test
    fun `seedDatabase counts existing incomplete allocations when checking free address headroom`() {
        newSeeder(landlords = 0, properties = 20, incompleteProperties = 20, generatedAddresses = 30, batchSize = 5).seedDatabase()

        assertThrows(IllegalStateException::class.java) {
            newSeeder(
                landlords = 0,
                properties = 10,
                incompleteProperties = 0,
                generatedAddresses = 0,
                minimumFreeAddresses = 1,
                batchSize = 5,
            ).seedDatabase()
        }
        assertEquals(20L, incompletePropertiesRepository.count())
        assertEquals(0L, propertyOwnershipRepository.count())
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
    fun `seedDatabase marks exactly the landlords with registered properties as having registered a property`() {
        newSeeder().seedDatabase()

        val mismatchedCount =
            jdbcTemplate.queryForObject(
                """
                SELECT count(*)
                FROM landlord l
                WHERE l.has_registered_property <> EXISTS (SELECT 1 FROM ownership_link ol WHERE ol.landlord_id = l.id)
                """.trimIndent(),
                Long::class.java,
            )
        val registeredCount =
            jdbcTemplate.queryForObject("SELECT count(*) FROM landlord WHERE has_registered_property", Long::class.java)!!

        assertEquals(0L, mismatchedCount)
        assertTrue(registeredCount > 0, "Expected some landlords to have registered a property")
        assertTrue(registeredCount < numOfLandlords, "Expected some landlords not to have registered a property")
    }

    @Test
    fun `seedDatabase gives every organisation landlord a registrant admin user and any extra users have their own prsdb user`() {
        newSeeder().seedDatabase()

        val organisationLandlords = organisationLandlordRepository.findAll()
        assertTrue(organisationLandlords.isNotEmpty(), "Expected at least one organisation landlord to be seeded")

        var sawOrganisationWithExtraUsers = false

        organisationLandlords.forEach { organisationLandlord ->
            val users = organisationalLandlordUserRepository.findByOrganisationalLandlord(organisationLandlord)
            assertTrue(users.isNotEmpty()) {
                "Expected at least one organisational landlord user for organisation landlord ${organisationLandlord.id}"
            }

            val registrant = users.firstOrNull()
            assertEquals(OrganisationalLandlordUserRole.ADMIN, registrant?.role) {
                "Expected the first (registrant) user for organisation landlord ${organisationLandlord.id} to be an admin"
            }
            assertEquals(organisationLandlord.registrantName, registrant?.name)
            assertEquals(organisationLandlord.registrantEmail, registrant?.email)

            val subjectIds = users.map { it.baseUser.id }
            assertEquals(subjectIds.size, subjectIds.distinct().size) {
                "Expected all organisational landlord users for organisation landlord ${organisationLandlord.id} " +
                    "to have distinct subject identifiers"
            }

            users.forEach { user -> assertTrue(prsdbUserRepository.existsById(user.baseUser.id)) }

            if (users.size > 1) sawOrganisationWithExtraUsers = true
        }

        assertTrue(sawOrganisationWithExtraUsers, "Expected at least one organisation landlord to have extra users beyond the registrant")
    }

    @Test
    fun `seedDatabase creates valid organisational landlord invitations for seeded organisations`() {
        newSeeder(batchSize = numOfLandlords).seedDatabase()

        val organisationLandlordIds =
            organisationLandlordRepository
                .findAll()
                .map { it.id }
                .toSet()

        val invitedOrganisationLandlordIds =
            jdbcTemplate.queryForList<Long>(
                "SELECT DISTINCT organisation_landlord_id FROM organisational_landlord_invitation",
            )
                .toSet()

        assertTrue(invitedOrganisationLandlordIds.isNotEmpty(), "Expected organisational landlord invitations to be seeded")
        assertTrue(invitedOrganisationLandlordIds.all { it in organisationLandlordIds }) {
            "Expected each organisational landlord invitation to reference a real organisation landlord"
        }
    }

    @Test
    fun `seedDatabase gives every property that requires freeholder records at least one freeholder`() {
        newSeeder().seedDatabase()

        val propertiesWithoutAFreeholder =
            jdbcTemplate.queryForList<Long>(
                "SELECT po.id FROM property_ownership po " +
                    "WHERE po.ownership_type IN (${
                        OwnershipType.entries.filter { it.requiresFreeholderRecords() }.joinToString(", ") { it.ordinal.toString() }
                    }) AND NOT EXISTS (SELECT 1 FROM freeholder f WHERE f.property_ownership_id = po.id)",
            )

        assertEquals(emptyList<Long>(), propertiesWithoutAFreeholder)

        val seededFreeholders = jdbcTemplate.queryForList<Long>("SELECT id FROM freeholder")
        assertTrue(seededFreeholders.isNotEmpty(), "Expected freeholders to be seeded")
    }

    @Test
    fun `seedDatabase seeds both named and placeholder freeholders`() {
        newSeeder().seedDatabase()

        val namedFreeholders = jdbcTemplate.queryForList<Long>("SELECT id FROM freeholder WHERE name IS NOT NULL")
        assertTrue(namedFreeholders.isNotEmpty(), "Expected named freeholders to be seeded")

        val placeholderFreeholders = jdbcTemplate.queryForList<Long>("SELECT id FROM freeholder WHERE name IS NULL")
        assertTrue(placeholderFreeholders.isNotEmpty(), "Expected placeholder freeholders to be seeded")
    }

    @Test
    fun `seedDatabase gives every named associated party exactly one contact method and placeholders none`() {
        newSeeder().seedDatabase()

        listOf("freeholder", "superior_landlord").forEach { table ->
            assertTrue(
                jdbcTemplate.queryForList<Long>("SELECT id FROM $table WHERE email IS NOT NULL").isNotEmpty(),
                "Expected some $table rows to be contactable by email",
            )
            assertTrue(
                jdbcTemplate.queryForList<Long>("SELECT id FROM $table WHERE address_id IS NOT NULL").isNotEmpty(),
                "Expected some $table rows to be contactable by post",
            )

            val rowsBreakingTheContactInvariant =
                jdbcTemplate.queryForList<Long>(
                    "SELECT id FROM $table WHERE " +
                        "(name IS NOT NULL AND NOT ((email IS NOT NULL) != (address_id IS NOT NULL))) " +
                        "OR (name IS NULL AND (email IS NOT NULL OR address_id IS NOT NULL))",
                )

            assertEquals(emptyList<Long>(), rowsBreakingTheContactInvariant, "Found $table rows breaking the contact invariant")
        }
    }

    @Test
    fun `seedDatabase only gives superior landlords to properties whose ownership type requires them`() {
        newSeeder().seedDatabase()

        val seededSuperiorLandlords = jdbcTemplate.queryForList<Long>("SELECT id FROM superior_landlord")
        assertTrue(seededSuperiorLandlords.isNotEmpty(), "Expected superior landlords to be seeded")

        val superiorLandlordsOnTheWrongProperties =
            jdbcTemplate.queryForList<Long>(
                "SELECT sl.id FROM superior_landlord sl " +
                    "JOIN property_ownership po ON po.id = sl.property_ownership_id " +
                    "WHERE po.ownership_type NOT IN (${
                        OwnershipType.entries.filter { it.requiresSuperiorLandlordInfo() }.joinToString(", ") { it.ordinal.toString() }
                    })",
            )

        assertEquals(emptyList<Long>(), superiorLandlordsOnTheWrongProperties)
    }

    @Test
    fun `seedDatabase gives some but not all properties a property manager`() {
        newSeeder().seedDatabase()

        val managedProperties = jdbcTemplate.queryForList<Long>("SELECT id FROM property_ownership WHERE property_manager_id IS NOT NULL")
        assertTrue(managedProperties.isNotEmpty(), "Expected some properties to have a property manager")

        val unmanagedProperties = jdbcTemplate.queryForList<Long>("SELECT id FROM property_ownership WHERE property_manager_id IS NULL")
        assertTrue(unmanagedProperties.isNotEmpty(), "Expected some properties to have no property manager")

        val seededPropertyManagers = jdbcTemplate.queryForList<Long>("SELECT id FROM property_manager")
        assertEquals(managedProperties.size, seededPropertyManagers.size)
    }

    // Guards against property_manager being dropped from updateIdSequences(). Its ids are inserted explicitly, which
    // does not advance the identity sequence, so without that the first manager created through the app would collide.
    @Test
    fun `seedDatabase advances the property manager id sequence past the seeded ids`() {
        newSeeder().seedDatabase()

        val maxSeededId =
            assertNotNull(
                jdbcTemplate.queryForList<Long>("SELECT MAX(id) FROM property_manager").single(),
                "Expected property managers to be seeded",
            )
        val nextSequenceValue =
            assertNotNull(
                jdbcTemplate.queryForList<Long>("SELECT nextval(pg_get_serial_sequence('property_manager', 'id'))").single(),
                "Expected the property manager id sequence to exist",
            )

        assertTrue(
            nextSequenceValue > maxSeededId,
            "Expected the property manager id sequence to be past $maxSeededId, but the next value was $nextSequenceValue",
        )
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
    fun `seedDatabase gives landlords the anniversary of their first property registration and their properties a matching renewal date`() {
        newSeeder().seedDatabase()

        assertEquals(
            0L,
            jdbcTemplate.queryForObject(
                """
                SELECT count(*)
                FROM landlord l
                LEFT JOIN (
                    SELECT ol.landlord_id, MIN(po.created_date) AT TIME ZONE 'Europe/London' AS first_registration_date
                    FROM ownership_link ol
                    JOIN property_ownership po ON po.id = ol.landlordship_id
                    GROUP BY ol.landlord_id
                ) fr ON fr.landlord_id = l.id
                WHERE (l.anniversary_month, l.anniversary_day) IS DISTINCT FROM
                      (EXTRACT(MONTH FROM fr.first_registration_date)::int, EXTRACT(DAY FROM fr.first_registration_date)::int)
                """.trimIndent(),
                Long::class.java,
            ),
        )
        assertEquals(
            0L,
            jdbcTemplate.queryForObject(
                """
                SELECT count(*)
                FROM property_ownership po
                JOIN ownership_link ol ON ol.landlordship_id = po.id
                JOIN landlord l ON l.id = ol.landlord_id
                WHERE (EXTRACT(MONTH FROM po.renewal_date), EXTRACT(DAY FROM po.renewal_date))
                          <> (l.anniversary_month, l.anniversary_day)
                  AND NOT (l.anniversary_month = 2 AND l.anniversary_day = 29)
                """.trimIndent(),
                Long::class.java,
            ),
        )
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

    @Test
    fun `seedDatabase produces the same data whatever the default time zone`() {
        val ukSnapshot = seedWithDefaultTimeZone("Europe/London")

        IntegrationTestHelper.resetDatabase(jdbcTemplate)

        val utcSnapshot = seedWithDefaultTimeZone("UTC")

        assertEquals(ukSnapshot, utcSnapshot)
    }

    // The snapshot is captured in the original time zone, as JDBC converts dates using the default time zone
    private fun seedWithDefaultTimeZone(timeZoneId: String): SeedSnapshot {
        val originalTimeZone = TimeZone.getDefault()
        try {
            TimeZone.setDefault(TimeZone.getTimeZone(timeZoneId))
            newSeeder().seedDatabase()
        } finally {
            TimeZone.setDefault(originalTimeZone)
        }
        return captureSeedSnapshot()
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
                           eicr_expiry_date, eic_issue_date, electrical_cert_type, epc_url, epc_expiry_date,
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
        val landlords: List<Map<String, Any?>>,
        val properties: List<Map<String, Any?>>,
        val incompleteProperties: List<Map<String, Any?>>,
        val compliance: List<Map<String, Any?>>,
        val uploads: List<Map<String, Any?>>,
    )
}
