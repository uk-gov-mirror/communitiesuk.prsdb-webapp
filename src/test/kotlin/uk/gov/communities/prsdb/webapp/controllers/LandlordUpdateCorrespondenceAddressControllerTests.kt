package uk.gov.communities.prsdb.webapp.controllers

import org.mockito.kotlin.eq
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.web.context.WebApplicationContext
import uk.gov.communities.prsdb.webapp.journeys.StepLifecycleOrchestrator
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.update.correspondenceAddress.UpdateCorrespondenceAddressJourneyFactory
import uk.gov.communities.prsdb.webapp.journeys.shared.stepConfig.LookupAddressStep
import uk.gov.communities.prsdb.webapp.services.PropertyOwnershipService

@WebMvcTest(LandlordUpdateCorrespondenceAddressController::class)
class LandlordUpdateCorrespondenceAddressControllerTests(
    @Autowired webContext: WebApplicationContext,
) : BasePropertyDetailsUpdateControllerTests(webContext) {
    @MockitoBean
    private lateinit var journeyFactory: UpdateCorrespondenceAddressJourneyFactory

    @MockitoBean
    override lateinit var propertyOwnershipService: PropertyOwnershipService

    @MockitoBean
    override lateinit var stepLifecycleOrchestrator: StepLifecycleOrchestrator.VisitableStepLifecycleOrchestrator

    override val propertyOwnershipId = 1L

    override val updateStepRoute =
        LandlordUpdateCorrespondenceAddressController.getUpdateCorrespondenceAddressRoute(propertyOwnershipId) +
            "/${LookupAddressStep.ROUTE_SEGMENT}"

    override val formContent = "postcode=FA1+1AA&houseNameOrNumber=1"

    override fun stubCreateJourneySteps() {
        whenever(journeyFactory.createJourneySteps(eq(propertyOwnershipId)))
            .thenReturn(
                mapOf(
                    LookupAddressStep.ROUTE_SEGMENT to stepLifecycleOrchestrator,
                ),
            )
    }
}
