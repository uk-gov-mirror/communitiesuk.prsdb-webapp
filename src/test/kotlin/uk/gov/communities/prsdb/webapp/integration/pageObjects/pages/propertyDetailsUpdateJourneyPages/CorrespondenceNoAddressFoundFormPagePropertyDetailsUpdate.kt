package uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyDetailsUpdateJourneyPages

import com.microsoft.playwright.Page
import uk.gov.communities.prsdb.webapp.controllers.LandlordUpdateCorrespondenceAddressController
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.basePages.NoAddressFoundFormPage
import uk.gov.communities.prsdb.webapp.journeys.shared.stepConfig.NoAddressFoundStep

class CorrespondenceNoAddressFoundFormPagePropertyDetailsUpdate(
    page: Page,
    urlArguments: Map<String, String>,
) : NoAddressFoundFormPage(
        page,
        urlPath(urlArguments["propertyOwnershipId"]!!.toLong()),
    ) {
    companion object {
        fun urlPath(propertyOwnershipId: Long): String =
            LandlordUpdateCorrespondenceAddressController.getUpdateCorrespondenceAddressRoute(propertyOwnershipId) +
                "/${NoAddressFoundStep.ROUTE_SEGMENT}"
    }
}
