package uk.gov.communities.prsdb.webapp.models.requestModels.formModels

import jakarta.validation.constraints.NotNull
import uk.gov.communities.prsdb.webapp.constants.enums.CorrespondenceEmailOption
import uk.gov.communities.prsdb.webapp.exceptions.NotNullFormModelValueIsNullException.Companion.notNullValue
import uk.gov.communities.prsdb.webapp.validation.ConstraintDescriptor
import uk.gov.communities.prsdb.webapp.validation.DelegatedPropertyConstraintValidator
import uk.gov.communities.prsdb.webapp.validation.EmailConstraintValidator
import uk.gov.communities.prsdb.webapp.validation.IsValidPrioritised
import uk.gov.communities.prsdb.webapp.validation.ValidatedBy

@IsValidPrioritised
class CorrespondenceEmailFormModel : FormModel {
    @NotNull(message = "registerProperty.correspondenceEmail.radios.error.missing")
    var whichEmail: CorrespondenceEmailOption? = null

    @ValidatedBy(
        constraints = [
            ConstraintDescriptor(
                messageKey = "registerProperty.correspondenceEmail.radios.option.differentEmail.input.error.missing",
                validatorType = DelegatedPropertyConstraintValidator::class,
                targetMethod = "isDifferentEmailAddressPresentIfSelected",
            ),
            ConstraintDescriptor(
                messageKey = "registerProperty.correspondenceEmail.radios.option.differentEmail.input.error.invalidFormat",
                validatorType = DelegatedPropertyConstraintValidator::class,
                targetMethod = "isDifferentEmailAddressValidFormatIfPresent",
            ),
        ],
    )
    var differentEmailAddress: String = ""

    fun getEmailAddress(accountEmail: () -> String): String =
        when (notNullValue(CorrespondenceEmailFormModel::whichEmail)) {
            CorrespondenceEmailOption.ACCOUNT_EMAIL -> accountEmail()
            CorrespondenceEmailOption.DIFFERENT_EMAIL -> differentEmailAddress
        }

    fun isDifferentEmailAddressPresentIfSelected(): Boolean =
        whichEmail != CorrespondenceEmailOption.DIFFERENT_EMAIL || differentEmailAddress.isNotBlank()

    fun isDifferentEmailAddressValidFormatIfPresent(): Boolean =
        whichEmail != CorrespondenceEmailOption.DIFFERENT_EMAIL ||
            differentEmailAddress.isBlank() ||
            EmailConstraintValidator().isValid(differentEmailAddress)
}
