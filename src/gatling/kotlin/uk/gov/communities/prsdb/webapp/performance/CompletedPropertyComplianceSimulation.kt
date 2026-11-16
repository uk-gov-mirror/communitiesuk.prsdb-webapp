package uk.gov.communities.prsdb.webapp.performance

import io.gatling.javaapi.core.CoreDsl.atOnceUsers
import io.gatling.javaapi.core.CoreDsl.scenario
import io.gatling.javaapi.core.Simulation
import io.gatling.javaapi.http.HttpDsl.http
import java.nio.file.Files
import java.nio.file.Path

// Uploads gas and electrical certificates on existing seeded properties, so it exercises real object storage and scanning.
class CompletedPropertyComplianceSimulation : Simulation() {
    init {
        val properties = System.getProperties().stringPropertyNames().associateWith { System.getProperty(it) }
        val config = BasicRunConfig.from(properties)
        val path =
            requireNotNull(properties["gatling.property.fixturePath"]?.takeIf { it.isNotBlank() }) {
                "gatling.property.fixturePath must identify the read-only export from data-nft-completed-property-fixtures.sql"
            }
        val certificatePath =
            requireNotNull(properties["gatling.registration.certificatePath"]?.takeIf { Files.isRegularFile(Path.of(it)) }) {
                "A synthetic PDF certificate is required for compliance update uploads"
            }
        val kinds = ComplianceUpdateJourney.completedPropertyKinds
        // Uses the fixtures after the ones CompletedPropertySimulation consumes, so no property is updated twice.
        val offset = properties["gatling.property.fixtureOffset"]?.toInt() ?: 0
        require(offset >= 0)
        val fixtures = CompletedPropertyFixture.parse(Files.readString(Path.of(path)), offset + config.iterations + 1).drop(offset)
        val scenarios =
            fixtures.mapIndexed { pass, fixture ->
                val prefix = if (pass == 0) "Warm-up: " else ""
                scenario("Completed property compliance ${if (pass == 0) "warm-up" else "iteration $pass"}")
                    .exec(OneLoginAuthentication.loginChain(config, fixture.subject, LOGIN_PREFIX, prefix))
                    .exec(ComplianceUpdateJourney.completedPropertyChain(config, fixture, certificatePath, prefix))
                    .injectOpen(atOnceUsers(1))
            }.reduce { previous, next -> previous.andThen(next) }
        setUp(scenarios)
            .protocols(http.baseUrl(config.baseUrl).acceptHeader("text/html").userAgentHeader("PRSDB-Gatling-Performance-Tests"))
            .assertions(
                BasicPerformanceAssertions.forRequests(
                    config,
                    OneLoginAuthentication.loginRequestNames(LOGIN_PREFIX) + kinds.flatMap { ComplianceUpdateJourney.requestNames(it) },
                ),
            )
    }

    private companion object {
        const val LOGIN_PREFIX = "Completed property compliance authentication"
    }
}
