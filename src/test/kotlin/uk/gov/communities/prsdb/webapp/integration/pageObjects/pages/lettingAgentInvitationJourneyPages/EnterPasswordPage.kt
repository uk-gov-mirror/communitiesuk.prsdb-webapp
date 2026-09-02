package uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.lettingAgentInvitationJourneyPages

import com.microsoft.playwright.Page
import uk.gov.communities.prsdb.webapp.integration.pageObjects.components.ErrorSummary
import uk.gov.communities.prsdb.webapp.integration.pageObjects.components.Form
import uk.gov.communities.prsdb.webapp.integration.pageObjects.components.Heading
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.basePages.BasePage
import uk.gov.communities.prsdb.webapp.journeys.lettingAgentInvitation.steps.EnterPasswordStep

class EnterPasswordPage(
    page: Page,
) : BasePage(page, "/${EnterPasswordStep.ROUTE_SEGMENT}") {
    val form = Form(page)
    val errorSummary = ErrorSummary(page)
    val heading = Heading(page.locator("h1.govuk-heading-l"))
    val passwordInput = page.locator("input[name='password']")
    val showPasswordButton = page.locator("button.govuk-password-input__toggle")
    val details = page.locator("details.govuk-details")

    fun submitPassword(password: String) {
        passwordInput.fill(password)
        form.submit()
    }
}
