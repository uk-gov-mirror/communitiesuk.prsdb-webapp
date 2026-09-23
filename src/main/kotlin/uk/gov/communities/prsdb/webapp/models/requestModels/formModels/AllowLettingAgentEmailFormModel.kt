package uk.gov.communities.prsdb.webapp.models.requestModels.formModels

import uk.gov.communities.prsdb.webapp.helpers.extensions.StringExtensions.Companion.isSameEmailAs
import uk.gov.communities.prsdb.webapp.validation.ConstraintDescriptor
import uk.gov.communities.prsdb.webapp.validation.DelegatedPropertyConstraintValidator
import uk.gov.communities.prsdb.webapp.validation.EmailConstraintValidator
import uk.gov.communities.prsdb.webapp.validation.IsValidPrioritised
import uk.gov.communities.prsdb.webapp.validation.NotBlankConstraintValidator
import uk.gov.communities.prsdb.webapp.validation.ValidatedBy

@IsValidPrioritised
class AllowLettingAgentEmailFormModel : FormModel {
    var landlordEmailAtStartOfJourney: String? = null

    @ValidatedBy(
        constraints = [
            ConstraintDescriptor(
                messageKey = "delegateToLettingAgent.allowLettingAgent.error.missing",
                validatorType = NotBlankConstraintValidator::class,
            ),
            ConstraintDescriptor(
                messageKey = "delegateToLettingAgent.allowLettingAgent.error.invalidFormat",
                validatorType = EmailConstraintValidator::class,
            ),
            ConstraintDescriptor(
                messageKey = "delegateToLettingAgent.allowLettingAgent.error.cannotUseLandlordEmail",
                validatorType = DelegatedPropertyConstraintValidator::class,
                targetMethod = "isEmailNotLandlord",
            ),
        ],
    )
    var emailAddress: String? = null

    fun isEmailNotLandlord(): Boolean {
        val submittedEmail = emailAddress ?: return true
        return !submittedEmail.isSameEmailAs(landlordEmailAtStartOfJourney)
    }
}
