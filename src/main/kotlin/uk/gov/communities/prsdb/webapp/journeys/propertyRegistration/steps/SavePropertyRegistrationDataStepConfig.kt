package uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps

import jakarta.persistence.EntityExistsException
import uk.gov.communities.prsdb.webapp.annotations.webAnnotations.JourneyFrameworkComponent
import uk.gov.communities.prsdb.webapp.exceptions.CyaDataHasChangedException
import uk.gov.communities.prsdb.webapp.journeys.AbstractInternalStepConfig
import uk.gov.communities.prsdb.webapp.journeys.Destination
import uk.gov.communities.prsdb.webapp.journeys.JourneyStep
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.PropertyRegistrationDataModelFactory
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.PropertyRegistrationJourneyState
import uk.gov.communities.prsdb.webapp.journeys.shared.Complete
import uk.gov.communities.prsdb.webapp.services.PropertyRegistrationService

@JourneyFrameworkComponent
class SavePropertyRegistrationDataStepConfig(
    private val propertyRegistrationService: PropertyRegistrationService,
    private val propertyRegistrationDataModelFactory: PropertyRegistrationDataModelFactory,
) : AbstractInternalStepConfig<Complete, PropertyRegistrationJourneyState>() {
    override fun mode(state: PropertyRegistrationJourneyState): Complete = Complete.COMPLETE

    override fun afterStepIsReached(state: PropertyRegistrationJourneyState) {
        if (state.answersLastChanged != state.answersConfirmedAt) {
            throw CyaDataHasChangedException("Answers changed after confirmation for journey ${state.journeyId}")
        }
        try {
            propertyRegistrationService.registerProperty(propertyRegistrationDataModelFactory.fromJourneyState(state))
        } catch (_: EntityExistsException) {
            state.propertyDetailsTask.addressTask.isAddressAlreadyRegistered = true
            return
        }
    }

    override fun resolveNextDestination(
        state: PropertyRegistrationJourneyState,
        defaultDestination: Destination,
    ): Destination =
        if (state.propertyDetailsTask.addressTask.isAddressAlreadyRegistered == true) {
            Destination(state.propertyDetailsTask.addressTask.alreadyRegisteredStep)
        } else {
            state.deleteJourney()
            defaultDestination
        }
}

@JourneyFrameworkComponent
class SavePropertyRegistrationDataStep(
    stepConfig: SavePropertyRegistrationDataStepConfig,
) : JourneyStep.InternalStep<Complete, PropertyRegistrationJourneyState>(stepConfig)
