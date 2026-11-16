package uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyReactivationJourneyPages

import com.microsoft.playwright.Page
import uk.gov.communities.prsdb.webapp.controllers.ReactivatePropertyController
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.basePages.BillsIncludedFormBasePage
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps.BillsIncludedStep

class BillsIncludedFormPagePropertyReactivation(
    page: Page,
    urlArguments: Map<String, String>,
) : BillsIncludedFormBasePage(
        page,
        ReactivatePropertyController.getPropertyReactivationBasePath(urlArguments["propertyOwnershipId"]!!.toLong()) +
            "/${BillsIncludedStep.ROUTE_SEGMENT}",
    )
