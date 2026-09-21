package uk.gov.communities.prsdb.webapp.journeys.shared.tasks

import uk.gov.communities.prsdb.webapp.journeys.JourneyStateService
import uk.gov.communities.prsdb.webapp.journeys.OrParents
import uk.gov.communities.prsdb.webapp.journeys.TaskWithoutDependencies
import uk.gov.communities.prsdb.webapp.journeys.doesNotHaveOutcome
import uk.gov.communities.prsdb.webapp.journeys.hasOutcome
import uk.gov.communities.prsdb.webapp.journeys.isComplete
import uk.gov.communities.prsdb.webapp.journeys.shared.states.AddressState
import uk.gov.communities.prsdb.webapp.journeys.shared.stepConfig.LookupAddressMode
import uk.gov.communities.prsdb.webapp.journeys.shared.stepConfig.LookupAddressStep
import uk.gov.communities.prsdb.webapp.journeys.shared.stepConfig.LookupAddressStepConfig
import uk.gov.communities.prsdb.webapp.journeys.shared.stepConfig.ManualAddressStep
import uk.gov.communities.prsdb.webapp.journeys.shared.stepConfig.NoAddressFoundStep
import uk.gov.communities.prsdb.webapp.journeys.shared.stepConfig.SelectAddressMode
import uk.gov.communities.prsdb.webapp.journeys.shared.stepConfig.SelectAddressStep
import uk.gov.communities.prsdb.webapp.models.dataModels.AddressDataModel

// A generic address task that owns its own four steps and IS its own route-scoped AddressState, so it can be added
// to a journey more than once (each instance isolated by its route). The self-stated / route-scoping machinery
// lives in SelfStatedRoutableTask; this class supplies the address steps, the AddressState data and the step flow.
//
// Structure only: makeSubJourney defines the step flow; content is left abstract. Content-specific subclasses
// (e.g. LandlordAddressTask, TrusteeAddressTask) supply the content hooks with their message keys.
// Genuinely instance-specific content (e.g. the update flow's submit button/warning) is still supplied where the
// task is added, by configuring the task's named steps in the routableTask DSL block.
abstract class AddressTask(
    journeyStateService: JourneyStateService,
    override val lookupAddressStep: LookupAddressStep,
    override val selectAddressStep: SelectAddressStep,
    override val noAddressFoundStep: NoAddressFoundStep,
    override val manualAddressStep: ManualAddressStep,
) : TaskWithoutDependencies<AddressState>(journeyStateService),
    AddressState {
    override var cachedAddresses: List<AddressDataModel>? by delegateProvider.nullableDelegate("cachedAddresses")
    override var cachedSelectedAddress: String? by delegateProvider.nullableDelegate("cachedSelectedAddress")
    override var isAddressAlreadyRegistered: Boolean? by delegateProvider.nullableDelegate("isAddressAlreadyRegistered")

    // Field-set content for the address steps, supplied by content-specific subclasses (e.g. LandlordAddressTask,
    // TrusteeAddressTask) and applied to the relevant steps in makeSubJourney.
    protected open val lookupAddressContentProperties: Map<String, Any?> = emptyMap()
    protected open val selectAddressContentProperties: Map<String, Any?> = emptyMap()
    protected open val manualAddressContentProperties: Map<String, Any?> = emptyMap()

    protected open val lookupAddressTemplate: String = LookupAddressStepConfig.DEFAULT_TEMPLATE

    override val taskState get() = this

    fun clearFormData() {
        lookupAddressStep.clearFormData()
        selectAddressStep.clearFormData()
        noAddressFoundStep.clearFormData()
        manualAddressStep.clearFormData()
        cachedAddresses = null
        cachedSelectedAddress = null
        isAddressAlreadyRegistered = null
    }

    override fun makeSubJourney(state: AddressState) =
        subJourney(state) {
            step<LookupAddressMode, LookupAddressStepConfig>(journey.lookupAddressStep) {
                routeSegment(LookupAddressStep.ROUTE_SEGMENT)
                nextStep { mode ->
                    when (mode) {
                        LookupAddressMode.ADDRESSES_FOUND -> journey.selectAddressStep
                        LookupAddressMode.NO_ADDRESSES_FOUND -> journey.noAddressFoundStep
                    }
                }
                stepSpecificInitialisation { withTemplate(lookupAddressTemplate) }
                withAdditionalContentProperties { lookupAddressContentProperties }
            }
            step(journey.selectAddressStep) {
                routeSegment(SelectAddressStep.ROUTE_SEGMENT)
                parents { journey.lookupAddressStep.hasOutcome(LookupAddressMode.ADDRESSES_FOUND) }
                nextStep { mode ->
                    when (mode) {
                        SelectAddressMode.MANUAL_ADDRESS -> journey.manualAddressStep
                        else -> exitStep
                    }
                }
                withAdditionalContentProperties { selectAddressContentProperties }
            }
            step(journey.noAddressFoundStep) {
                routeSegment(NoAddressFoundStep.ROUTE_SEGMENT)
                parents { journey.lookupAddressStep.hasOutcome(LookupAddressMode.NO_ADDRESSES_FOUND) }
                nextStep { journey.manualAddressStep }
            }
            step(journey.manualAddressStep) {
                routeSegment(ManualAddressStep.ROUTE_SEGMENT)
                parents {
                    OrParents(
                        journey.selectAddressStep.hasOutcome(SelectAddressMode.MANUAL_ADDRESS),
                        journey.noAddressFoundStep.isComplete(),
                    )
                }
                nextStep { exitStep }
                withAdditionalContentProperties { manualAddressContentProperties }
            }
            exitStep {
                parents {
                    OrParents(
                        journey.selectAddressStep.doesNotHaveOutcome(SelectAddressMode.MANUAL_ADDRESS),
                        journey.manualAddressStep.isComplete(),
                    )
                }
            }
        }
}
