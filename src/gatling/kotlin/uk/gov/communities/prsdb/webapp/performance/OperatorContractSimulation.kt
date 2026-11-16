package uk.gov.communities.prsdb.webapp.performance

import io.gatling.javaapi.core.CoreDsl.atOnceUsers
import io.gatling.javaapi.core.CoreDsl.global
import io.gatling.javaapi.core.CoreDsl.scenario
import io.gatling.javaapi.core.Simulation
import io.gatling.javaapi.http.HttpDsl.http

class OperatorContractSimulation : Simulation() {
    init {
        val config =
            BasicRunConfig.from(
                System.getProperties().stringPropertyNames().associateWith { System.getProperty(it) },
            )
        val journey = OperatorJourney.dashboard(config)
        setUp(
            scenario("Operator HTTP contract").exec(journey).injectOpen(atOnceUsers(1)),
        ).protocols(http.baseUrl(config.baseUrl)).assertions(global().failedRequests().count().`is`(0L))
    }
}
