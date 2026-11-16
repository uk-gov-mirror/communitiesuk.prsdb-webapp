package uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyReactivationJourneyPages

import com.microsoft.playwright.Page
import uk.gov.communities.prsdb.webapp.controllers.ReactivatePropertyController
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.basePages.RentFrequencyFormBasePage
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps.RentFrequencyStep

class RentFrequencyFormPagePropertyReactivation(
    page: Page,
    urlArguments: Map<String, String>,
) : RentFrequencyFormBasePage(
        page,
        ReactivatePropertyController.getPropertyReactivationBasePath(urlArguments["propertyOwnershipId"]!!.toLong()) +
            "/${RentFrequencyStep.ROUTE_SEGMENT}",
    )
