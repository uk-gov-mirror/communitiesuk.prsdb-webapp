package uk.gov.communities.prsdb.webapp.performance

import io.gatling.javaapi.core.CoreDsl.atOnceUsers
import io.gatling.javaapi.core.CoreDsl.details
import io.gatling.javaapi.core.CoreDsl.global
import io.gatling.javaapi.core.CoreDsl.nothingFor
import io.gatling.javaapi.core.CoreDsl.scenario
import io.gatling.javaapi.core.PopulationBuilder
import io.gatling.javaapi.core.ScenarioBuilder
import io.gatling.javaapi.core.Simulation
import io.gatling.javaapi.http.HttpDsl.flushCookieJar
import io.gatling.javaapi.http.HttpDsl.http
import java.nio.file.Files
import java.nio.file.Path
import java.time.Duration

// The landlord proxy is not a measurement of the forecast 26/minute paid property registrations.
class BaselineLoadSimulation : Simulation() {
    init {
        val properties = System.getProperties().stringPropertyNames().associateWith { System.getProperty(it) }
        val config = BasicRunConfig.from(properties).copy(thinkTime = Duration.ofSeconds(2))
        val registrationSubject =
            requireNotNull(properties["gatling.basic.registrationSubject"]?.takeIf { it.isNotBlank() }) {
                "gatling.basic.registrationSubject must be provided for the registration journey"
            }
        val schedules = BaselineLoadProfile.schedules(properties).mapValues { it.value.arrivalTimesMillis() }

        fun times(family: BaselineFamily) = schedules.getValue(family)
        val registrationTimes = times(BaselineFamily.LANDLORD)
        val updateTimes = times(BaselineFamily.SAVED)
        val propertyTimes = times(BaselineFamily.PROPERTY)
        val profileTimes = times(BaselineFamily.ORGANISATION_PROFILE)
        val complianceTimes = times(BaselineFamily.COMPLIANCE)
        val teamTimes = times(BaselineFamily.ORGANISATION_TEAM)
        val jointTimes = times(BaselineFamily.JOINT)
        val agentTimes = times(BaselineFamily.AGENT)
        val operatorTimes = times(BaselineFamily.OPERATOR)
        val offboardingTimes = times(BaselineFamily.OFFBOARDING)
        val offset = properties["gatling.ramp.fixtureOffset"]?.toInt() ?: 22
        require(offset >= 22) { "Baseline ramp fixtures must skip the correctness-stage allocations" }
        val operatorOffset = properties["gatling.ramp.operatorFixtureOffset"]?.toInt() ?: config.iterations + 1
        require(operatorOffset >= config.iterations + 1) { "Operator ramp fixtures must skip correctness-stage targets" }
        require(properties["gatling.council.emailDeliveryStub"] == "true") {
            "Operator invitations require gatling.council.emailDeliveryStub=true"
        }
        val password = requireNotNull(properties["gatling.agent.password"]?.takeIf { it.isNotBlank() })
        val runId =
            requireNotNull(properties["gatling.organisation.runId"]?.takeIf { it.matches(Regex("[a-z0-9-]{1,40}")) })
        val certificatePath =
            requireNotNull(properties["gatling.registration.certificatePath"]?.takeIf { Files.isRegularFile(Path.of(it)) }) {
                "A synthetic PDF certificate is required for compliance uploads"
            }

        fun fixtureJson(key: String) =
            Files.readString(
                Path.of(requireNotNull(properties[key]?.takeIf { it.isNotBlank() }) { "$key must identify a read-only fixture export" }),
            )

        val savedFixtures =
            SavedRegistrationFixture.parse(fixtureJson("gatling.saved.fixturePath"), offset + updateTimes.size)
                .drop(offset).take(updateTimes.size)
        val propertyFixtures =
            CompletedPropertyFixture.parse(fixtureJson("gatling.property.fixturePath"), offset + propertyTimes.size + complianceTimes.size)
                .drop(offset)
        val organisationFixtures =
            OrganisationFixture.parse(fixtureJson("gatling.organisation.fixturePath"), offset + profileTimes.size)
                .drop(offset).take(profileTimes.size)
        val teamFixtures = OrganisationFixture.parse(fixtureJson("gatling.organisation.operationsFixturePath"), teamTimes.size)
        require(teamFixtures.all { it.invitationToken != null }) { "Organisation team invitations must be prepared via HTTP" }
        val jointFixtures = JointLandlordFixture.parseInvitations(fixtureJson("gatling.joint.fixturePath"), jointTimes.size)
        val agentFixtures = AgentFixture.parseAccessFixtures(fixtureJson("gatling.agent.accessFixturePath"), agentTimes.size, false)
        val operatorFixtures =
            OperatorFixture.parse(fixtureJson("gatling.operator.fixturePath"), operatorOffset + operatorTimes.size).drop(operatorOffset)
        val offboardingFixtures = OffboardingFixture.parse(fixtureJson("gatling.offboarding.fixturePath"), offboardingTimes.size)
        val reservations = WorkloadReservations()
        reservations.reserve(subjects = savedFixtures.map { it.subject })
        reservations.reserve(subjects = propertyFixtures.map { it.subject }, properties = propertyFixtures.map { it.propertyOwnershipId })
        reservations.reserve(
            subjects = organisationFixtures.map { it.subject },
            organisations = organisationFixtures.map { it.organisationId },
        )
        reservations.reserve(
            subjects =
                teamFixtures.flatMap {
                    listOf(
                        it.subject,
                        OrganisationOperationsJourney.acceptSubject(runId, it),
                        "$registrationSubject-organisation-${it.organisationId}",
                    )
                },
            organisations = teamFixtures.map { it.organisationId },
        )
        reservations.reserve(
            subjects = jointFixtures.flatMap { listOf(it.ownerSubject, requireNotNull(it.inviteeSubject)) },
            properties = jointFixtures.map { it.propertyOwnershipId },
        )
        reservations.reserve(
            subjects = agentFixtures.map { it.ownerSubject },
            properties = agentFixtures.map { it.propertyOwnershipId },
        )
        reservations.reserve(subjects = operatorFixtures.flatMap { listOf(it.subject, it.targetSubject) })
        reservations.reserve(
            subjects = offboardingFixtures.map { it.subject },
            properties = offboardingFixtures.map { it.propertyOwnershipId },
        )
        val registrationsData = registrationTimes.mapIndexed { index, _ -> BasicIterationData.forPass(registrationSubject, index + 1) }
        reservations.reserve(subjects = registrationsData.map { it.registrationSubject })

        val registrations =
            registrationsData.zip(registrationTimes).mapIndexed { index, (data, at) ->
                scenario("Baseline landlord registration ${index + 1}")
                    .exec(OneLoginAuthentication.loginChain(config, data.registrationSubject, "Registration authentication", ""))
                    .exec(IndividualLandlordRegistrationJourney.chain(config, data))
                    .arrive(at)
            }
        val saved =
            savedFixtures.zip(updateTimes) { fixture, at ->
                scenario("Baseline saved registration ${fixture.subject}")
                    .exec(OneLoginAuthentication.loginChain(config, fixture.subject, "Saved registration authentication", ""))
                    .exec(SavedRegistrationJourney.chain(config, fixture, ""))
                    .arrive(at)
            }
        val completed =
            propertyFixtures.take(propertyTimes.size).zip(propertyTimes) { fixture, at ->
                scenario("Baseline completed property ${fixture.subject}")
                    .exec(OneLoginAuthentication.loginChain(config, fixture.subject, "Completed property authentication", ""))
                    .exec(PropertyBranchJourney.chain(config, fixture, ""))
                    .exec(CompletedPropertyUpdateJourney.chain(config, fixture, ""))
                    .arrive(at)
            }
        val organisations =
            organisationFixtures.zip(profileTimes) { fixture, at ->
                scenario("Baseline organisation profile ${fixture.subject}")
                    .exec(OneLoginAuthentication.loginChain(config, fixture.subject, "Organisation authentication", ""))
                    .exec(OrganisationProfileJourney.chain(config, fixture, ""))
                    .arrive(at)
            }
        val compliance =
            propertyFixtures.drop(propertyTimes.size).zip(complianceTimes) { fixture, at ->
                scenario("Baseline compliance ${fixture.subject}")
                    .exec(OneLoginAuthentication.loginChain(config, fixture.subject, "Compliance authentication", ""))
                    .exec(ComplianceUpdateJourney.completedPropertyChain(config, fixture, certificatePath, COMPLIANCE_PREFIX))
                    .arrive(at)
            }
        val teams =
            teamFixtures.zip(teamTimes) { fixture, at ->
                val subject = "$registrationSubject-organisation-${fixture.organisationId}"
                scenario("Baseline organisation team ${fixture.subject}")
                    .exec(OneLoginAuthentication.loginChain(config, subject, "Organisation registration authentication", ""))
                    .exec(OrganisationRegistrationJourney.journey(config, subject, "company", TEAM_PREFIX))
                    .exec(flushCookieJar())
                    .exec(OneLoginAuthentication.loginChain(config, fixture.subject, "Organisation team authentication", ""))
                    .exec(OrganisationOperationsJourney.team(config, fixture, "$runId-load", TEAM_PREFIX))
                    .exec(OrganisationOperationsJourney.changeAccessLevel(config, TEAM_PREFIX))
                    .exec(flushCookieJar())
                    .exec(OrganisationOperationsJourney.accept(config, fixture, runId, TEAM_PREFIX))
                    .arrive(at)
            }
        val joints =
            jointFixtures.zip(jointTimes) { fixture, at ->
                scenario("Baseline joint invitation ${fixture.inviteeSubject}")
                    .exec(OneLoginAuthentication.loginChain(config, requireNotNull(fixture.inviteeSubject), "Joint authentication", ""))
                    .exec(JointLandlordJourney.decisionChain(config, fixture, JOINT_PREFIX))
                    .arrive(at)
            }
        val agents =
            agentFixtures.zip(agentTimes) { fixture, at ->
                scenario("Baseline agent ${fixture.ownerSubject}")
                    .exec(AgentJourney.accessAndUpdateChain(config, fixture, password, AGENT_PREFIX))
                    .arrive(at)
            }
        val operators =
            operatorFixtures.zip(operatorTimes) { fixture, at ->
                scenario("Baseline operator ${fixture.subject}")
                    .exec(OneLoginAuthentication.loginChain(config, fixture.subject, "Operator authentication", ""))
                    .exec(OperatorJourney.operations(config, fixture, OPERATOR_PREFIX))
                    .arrive(at)
            }
        val offboarding =
            offboardingFixtures.zip(offboardingTimes) { fixture, at ->
                scenario("Baseline offboarding ${fixture.subject}")
                    .exec(OneLoginAuthentication.loginChain(config, fixture.subject, "Offboarding authentication", ""))
                    .exec(OffboardingJourney.chain(config, fixture, OFFBOARDING_PREFIX))
                    .arrive(at)
            }
        val coverage =
            listOf(
                details("$TEAM_PREFIX${OrganisationRegistrationJourney.requestNames("company").last()}")
                    .successfulRequests().count().`is`(teamTimes.size.toLong()),
                details("$TEAM_PREFIX${OrganisationOperationsJourney.teamRequestNames.last()}")
                    .successfulRequests().count().`is`(teamTimes.size.toLong()),
                details("$TEAM_PREFIX${OrganisationOperationsJourney.accessLevelRequestNames.last()}")
                    .successfulRequests().count().`is`(teamTimes.size.toLong()),
                details("$TEAM_PREFIX${OrganisationOperationsJourney.acceptRequestNames.last()}")
                    .successfulRequests().count().`is`(teamTimes.size.toLong()),
                details("$JOINT_PREFIX${JointLandlordJourney.decisionRequestNames.last()}")
                    .successfulRequests().count().`is`(jointTimes.size.toLong()),
                details("$AGENT_PREFIX${AgentJourney.accessRequestNames(false).last()}")
                    .successfulRequests().count().`is`(agentTimes.size.toLong()),
                details("$OFFBOARDING_PREFIX${OffboardingJourney.requestNames.last()}")
                    .successfulRequests().count().`is`(offboardingTimes.size.toLong()),
            ) +
                OperatorJourney.operationRequestNames.map {
                    details("$OPERATOR_PREFIX$it").successfulRequests().count().`is`(operatorTimes.size.toLong())
                } +
                ComplianceUpdateJourney.completedPropertyKinds.map {
                    details("$COMPLIANCE_PREFIX${ComplianceUpdateJourney.requestNames(it).last()}")
                        .successfulRequests().count().`is`(complianceTimes.size.toLong())
                }
        setUp(registrations + saved + completed + organisations + compliance + teams + joints + agents + operators + offboarding)
            .protocols(http.baseUrl(config.baseUrl).acceptHeader("text/html").userAgentHeader("PRSDB-Gatling-Performance-Tests"))
            .assertions(
                listOf(
                    global().failedRequests().percent().`is`(0.0),
                    details(IndividualLandlordRegistrationJourney.DASHBOARD_REQUEST)
                        .successfulRequests().count().`is`(registrationTimes.size.toLong()),
                    details(SavedRegistrationJourney.requestNames.last()).successfulRequests().count().`is`(updateTimes.size.toLong()),
                    details(CompletedPropertyUpdateJourney.requestNames.last())
                        .successfulRequests().count().`is`(propertyTimes.size.toLong()),
                    details(OrganisationProfileJourney.requestNames.last()).successfulRequests().count().`is`(profileTimes.size.toLong()),
                ) + coverage,
            )
    }

    private fun ScenarioBuilder.arrive(millis: Long): PopulationBuilder = injectOpen(nothingFor(Duration.ofMillis(millis)), atOnceUsers(1))

    private companion object {
        const val TEAM_PREFIX = "Concurrent organisation team: "
        const val COMPLIANCE_PREFIX = "Concurrent compliance: "
        const val JOINT_PREFIX = "Concurrent joint: "
        const val AGENT_PREFIX = "Concurrent agent: "
        const val OPERATOR_PREFIX = "Concurrent operator: "
        const val OFFBOARDING_PREFIX = "Concurrent offboarding: "
    }
}
