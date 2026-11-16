package uk.gov.communities.prsdb.webapp.performance

import io.gatling.javaapi.core.CoreDsl.atOnceUsers
import io.gatling.javaapi.core.CoreDsl.scenario
import io.gatling.javaapi.core.Simulation
import io.gatling.javaapi.http.HttpDsl.http
import java.nio.file.Files
import java.nio.file.Path

class AgentSimulation : Simulation() {
    init {
        val properties = System.getProperties().stringPropertyNames().associateWith { System.getProperty(it) }
        val config = BasicRunConfig.from(properties)
        val fixturePath =
            requireNotNull(properties["gatling.agent.fixturePath"]?.takeIf { it.isNotBlank() }) {
                "gatling.agent.fixturePath must identify the read-only export from " +
                    "data-nft-agent-fixtures.sql"
            }
        require(properties["gatling.agent.mode"] in listOf(null, "prepare-delegation")) {
            "AgentSimulation only prepares pending delegations"
        }
        val families = AgentJourney.preparationFamilies
        val required = families.size * (config.iterations + 1)
        val fixtures = AgentFixture.parse(Files.readString(Path.of(fixturePath)), required)
        require(fixtures.all { it.token == null }) {
            "Agent owner delegation fixtures must not contain existing access tokens"
        }
        val scenarios =
            fixtures.mapIndexed { index, fixture ->
                val family = families[index % families.size]
                val pass = index / families.size
                val familyPrefix = AgentJourney.preparationPrefix(family)
                val warmUpPrefix = if (pass == 0) "Warm-up: " else ""
                val prefix = warmUpPrefix + familyPrefix
                val preparedEmail = AgentJourney.preparationEmail(family, fixture.propertyOwnershipId)
                scenario("Letting agent $familyPrefix${if (pass == 0) "warm-up" else "iteration $pass"} ${fixture.propertyOwnershipId}")
                    .exec(OneLoginAuthentication.loginChain(config, fixture.ownerSubject, loginPrefix(family), warmUpPrefix))
                    .exec(AgentJourney.ownerChain(config, fixture, prefix, preparedEmail))
                    .injectOpen(atOnceUsers(1))
            }.reduce { previous, next -> previous.andThen(next) }
        setUp(scenarios)
            .protocols(http.baseUrl(config.baseUrl).acceptHeader("text/html").userAgentHeader("PRSDB-Gatling-Performance-Tests"))
            .assertions(
                BasicPerformanceAssertions.forRequests(
                    config,
                    families.flatMap { family ->
                        OneLoginAuthentication.loginRequestNames(loginPrefix(family)) +
                            AgentJourney.preparationRequestNames.map { AgentJourney.preparationPrefix(family) + it }
                    },
                ),
            )
    }

    companion object {
        private const val LOGIN_PREFIX = "Letting agent owner authentication"

        // The prefix becomes a Gatling session key in OneLoginAuthentication, so it must not contain EL-reserved characters.
        internal fun loginPrefix(family: String?) = family?.let { "$LOGIN_PREFIX $it preparation" } ?: LOGIN_PREFIX
    }
}
