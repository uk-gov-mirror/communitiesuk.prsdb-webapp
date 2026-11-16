package uk.gov.communities.prsdb.webapp.performance

import io.gatling.javaapi.core.CoreDsl.atOnceUsers
import io.gatling.javaapi.core.CoreDsl.scenario
import io.gatling.javaapi.core.Simulation
import io.gatling.javaapi.http.HttpDsl.http

class OffboardingContractSimulation : Simulation() {
    init {
        val properties = System.getProperties().stringPropertyNames().associateWith { System.getProperty(it) }
        val config = BasicRunConfig.from(properties)
        val fixture =
            OffboardingFixture(
                subject = "contract-landlord",
                propertyOwnershipId = 101,
                address = "1 Fixture Road, Testtown, AB1 2CD",
            )
        setUp(
            scenario("Property deregistration HTTP contract")
                .exec(OffboardingJourney.chain(config, fixture))
                .injectOpen(atOnceUsers(1)),
        ).protocols(http.baseUrl(config.baseUrl).acceptHeader("text/html").userAgentHeader("PRSDB-Gatling-Performance-Tests"))
            .assertions(BasicPerformanceAssertions.forRequests(config, OffboardingJourney.requestNames))
    }
}
