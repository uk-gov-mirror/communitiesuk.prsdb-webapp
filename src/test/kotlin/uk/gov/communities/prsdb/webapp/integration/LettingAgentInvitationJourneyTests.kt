package uk.gov.communities.prsdb.webapp.integration

import com.microsoft.playwright.Page
import org.junit.jupiter.api.Test
import uk.gov.communities.prsdb.webapp.constants.DELEGATE_TO_LETTING_AGENT
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.basePages.BasePage.Companion.assertPageIs
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.lettingAgentInvitationJourneyPages.EnterPasswordPage
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.lettingAgentInvitationJourneyPages.HasPasswordPage
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.lettingAgentInvitationJourneyPages.PasswordCreationConfirmationPage
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.lettingAgentInvitationJourneyPages.SetPasswordPage
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.lettingAgentInvitationJourneyPages.StoreAccessPage
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.lettingAgentInvitationJourneyPages.ValidateTokenPage

class LettingAgentInvitationJourneyTests : IntegrationTestWithMutableData("data-local.sql") {
    private val tokenWithoutPassword = "3334abcd-5678-abcd-1234-567abcd1111a"

    private val tokenWithPassword = "3334abcd-5678-abcd-1234-567abcd1111b"

    private val seededPassword = "password1" // pragma: allowlist secret

    @Test
    fun `user who does not have a password can walk the set password journey`(page: Page) {
        featureFlagManager.enable(DELEGATE_TO_LETTING_AGENT)

        val validateTokenPage = navigator.goToLettingAgentInvitationJourney(tokenWithoutPassword)
        // TODO PDJB-1658: Update when validate token step is implemented
        assertPageIs(page, ValidateTokenPage::class)
        validateTokenPage.form.submit()

        val hasPasswordPage = assertPageIs(page, HasPasswordPage::class)
        // TODO PDJB-1658: Remove this step from the journey test
        hasPasswordPage.submitNoPassword()

        val rawPassword = "password1" // pragma: allowlist secret
        val setPasswordPage = assertPageIs(page, SetPasswordPage::class)
        setPasswordPage.submitPasswords(rawPassword, rawPassword)

        // TODO PDJB-1567: Update when password creation confirmation page is implemented
        val confirmationPage = assertPageIs(page, PasswordCreationConfirmationPage::class)
        confirmationPage.form.submit()

        // TODO PDJB-1659: Remove this step from the journey test
        val storeAccessPage = assertPageIs(page, StoreAccessPage::class)
        storeAccessPage.form.submit()

        // TODO PDJB-1570: Assert redirect to letting agent property record page
    }

    @Test
    fun `user who has a password can walk the enter password journey`(page: Page) {
        featureFlagManager.enable(DELEGATE_TO_LETTING_AGENT)

        val validateTokenPage = navigator.goToLettingAgentInvitationJourney(tokenWithPassword)
        // TODO PDJB-1658: Update when validate token step is implemented
        assertPageIs(page, ValidateTokenPage::class)
        validateTokenPage.form.submit()

        val hasPasswordPage = assertPageIs(page, HasPasswordPage::class)
        // TODO PDJB-1658: Remove this step from the journey test
        hasPasswordPage.submitHasPassword()

        val enterPasswordPage = assertPageIs(page, EnterPasswordPage::class)
        enterPasswordPage.submitPassword(seededPassword)

        // TODO PDJB-1659: Remove this step from the journey test
        val storeAccessPage = assertPageIs(page, StoreAccessPage::class)
        storeAccessPage.form.submit()

        // TODO PDJB-1570: Assert redirect to letting agent property record page
    }
}
