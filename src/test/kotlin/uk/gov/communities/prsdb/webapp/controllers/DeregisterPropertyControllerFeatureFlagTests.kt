package uk.gov.communities.prsdb.webapp.controllers

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import org.springframework.web.servlet.ModelAndView
import uk.gov.communities.prsdb.webapp.config.featureFlags.FeatureFlagTestCallingEndpoints
import uk.gov.communities.prsdb.webapp.constants.ACTIVE_INACTIVE
import uk.gov.communities.prsdb.webapp.constants.CONFIRMATION_PATH_SEGMENT
import uk.gov.communities.prsdb.webapp.controllers.DeregisterPropertyController.Companion.getPropertyDeregistrationBasePath
import uk.gov.communities.prsdb.webapp.controllers.DeregisterPropertyController.Companion.getPropertyDeregistrationPath
import uk.gov.communities.prsdb.webapp.journeys.StepLifecycleOrchestrator
import uk.gov.communities.prsdb.webapp.journeys.propertyDeregistration.PropertyDeregistrationJourneyFactory
import uk.gov.communities.prsdb.webapp.journeys.propertyDeregistration.stepConfig.CheckCanDeregisterStep
import uk.gov.communities.prsdb.webapp.services.PropertyDeregistrationService
import uk.gov.communities.prsdb.webapp.services.PropertyOwnershipService

// TODO PDJB-2149: Delete this class when the ACTIVE_INACTIVE flag is removed
class DeregisterPropertyControllerFeatureFlagTests : FeatureFlagTestCallingEndpoints() {
    @MockitoBean
    private lateinit var journeyFactory: PropertyDeregistrationJourneyFactory

    @MockitoBean
    private lateinit var propertyOwnershipService: PropertyOwnershipService

    @MockitoBean
    private lateinit var propertyDeregistrationService: PropertyDeregistrationService

    @MockitoBean
    private lateinit var stepLifecycleOrchestrator: StepLifecycleOrchestrator.VisitableStepLifecycleOrchestrator

    private val propertyOwnershipId = 1L
    private val redirectModelAndView = ModelAndView("redirect:/next")

    @BeforeEach
    fun setUp() {
        whenever(propertyOwnershipService.isCurrentUserLandlord(propertyOwnershipId)).thenReturn(true)
        whenever(journeyFactory.createJourneySteps(propertyOwnershipId))
            .thenReturn(mapOf(CheckCanDeregisterStep.ROUTE_SEGMENT to stepLifecycleOrchestrator))
        whenever(stepLifecycleOrchestrator.getStepModelAndView()).thenReturn(redirectModelAndView)
        whenever(stepLifecycleOrchestrator.postStepModelAndView(any())).thenReturn(redirectModelAndView)
    }

    @WithMockUser(roles = ["INDIVIDUAL_LANDLORD"])
    @Test
    fun `deregistration journey is unavailable if the active inactive feature flag is enabled`() {
        featureFlagManager.enableFeature(ACTIVE_INACTIVE)

        mvc
            .get(getPropertyDeregistrationPath(propertyOwnershipId))
            .andExpect { status { isNotFound() } }
    }

    @WithMockUser(roles = ["INDIVIDUAL_LANDLORD"])
    @Test
    fun `deregistration journey is available if the active inactive feature flag is disabled`() {
        featureFlagManager.disableFeature(ACTIVE_INACTIVE)

        mvc
            .get(getPropertyDeregistrationPath(propertyOwnershipId))
            .andExpect { status { is3xxRedirection() } }
    }

    @WithMockUser(roles = ["INDIVIDUAL_LANDLORD"])
    @Test
    fun `submitting the deregistration journey is unavailable if the active inactive feature flag is enabled`() {
        featureFlagManager.enableFeature(ACTIVE_INACTIVE)

        mvc
            .post(getPropertyDeregistrationPath(propertyOwnershipId)) {
                param("formData", "")
                with(csrf())
            }.andExpect { status { isNotFound() } }
    }

    @WithMockUser(roles = ["INDIVIDUAL_LANDLORD"])
    @Test
    fun `submitting the deregistration journey is available if the active inactive feature flag is disabled`() {
        featureFlagManager.disableFeature(ACTIVE_INACTIVE)

        mvc
            .post(getPropertyDeregistrationPath(propertyOwnershipId)) {
                param("formData", "")
                with(csrf())
            }.andExpect { status { is3xxRedirection() } }
    }

    @WithMockUser(roles = ["INDIVIDUAL_LANDLORD"])
    @Test
    fun `deregistration confirmation page is unavailable if the active inactive feature flag is enabled`() {
        featureFlagManager.enableFeature(ACTIVE_INACTIVE)
        whenever(propertyDeregistrationService.getDeregisteredPropertyOwnershipIdsFromSession())
            .thenReturn(mutableListOf(propertyOwnershipId))

        mvc
            .get("${getPropertyDeregistrationBasePath(propertyOwnershipId)}/$CONFIRMATION_PATH_SEGMENT")
            .andExpect { status { isNotFound() } }
    }
}
