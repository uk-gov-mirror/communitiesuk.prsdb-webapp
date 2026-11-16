package uk.gov.communities.prsdb.webapp.performance

import io.gatling.javaapi.core.CoreDsl.atOnceUsers
import io.gatling.javaapi.core.CoreDsl.scenario
import io.gatling.javaapi.core.Simulation
import io.gatling.javaapi.http.HttpDsl.http
import java.nio.file.Files
import java.nio.file.Path

class AgentAccessSimulation : Simulation() {
    init {
        val properties = System.getProperties().stringPropertyNames().associateWith { System.getProperty(it) }
        val config = BasicRunConfig.from(properties)
        val fixturePath =
            requireNotNull(properties["gatling.agent.accessFixturePath"]?.takeIf { it.isNotBlank() }) {
                "gatling.agent.accessFixturePath must identify the read-only export from data-nft-agent-access-fixtures.sql"
            }
        val password =
            requireNotNull(properties["gatling.agent.password"]?.takeIf { it.isNotBlank() }) {
                "gatling.agent.password must provide a one-time password for unclaimed agent invitations"
            }
        val fixtures =
            AgentFixture.parseAccessFixtures(
                Files.readString(Path.of(fixturePath)),
                config.iterations + 1,
                hasPassword = false,
            )
        val scenarios =
            fixtures.mapIndexed { pass, fixture ->
                val prefix = if (pass == 0) "Warm-up: " else ""
                scenario("Letting-agent access ${if (pass == 0) "warm-up" else "iteration $pass"}")
                    .exec(AgentJourney.accessAndUpdateChain(config, fixture, password, prefix))
                    .injectOpen(atOnceUsers(1))
            }.reduce { previous, next -> previous.andThen(next) }
        setUp(scenarios)
            .protocols(http.baseUrl(config.baseUrl).acceptHeader("text/html").userAgentHeader("PRSDB-Gatling-Performance-Tests"))
            .assertions(BasicPerformanceAssertions.forRequests(config, AgentJourney.accessRequestNames(false)))
    }
}
