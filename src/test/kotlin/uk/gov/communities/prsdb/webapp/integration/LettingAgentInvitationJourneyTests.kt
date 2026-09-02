package uk.gov.communities.prsdb.webapp.integration

import com.microsoft.playwright.Page
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.crypto.password.PasswordEncoder
import uk.gov.communities.prsdb.webapp.constants.DELEGATE_TO_LETTING_AGENT
import uk.gov.communities.prsdb.webapp.controllers.LettingAgentInvitationController
import uk.gov.communities.prsdb.webapp.database.repository.LettingAgentAccessRepository
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.basePages.BasePage.Companion.assertPageIs
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.lettingAgentInvitationJourneyPages.EnterPasswordPage
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.lettingAgentInvitationJourneyPages.HasPasswordPage
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.lettingAgentInvitationJourneyPages.PasswordCreationConfirmationPage
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.lettingAgentInvitationJourneyPages.SetPasswordPage
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.lettingAgentInvitationJourneyPages.StoreAccessPage
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.lettingAgentInvitationJourneyPages.ValidateTokenPage
import uk.gov.communities.prsdb.webapp.journeys.lettingAgentInvitation.steps.StoreAccessStep
import java.util.UUID

class LettingAgentInvitationJourneyTests : IntegrationTestWithMutableData("data-local.sql") {
    @Autowired
    private lateinit var lettingAgentAccessRepository: LettingAgentAccessRepository

    @Autowired
    private lateinit var passwordEncoder: PasswordEncoder

    private val validToken = "3334abcd-5678-abcd-1234-567abcd1111a"

    @Test
    fun `user who does not have a password can walk the set password journey`(page: Page) {
        featureFlagManager.enable(DELEGATE_TO_LETTING_AGENT)

        val validateTokenPage = navigator.goToLettingAgentInvitationJourney(validToken)
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
        val rawPassword = "password1" // pragma: allowlist secret
        setPasswordForInvitation(rawPassword)

        val enterPasswordPage = goToEnterPasswordPage(page)
        enterPasswordPage.submitPassword(rawPassword)

        // TODO PDJB-1659: Remove this step from the journey test
        val storeAccessPage = assertPageIs(page, StoreAccessPage::class)
        storeAccessPage.form.submit()

        // TODO PDJB-1570: Assert redirect to letting agent property record page
    }

    @Test
    fun `navigating directly to store access without entering the password does not grant access`(page: Page) {
        featureFlagManager.enable(DELEGATE_TO_LETTING_AGENT)
        setPasswordForInvitation("password1")

        goToEnterPasswordPage(page)
        val journeyId = page.url().substringAfter("journeyId=")

        navigator.navigate(
            "${LettingAgentInvitationController.LETTING_AGENT_INVITATION_ROUTE}/${StoreAccessStep.ROUTE_SEGMENT}" +
                "?journeyId=$journeyId",
        )

        assertFalse(page.url().contains(StoreAccessStep.ROUTE_SEGMENT))
    }

    private fun setPasswordForInvitation(rawPassword: String) {
        val invitation = lettingAgentAccessRepository.findByToken(UUID.fromString(validToken))!!
        lettingAgentAccessRepository.setEncodedPasswordIfAbsent(invitation.id, passwordEncoder.encode(rawPassword))
    }

    private fun goToEnterPasswordPage(page: Page): EnterPasswordPage {
        val validateTokenPage = navigator.goToLettingAgentInvitationJourney(validToken)
        // TODO PDJB-1658: Update when validate token step is implemented
        assertPageIs(page, ValidateTokenPage::class)
        validateTokenPage.form.submit()

        val hasPasswordPage = assertPageIs(page, HasPasswordPage::class)
        // TODO PDJB-1658: Remove this step from the journey test
        hasPasswordPage.submitHasPassword()

        return assertPageIs(page, EnterPasswordPage::class)
    }
}
