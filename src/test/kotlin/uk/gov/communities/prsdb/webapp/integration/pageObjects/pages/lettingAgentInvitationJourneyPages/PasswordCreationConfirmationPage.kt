package uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.lettingAgentInvitationJourneyPages

import com.microsoft.playwright.Page
import uk.gov.communities.prsdb.webapp.integration.pageObjects.components.Form
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.basePages.BasePage
import uk.gov.communities.prsdb.webapp.journeys.lettingAgentInvitation.steps.ConfirmationStep

// TODO PDJB-1567: Page object is intentionally minimal for now. Extend with content assertions (banner,
//  update link, password row) once the upstream stub steps can drive real data onto this page.
class PasswordCreationConfirmationPage(
    page: Page,
) : BasePage(page, "/${ConfirmationStep.ROUTE_SEGMENT}") {
    val form = Form(page)
}
