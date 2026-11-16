package uk.gov.communities.prsdb.webapp.performance

import io.gatling.javaapi.core.CoreDsl.atOnceUsers
import io.gatling.javaapi.core.CoreDsl.scenario
import io.gatling.javaapi.core.Simulation
import io.gatling.javaapi.http.HttpDsl.http
import java.nio.file.Files
import java.nio.file.Path

class OffboardingSimulation : Simulation() {
    init {
        val properties = System.getProperties().stringPropertyNames().associateWith { System.getProperty(it) }
        val config = BasicRunConfig.from(properties)
        val fixturePath =
            requireNotNull(properties["gatling.offboarding.fixturePath"]?.takeIf { it.isNotBlank() }) {
                "gatling.offboarding.fixturePath must identify the read-only property deregistration export"
            }
        val fixtures = OffboardingFixture.parse(Files.readString(Path.of(fixturePath)), config.iterations + 1)
        val scenarios =
            fixtures.mapIndexed { pass, fixture ->
                val prefix = if (pass == 0) "Warm-up: " else ""
                scenario("Property deregistration ${if (pass == 0) "warm-up" else "iteration $pass"}")
                    .exec(OneLoginAuthentication.loginChain(config, fixture.subject, LOGIN_PREFIX, prefix))
                    .exec(OffboardingJourney.chain(config, fixture, prefix))
                    .injectOpen(atOnceUsers(1))
            }.reduce { previous, next -> previous.andThen(next) }
        setUp(scenarios)
            .protocols(http.baseUrl(config.baseUrl).acceptHeader("text/html").userAgentHeader("PRSDB-Gatling-Performance-Tests"))
            .assertions(
                BasicPerformanceAssertions.forRequests(
                    config,
                    OneLoginAuthentication.loginRequestNames(LOGIN_PREFIX) + OffboardingJourney.requestNames,
                ),
            )
    }

    private companion object {
        const val LOGIN_PREFIX = "Offboarding authentication"
    }
}
