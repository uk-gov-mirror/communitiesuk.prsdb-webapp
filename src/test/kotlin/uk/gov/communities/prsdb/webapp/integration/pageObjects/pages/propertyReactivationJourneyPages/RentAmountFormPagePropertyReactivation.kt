package uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyReactivationJourneyPages

import com.microsoft.playwright.Page
import uk.gov.communities.prsdb.webapp.controllers.ReactivatePropertyController
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.basePages.RentAmountFormBasePage
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps.RentAmountStep

class RentAmountFormPagePropertyReactivation(
    page: Page,
    urlArguments: Map<String, String>,
) : RentAmountFormBasePage(
        page,
        ReactivatePropertyController.getPropertyReactivationBasePath(urlArguments["propertyOwnershipId"]!!.toLong()) +
            "/${RentAmountStep.ROUTE_SEGMENT}",
    )
