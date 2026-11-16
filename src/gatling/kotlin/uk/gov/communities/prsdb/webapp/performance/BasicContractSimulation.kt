package uk.gov.communities.prsdb.webapp.performance

import io.gatling.javaapi.core.ChainBuilder
import io.gatling.javaapi.core.CoreDsl.atOnceUsers
import io.gatling.javaapi.core.CoreDsl.bodyString
import io.gatling.javaapi.core.CoreDsl.css
import io.gatling.javaapi.core.CoreDsl.exec
import io.gatling.javaapi.core.CoreDsl.repeat
import io.gatling.javaapi.core.CoreDsl.scenario
import io.gatling.javaapi.core.Simulation
import io.gatling.javaapi.http.HttpDsl.header
import io.gatling.javaapi.http.HttpDsl.http
import io.gatling.javaapi.http.HttpDsl.status

class BasicContractSimulation : Simulation() {
    init {
        val config =
            BasicRunConfig.from(
                System.getProperties().stringPropertyNames().associateWith { System.getProperty(it) },
            )
        if (System.getProperty("gatling.contract.journey") == "address") {
            val warmUp = BasicIterationData.forPass("contract", null)
            val measured = BasicIterationData.forPass("contract", 1)
            setUp(
                scenario("Address update HTTP contract")
                    .exec(LandlordAddressUpdateJourney.chain(config, warmUp, manual = false))
                    .exec(LandlordAddressUpdateJourney.chain(config, warmUp, manual = true))
                    .exec(LandlordAddressUpdateJourney.chain(config, measured, manual = false))
                    .exec(LandlordAddressUpdateJourney.chain(config, measured, manual = true))
                    .injectOpen(atOnceUsers(1)),
            ).protocols(http.baseUrl(config.baseUrl))
                .assertions(
                    BasicPerformanceAssertions.forRequests(
                        config,
                        LandlordAddressUpdateJourney.requestNames(false) + LandlordAddressUpdateJourney.requestNames(true),
                    ),
                )
        } else if (System.getProperty("gatling.contract.journey") == "identity") {
            val passes = listOf(BasicIterationData.forPass("contract", null), BasicIterationData.forPass("contract", 1))
            setUp(
                scenario("Identity update HTTP contract")
                    .exec(
                        passes.map { data ->
                            LandlordIdentityUpdateJourney.chain(config, data, dateOfBirth = false)
                                .exec(LandlordIdentityUpdateJourney.chain(config, data, dateOfBirth = true))
                        },
                    ).injectOpen(atOnceUsers(1)),
            ).protocols(http.baseUrl(config.baseUrl))
                .assertions(
                    BasicPerformanceAssertions.forRequests(
                        config,
                        LandlordIdentityUpdateJourney.requestNames(false) + LandlordIdentityUpdateJourney.requestNames(true),
                    ),
                )
        } else if (System.getProperty("gatling.contract.journey") == "landlord-read") {
            val passes = listOf(BasicIterationData.forPass("contract", null), BasicIterationData.forPass("contract", 1))
            setUp(
                scenario("Landlord read HTTP contract")
                    .exec(passes.map { LandlordReadJourney.chain(config, it) })
                    .injectOpen(atOnceUsers(1)),
            ).protocols(http.baseUrl(config.baseUrl))
                .assertions(BasicPerformanceAssertions.forRequests(config, LandlordReadJourney.requestNames))
        } else if (System.getProperty("gatling.contract.journey") == "saved-registration") {
            setUp(
                scenario("Saved registration HTTP contract")
                    .exec(
                        SavedRegistrationJourney.chain(
                            config,
                            SavedRegistrationFixture("owner-a", "resume-warmup", "delete-warmup", "1 Fixture Road, EG1 2AA"),
                            "Warm-up: ",
                        ),
                    ).exec(
                        SavedRegistrationJourney.chain(
                            config,
                            SavedRegistrationFixture("owner-b", "resume-measured", "delete-measured", "1 Fixture Road, EG1 2AA"),
                            "",
                        ),
                    ).injectOpen(atOnceUsers(1)),
            ).protocols(http.baseUrl(config.baseUrl))
                .assertions(BasicPerformanceAssertions.forRequests(config, SavedRegistrationJourney.requestNames))
        } else if (System.getProperty("gatling.contract.journey") == "completed-property") {
            setUp(
                scenario("Completed property HTTP contract")
                    .exec(
                        CompletedPropertyUpdateJourney.chain(
                            config,
                            CompletedPropertyFixture("owner-a", 11, "1 Fixture Road"),
                            "Warm-up: ",
                        ),
                    )
                    .exec(CompletedPropertyUpdateJourney.chain(config, CompletedPropertyFixture("owner-b", 12, "1 Fixture Road"), ""))
                    .injectOpen(atOnceUsers(1)),
            ).protocols(http.baseUrl(config.baseUrl))
                .assertions(BasicPerformanceAssertions.forRequests(config, CompletedPropertyUpdateJourney.requestNames))
        } else if (System.getProperty("gatling.contract.journey") == "organisation") {
            setUp(
                scenario("Organisation profile HTTP contract")
                    .exec(
                        OrganisationProfileJourney.chain(
                            config,
                            OrganisationFixture("admin-a", 100, 200, "Fixture organisation"),
                            "Warm-up: ",
                        ),
                    )
                    .exec(
                        OrganisationProfileJourney.chain(config, OrganisationFixture("admin-b", 101, 200, "Performance Organisation"), ""),
                    )
                    .injectOpen(atOnceUsers(1)),
            ).protocols(http.baseUrl(config.baseUrl))
                .assertions(BasicPerformanceAssertions.forRequests(config, OrganisationProfileJourney.requestNames))
        } else {
            setUp(
                scenario("HTTP contract")
                    .exec(contractChain("Warm-up: "))
                    .repeat(config.iterations)
                    .on(contractChain(""))
                    .injectOpen(atOnceUsers(1)),
            ).protocols(http.baseUrl(config.baseUrl))
                .assertions(BasicPerformanceAssertions.forRequests(config, listOf("Get form", "Submit form")))
        }
    }

    private fun contractChain(requestNamePrefix: String): ChainBuilder =
        exec(
            http("${requestNamePrefix}Get form")
                .get("/form")
                .check(status().`is`(200))
                .check(css("input[name='_csrf']", "value").saveAs("csrfToken"))
                .check(css("form", "action").saveAs("formAction")),
        ).exitHereIfFailed()
            .doIf { !java.lang.Boolean.getBoolean("gatling.contract.skipSubmit") }.then(
                http("${requestNamePrefix}Submit form")
                    .post("#{formAction}")
                    .formParam("_csrf", "#{csrfToken}")
                    .formParam("value", "value + & spaces")
                    .check(status().`is`(200))
                    .check(header("Content-Type").`is`("text/plain"))
                    .check(bodyString().`is`("Submission received (no-op)")),
            )
}
