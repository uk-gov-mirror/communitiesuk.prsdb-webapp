package uk.gov.communities.prsdb.webapp.models.requestModels.formModels

import jakarta.validation.constraints.NotNull
import uk.gov.communities.prsdb.webapp.journeys.shared.YesOrNo
import uk.gov.communities.prsdb.webapp.validation.IsValidPrioritised

// TODO PDJB-1983: replace with the real "Are you sure you want to remove (property manager)?" form model
@IsValidPrioritised
class TemporaryPropertyManagerYesOrNoFormModel : FormModel {
    @NotNull(message = "registerProperty.propertyManager.todo.radios.error.missing")
    var answer: Boolean? = null

    fun toYesOrNo(): YesOrNo? = answer?.let { if (it) YesOrNo.YES else YesOrNo.NO }
}
