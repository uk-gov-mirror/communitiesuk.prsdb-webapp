package uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyDeactivationJourneyPages

import com.microsoft.playwright.Page
import uk.gov.communities.prsdb.webapp.controllers.DeactivatePropertyController
import uk.gov.communities.prsdb.webapp.integration.pageObjects.components.Button
import uk.gov.communities.prsdb.webapp.integration.pageObjects.components.Link
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.basePages.BasePage
import uk.gov.communities.prsdb.webapp.journeys.propertyDeactivation.stepConfig.ConfirmDeactivationStep

class ConfirmPagePropertyDeactivation(
    page: Page,
    urlArguments: Map<String, String>,
) : BasePage(
        page,
        DeactivatePropertyController.getPropertyDeactivationBasePath(urlArguments["propertyOwnershipId"]!!.toLong()) +
            "/${ConfirmDeactivationStep.ROUTE_SEGMENT}",
    ) {
    val heading
        get() = page.locator("h1")

    val landlordAccountHeading
        get() = page.locator("h3:has-text(\"Your landlord account\")")

    val confirmButton = Button(page.locator("button.govuk-button--warning"))
    val cancelLink = Link.byText(page, "Cancel")

    fun submitConfirm() {
        confirmButton.clickAndWait()
    }
}
