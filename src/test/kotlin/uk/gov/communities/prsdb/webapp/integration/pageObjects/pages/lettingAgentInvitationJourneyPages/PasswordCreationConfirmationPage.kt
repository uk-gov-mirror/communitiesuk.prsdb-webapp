package uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.lettingAgentInvitationJourneyPages

import com.microsoft.playwright.Page
import uk.gov.communities.prsdb.webapp.integration.pageObjects.components.BackLink
import uk.gov.communities.prsdb.webapp.integration.pageObjects.components.ConfirmationBanner
import uk.gov.communities.prsdb.webapp.integration.pageObjects.components.Form
import uk.gov.communities.prsdb.webapp.integration.pageObjects.components.Link
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.basePages.BasePage
import uk.gov.communities.prsdb.webapp.journeys.lettingAgentInvitation.steps.ConfirmationStep

class PasswordCreationConfirmationPage(
    page: Page,
) : BasePage(page, "/${ConfirmationStep.ROUTE_SEGMENT}") {
    val confirmationBanner = ConfirmationBanner(page)
    val updateLink = Link(page.locator("#update-link"))
    val insetText = page.locator(".govuk-inset-text")
    val backLink = BackLink.default(page)
    val form = Form(page)
}
