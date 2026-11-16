package uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.NullSource
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.whenever
import uk.gov.communities.prsdb.webapp.constants.PROVIDE_THIS_LATER_BUTTON_ACTION_NAME
import uk.gov.communities.prsdb.webapp.journeys.UnrecoverableJourneyStateException
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.states.HouseholdsAndTenantsState
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.tasks.HouseholdsAndTenantsDependencies
import uk.gov.communities.prsdb.webapp.testHelpers.mockObjects.AlwaysTrueValidator

@ExtendWith(MockitoExtension::class)
class HouseholdStepConfigProvideLaterTests {
    private val routeSegment = HouseholdStep.ROUTE_SEGMENT

    @Mock
    lateinit var mockJourneyState: HouseholdsAndTenantsState

    @Mock
    lateinit var dependencies: HouseholdsAndTenantsDependencies

    private fun setupStepConfig(): HouseholdStepConfig {
        val stepConfig = HouseholdStepConfig()
        stepConfig.urlPath = routeSegment
        stepConfig.validator = AlwaysTrueValidator()
        return stepConfig
    }

    private fun setUpDependenciesWith(allowProvideLater: Boolean) {
        whenever(dependencies.allowProvideTenancyDetailsLaterRoute).thenReturn(allowProvideLater)
        whenever(mockJourneyState.dependencies).thenReturn(dependencies)
    }

    @Test
    fun `Mode returns null when form model is not present`() {
        val stepConfig = setupStepConfig()
        whenever(mockJourneyState.getStepData(routeSegment)).thenReturn(null)

        val result = stepConfig.mode(mockJourneyState)

        assertNull(result)
    }

    @Test
    fun `Mode returns COMPLETE when numberOfHouseholds present and action is not provideThisLater`() {
        val stepConfig = setupStepConfig()
        whenever(mockJourneyState.getStepData(routeSegment)).thenReturn(
            mapOf(
                "numberOfHouseholds" to "2",
                "action" to "continue",
            ),
        )

        val result = stepConfig.mode(mockJourneyState)

        assertEquals(HouseholdMode.COMPLETE, result)
    }

    @Test
    fun `Mode returns PROVIDE_THIS_LATER when action is provideThisLater`() {
        val stepConfig = setupStepConfig()
        setUpDependenciesWith(allowProvideLater = true)
        whenever(mockJourneyState.getStepData(routeSegment)).thenReturn(
            mapOf(
                "numberOfHouseholds" to "",
                "action" to PROVIDE_THIS_LATER_BUTTON_ACTION_NAME,
            ),
        )

        val result = stepConfig.mode(mockJourneyState)

        assertEquals(HouseholdMode.PROVIDE_THIS_LATER, result)
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = ["", "2"])
    fun `Mode returns PROVIDE_THIS_LATER for various numberOfHouseholds when action is provideThisLater`(numberOfHouseholds: String?) {
        val stepConfig = setupStepConfig()
        setUpDependenciesWith(allowProvideLater = true)
        whenever(mockJourneyState.getStepData(routeSegment)).thenReturn(
            mapOf(
                "numberOfHouseholds" to numberOfHouseholds,
                "action" to PROVIDE_THIS_LATER_BUTTON_ACTION_NAME,
            ),
        )

        val result = stepConfig.mode(mockJourneyState)

        assertEquals(HouseholdMode.PROVIDE_THIS_LATER, result)
    }

    @Test
    fun `Mode throws when action is provideThisLater but route is disallowed`() {
        val stepConfig = setupStepConfig()
        setUpDependenciesWith(allowProvideLater = false)

        whenever(mockJourneyState.dependencies.allowProvideTenancyDetailsLaterRoute).thenReturn(false)
        whenever(mockJourneyState.journeyId).thenReturn("test")
        whenever(mockJourneyState.getStepData(routeSegment)).thenReturn(
            mapOf(
                "numberOfHouseholds" to "",
                "action" to PROVIDE_THIS_LATER_BUTTON_ACTION_NAME,
            ),
        )

        assertThrows(UnrecoverableJourneyStateException::class.java) {
            stepConfig.mode(mockJourneyState)
        }
    }
}
