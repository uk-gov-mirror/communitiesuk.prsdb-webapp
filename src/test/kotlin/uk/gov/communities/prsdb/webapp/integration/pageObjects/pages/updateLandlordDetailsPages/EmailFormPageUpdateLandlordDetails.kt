package uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.updateLandlordDetailsPages

import com.microsoft.playwright.Page
import uk.gov.communities.prsdb.webapp.controllers.UpdateLandlordEmailController.Companion.UPDATE_EMAIL_ROUTE
import uk.gov.communities.prsdb.webapp.integration.pageObjects.components.InsetText
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.basePages.EmailFormPage
import uk.gov.communities.prsdb.webapp.journeys.landlordRegistration.stepConfig.EmailStep

class EmailFormPageUpdateLandlordDetails(
    page: Page,
) : EmailFormPage(page, "$UPDATE_EMAIL_ROUTE/${EmailStep.ROUTE_SEGMENT}") {
    val insetText = InsetText(page)
}
