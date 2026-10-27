package uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps

import uk.gov.communities.prsdb.webapp.annotations.webAnnotations.JourneyFrameworkComponent
import uk.gov.communities.prsdb.webapp.journeys.AbstractInternalStepConfig
import uk.gov.communities.prsdb.webapp.journeys.JourneyStep
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.states.EpcDetailState
import uk.gov.communities.prsdb.webapp.services.EpcLookupService

@JourneyFrameworkComponent
class EpcLookupByUprnStepConfig(
    private val epcLookupService: EpcLookupService,
) : AbstractInternalStepConfig<EpcLookupByUprnMode, EpcDetailState>() {
    override fun mode(state: EpcDetailState): EpcLookupByUprnMode? =
        if (state.epcRetrievedByUprn != null) EpcLookupByUprnMode.EPC_FOUND else EpcLookupByUprnMode.NOT_FOUND

    override fun afterStepIsReached(state: EpcDetailState) {
        val previousEpcRetrievedByUprn = state.epcRetrievedByUprn
        val newEpcRetrievedByUprn = state.uprn?.let { epcLookupService.getEpcByUprn(it) }
        state.epcRetrievedByUprn = newEpcRetrievedByUprn
        if (newEpcRetrievedByUprn != previousEpcRetrievedByUprn) {
            state.epcRetrievedByUprnUpdatedSinceUserReview = true
            state.markAnswersChanged()
        }
    }
}

@JourneyFrameworkComponent
final class EpcLookupByUprnStep(
    stepConfig: EpcLookupByUprnStepConfig,
) : JourneyStep.InternalStep<EpcLookupByUprnMode, EpcDetailState>(stepConfig)

enum class EpcLookupByUprnMode {
    EPC_FOUND,
    NOT_FOUND,
}
