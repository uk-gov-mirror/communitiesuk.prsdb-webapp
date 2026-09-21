package uk.gov.communities.prsdb.webapp.journeys.shared.stepConfig

import uk.gov.communities.prsdb.webapp.annotations.webAnnotations.JourneyFrameworkComponent
import uk.gov.communities.prsdb.webapp.constants.FORM_MODEL_ATTR_NAME
import uk.gov.communities.prsdb.webapp.exceptions.NotNullFormModelValueIsNullException.Companion.notNullValue
import uk.gov.communities.prsdb.webapp.journeys.AbstractRequestableStepConfig
import uk.gov.communities.prsdb.webapp.journeys.JourneyStep.RequestableStep
import uk.gov.communities.prsdb.webapp.journeys.shared.states.AddressSearchState
import uk.gov.communities.prsdb.webapp.models.requestModels.formModels.LookupAddressFormModel
import uk.gov.communities.prsdb.webapp.services.AddressService

@JourneyFrameworkComponent
class LookupAddressStepConfig(
    private val addressService: AddressService,
) : AbstractRequestableStepConfig<LookupAddressMode, LookupAddressFormModel, AddressSearchState>() {
    override val formModelClass = LookupAddressFormModel::class

    override fun getStepSpecificContent(state: AddressSearchState) =
        mapOf(
            "postcodeLabel" to "addressForms.lookupAddress.postcode.label",
            "postcodeHint" to "addressForms.lookupAddress.postcode.hint",
            "houseNameOrNumberLabel" to "addressForms.lookupAddress.houseNameOrNumber.label",
            "houseNameOrNumberHint" to "addressForms.lookupAddress.houseNameOrNumber.hint",
            "submitButtonText" to "forms.buttons.findAddress",
        )

    override fun chooseTemplate(state: AddressSearchState) = template

    override fun resolvePageContent(
        state: AddressSearchState,
        defaultContent: Map<String, Any?>,
    ): Map<String, Any?> {
        val prefillPostcode = defaultContent[PREFILL_POSTCODE] as? String ?: return defaultContent
        val formModel = defaultContent[FORM_MODEL_ATTR_NAME] as? LookupAddressFormModel ?: return defaultContent
        if (!formModel.postcode.isNullOrBlank()) return defaultContent
        formModel.postcode = prefillPostcode
        formModel.houseNameOrNumber = defaultContent[PREFILL_HOUSE_NAME_OR_NUMBER] as? String
        return defaultContent + (FORM_MODEL_ATTR_NAME to formModel)
    }

    companion object {
        const val DEFAULT_TEMPLATE = "forms/lookupAddressForm"
        const val PREFILL_POSTCODE = "lookupPrefillPostcode"
        const val PREFILL_HOUSE_NAME_OR_NUMBER = "lookupPrefillHouseNameOrNumber"
    }

    override fun mode(state: AddressSearchState) =
        state.cachedAddresses?.let {
            when (it.isEmpty()) {
                true -> LookupAddressMode.NO_ADDRESSES_FOUND
                false -> LookupAddressMode.ADDRESSES_FOUND
            }
        }

    override fun afterStepDataIsAdded(state: AddressSearchState) {
        val formModel = getFormModelFromState(state)
        val houseNameOrNumber = formModel.notNullValue(LookupAddressFormModel::houseNameOrNumber)
        val postcode = formModel.notNullValue(LookupAddressFormModel::postcode)
        state.cachedAddresses = addressService.searchForAddresses(houseNameOrNumber, postcode, restrictToEngland)
    }

    private var restrictToEngland: Boolean = false

    fun restrictToEngland(): LookupAddressStepConfig {
        this.restrictToEngland = true
        return this
    }

    private var template: String = DEFAULT_TEMPLATE

    fun withTemplate(template: String): LookupAddressStepConfig {
        this.template = template
        return this
    }
}

@JourneyFrameworkComponent
final class LookupAddressStep(
    stepConfig: LookupAddressStepConfig,
) : RequestableStep<LookupAddressMode, LookupAddressFormModel, AddressSearchState>(stepConfig) {
    companion object {
        const val ROUTE_SEGMENT = "lookup-address"
    }
}

enum class LookupAddressMode {
    ADDRESSES_FOUND,
    NO_ADDRESSES_FOUND,
}
