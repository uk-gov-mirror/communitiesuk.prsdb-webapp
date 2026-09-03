package uk.gov.communities.prsdb.webapp.integration

import com.microsoft.playwright.Page
import com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.crypto.password.PasswordEncoder
import uk.gov.communities.prsdb.webapp.constants.DELEGATE_TO_LETTING_AGENT
import uk.gov.communities.prsdb.webapp.database.repository.LettingAgentAccessRepository
import uk.gov.communities.prsdb.webapp.integration.pageObjects.components.BaseComponent
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.basePages.BasePage.Companion.assertPageIs
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.lettingAgentInvitationJourneyPages.EnterPasswordPage
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.lettingAgentInvitationJourneyPages.PasswordCreationConfirmationPage
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.lettingAgentInvitationJourneyPages.SetPasswordPage
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.lettingAgentInvitationJourneyPages.StoreAccessPage
import java.util.UUID

class LettingAgentInvitationSinglePageTests : IntegrationTestWithMutableData("data-local.sql") {
    @Autowired
    private lateinit var lettingAgentAccessRepository: LettingAgentAccessRepository

    @Autowired
    private lateinit var passwordEncoder: PasswordEncoder

    private val validToken = "3334abcd-5678-abcd-1234-567abcd1111a"

    private val existingPassword = "password1" // pragma: allowlist secret

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
            assertThat(setPasswordPage.errorSummary.errorLinks("Enter the same password in both fields")).hasCount(1)
        }

        @Test
        fun `submitting matching valid passwords proceeds to confirmation`(page: Page) {
            val setPasswordPage = navigator.skipToLettingAgentInvitationSetPasswordPage(validToken)

            setPasswordPage.submitPasswords("password1", "password1")

            assertPageIs(page, PasswordCreationConfirmationPage::class)
        }
    }

    @Nested
    inner class EnterPasswordContent {
        @BeforeEach
        fun setPassword() = setPasswordForInvitation()

        @Test
        fun `the page shows the property address and guidance on getting a new password`() {
            val enterPasswordPage = navigator.skipToLettingAgentInvitationEnterPasswordPage(validToken)

            BaseComponent.assertThat(enterPasswordPage.heading).containsText("Enter the password for this property")
            assertThat(enterPasswordPage.details).containsText("I do not know this password")
            assertThat(enterPasswordPage.details).containsText("ask the landlord to send you a new link")
        }

        @Test
        fun `the password input is masked and has a show toggle`() {
            val enterPasswordPage = navigator.skipToLettingAgentInvitationEnterPasswordPage(validToken)

            assertThat(enterPasswordPage.passwordInput).hasAttribute("type", "password")
            assertThat(enterPasswordPage.showPasswordButton).hasText("Show")
        }
    }

    @Nested
    inner class EnterPasswordValidation {
        @BeforeEach
        fun setPassword() = setPasswordForInvitation()

        @Test
        fun `submitting a blank password shows a required error message`(page: Page) {
            val enterPasswordPage = navigator.skipToLettingAgentInvitationEnterPasswordPage(validToken)

            enterPasswordPage.form.submit()

            assertPageIs(page, EnterPasswordPage::class)
            BaseComponent.assertThat(enterPasswordPage.errorSummary).containsText("Enter your password")
        }

        @Test
        fun `submitting an incorrect password shows an incorrect password error`(page: Page) {
            val enterPasswordPage = navigator.skipToLettingAgentInvitationEnterPasswordPage(validToken)

            enterPasswordPage.submitPassword("wrongPassword")

            assertPageIs(page, EnterPasswordPage::class)
            BaseComponent.assertThat(enterPasswordPage.errorSummary).containsText("The password you entered is not correct")
        }

        @Test
        fun `submitting an incorrect password does not echo the entered password back to the page`(page: Page) {
            val enterPasswordPage = navigator.skipToLettingAgentInvitationEnterPasswordPage(validToken)

            enterPasswordPage.submitPassword("wrongPassword")

            assertPageIs(page, EnterPasswordPage::class)
            assertThat(enterPasswordPage.passwordInput).hasValue("")
        }

        @Test
        fun `submitting the correct password proceeds to the next step`(page: Page) {
            val enterPasswordPage = navigator.skipToLettingAgentInvitationEnterPasswordPage(validToken)

            enterPasswordPage.submitPassword(existingPassword)

            // TODO PDJB-1659: Assert the letting agent's access has been stored
            assertPageIs(page, StoreAccessPage::class)
        }
    }

    private fun setPasswordForInvitation() {
        val invitation = lettingAgentAccessRepository.findByToken(UUID.fromString(validToken))!!
        lettingAgentAccessRepository.setEncodedPasswordIfAbsent(invitation.id, passwordEncoder.encode(existingPassword))
    }
}
