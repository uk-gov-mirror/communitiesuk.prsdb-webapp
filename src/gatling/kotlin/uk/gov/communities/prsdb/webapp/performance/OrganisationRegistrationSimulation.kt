package uk.gov.communities.prsdb.webapp.performance

import io.gatling.javaapi.core.CoreDsl.atOnceUsers
import io.gatling.javaapi.core.CoreDsl.scenario
import io.gatling.javaapi.core.Simulation
import io.gatling.javaapi.http.HttpDsl.http

class OrganisationRegistrationSimulation : Simulation() {
    init {
        val properties = System.getProperties().stringPropertyNames().associateWith { System.getProperty(it) }
        val config = BasicRunConfig.from(properties)
        val variant = properties["gatling.organisation.registrationVariant"] ?: "company"
        require(variant in OrganisationRegistrationJourney.variants) {
            "gatling.organisation.registrationVariant must be one of ${OrganisationRegistrationJourney.variants}"
        }
        val subjectPrefix =
            requireNotNull(properties["gatling.organisation.registrationSubjectPrefix"]?.takeIf { it.isNotBlank() }) {
                "gatling.organisation.registrationSubjectPrefix must be a fresh One Login subject prefix"
            }
        require(subjectPrefix != config.landlordSubject) { "Organisation registration needs subjects distinct from the seeded landlord" }

        val registrations =
            (0..config.iterations)
                .map { pass ->
                    val subject = subject(subjectPrefix, variant, pass)
                    val prefix = if (pass == 0) "Warm-up: " else ""
                    scenario("Organisation registration $variant (${if (pass == 0) "warm-up" else "iteration $pass"})")
                        .exec(
                            OneLoginAuthentication.loginChain(
                                config,
                                subject,
                                OrganisationRegistrationJourney.AUTHENTICATION_PREFIX,
                                prefix,
                            ),
                        )
                        .exec(OrganisationRegistrationJourney.journey(config, subject, variant, prefix))
                        .injectOpen(atOnceUsers(1))
                }.reduce { previous, next -> previous.andThen(next) }

        setUp(registrations)
            .protocols(
                http
                    .baseUrl(config.baseUrl)
                    .acceptHeader("text/html")
                    .userAgentHeader("PRSDB-Gatling-Performance-Tests"),
            ).assertions(
                BasicPerformanceAssertions.forRequests(
                    config,
                    OneLoginAuthentication.loginRequestNames(OrganisationRegistrationJourney.AUTHENTICATION_PREFIX) +
                        OrganisationRegistrationJourney.requestNames(variant),
                ),
            )
    }

    companion object {
        fun subject(
            prefix: String,
            variant: String,
            pass: Int,
        ) = "$prefix-$variant-${if (pass == 0) "warm-up" else pass.toString()}"
    }
}
