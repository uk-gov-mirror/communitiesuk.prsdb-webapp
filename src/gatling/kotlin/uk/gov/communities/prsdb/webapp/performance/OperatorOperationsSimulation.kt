package uk.gov.communities.prsdb.webapp.performance

import io.gatling.javaapi.core.CoreDsl.atOnceUsers
import io.gatling.javaapi.core.CoreDsl.scenario
import io.gatling.javaapi.core.Simulation
import io.gatling.javaapi.http.HttpDsl.http
import java.nio.file.Files
import java.nio.file.Path

class OperatorOperationsSimulation : Simulation() {
    init {
        val properties = System.getProperties().stringPropertyNames().associateWith { System.getProperty(it) }
        val config = BasicRunConfig.from(properties)
        require(properties["gatling.council.emailDeliveryStub"] == "true") {
            "Operator invitations require gatling.council.emailDeliveryStub=true"
        }
        val fixturePath =
            requireNotNull(properties["gatling.operator.fixturePath"]) {
                "gatling.operator.fixturePath must identify a fresh export from data-nft-operator-fixtures.sql"
            }
        val fixtures = OperatorFixture.parse(Files.readString(Path.of(fixturePath)), config.iterations + 1)
        val populations =
            fixtures.mapIndexed { pass, fixture ->
                val prefix = if (pass == 0) "Warm-up: " else ""
                val chain =
                    OneLoginAuthentication.loginChain(config, fixture.subject, "Operator authentication", prefix)
                        .exec(OperatorJourney.operations(config, fixture, prefix))
                scenario("Operator operations ${if (pass == 0) "warm-up" else "iteration $pass"}")
                    .exec(chain).injectOpen(atOnceUsers(1))
            }.reduce { previous, next -> previous.andThen(next) }
        setUp(populations).protocols(
            http.baseUrl(config.baseUrl).acceptHeader("text/html").userAgentHeader("PRSDB-Gatling-Performance-Tests"),
        ).assertions(BasicPerformanceAssertions.forRequests(config, OperatorJourney.operationRequestNames))
    }
}
