package uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.lettingAgentInvitationJourneyPages

import com.microsoft.playwright.Page
import uk.gov.communities.prsdb.webapp.integration.pageObjects.components.ErrorSummary
import uk.gov.communities.prsdb.webapp.integration.pageObjects.components.Form
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.basePages.BasePage
import uk.gov.communities.prsdb.webapp.journeys.lettingAgentInvitation.steps.SetPasswordStep

class SetPasswordPage(
    page: Page,
) : BasePage(page, "/${SetPasswordStep.ROUTE_SEGMENT}") {
    val form = Form(page)
    val errorSummary = ErrorSummary(page)
    val passwordInput = page.locator("input[name='password']")
    val confirmPasswordInput = page.locator("input[name='confirmPassword']")

    fun submitPasswords(
        password: String,
        confirmPassword: String,
    ) {
        passwordInput.fill(password)
        confirmPasswordInput.fill(confirmPassword)
        form.submit()
    }
}
