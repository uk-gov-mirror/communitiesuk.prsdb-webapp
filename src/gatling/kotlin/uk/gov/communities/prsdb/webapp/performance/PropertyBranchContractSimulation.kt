package uk.gov.communities.prsdb.webapp.performance

import io.gatling.javaapi.core.CoreDsl.atOnceUsers
import io.gatling.javaapi.core.CoreDsl.scenario
import io.gatling.javaapi.core.Simulation
import io.gatling.javaapi.http.HttpDsl.http

class PropertyBranchContractSimulation : Simulation() {
    init {
        val config = BasicRunConfig.from(System.getProperties().stringPropertyNames().associateWith { System.getProperty(it) })
        setUp(
            scenario("Additional property contracts")
                .exec(PropertyBranchJourney.chain(config, CompletedPropertyFixture("a", 11, "1 Fixture Road"), "Warm-up: "))
                .exec(PropertyBranchJourney.chain(config, CompletedPropertyFixture("b", 12, "1 Fixture Road"), ""))
                .injectOpen(atOnceUsers(1)),
        )
            .protocols(http.baseUrl(config.baseUrl))
            .assertions(BasicPerformanceAssertions.forRequests(config, PropertyBranchJourney.requestNames))
    }
}
