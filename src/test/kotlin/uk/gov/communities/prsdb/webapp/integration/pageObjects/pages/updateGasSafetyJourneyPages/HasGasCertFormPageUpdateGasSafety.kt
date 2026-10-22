package uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.updateGasSafetyJourneyPages

import com.microsoft.playwright.Locator
import com.microsoft.playwright.Page
import uk.gov.communities.prsdb.webapp.constants.PROVIDE_THIS_LATER_BUTTON_ACTION_NAME
import uk.gov.communities.prsdb.webapp.controllers.LandlordUpdateGasSafetyController
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.basePages.HasGasCertFormBasePage
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps.HasGasCertStep

class HasGasCertFormPageUpdateGasSafety(
    page: Page,
    urlArguments: Map<String, String>,
) : HasGasCertFormBasePage(
        page,
        LandlordUpdateGasSafetyController.UPDATE_GAS_SAFETY_ROUTE
            .replace("{propertyOwnershipId}", urlArguments["propertyOwnershipId"]!!) +
            "/${HasGasCertStep.ROUTE_SEGMENT}",
    ) {
    val provideThisLaterButton: Locator =
        page.locator("button[name='action'][value='$PROVIDE_THIS_LATER_BUTTON_ACTION_NAME']")
}
