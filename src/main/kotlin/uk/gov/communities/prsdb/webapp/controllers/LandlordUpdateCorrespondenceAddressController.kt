package uk.gov.communities.prsdb.webapp.controllers

import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.servlet.ModelAndView
import uk.gov.communities.prsdb.webapp.annotations.webAnnotations.AvailableWhenFeatureEnabled
import uk.gov.communities.prsdb.webapp.annotations.webAnnotations.PrsdbController
import uk.gov.communities.prsdb.webapp.constants.CORRESPONDENCE_ADDRESS
import uk.gov.communities.prsdb.webapp.constants.LANDLORD_PATH_SEGMENT
import uk.gov.communities.prsdb.webapp.constants.PROPERTY_DETAILS_SEGMENT
import uk.gov.communities.prsdb.webapp.controllers.LandlordUpdateCorrespondenceAddressController.Companion.UPDATE_CORRESPONDENCE_ADDRESS_ROUTE
import uk.gov.communities.prsdb.webapp.journeys.FormData
import uk.gov.communities.prsdb.webapp.journeys.JourneyStepDispatcher
import uk.gov.communities.prsdb.webapp.journeys.StepLifecycleOrchestrator
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.update.correspondenceAddress.UpdateCorrespondenceAddressJourneyFactory
import uk.gov.communities.prsdb.webapp.services.PropertyOwnershipService
import java.security.Principal

@PrsdbController
@RequestMapping(UPDATE_CORRESPONDENCE_ADDRESS_ROUTE)
@PreAuthorize("hasRole('LANDLORD')")
class LandlordUpdateCorrespondenceAddressController(
    private val journeyFactory: UpdateCorrespondenceAddressJourneyFactory,
    private val propertyOwnershipService: PropertyOwnershipService,
) {
    @GetMapping("/{*stepPath}")
    @AvailableWhenFeatureEnabled(CORRESPONDENCE_ADDRESS)
    fun getUpdateStep(
        principal: Principal,
        @PathVariable propertyOwnershipId: Long,
        @PathVariable stepPath: String,
    ): ModelAndView {
        propertyOwnershipService.throwIfCurrentUserNotAuthorizedToEdit(propertyOwnershipId)
        return dispatchJourneyStep(stepPath, propertyOwnershipId, principal) { getStepModelAndView() }
    }

    @PostMapping("/{*stepPath}")
    @AvailableWhenFeatureEnabled(CORRESPONDENCE_ADDRESS)
    fun postUpdateStep(
        principal: Principal,
        @PathVariable propertyOwnershipId: Long,
        @PathVariable stepPath: String,
        @RequestParam formData: FormData,
    ): ModelAndView {
        propertyOwnershipService.throwIfCurrentUserNotAuthorizedToEdit(propertyOwnershipId)
        return dispatchJourneyStep(stepPath, propertyOwnershipId, principal) { postStepModelAndView(formData) }
    }

    private fun dispatchJourneyStep(
        stepPath: String,
        propertyOwnershipId: Long,
        principal: Principal,
        dispatch: StepLifecycleOrchestrator.() -> ModelAndView,
    ): ModelAndView =
        JourneyStepDispatcher.handleInitialisableRequest(
            rawStepPath = stepPath,
            createRoutingMap = {
                journeyFactory.createJourneySteps(propertyOwnershipId)
            },
            initialiseJourney = {
                journeyFactory.initialiseJourneyState(
                    Pair(propertyOwnershipId, principal),
                    propertyOwnershipService.getLastModifiedDate(propertyOwnershipId),
                )
            },
            dispatch = dispatch,
        )

    companion object {
        const val UPDATE_CORRESPONDENCE_ADDRESS_ROUTE =
            "/$LANDLORD_PATH_SEGMENT/$PROPERTY_DETAILS_SEGMENT/{propertyOwnershipId}/update-correspondence-address"

        fun getUpdateCorrespondenceAddressRoute(propertyOwnershipId: Long): String =
            UPDATE_CORRESPONDENCE_ADDRESS_ROUTE.replace("{propertyOwnershipId}", propertyOwnershipId.toString())
    }
}
