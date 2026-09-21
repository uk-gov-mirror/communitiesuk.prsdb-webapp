package uk.gov.communities.prsdb.webapp.journeys.propertyRegistration

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.springframework.beans.factory.ObjectFactory
import uk.gov.communities.prsdb.webapp.config.managers.FeatureFlagManager
import uk.gov.communities.prsdb.webapp.constants.CORRESPONDENCE_ADDRESS
import uk.gov.communities.prsdb.webapp.constants.DELEGATE_TO_LETTING_AGENT
import uk.gov.communities.prsdb.webapp.constants.PROPERTY_REGISTRATION_RESTRUCTURE_AND_SKIPPING
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps.CorrespondenceEmailStep
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps.WhoProvidesRentalDetailsStep
import uk.gov.communities.prsdb.webapp.journeys.shared.stepConfig.LookupAddressStep
import uk.gov.communities.prsdb.webapp.journeys.shared.tasks.CorrespondenceAddressTask

class PropertyRegistrationJourneyFactoryTests {
    @Test
    fun `createJourneySteps treats the who-provides answer as an unknown checkable element when the delegate feature flag is disabled`() {
        val factory = factoryFor(checkingAnswersFor = WhoProvidesRentalDetailsStep.ROUTE_SEGMENT, delegateEnabled = false)

        val exception = assertThrows<IllegalStateException> { factory.createJourneySteps() }

        assertEquals("Unknown checkable element ${WhoProvidesRentalDetailsStep.ROUTE_SEGMENT}", exception.message)
    }

    @ParameterizedTest
    @MethodSource("disabledCorrespondenceJourneys")
    fun `createJourneySteps rejects contact changes when either required feature is disabled`(
        checkingAnswersFor: String,
        correspondenceEnabled: Boolean,
        restructureEnabled: Boolean,
    ) {
        val factory =
            factoryFor(
                checkingAnswersFor,
                correspondenceEnabled = correspondenceEnabled,
                restructureEnabled = restructureEnabled,
            )

        val exception = assertThrows<IllegalStateException> { factory.createJourneySteps() }

        assertEquals("Unknown checkable element $checkingAnswersFor", exception.message)
    }

    private fun factoryFor(
        checkingAnswersFor: String?,
        delegateEnabled: Boolean = false,
        correspondenceEnabled: Boolean = false,
        restructureEnabled: Boolean = false,
    ): PropertyRegistrationJourneyFactory {
        val state = mock<PropertyRegistrationJourneyState> { on { this.checkingAnswersFor } doReturn checkingAnswersFor }
        val stateFactory = mock<ObjectFactory<PropertyRegistrationJourneyState>> { on { getObject() } doReturn state }
        val featureFlagManager =
            mock<FeatureFlagManager> {
                on { checkFeature(DELEGATE_TO_LETTING_AGENT) } doReturn delegateEnabled
                on { checkFeature(CORRESPONDENCE_ADDRESS) } doReturn correspondenceEnabled
                on { checkFeature(PROPERTY_REGISTRATION_RESTRUCTURE_AND_SKIPPING) } doReturn restructureEnabled
            }
        return PropertyRegistrationJourneyFactory(stateFactory, featureFlagManager)
    }

    companion object {
        @JvmStatic
        fun disabledCorrespondenceJourneys() =
            listOf(
                CorrespondenceEmailStep.ROUTE_SEGMENT,
                "${CorrespondenceAddressTask.ROUTE_SEGMENT}/${LookupAddressStep.ROUTE_SEGMENT}",
            ).flatMap { route ->
                listOf(
                    Arguments.of(route, false, false),
                    Arguments.of(route, false, true),
                    Arguments.of(route, true, false),
                )
            }
    }
}
