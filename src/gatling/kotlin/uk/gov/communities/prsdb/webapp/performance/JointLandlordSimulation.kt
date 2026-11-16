package uk.gov.communities.prsdb.webapp.performance

import io.gatling.javaapi.core.CoreDsl.atOnceUsers
import io.gatling.javaapi.core.CoreDsl.scenario
import io.gatling.javaapi.core.Simulation
import io.gatling.javaapi.http.HttpDsl.http
import java.nio.file.Files
import java.nio.file.Path

class JointLandlordSimulation : Simulation() {
    init {
        val properties = System.getProperties().stringPropertyNames().associateWith { System.getProperty(it) }
        val config = BasicRunConfig.from(properties)
        val fixturePath =
            requireNotNull(properties["gatling.joint.fixturePath"]?.takeIf { it.isNotBlank() }) {
                "gatling.joint.fixturePath must identify the read-only export from data-nft-joint-invite-fixtures.sql"
            }
        val json = Files.readString(Path.of(fixturePath))
        val fixtures = JointLandlordFixture.parsePreparations(json, config.iterations + 1)
        val requestNames = JointLandlordJourney.preparationRequestNames
        val scenarios =
            fixtures.mapIndexed { pass, fixture ->
                val prefix = if (pass == 0) "Warm-up: " else ""
                scenario("Joint landlord ${if (pass == 0) "warm-up" else "iteration $pass"}")
                    .exec(OneLoginAuthentication.loginChain(config, fixture.ownerSubject, LOGIN_PREFIX, prefix))
                    .exec(JointLandlordJourney.chain(config, fixture, prefix))
                    .injectOpen(atOnceUsers(1))
            }.reduce { previous, next -> previous.andThen(next) }
        setUp(scenarios)
            .protocols(http.baseUrl(config.baseUrl).acceptHeader("text/html").userAgentHeader("PRSDB-Gatling-Performance-Tests"))
            .assertions(
                BasicPerformanceAssertions.forRequests(
                    config,
                    OneLoginAuthentication.loginRequestNames(LOGIN_PREFIX) + requestNames,
                ),
            )
    }

    private companion object {
        const val LOGIN_PREFIX = "Joint landlord authentication"
    }
}
