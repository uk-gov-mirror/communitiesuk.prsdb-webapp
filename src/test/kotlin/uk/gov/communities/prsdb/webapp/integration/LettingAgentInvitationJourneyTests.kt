package uk.gov.communities.prsdb.webapp.integration

import com.microsoft.playwright.Page
import com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.communities.prsdb.webapp.constants.DELEGATE_TO_LETTING_AGENT
import uk.gov.communities.prsdb.webapp.integration.pageObjects.components.BaseComponent
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.basePages.BasePage.Companion.assertPageIs
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.lettingAgentInvitationJourneyPages.EnterPasswordPage
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.lettingAgentInvitationJourneyPages.PasswordCreationConfirmationPage
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.lettingAgentInvitationJourneyPages.SetPasswordPage
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.lettingAgentInvitationJourneyPages.StoreAccessPage
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.lettingAgentInvitationJourneyPages.ValidateTokenPage

class LettingAgentInvitationJourneyTests : IntegrationTestWithMutableData("data-local.sql") {
    private val tokenWithoutPassword = "11111111-1111-4111-8111-111111111111"
    private val tokenWithPassword = "22222222-2222-4222-8222-222222222222"

    @Test
    fun `user who does not have a password can walk the set password journey`(page: Page) {
        featureFlagManager.enable(DELEGATE_TO_LETTING_AGENT)

        val validateTokenPage = navigator.goToLettingAgentInvitationJourney(tokenWithoutPassword)
        // TODO PDJB-1660: Update when validate token step is implemented
        assertPageIs(page, ValidateTokenPage::class)
        validateTokenPage.form.submit()

        // TODO PDJB-1566: Update when set password page is implemented
        val setPasswordPage = assertPageIs(page, SetPasswordPage::class)
        setPasswordPage.form.submit()

        val confirmationPage = assertPageIs(page, PasswordCreationConfirmationPage::class)
        BaseComponent
            .assertThat(confirmationPage.confirmationBanner)
            .containsText("Property password created")
        assertThat(confirmationPage.insetText).containsText("Check you have a copy of this link")
        // TODO PDJB-1661: Update the expected update link once the real invitation link is wired in
        assertThat(confirmationPage.updateLink.locator).hasAttribute("href", "https://example.com")
        assertThat(confirmationPage.updateLink.locator).hasText("https://example.com")
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
        // TODO PDJB-1660: Update when validate token step is implemented
        assertPageIs(page, ValidateTokenPage::class)
        validateTokenPage.form.submit()

        // TODO PDJB-1568: Update when enter password page is implemented
        val enterPasswordPage = assertPageIs(page, EnterPasswordPage::class)
        enterPasswordPage.form.submit()

        // TODO PDJB-1659: Remove this step from the journey test
        val storeAccessPage = assertPageIs(page, StoreAccessPage::class)
        storeAccessPage.form.submit()

        // TODO PDJB-1570: Assert redirect to letting agent property record page
    }
}
