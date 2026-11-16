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
import uk.gov.communities.prsdb.webapp.controllers.DeactivatePropertyController.Companion.getPropertyDeactivationPath
import uk.gov.communities.prsdb.webapp.journeys.StepLifecycleOrchestrator
import uk.gov.communities.prsdb.webapp.journeys.propertyDeactivation.PropertyDeactivationJourneyFactory
import uk.gov.communities.prsdb.webapp.journeys.propertyDeactivation.stepConfig.CheckCanDeactivateStep
import uk.gov.communities.prsdb.webapp.services.PropertyOwnershipService

// TODO PDJB-2149: Delete this class when the ACTIVE_INACTIVE flag is removed
class DeactivatePropertyControllerFeatureFlagTests : FeatureFlagTestCallingEndpoints() {
    @MockitoBean
    private lateinit var journeyFactory: PropertyDeactivationJourneyFactory

    @MockitoBean
    private lateinit var propertyOwnershipService: PropertyOwnershipService

    @MockitoBean
    private lateinit var stepLifecycleOrchestrator: StepLifecycleOrchestrator.VisitableStepLifecycleOrchestrator

    private val propertyOwnershipId = 1L
    private val redirectModelAndView = ModelAndView("redirect:/next")

    @BeforeEach
    fun setUp() {
        whenever(propertyOwnershipService.isCurrentUserLandlord(propertyOwnershipId)).thenReturn(true)
        whenever(journeyFactory.createJourneySteps(propertyOwnershipId))
            .thenReturn(mapOf(CheckCanDeactivateStep.ROUTE_SEGMENT to stepLifecycleOrchestrator))
        whenever(stepLifecycleOrchestrator.getStepModelAndView()).thenReturn(redirectModelAndView)
        whenever(stepLifecycleOrchestrator.postStepModelAndView(any())).thenReturn(redirectModelAndView)
    }

    @WithMockUser(roles = ["INDIVIDUAL_LANDLORD"])
    @Test
    fun `deactivation journey is unavailable if the active inactive feature flag is disabled`() {
        featureFlagManager.disableFeature(ACTIVE_INACTIVE)

        mvc
            .get(getPropertyDeactivationPath(propertyOwnershipId))
            .andExpect { status { isNotFound() } }
    }

    @WithMockUser(roles = ["INDIVIDUAL_LANDLORD"])
    @Test
    fun `deactivation journey is available if the active inactive feature flag is enabled`() {
        featureFlagManager.enableFeature(ACTIVE_INACTIVE)

        mvc
            .get(getPropertyDeactivationPath(propertyOwnershipId))
            .andExpect { status { is3xxRedirection() } }
    }

    @WithMockUser(roles = ["INDIVIDUAL_LANDLORD"])
    @Test
    fun `submitting the deactivation journey is unavailable if the active inactive feature flag is disabled`() {
        featureFlagManager.disableFeature(ACTIVE_INACTIVE)

        mvc
            .post(getPropertyDeactivationPath(propertyOwnershipId)) {
                param("formData", "")
                with(csrf())
            }.andExpect { status { isNotFound() } }
    }

    @WithMockUser(roles = ["INDIVIDUAL_LANDLORD"])
    @Test
    fun `submitting the deactivation journey is available if the active inactive feature flag is enabled`() {
        featureFlagManager.enableFeature(ACTIVE_INACTIVE)

        mvc
            .post(getPropertyDeactivationPath(propertyOwnershipId)) {
                param("formData", "")
                with(csrf())
            }.andExpect { status { is3xxRedirection() } }
    }
}
