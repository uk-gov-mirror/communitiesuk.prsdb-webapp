package uk.gov.communities.prsdb.webapp.performance

import io.gatling.javaapi.core.CoreDsl.atOnceUsers
import io.gatling.javaapi.core.CoreDsl.scenario
import io.gatling.javaapi.core.Simulation
import io.gatling.javaapi.http.HttpDsl.http

class OrganisationContractSimulation : Simulation() {
    init {
        val properties = System.getProperties().stringPropertyNames().associateWith { System.getProperty(it) }
        val config = BasicRunConfig.from(properties)
        val operation = properties["gatling.contract.organisation"] ?: "team"
        val fixtures =
            listOf(
                OrganisationFixture("admin-a", 100, 200, "Fixture organisation", "invitation-100"),
                OrganisationFixture("admin-b", 101, 200, "Fixture organisation", "invitation-101"),
            )
        val chains =
            fixtures.mapIndexed { pass, fixture ->
                val prefix = if (pass == 0) "Warm-up: " else ""
                when {
                    operation == "team" -> OrganisationOperationsJourney.team(config, fixture, "contract", prefix)
                    operation == "accept" -> OrganisationOperationsJourney.accept(config, fixture, "contract", prefix)
                    operation == "change-access-level" -> OrganisationOperationsJourney.changeAccessLevel(config, prefix)
                    else -> error("Unsupported organisation operation")
                }
            }
        val requestNames =
            when {
                operation == "team" -> OrganisationOperationsJourney.teamRequestNames
                operation == "accept" -> OrganisationOperationsJourney.acceptRequestNames
                operation == "change-access-level" -> OrganisationOperationsJourney.accessLevelRequestNames
                else -> error("Unsupported organisation operation")
            }
        setUp(
            scenario("Organisation $operation HTTP contract")
                .exec(chains)
                .injectOpen(atOnceUsers(1)),
        ).protocols(http.baseUrl(config.baseUrl))
            .assertions(BasicPerformanceAssertions.forRequests(config, requestNames))
    }
}
