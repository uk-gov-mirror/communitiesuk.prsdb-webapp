package uk.gov.communities.prsdb.webapp.controllers

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.server.ResponseStatusException
import org.springframework.web.servlet.ModelAndView
import org.springframework.web.util.UriTemplate
import uk.gov.communities.prsdb.webapp.annotations.webAnnotations.AllowIfLandlord
import uk.gov.communities.prsdb.webapp.annotations.webAnnotations.AvailableWhenFeatureEnabled
import uk.gov.communities.prsdb.webapp.annotations.webAnnotations.PrsdbController
import uk.gov.communities.prsdb.webapp.constants.ACTIVE_INACTIVE
import uk.gov.communities.prsdb.webapp.constants.DEACTIVATE_PROPERTY_JOURNEY_URL
import uk.gov.communities.prsdb.webapp.constants.LANDLORD_PATH_SEGMENT
import uk.gov.communities.prsdb.webapp.controllers.DeactivatePropertyController.Companion.PROPERTY_DEACTIVATION_ROUTE
import uk.gov.communities.prsdb.webapp.exceptions.PropertyOwnershipMismatchException
import uk.gov.communities.prsdb.webapp.journeys.FormData
import uk.gov.communities.prsdb.webapp.journeys.JourneyStepDispatcher
import uk.gov.communities.prsdb.webapp.journeys.StepLifecycleOrchestrator
import uk.gov.communities.prsdb.webapp.journeys.propertyDeactivation.PropertyDeactivationJourneyFactory
import uk.gov.communities.prsdb.webapp.journeys.propertyDeactivation.stepConfig.CheckCanDeactivateStep
import uk.gov.communities.prsdb.webapp.services.PropertyOwnershipService

@AllowIfLandlord
@PrsdbController
@RequestMapping(PROPERTY_DEACTIVATION_ROUTE)
class DeactivatePropertyController(
    private val propertyDeactivationJourneyFactory: PropertyDeactivationJourneyFactory,
    private val propertyOwnershipService: PropertyOwnershipService,
) {
    // TODO PDJB-2149: Remove this annotation when the ACTIVE_INACTIVE flag is removed
    @AvailableWhenFeatureEnabled(ACTIVE_INACTIVE)
    @GetMapping("/{*stepPath}")
    fun getJourneyStep(
        @PathVariable stepPath: String,
        @PathVariable("propertyOwnershipId") propertyOwnershipId: Long,
    ): ModelAndView {
        throwExceptionIfCurrentUserIsUnauthorizedToDeactivateProperty(propertyOwnershipId)
        return dispatchJourneyStep(stepPath, propertyOwnershipId) { getStepModelAndView() }
    }

    // TODO PDJB-2149: Remove this annotation when the ACTIVE_INACTIVE flag is removed
    @AvailableWhenFeatureEnabled(ACTIVE_INACTIVE)
    @PostMapping("/{*stepPath}")
    fun postJourneyData(
        @PathVariable stepPath: String,
        @PathVariable("propertyOwnershipId") propertyOwnershipId: Long,
        @RequestParam formData: FormData,
    ): ModelAndView {
        throwExceptionIfCurrentUserIsUnauthorizedToDeactivateProperty(propertyOwnershipId)
        return dispatchJourneyStep(stepPath, propertyOwnershipId) { postStepModelAndView(formData) }
    }

    private fun dispatchJourneyStep(
        stepPath: String,
        propertyOwnershipId: Long,
        dispatch: StepLifecycleOrchestrator.() -> ModelAndView,
    ): ModelAndView =
        JourneyStepDispatcher.handleInitialisableRequest(
            rawStepPath = stepPath,
            createRoutingMap = { propertyDeactivationJourneyFactory.createJourneySteps(propertyOwnershipId) },
            initialiseJourney = { propertyDeactivationJourneyFactory.initializeJourneyState(propertyOwnershipId) },
            dispatch = dispatch,
            startNewJourneyOn = { it is PropertyOwnershipMismatchException },
        )

    private fun throwExceptionIfCurrentUserIsUnauthorizedToDeactivateProperty(propertyOwnershipId: Long) {
        if (!propertyOwnershipService.isCurrentUserLandlord(propertyOwnershipId)) {
            throw ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "The current user is not authorised to deactivate property ownership $propertyOwnershipId",
            )
        }
    }

    companion object {
        const val PROPERTY_DEACTIVATION_ROUTE = "/$LANDLORD_PATH_SEGMENT/$DEACTIVATE_PROPERTY_JOURNEY_URL/{propertyOwnershipId}"

        fun getPropertyDeactivationBasePath(propertyOwnershipId: Long): String =
            UriTemplate(PROPERTY_DEACTIVATION_ROUTE)
                .expand(propertyOwnershipId)
                .toASCIIString()

        fun getPropertyDeactivationPath(propertyOwnershipId: Long): String =
            "${getPropertyDeactivationBasePath(propertyOwnershipId)}/${CheckCanDeactivateStep.ROUTE_SEGMENT}"
    }
}
