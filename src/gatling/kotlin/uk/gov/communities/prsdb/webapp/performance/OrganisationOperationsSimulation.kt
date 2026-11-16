package uk.gov.communities.prsdb.webapp.performance

import io.gatling.javaapi.core.CoreDsl.atOnceUsers
import io.gatling.javaapi.core.CoreDsl.scenario
import io.gatling.javaapi.core.Simulation
import io.gatling.javaapi.http.HttpDsl.http
import java.nio.file.Files
import java.nio.file.Path

class OrganisationOperationsSimulation : Simulation() {
    init {
        val properties = System.getProperties().stringPropertyNames().associateWith { System.getProperty(it) }
        val config = BasicRunConfig.from(properties)
        val operation = properties["gatling.organisation.operation"]
        require(operation in OPERATIONS) { "gatling.organisation.operation must be one of ${OPERATIONS.joinToString()}" }
        val path =
            requireNotNull(properties["gatling.organisation.operationsFixturePath"]?.takeIf { it.isNotBlank() }) {
                "gatling.organisation.operationsFixturePath must identify the read-only export from " +
                    "data-nft-organisation-fixtures.sql, or data-nft-organisation-team-fixtures.sql for acceptance"
            }
        val offset = properties["gatling.organisation.fixtureOffset"]?.toInt() ?: 0
        require(offset >= 0) { "gatling.organisation.fixtureOffset must not be negative" }
        val runId =
            properties["gatling.organisation.runId"]?.takeIf { it.matches(Regex("[a-z0-9-]{1,40}")) }
                ?: if (operation == "team" || operation == "accept") {
                    error("gatling.organisation.runId must be 1-40 lowercase letters, digits or hyphens")
                } else {
                    ""
                }
        val fixtures =
            OrganisationFixture.parse(Files.readString(Path.of(path)), offset + config.iterations + 1).drop(offset)
        val scenarios =
            fixtures.mapIndexed { pass, fixture ->
                val prefix = if (pass == 0) "Warm-up: " else ""
                val name = "Organisation $operation ${if (pass == 0) "warm-up" else "iteration $pass"}"
                // The invitee is a fresh One Login user who signs in inside the accept journey, not the seeded administrator.
                if (operation == "accept") {
                    scenario(name).exec(OrganisationOperationsJourney.accept(config, fixture, runId, prefix)).injectOpen(atOnceUsers(1))
                } else {
                    scenario(name)
                        .exec(OneLoginAuthentication.loginChain(config, fixture.subject, LOGIN_PREFIX, prefix))
                        .exec(
                            when (operation) {
                                "team" -> OrganisationOperationsJourney.team(config, fixture, runId, prefix)
                                "change-access-level" -> OrganisationOperationsJourney.changeAccessLevel(config, prefix)
                                else -> error("Unsupported organisation operation")
                            },
                        ).injectOpen(atOnceUsers(1))
                }
            }.reduce { previous, next -> previous.andThen(next) }
        setUp(scenarios)
            .protocols(http.baseUrl(config.baseUrl).acceptHeader("text/html").userAgentHeader("PRSDB-Gatling-Performance-Tests"))
            .assertions(
                BasicPerformanceAssertions.forRequests(
                    config,
                    when (operation) {
                        "accept" -> OrganisationOperationsJourney.acceptRequestNames
                        else ->
                            OneLoginAuthentication.loginRequestNames(LOGIN_PREFIX) +
                                when (operation) {
                                    "team" -> OrganisationOperationsJourney.teamRequestNames
                                    "change-access-level" -> OrganisationOperationsJourney.accessLevelRequestNames
                                    else -> error("Unsupported organisation operation")
                                }
                    },
                ),
            )
    }

    private companion object {
        const val LOGIN_PREFIX = "Organisation authentication"
        val OPERATIONS = listOf("team", "accept", "change-access-level")
    }
}
