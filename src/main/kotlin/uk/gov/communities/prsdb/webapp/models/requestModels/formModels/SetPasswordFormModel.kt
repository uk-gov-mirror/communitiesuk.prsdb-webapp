package uk.gov.communities.prsdb.webapp.models.requestModels.formModels

import uk.gov.communities.prsdb.webapp.validation.ConstraintDescriptor
import uk.gov.communities.prsdb.webapp.validation.IsValidPrioritised
import uk.gov.communities.prsdb.webapp.validation.LengthConstraintValidator
import uk.gov.communities.prsdb.webapp.validation.NotBlankConstraintValidator
import uk.gov.communities.prsdb.webapp.validation.PasswordConstraintValidator
import uk.gov.communities.prsdb.webapp.validation.ValidatedBy

@IsValidPrioritised
class SetPasswordFormModel : FormModel {
    @ValidatedBy(
        constraints = [
            ConstraintDescriptor(
                messageKey = "lettingAgentInvitation.setPassword.password.error.missing",
                validatorType = NotBlankConstraintValidator::class,
            ),
            ConstraintDescriptor(
                messageKey = "lettingAgentInvitation.setPassword.password.error.invalid",
                validatorType = PasswordConstraintValidator::class,
            ),
            ConstraintDescriptor(
                messageKey = "lettingAgentInvitation.setPassword.password.error.invalid",
                validatorType = LengthConstraintValidator::class,
                validatorArgs = ["0", "255"],
            ),
        ],
    )
    var password: String = ""

    @ValidatedBy(
        constraints = [
            ConstraintDescriptor(
                messageKey = "lettingAgentInvitation.setPassword.confirmPassword.error.missing",
                validatorType = NotBlankConstraintValidator::class,
            ),
        ],
    )
    var confirmPassword: String = ""
}
