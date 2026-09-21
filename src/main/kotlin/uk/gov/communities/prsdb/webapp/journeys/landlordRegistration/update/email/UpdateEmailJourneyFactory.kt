package uk.gov.communities.prsdb.webapp.journeys.landlordRegistration.update.email

import org.springframework.beans.factory.ObjectFactory
import uk.gov.communities.prsdb.webapp.annotations.webAnnotations.JourneyFrameworkComponent
import uk.gov.communities.prsdb.webapp.annotations.webAnnotations.PrsdbWebService
import uk.gov.communities.prsdb.webapp.controllers.LandlordDetailsController.Companion.LANDLORD_DETAILS_FOR_LANDLORD_ROUTE
import uk.gov.communities.prsdb.webapp.journeys.AbstractJourneyState
import uk.gov.communities.prsdb.webapp.journeys.JourneyState
import uk.gov.communities.prsdb.webapp.journeys.JourneyStateService
import uk.gov.communities.prsdb.webapp.journeys.StepLifecycleOrchestrator
import uk.gov.communities.prsdb.webapp.journeys.builders.JourneyBuilder.Companion.journey
import uk.gov.communities.prsdb.webapp.journeys.isComplete
import uk.gov.communities.prsdb.webapp.journeys.landlordRegistration.stepConfig.EmailStep
import uk.gov.communities.prsdb.webapp.journeys.landlordRegistration.stepConfig.EmailStepConfig
import uk.gov.communities.prsdb.webapp.journeys.shared.Complete
import java.security.Principal

@PrsdbWebService
class UpdateEmailJourneyFactory(
    private val stateFactory: ObjectFactory<UpdateEmailJourney>,
) {
    fun createJourneySteps(): Map<String, StepLifecycleOrchestrator> {
        val state = stateFactory.getObject()

        return journey(state) {
            unreachableStepUrl { LANDLORD_DETAILS_FOR_LANDLORD_ROUTE }
            step<Complete, EmailStepConfig>(journey.emailStep) {
                routeSegment(EmailStep.ROUTE_SEGMENT)
                backUrl { LANDLORD_DETAILS_FOR_LANDLORD_ROUTE }
                nextStep { journey.completeEmailUpdateStep }
                initialStep()
                stepSpecificInitialisation {
                    withCorrespondenceTemplateIfFlagIsSet()
                }
            }
            step(journey.completeEmailUpdateStep) {
                parents { journey.emailStep.isComplete() }
                nextUrl { LANDLORD_DETAILS_FOR_LANDLORD_ROUTE }
            }
        }
    }

    fun initializeJourneyState(user: Principal): String = stateFactory.getObject().initializeOrRestoreState(user)
}

@JourneyFrameworkComponent
class UpdateEmailJourney(
    override val emailStep: EmailStep,
    override val completeEmailUpdateStep: CompleteEmailUpdateStep,
    journeyStateService: JourneyStateService,
    private val journeyName: String = "email",
) : AbstractJourneyState(journeyStateService),
    UpdateEmailJourneyState {
    override fun generateJourneyId(seed: Any?): String {
        val user: Principal? = seed as? Principal

        return super<AbstractJourneyState>.generateJourneyId(
            user?.let { "Update $journeyName for landlord ${it.name}" },
        )
    }
}

interface UpdateEmailJourneyState : JourneyState {
    val emailStep: EmailStep
    val completeEmailUpdateStep: CompleteEmailUpdateStep
}
