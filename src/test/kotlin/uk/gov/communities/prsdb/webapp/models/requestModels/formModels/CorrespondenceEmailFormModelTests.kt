package uk.gov.communities.prsdb.webapp.models.requestModels.formModels

import jakarta.validation.Validation
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import uk.gov.communities.prsdb.webapp.constants.enums.CorrespondenceEmailOption
import uk.gov.communities.prsdb.webapp.exceptions.NotNullFormModelValueIsNullException

class CorrespondenceEmailFormModelTests {
    private val validator = Validation.buildDefaultValidatorFactory().validator

    @Test
    fun `getEmailAddress uses account email instead of a retained different email`() {
        val model =
            CorrespondenceEmailFormModel().apply {
                correspondenceEmailOption = CorrespondenceEmailOption.ACCOUNT_EMAIL
                differentEmailAddress = "previous@example.com"
            }

        assertThat(model.getEmailAddress { "account@example.com" }).isEqualTo("account@example.com")
    }

    @Test
    fun `getEmailAddress uses the different email without resolving the account email`() {
        val model =
            CorrespondenceEmailFormModel().apply {
                correspondenceEmailOption = CorrespondenceEmailOption.DIFFERENT_EMAIL
                differentEmailAddress = "chosen@example.com"
            }

        assertThat(model.getEmailAddress { error("Account email must not be read") }).isEqualTo("chosen@example.com")
    }

    @Test
    fun `getEmailAddress rejects a missing choice instead of substituting contact data`() {
        assertThrows<NotNullFormModelValueIsNullException> {
            CorrespondenceEmailFormModel().getEmailAddress { "account@example.com" }
        }
    }

    @Test
    fun `is invalid when no option is selected`() {
        val model = CorrespondenceEmailFormModel()

        val violations = validator.validate(model)

        assertThat(violations).hasSize(1)
        val violation = violations.single()
        assertThat(violation.propertyPath.toString()).isEqualTo("correspondenceEmailOption")
        assertThat(violation.messageTemplate).isEqualTo("registerProperty.correspondenceEmail.radios.error.missing")
    }

    @Test
    fun `is valid when account email is selected and no different email is entered`() {
        val model =
            CorrespondenceEmailFormModel().apply {
                correspondenceEmailOption = CorrespondenceEmailOption.ACCOUNT_EMAIL
            }

        val violations = validator.validate(model)

        assertThat(violations).isEmpty()
    }

    @Test
    fun `is valid when account email is selected and a stale invalid different email is present`() {
        val model =
            CorrespondenceEmailFormModel().apply {
                correspondenceEmailOption = CorrespondenceEmailOption.ACCOUNT_EMAIL
                differentEmailAddress = "not-an-email"
            }

        val violations = validator.validate(model)

        assertThat(violations).isEmpty()
    }

    @Test
    fun `is invalid when different email is selected but no email is entered`() {
        val model =
            CorrespondenceEmailFormModel().apply {
                correspondenceEmailOption = CorrespondenceEmailOption.DIFFERENT_EMAIL
                differentEmailAddress = ""
            }

        val violations = validator.validate(model)

        assertThat(violations).hasSize(1)
        val violation = violations.single()
        assertThat(violation.propertyPath.toString()).isEqualTo("differentEmailAddress")
        assertThat(violation.messageTemplate)
            .isEqualTo("registerProperty.correspondenceEmail.radios.option.differentEmail.input.error.missing")
    }

    @Test
    fun `is invalid when different email is selected and email is not a valid format`() {
        val model =
            CorrespondenceEmailFormModel().apply {
                correspondenceEmailOption = CorrespondenceEmailOption.DIFFERENT_EMAIL
                differentEmailAddress = "not-an-email"
            }

        val violations = validator.validate(model)

        assertThat(violations).hasSize(1)
        val violation = violations.single()
        assertThat(violation.propertyPath.toString()).isEqualTo("differentEmailAddress")
        assertThat(violation.messageTemplate)
            .isEqualTo("registerProperty.correspondenceEmail.radios.option.differentEmail.input.error.invalidFormat")
    }

    @Test
    fun `is valid when different email is selected and a valid email is entered`() {
        val model =
            CorrespondenceEmailFormModel().apply {
                correspondenceEmailOption = CorrespondenceEmailOption.DIFFERENT_EMAIL
                differentEmailAddress = "someone@example.com"
            }

        val violations = validator.validate(model)

        assertThat(violations).isEmpty()
    }
}
