package uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyDetailsUpdateJourneyPages

import com.microsoft.playwright.Page
import uk.gov.communities.prsdb.webapp.controllers.LandlordUpdateCorrespondenceAddressController
import uk.gov.communities.prsdb.webapp.integration.pageObjects.components.Heading
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.basePages.LookupAddressFormPage
import uk.gov.communities.prsdb.webapp.journeys.shared.stepConfig.LookupAddressStep

class CorrespondenceLookupAddressFormPagePropertyDetailsUpdate(
    page: Page,
    urlArguments: Map<String, String>,
) : LookupAddressFormPage(
        page,
        urlPath(urlArguments["propertyOwnershipId"]!!.toLong()),
    ) {
    val heading = Heading(page.locator("h1"))

    companion object {
        fun urlPath(propertyOwnershipId: Long): String =
            LandlordUpdateCorrespondenceAddressController.getUpdateCorrespondenceAddressRoute(propertyOwnershipId) +
                "/${LookupAddressStep.ROUTE_SEGMENT}"
    }
}
