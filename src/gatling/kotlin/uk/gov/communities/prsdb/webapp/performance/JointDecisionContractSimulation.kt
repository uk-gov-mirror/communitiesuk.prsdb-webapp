package uk.gov.communities.prsdb.webapp.performance

import io.gatling.javaapi.core.CoreDsl.atOnceUsers
import io.gatling.javaapi.core.CoreDsl.scenario
import io.gatling.javaapi.core.Simulation
import io.gatling.javaapi.http.HttpDsl.http

class JointDecisionContractSimulation : Simulation() {
    init {
        val properties = System.getProperties().stringPropertyNames().associateWith { System.getProperty(it) }
        val config = BasicRunConfig.from(properties)
        val warmUp =
            JointLandlordFixture(
                "owner-warmup",
                101,
                "1 Fixture Road",
                201,
                "00000000-0000-0000-0000-000000000201",
                "invitee-warmup",
                "invitee-101@example.test",
            )
        val measured =
            JointLandlordFixture(
                "owner-measured",
                102,
                "2 Fixture Road",
                202,
                "00000000-0000-0000-0000-000000000202",
                "invitee-measured",
                "invitee-102@example.test",
            )
        setUp(
            scenario("Joint invitation decision HTTP contract")
                .exec(JointLandlordJourney.decisionChain(config, warmUp, "Warm-up: "))
                .exec(JointLandlordJourney.decisionChain(config, measured))
                .injectOpen(atOnceUsers(1)),
        ).protocols(http.baseUrl(config.baseUrl).acceptHeader("text/html").userAgentHeader("PRSDB-Gatling-Performance-Tests"))
            .assertions(
                BasicPerformanceAssertions.forRequests(config, JointLandlordJourney.decisionRequestNames),
            )
    }
}
