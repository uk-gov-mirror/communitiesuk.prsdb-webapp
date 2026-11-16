package uk.gov.communities.prsdb.webapp.performance

import io.gatling.javaapi.core.CoreDsl.atOnceUsers
import io.gatling.javaapi.core.CoreDsl.exec
import io.gatling.javaapi.core.CoreDsl.scenario
import io.gatling.javaapi.core.Simulation
import io.gatling.javaapi.http.HttpDsl.http
import java.time.Duration

class ComplianceUpdateContractSimulation : Simulation() {
    init {
        val properties = System.getProperties().stringPropertyNames().associateWith { System.getProperty(it) }
        val config = BasicRunConfig.from(properties)
        val certificatePath = requireNotNull(properties["gatling.contract.certificatePath"])
        val fixture = RegistrationFixture("synthetic-landlord", "1 Fixture Road, FA1 1AA", "FA1 1AA", "1", 1, 901)
        val scanPollInterval =
            properties["gatling.contract.scanPollIntervalMillis"]?.toLongOrNull()?.let { intervalMillis ->
                require(intervalMillis > 0) { "gatling.contract.scanPollIntervalMillis must be positive" }
                Duration.ofMillis(intervalMillis)
            } ?: Duration.ofMillis(250)
        val scanWaitLimit =
            properties["gatling.contract.scanWaitLimitMillis"]?.toLongOrNull()?.let { waitLimitMillis ->
                require(waitLimitMillis > 0) { "gatling.contract.scanWaitLimitMillis must be positive" }
                Duration.ofMillis(waitLimitMillis)
            } ?: Duration.ofMinutes(5)
        val registeredChain =
            ComplianceUpdateKind.entries.fold(
                exec { session -> session.set("regPropertyUrl", "${config.baseUrl}/landlord/property-details/100?withBackUrl=17") },
            ) { chain, kind ->
                chain.exec(
                    ComplianceUpdateJourney.chain(
                        config,
                        fixture,
                        kind,
                        certificatePath,
                        expectSuccessBanner = properties["gatling.contract.expectSuccessBanner"].toBoolean(),
                        scanPollInterval = scanPollInterval,
                        scanWaitLimit = scanWaitLimit,
                    ),
                )
            }
                .exec(ComplianceUpdateJourney.verifyAll(config, fixture))
        val completedProperty = properties["gatling.contract.completedProperty"].toBoolean()
        val chain =
            if (completedProperty) {
                ComplianceUpdateJourney.completedPropertyChain(
                    config,
                    CompletedPropertyFixture("synthetic-landlord", 100, "1 Fixture Road"),
                    certificatePath,
                )
            } else {
                registeredChain
            }
        setUp(scenario("Compliance update HTTP contract").exec(chain).injectOpen(atOnceUsers(1)))
            .protocols(http.baseUrl(config.baseUrl))
            .assertions(
                BasicPerformanceAssertions.forRequests(
                    config,
                    if (completedProperty) {
                        ComplianceUpdateJourney.completedPropertyKinds.flatMap { ComplianceUpdateJourney.requestNames(it) }
                    } else {
                        ComplianceUpdateKind.entries.flatMap { ComplianceUpdateJourney.requestNames(it) } +
                            ComplianceUpdateJourney.finalRequest
                    },
                ),
            )
    }
}
