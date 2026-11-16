package uk.gov.communities.prsdb.webapp.performance

import io.gatling.javaapi.core.CoreDsl.atOnceUsers
import io.gatling.javaapi.core.CoreDsl.scenario
import io.gatling.javaapi.core.Simulation
import io.gatling.javaapi.http.HttpDsl.http
import java.nio.file.Files
import java.nio.file.Path

class CompletedPropertySimulation : Simulation() {
    init {
        val properties = System.getProperties().stringPropertyNames().associateWith { System.getProperty(it) }
        val config = BasicRunConfig.from(properties)
        val path =
            requireNotNull(properties["gatling.property.fixturePath"]?.takeIf { it.isNotBlank() }) {
                "gatling.property.fixturePath must identify the read-only export from data-nft-completed-property-fixtures.sql"
            }
        val fixtures = CompletedPropertyFixture.parse(Files.readString(Path.of(path)), config.iterations + 1)
        val scenarios =
            fixtures.mapIndexed { pass, fixture ->
                val prefix = if (pass == 0) "Warm-up: " else ""
                scenario("Owned completed property ${if (pass == 0) "warm-up" else "iteration $pass"}")
                    .exec(OneLoginAuthentication.loginChain(config, fixture.subject, LOGIN_PREFIX, prefix))
                    .exec(PropertyBranchJourney.chain(config, fixture, prefix))
                    .exec(CompletedPropertyUpdateJourney.chain(config, fixture, prefix))
                    .injectOpen(atOnceUsers(1))
            }.reduce { previous, next -> previous.andThen(next) }
        setUp(scenarios)
            .protocols(http.baseUrl(config.baseUrl).acceptHeader("text/html").userAgentHeader("PRSDB-Gatling-Performance-Tests"))
            .assertions(
                BasicPerformanceAssertions.forRequests(
                    config,
                    OneLoginAuthentication.loginRequestNames(LOGIN_PREFIX) + PropertyBranchJourney.requestNames +
                        CompletedPropertyUpdateJourney.requestNames,
                ),
            )
    }

    private companion object {
        const val LOGIN_PREFIX = "Completed property authentication"
    }
}
