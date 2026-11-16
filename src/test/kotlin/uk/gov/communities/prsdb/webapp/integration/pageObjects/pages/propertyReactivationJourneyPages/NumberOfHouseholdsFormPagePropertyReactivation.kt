package uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyReactivationJourneyPages

import com.microsoft.playwright.Page
import uk.gov.communities.prsdb.webapp.controllers.ReactivatePropertyController
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.basePages.NumberOfHouseholdsFormBasePage
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps.HouseholdStep

class NumberOfHouseholdsFormPagePropertyReactivation(
    page: Page,
    urlArguments: Map<String, String>,
) : NumberOfHouseholdsFormBasePage(
        page,
        ReactivatePropertyController.getPropertyReactivationBasePath(urlArguments["propertyOwnershipId"]!!.toLong()) +
            "/${HouseholdStep.ROUTE_SEGMENT}",
    )
