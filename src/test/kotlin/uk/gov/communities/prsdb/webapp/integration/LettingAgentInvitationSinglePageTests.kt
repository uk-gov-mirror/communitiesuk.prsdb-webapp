package uk.gov.communities.prsdb.webapp.integration

import com.microsoft.playwright.Page
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.communities.prsdb.webapp.constants.DELEGATE_TO_LETTING_AGENT
import uk.gov.communities.prsdb.webapp.integration.pageObjects.components.BaseComponent
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.basePages.BasePage.Companion.assertPageIs
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.lettingAgentInvitationJourneyPages.PasswordCreationConfirmationPage
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.lettingAgentInvitationJourneyPages.SetPasswordPage

class LettingAgentInvitationSinglePageTests : IntegrationTestWithMutableData("data-local.sql") {
    private val validToken = "3334abcd-5678-abcd-1234-567abcd1111a"

    @BeforeEach
    fun enableFeatureFlag() {
        featureFlagManager.enable(DELEGATE_TO_LETTING_AGENT)
    }

    @Nested
    inner class SetPasswordValidation {
        @Test
        fun `submitting blank passwords shows required error messages`(page: Page) {
            val setPasswordPage = navigator.skipToLettingAgentInvitationSetPasswordPage(validToken)

            setPasswordPage.form.submit()

            assertPageIs(page, SetPasswordPage::class)
            BaseComponent.assertThat(setPasswordPage.errorSummary).containsText("Enter a password")
            BaseComponent.assertThat(setPasswordPage.errorSummary).containsText("Re-type your password")
        }

        @Test
        fun `submitting a weak password shows strength error`(page: Page) {
            val setPasswordPage = navigator.skipToLettingAgentInvitationSetPasswordPage(validToken)

            setPasswordPage.submitPasswords("short", "short")

            assertPageIs(page, SetPasswordPage::class)
            BaseComponent
                .assertThat(setPasswordPage.errorSummary)
                .containsText("Password must be at least 8 characters and include letters and numbers")
        }

        @Test
        fun `submitting mismatched passwords shows mismatch error`(page: Page) {
            val setPasswordPage = navigator.skipToLettingAgentInvitationSetPasswordPage(validToken)

            setPasswordPage.submitPasswords("password1", "password2")

            assertPageIs(page, SetPasswordPage::class)
            BaseComponent.assertThat(setPasswordPage.errorSummary).containsText("Enter the same password in both fields")
        }

        @Test
        fun `submitting matching valid passwords proceeds to confirmation`(page: Page) {
            val setPasswordPage = navigator.skipToLettingAgentInvitationSetPasswordPage(validToken)

            setPasswordPage.submitPasswords("password1", "password1")

            assertPageIs(page, PasswordCreationConfirmationPage::class)
        }
    }
}
