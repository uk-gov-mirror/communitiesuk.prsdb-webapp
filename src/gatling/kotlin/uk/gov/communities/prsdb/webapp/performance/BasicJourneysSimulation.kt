package uk.gov.communities.prsdb.webapp.performance

import io.gatling.javaapi.core.CoreDsl.atOnceUsers
import io.gatling.javaapi.core.CoreDsl.scenario
import io.gatling.javaapi.core.Simulation
import io.gatling.javaapi.http.HttpDsl.http

class BasicJourneysSimulation : Simulation() {
    init {
        val properties = System.getProperties().stringPropertyNames().associateWith { System.getProperty(it) }
        val config =
            BasicRunConfig.from(properties)
        val registrationSubject =
            requireNotNull(properties["gatling.basic.registrationSubject"]?.takeIf { it.isNotBlank() }) {
                "gatling.basic.registrationSubject must be provided for the registration journey"
            }
        require(registrationSubject != config.landlordSubject) {
            "The registration subject must be distinct from the seeded phone/email-update landlord"
        }
        val warmUpData = BasicIterationData.forPass(registrationSubject, null)
        val profileUpdates =
            (listOf(warmUpData) + (1..config.iterations).map { BasicIterationData.forPass(registrationSubject, it) })
                .mapIndexed { pass, iterationData ->
                    scenario("Seeded landlord profile updates (${passName(pass)})")
                        .exec(OneLoginAuthentication.chain(config, iterationData))
                        .exec(LandlordReadJourney.chain(config, iterationData))
                        .exec(LandlordIdentityUpdateJourney.chain(config, iterationData, dateOfBirth = false))
                        .exec(LandlordIdentityUpdateJourney.chain(config, iterationData, dateOfBirth = true))
                        .exec(LandlordPhoneUpdateJourney.chain(config, iterationData))
                        .exec(LandlordEmailUpdateJourney.chain(config, iterationData))
                        .exec(LandlordAddressUpdateJourney.chain(config, iterationData, manual = false))
                        .exec(LandlordAddressUpdateJourney.chain(config, iterationData, manual = true))
                        .injectOpen(atOnceUsers(1))
                }.reduce { previous, next -> previous.andThen(next) }

        val registrations =
            (listOf(warmUpData) + (1..config.iterations).map { BasicIterationData.forPass(registrationSubject, it) })
                .mapIndexed { pass, iterationData ->
                    scenario("Fresh individual landlord registration (${passName(pass)})")
                        .exec(
                            OneLoginAuthentication.loginChain(
                                config,
                                iterationData.registrationSubject,
                                REGISTRATION_LOGIN_PREFIX,
                                iterationData.requestNamePrefix,
                            ),
                        ).exec(IndividualLandlordRegistrationJourney.chain(config, iterationData))
                        .injectOpen(atOnceUsers(1))
                }.reduce { previous, next -> previous.andThen(next) }

        setUp(
            profileUpdates.andThen(registrations),
        ).protocols(
            http
                .baseUrl(config.baseUrl)
                .acceptHeader("text/html")
                // The CloudFront WAF blocks requests without a User-Agent header
                .userAgentHeader("PRSDB-Gatling-Performance-Tests"),
        ).assertions(
            BasicPerformanceAssertions.forRequests(
                config,
                OneLoginAuthentication.applicationRequestNames +
                    LandlordReadJourney.requestNames +
                    LandlordIdentityUpdateJourney.requestNames(false) +
                    LandlordIdentityUpdateJourney.requestNames(true) +
                    LandlordPhoneUpdateJourney.requestNames +
                    LandlordEmailUpdateJourney.requestNames +
                    LandlordAddressUpdateJourney.requestNames(false) +
                    LandlordAddressUpdateJourney.requestNames(true) +
                    OneLoginAuthentication.loginRequestNames(REGISTRATION_LOGIN_PREFIX) +
                    IndividualLandlordRegistrationJourney.requestNames,
            ),
        )
    }

    companion object {
        private const val REGISTRATION_LOGIN_PREFIX = "Registration authentication"

        // Gatling requires unique scenario names; pass 0 is the unmeasured warm-up.
        private fun passName(pass: Int) = if (pass == 0) "warm-up" else "iteration $pass"
    }
}
