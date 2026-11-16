package uk.gov.communities.prsdb.webapp.performance

import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BaselineWorkflowTests {
    @Test
    fun `workflow ramp supplies all mandatory basic configuration`() {
        val command =
            Files.readString(Path.of(".github/workflows/run-performance-tests.yml"))
                .substringAfter("--simulation ${BaselineLoadSimulation::class.java.name}")
                .substringBefore("\n      - name:")
        val arguments =
            command.lineSequence().map { it.trim().removeSuffix("\\").trim() }
                .filter { it.startsWith("-Dgatling.") }
                .associate {
                    val (key, value) = it.removePrefix("-D").split("=", limit = 2)
                    key to value.removeSurrounding("\"")
                }
        val config = BasicRunConfig.from(arguments)
        assertEquals(PerformanceTarget.NFT, config.target)
        assertTrue(config.landlordSubject.isNotBlank())
        val mandatory =
            listOf(
                "gatling.basic.registrationSubject",
                "gatling.saved.fixturePath",
                "gatling.property.fixturePath",
                "gatling.organisation.fixturePath",
                "gatling.organisation.operationsFixturePath",
                "gatling.organisation.runId",
                "gatling.joint.fixturePath",
                "gatling.agent.accessFixturePath",
                "gatling.agent.password",
                "gatling.operator.fixturePath",
                "gatling.offboarding.fixturePath",
                "gatling.registration.certificatePath",
                "gatling.certificate.downloadOrigin",
                "gatling.ramp.rampMinutes",
                "gatling.ramp.holdMinutes",
                "gatling.ramp.registrationsPerMinute",
                "gatling.ramp.updatesPerMinute",
                "gatling.ramp.additionalPerMinute",
                "gatling.ramp.fixtureOffset",
                "gatling.ramp.operatorFixtureOffset",
            )
        mandatory.forEach { key -> assertTrue(!arguments[key].isNullOrBlank(), "Missing required workflow argument $key") }
        assertEquals("true", arguments["gatling.council.emailDeliveryStub"])
        val schedules = BaselineLoadProfile.schedules(arguments).mapValues { it.value.arrivalTimesMillis().size }
        val updates = schedules.getValue(BaselineFamily.PROPERTY)
        val compliance = schedules.getValue(BaselineFamily.COMPLIANCE)
        val team = schedules.getValue(BaselineFamily.ORGANISATION_TEAM)
        val operators = schedules.getValue(BaselineFamily.OPERATOR)
        val offset = arguments.getValue("gatling.ramp.fixtureOffset").toInt()
        val operatorOffset = arguments.getValue("gatling.ramp.operatorFixtureOffset").toInt()
        assertTrue(offset >= 2 * (config.iterations + 1), "Ramp must skip property-update and certificate correctness allocations")
        assertTrue(offset + schedules.getValue(BaselineFamily.SAVED) <= 101, "Saved fixture pool must contain all ramp actors")
        assertTrue(
            offset + schedules.getValue(BaselineFamily.ORGANISATION_PROFILE) <= 101,
            "Organisation profile pool must contain all ramp actors",
        )
        assertTrue(offset + updates + compliance <= 121, "Completed property pool must include exclusive certificate actors")
        assertTrue(operatorOffset >= config.iterations + 1, "Operator ramp must skip correctness actors")
        assertTrue(operatorOffset + operators <= 28, "Operator correctness and ramp must fit the minimum exported pool")
        assertTrue(team <= 17, "Organisation team ramp must fit its exclusive exported pool")
        assertEquals(767, schedules.values.sum())
        assertTrue("gatling.registration.paymentBaseUrl" !in arguments, "Baseline must not require the unfinished payment ingress")
    }
}
