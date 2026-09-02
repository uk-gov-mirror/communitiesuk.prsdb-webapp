package uk.gov.communities.prsdb.webapp.journeys.lettingAgentInvitation

import org.springframework.beans.factory.ObjectFactory
import uk.gov.communities.prsdb.webapp.annotations.webAnnotations.JourneyFrameworkComponent
import uk.gov.communities.prsdb.webapp.annotations.webAnnotations.PrsdbWebService
import uk.gov.communities.prsdb.webapp.journeys.AbstractJourneyState
import uk.gov.communities.prsdb.webapp.journeys.Destination
import uk.gov.communities.prsdb.webapp.journeys.JourneyState
import uk.gov.communities.prsdb.webapp.journeys.JourneyStateService
import uk.gov.communities.prsdb.webapp.journeys.OrParents
import uk.gov.communities.prsdb.webapp.journeys.StepLifecycleOrchestrator
import uk.gov.communities.prsdb.webapp.journeys.builders.JourneyBuilder.Companion.journey
import uk.gov.communities.prsdb.webapp.journeys.hasOutcome
import uk.gov.communities.prsdb.webapp.journeys.isComplete
import uk.gov.communities.prsdb.webapp.journeys.lettingAgentInvitation.steps.ConfirmationStep
import uk.gov.communities.prsdb.webapp.journeys.lettingAgentInvitation.steps.EnterPasswordStep
import uk.gov.communities.prsdb.webapp.journeys.lettingAgentInvitation.steps.HasPasswordStep
import uk.gov.communities.prsdb.webapp.journeys.lettingAgentInvitation.steps.PasswordStatus
import uk.gov.communities.prsdb.webapp.journeys.lettingAgentInvitation.steps.SetPasswordStep
import uk.gov.communities.prsdb.webapp.journeys.lettingAgentInvitation.steps.StartStep
import uk.gov.communities.prsdb.webapp.journeys.lettingAgentInvitation.steps.StoreAccessStep
import uk.gov.communities.prsdb.webapp.journeys.lettingAgentInvitation.steps.ValidateTokenStep
import java.util.UUID

@PrsdbWebService
class LettingAgentInvitationJourneyFactory(
    private val stateFactory: ObjectFactory<LettingAgentInvitationJourney>,
) {
    fun createJourneySteps(): Map<String, StepLifecycleOrchestrator> {
        val state = stateFactory.getObject()

        return journey(state) {
            unreachableStepStep { journey.startStep }
            configure {
                withAdditionalContentProperty { "title" to "lettingAgentInvitation.title" }
            }
            step(journey.startStep) {
                routeSegment(StartStep.ROUTE_SEGMENT)
                initialStep()
                nextStep { journey.validateTokenStep }
            }
            step(journey.validateTokenStep) {
                routeSegment(ValidateTokenStep.ROUTE_SEGMENT)
                parents { journey.startStep.isComplete() }
                nextStep { journey.hasPasswordStep }
            }
            step(journey.hasPasswordStep) {
                parents { journey.validateTokenStep.isComplete() }
                nextStep { status ->
                    when (status) {
                        PasswordStatus.HAS_PASSWORD -> journey.enterPasswordStep
                        PasswordStatus.NO_PASSWORD -> journey.setPasswordStep
                    }
                }
            }
            step(journey.setPasswordStep) {
                routeSegment(SetPasswordStep.ROUTE_SEGMENT)
                parents { journey.hasPasswordStep.hasOutcome(PasswordStatus.NO_PASSWORD) }
                nextStep { journey.confirmationStep }
            }
            step(journey.confirmationStep) {
                routeSegment(ConfirmationStep.ROUTE_SEGMENT)
                parents { journey.setPasswordStep.isComplete() }
                nextStep { journey.storeAccessStep }
            }
            step(journey.enterPasswordStep) {
                routeSegment(EnterPasswordStep.ROUTE_SEGMENT)
                parents { journey.hasPasswordStep.hasOutcome(PasswordStatus.HAS_PASSWORD) }
                nextStep { journey.storeAccessStep }
            }
            step(journey.storeAccessStep) {
                routeSegment(StoreAccessStep.ROUTE_SEGMENT)
                parents {
                    OrParents(
                        journey.confirmationStep.isComplete(),
                        journey.enterPasswordStep.isComplete(),
                    )
                }
                // TODO PDJB-1570: Replace the homepage placeholder with the letting-agent destination.
                nextDestination { Destination.ExternalUrl("/") }
            }
        }
    }

    fun initializeJourneyState(token: UUID): String {
        val state = stateFactory.getObject()
        return state.initializeState(token)
    }
}

@JourneyFrameworkComponent("lettingAgentInvitationJourney")
class LettingAgentInvitationJourney(
    override val startStep: StartStep,
    override val validateTokenStep: ValidateTokenStep,
    override val hasPasswordStep: HasPasswordStep,
    override val setPasswordStep: SetPasswordStep,
    override val confirmationStep: ConfirmationStep,
    override val enterPasswordStep: EnterPasswordStep,
    override val storeAccessStep: StoreAccessStep,
    journeyStateService: JourneyStateService,
) : AbstractJourneyState(journeyStateService),
    LettingAgentInvitationJourneyState {
    override var invitationToken: String? by delegateProvider.nullableDelegate("invitationToken")
    override var hasPassword: Boolean? by delegateProvider.nullableDelegate("hasPassword")

    override fun generateJourneyId(seed: Any?): String {
        val token = seed as? UUID
        return super<AbstractJourneyState>.generateJourneyId(
            token?.let { "Letting agent invitation journey for token $it at time ${System.currentTimeMillis()}" },
        )
    }
}

interface LettingAgentInvitationJourneyState : JourneyState {
    val startStep: StartStep
    val validateTokenStep: ValidateTokenStep
    val hasPasswordStep: HasPasswordStep
    val setPasswordStep: SetPasswordStep
    val confirmationStep: ConfirmationStep
    val enterPasswordStep: EnterPasswordStep
    val storeAccessStep: StoreAccessStep
    var invitationToken: String?
    var hasPassword: Boolean?
}
