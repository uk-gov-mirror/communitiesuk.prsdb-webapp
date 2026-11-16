package uk.gov.communities.prsdb.webapp.controllers

import org.springframework.http.HttpStatus
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.server.ResponseStatusException
import org.springframework.web.servlet.ModelAndView
import org.springframework.web.util.UriTemplate
import uk.gov.communities.prsdb.webapp.annotations.webAnnotations.AllowIfLandlord
import uk.gov.communities.prsdb.webapp.annotations.webAnnotations.AvailableWhenFeatureDisabled
import uk.gov.communities.prsdb.webapp.annotations.webAnnotations.PrsdbController
import uk.gov.communities.prsdb.webapp.constants.ACTIVE_INACTIVE
import uk.gov.communities.prsdb.webapp.constants.CONFIRMATION_PATH_SEGMENT
import uk.gov.communities.prsdb.webapp.constants.DEREGISTER_PROPERTY_JOURNEY_URL
import uk.gov.communities.prsdb.webapp.constants.LANDLORD_PATH_SEGMENT
import uk.gov.communities.prsdb.webapp.controllers.DeregisterPropertyController.Companion.PROPERTY_DEREGISTRATION_ROUTE
import uk.gov.communities.prsdb.webapp.controllers.LandlordController.Companion.LANDLORD_DASHBOARD_URL
import uk.gov.communities.prsdb.webapp.exceptions.PropertyOwnershipMismatchException
import uk.gov.communities.prsdb.webapp.journeys.FormData
import uk.gov.communities.prsdb.webapp.journeys.JourneyStepDispatcher
import uk.gov.communities.prsdb.webapp.journeys.StepLifecycleOrchestrator
import uk.gov.communities.prsdb.webapp.journeys.propertyDeregistration.PropertyDeregistrationJourneyFactory
import uk.gov.communities.prsdb.webapp.journeys.propertyDeregistration.stepConfig.CheckCanDeregisterStep
import uk.gov.communities.prsdb.webapp.services.PropertyDeregistrationService
import uk.gov.communities.prsdb.webapp.services.PropertyOwnershipService

// TODO PDJB-2149: Remove this controller when the ACTIVE_INACTIVE flag is removed
@AllowIfLandlord
@PrsdbController
@RequestMapping(PROPERTY_DEREGISTRATION_ROUTE)
class DeregisterPropertyController(
    private val propertyDeregistrationJourneyFactory: PropertyDeregistrationJourneyFactory,
    private val propertyOwnershipService: PropertyOwnershipService,
    private val propertyDeregistrationService: PropertyDeregistrationService,
) {
    @AvailableWhenFeatureDisabled(ACTIVE_INACTIVE)
    @GetMapping("/{*stepPath}")
    fun getJourneyStep(
        @PathVariable stepPath: String,
        @PathVariable("propertyOwnershipId") propertyOwnershipId: Long,
    ): ModelAndView {
        throwExceptionIfCurrentUserIsUnauthorizedToDeregisterProperty(propertyOwnershipId)
        return dispatchJourneyStep(stepPath, propertyOwnershipId) { getStepModelAndView() }
    }

    @AvailableWhenFeatureDisabled(ACTIVE_INACTIVE)
    @PostMapping("/{*stepPath}")
    fun postJourneyData(
        @PathVariable stepPath: String,
        @PathVariable("propertyOwnershipId") propertyOwnershipId: Long,
        @RequestParam formData: FormData,
    ): ModelAndView {
        throwExceptionIfCurrentUserIsUnauthorizedToDeregisterProperty(propertyOwnershipId)
        return dispatchJourneyStep(stepPath, propertyOwnershipId) { postStepModelAndView(formData) }
    }

    private fun dispatchJourneyStep(
        stepPath: String,
        propertyOwnershipId: Long,
        dispatch: StepLifecycleOrchestrator.() -> ModelAndView,
    ): ModelAndView =
        JourneyStepDispatcher.handleInitialisableRequest(
            rawStepPath = stepPath,
            createRoutingMap = { propertyDeregistrationJourneyFactory.createJourneySteps(propertyOwnershipId) },
            initialiseJourney = { propertyDeregistrationJourneyFactory.initializeJourneyState(propertyOwnershipId) },
            dispatch = dispatch,
            startNewJourneyOn = { it is PropertyOwnershipMismatchException },
        )

    @AvailableWhenFeatureDisabled(ACTIVE_INACTIVE)
    @GetMapping("/$CONFIRMATION_PATH_SEGMENT")
    fun getConfirmation(
        model: Model,
        @PathVariable("propertyOwnershipId") propertyOwnershipId: Long,
    ): String {
        checkPropertyHasBeenDeregisteredInThisSession(propertyOwnershipId)

        model.addAttribute("landlordDashboardUrl", LANDLORD_DASHBOARD_URL)
        model.addAttribute("address", propertyDeregistrationService.getDeregisteredPropertyAddress(propertyOwnershipId))
        return "deregisterPropertyConfirmation"
    }

    private fun throwExceptionIfCurrentUserIsUnauthorizedToDeregisterProperty(propertyOwnershipId: Long) {
        if (!isCurrentUserAuthorizedToDeregisterProperty(propertyOwnershipId)) {
            throw ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "The current user is not authorised to delete property ownership $propertyOwnershipId",
            )
        }
    }

    private fun isCurrentUserAuthorizedToDeregisterProperty(propertyOwnershipId: Long): Boolean =
        propertyOwnershipService.isCurrentUserLandlord(propertyOwnershipId)

    private fun checkPropertyHasBeenDeregisteredInThisSession(propertyOwnershipId: Long) {
        if (propertyOwnershipId !in propertyDeregistrationService.getDeregisteredPropertyOwnershipIdsFromSession()) {
            throw ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "PropertyOwnershipId $propertyOwnershipId was not found in the list of deregistered propertyOwnershipIds in the session",
            )
        }

        if (propertyOwnershipService.retrievePropertyOwnershipById(propertyOwnershipId) != null) {
            throw ResponseStatusException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Property ownership $propertyOwnershipId was found in the database",
            )
        }
    }

    companion object {
        const val PROPERTY_DEREGISTRATION_ROUTE = "/$LANDLORD_PATH_SEGMENT/$DEREGISTER_PROPERTY_JOURNEY_URL/{propertyOwnershipId}"

        fun getPropertyDeregistrationBasePath(propertyOwnershipId: Long): String =
            UriTemplate(PROPERTY_DEREGISTRATION_ROUTE)
                .expand(propertyOwnershipId)
                .toASCIIString()

        fun getPropertyDeregistrationPath(propertyOwnershipId: Long): String =
            "${getPropertyDeregistrationBasePath(propertyOwnershipId)}/${CheckCanDeregisterStep.ROUTE_SEGMENT}"
    }
}
