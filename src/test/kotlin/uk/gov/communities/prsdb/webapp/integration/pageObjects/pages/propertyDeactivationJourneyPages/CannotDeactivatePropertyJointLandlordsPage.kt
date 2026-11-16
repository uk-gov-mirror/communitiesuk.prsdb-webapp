package uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyDeactivationJourneyPages

import com.microsoft.playwright.Page
import uk.gov.communities.prsdb.webapp.controllers.DeactivatePropertyController
import uk.gov.communities.prsdb.webapp.integration.pageObjects.components.Link
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.basePages.BasePage
import uk.gov.communities.prsdb.webapp.journeys.propertyDeactivation.stepConfig.CannotDeactivateStep

class CannotDeactivatePropertyJointLandlordsPage(
    page: Page,
    urlArguments: Map<String, String>,
) : BasePage(
        page,
        DeactivatePropertyController.getPropertyDeactivationBasePath(urlArguments["propertyOwnershipId"]!!.toLong()) +
            "/${CannotDeactivateStep.ROUTE_SEGMENT}",
    ) {
    val heading
        get() = page.locator("h1")

    val leavePropertyLink = Link.byText(page, "I’m no longer a landlord for this property")
}
