package uk.gov.communities.prsdb.webapp.journeys.example

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.server.ResponseStatusException
import org.springframework.web.servlet.ModelAndView
import uk.gov.communities.prsdb.webapp.annotations.webAnnotations.PrsdbController
import uk.gov.communities.prsdb.webapp.forms.PageData
import uk.gov.communities.prsdb.webapp.journeys.JourneyStateService
import uk.gov.communities.prsdb.webapp.journeys.NoSuchJourneyException

@PrsdbController
@RequestMapping("new-journey")
class FooJourneyController(
    val journeyFactory: FooExampleJourneyFactory,
) {
    @GetMapping("{propertyId}/{stepName}")
    fun getStep(
        @PathVariable("propertyId") propertyId: Long,
        @PathVariable("stepName") stepName: String,
    ): ModelAndView =
        try {
            println("Getting step $stepName for property $propertyId")
            journeyFactory.createJourneySteps(propertyId)[stepName]?.getStepModelAndView()
                ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Step not found")
        } catch (_: NoSuchJourneyException) {
            val journeyId = journeyFactory.initializeJourneyState(propertyId)
            val redirectUrl = JourneyStateService.urlWithJourneyState(stepName, journeyId)
            ModelAndView("redirect:$redirectUrl")
        }

    @PostMapping("{propertyId}/{stepName}")
    fun postStep(
        @PathVariable("propertyId") propertyId: Long,
        @PathVariable("stepName") stepName: String,
        @RequestParam formData: PageData,
    ): ModelAndView =
        try {
            println("Posting step $stepName for property $propertyId with data $formData")
            journeyFactory.createJourneySteps(propertyId)[stepName]?.postStepModelAndView(formData)
                ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Step not found")
        } catch (_: NoSuchJourneyException) {
            val journeyId = journeyFactory.initializeJourneyState(propertyId)
            val redirectUrl = JourneyStateService.urlWithJourneyState(stepName, journeyId)
            ModelAndView("redirect:$redirectUrl")
        }
}
