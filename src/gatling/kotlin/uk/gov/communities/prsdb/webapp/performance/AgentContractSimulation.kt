package uk.gov.communities.prsdb.webapp.performance

import io.gatling.javaapi.core.CoreDsl.atOnceUsers
import io.gatling.javaapi.core.CoreDsl.scenario
import io.gatling.javaapi.core.Simulation
import io.gatling.javaapi.http.HttpDsl.http

class AgentContractSimulation : Simulation() {
    init {
        val config =
            BasicRunConfig.from(
                System.getProperties().stringPropertyNames().associateWith { System.getProperty(it) },
            )
        val warmUp = AgentFixture("owner-warmup", 101, "1 Fixture Road", null, null, null)
        val measured = AgentFixture("owner-measured", 102, "2 Fixture Road", null, null, null)
        setUp(
            scenario("Letting-agent HTTP contract")
                .exec(AgentJourney.ownerChain(config, warmUp, "Warm-up: "))
                .exec(AgentJourney.ownerChain(config, measured))
                .injectOpen(atOnceUsers(1)),
        ).protocols(http.baseUrl(config.baseUrl).acceptHeader("text/html").userAgentHeader("PRSDB-Gatling-Performance-Tests"))
            .assertions(BasicPerformanceAssertions.forRequests(config, AgentJourney.ownerRequestNames))
    }
}
