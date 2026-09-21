package uk.gov.communities.prsdb.webapp.journeys.shared.tasks

import uk.gov.communities.prsdb.webapp.annotations.webAnnotations.JourneyFrameworkComponent
import uk.gov.communities.prsdb.webapp.journeys.JourneyStateService
import uk.gov.communities.prsdb.webapp.journeys.shared.stepConfig.LookupAddressStep
import uk.gov.communities.prsdb.webapp.journeys.shared.stepConfig.ManualAddressStep
import uk.gov.communities.prsdb.webapp.journeys.shared.stepConfig.NoAddressFoundStep
import uk.gov.communities.prsdb.webapp.journeys.shared.stepConfig.SelectAddressStep

@JourneyFrameworkComponent
class CorrespondenceAddressTask(
    journeyStateService: JourneyStateService,
    lookupAddressStep: LookupAddressStep,
    selectAddressStep: SelectAddressStep,
    noAddressFoundStep: NoAddressFoundStep,
    manualAddressStep: ManualAddressStep,
) : AddressTask(
        journeyStateService,
        lookupAddressStep,
        selectAddressStep,
        noAddressFoundStep,
        manualAddressStep,
    ) {
    override val lookupAddressTemplate = "forms/correspondenceLookupAddressForm"

    override val selectAddressContentProperties: Map<String, Any?> =
        mapOf(
            "fieldSetHeading" to "addressForms.selectAddress.correspondence.fieldSetHeading",
        )

    override val manualAddressContentProperties: Map<String, Any?> =
        mapOf(
            "fieldSetHeading" to "addressForms.manualAddress.correspondence.fieldSetHeading",
            "fieldSetHint" to null,
            "showCorrespondenceInset" to true,
        )

    companion object {
        const val ROUTE_SEGMENT = "correspondence-address"
    }
}
