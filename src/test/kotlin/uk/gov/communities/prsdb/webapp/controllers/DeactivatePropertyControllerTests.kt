package uk.gov.communities.prsdb.webapp.controllers

import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.get
import org.springframework.web.context.WebApplicationContext
import org.springframework.web.servlet.ModelAndView
import uk.gov.communities.prsdb.webapp.constants.DEACTIVATE_PROPERTY_JOURNEY_URL
import uk.gov.communities.prsdb.webapp.constants.LANDLORD_PATH_SEGMENT
import uk.gov.communities.prsdb.webapp.controllers.DeactivatePropertyController.Companion.getPropertyDeactivationPath
import uk.gov.communities.prsdb.webapp.exceptions.PropertyOwnershipMismatchException
import uk.gov.communities.prsdb.webapp.journeys.NoSuchJourneyException
import uk.gov.communities.prsdb.webapp.journeys.StepLifecycleOrchestrator
import uk.gov.communities.prsdb.webapp.journeys.propertyDeactivation.PropertyDeactivationJourneyFactory
import uk.gov.communities.prsdb.webapp.journeys.propertyDeactivation.stepConfig.CheckCanDeactivateStep
import uk.gov.communities.prsdb.webapp.services.PropertyOwnershipService
import kotlin.test.assertEquals

@WebMvcTest(DeactivatePropertyController::class)
class DeactivatePropertyControllerTests(
    @Autowired val webContext: WebApplicationContext,
) : ControllerTest(webContext) {
    @MockitoBean
    private lateinit var propertyDeactivationJourneyFactory: PropertyDeactivationJourneyFactory

    @MockitoBean
    private lateinit var propertyOwnershipService: PropertyOwnershipService

    @MockitoBean
    private lateinit var mockStepLifecycleOrchestrator: StepLifecycleOrchestrator.VisitableStepLifecycleOrchestrator

    private val propertyOwnershipId = 1L

    @Test
    fun `getPropertyDeactivationPath returns a path to the initial deactivate step`() {
        val propertyDeactivationPath = getPropertyDeactivationPath(propertyOwnershipId)

        assertEquals(
            "/$LANDLORD_PATH_SEGMENT/$DEACTIVATE_PROPERTY_JOURNEY_URL/$propertyOwnershipId/${CheckCanDeactivateStep.ROUTE_SEGMENT}",
            propertyDeactivationPath,
        )
    }

    @Test
    @WithMockUser(roles = ["INDIVIDUAL_LANDLORD"], value = "user")
    fun `getJourneyStep for the initial step returns 200 for the landlord who owns this property`() {
        whenever(propertyOwnershipService.isCurrentUserLandlord(eq(propertyOwnershipId))).thenReturn(true)
        whenever(propertyDeactivationJourneyFactory.createJourneySteps(propertyOwnershipId))
            .thenReturn(mapOf(CheckCanDeactivateStep.ROUTE_SEGMENT to mockStepLifecycleOrchestrator))
        whenever(mockStepLifecycleOrchestrator.getStepModelAndView())
            .thenReturn(ModelAndView("placeholder", mapOf("title" to "placeholder")))

        mvc
            .get(getPropertyDeactivationPath(propertyOwnershipId))
            .andExpect { status { isOk() } }
    }

    @Test
    @WithMockUser(roles = ["INDIVIDUAL_LANDLORD"], value = "user")
    fun `getJourneyStep returns 404 for a landlord who does not own this property`() {
        whenever(propertyOwnershipService.isCurrentUserLandlord(eq(propertyOwnershipId))).thenReturn(false)

        mvc
            .get(getPropertyDeactivationPath(propertyOwnershipId))
            .andExpect { status { isNotFound() } }
    }

    @Test
    @WithMockUser(roles = ["INDIVIDUAL_LANDLORD"], value = "user")
    fun `getJourneyStep redirects to initialize journey when no journey state exists`() {
        val journeyId = "test-journey-id"
        whenever(propertyOwnershipService.isCurrentUserLandlord(eq(propertyOwnershipId))).thenReturn(true)
        whenever(propertyDeactivationJourneyFactory.createJourneySteps(propertyOwnershipId))
            .thenThrow(NoSuchJourneyException())
        whenever(propertyDeactivationJourneyFactory.initializeJourneyState(any())).thenReturn(journeyId)

        mvc
            .get(getPropertyDeactivationPath(propertyOwnershipId))
            .andExpect {
                status { is3xxRedirection() }
                redirectedUrl("${getPropertyDeactivationPath(propertyOwnershipId)}?journeyId=$journeyId")
            }
    }

    @Test
    @WithMockUser(roles = ["INDIVIDUAL_LANDLORD"], value = "user")
    fun `getJourneyStep redirects to initialize journey when property ownership does not match`() {
        val journeyId = "test-journey-id"
        whenever(propertyOwnershipService.isCurrentUserLandlord(eq(propertyOwnershipId))).thenReturn(true)
        whenever(propertyDeactivationJourneyFactory.createJourneySteps(propertyOwnershipId))
            .thenThrow(PropertyOwnershipMismatchException("mismatch"))
        whenever(propertyDeactivationJourneyFactory.initializeJourneyState(any())).thenReturn(journeyId)

        mvc
            .get(getPropertyDeactivationPath(propertyOwnershipId))
            .andExpect {
                status { is3xxRedirection() }
                redirectedUrl("${getPropertyDeactivationPath(propertyOwnershipId)}?journeyId=$journeyId")
            }
    }
}
