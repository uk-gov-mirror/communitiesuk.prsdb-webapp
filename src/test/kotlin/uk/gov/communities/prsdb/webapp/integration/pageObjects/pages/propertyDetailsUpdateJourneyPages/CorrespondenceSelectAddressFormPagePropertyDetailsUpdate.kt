package uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyDetailsUpdateJourneyPages

import com.microsoft.playwright.Page
import uk.gov.communities.prsdb.webapp.controllers.LandlordUpdateCorrespondenceAddressController
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.basePages.SelectAddressFormPage
import uk.gov.communities.prsdb.webapp.journeys.shared.stepConfig.SelectAddressStep

class CorrespondenceSelectAddressFormPagePropertyDetailsUpdate(
    page: Page,
    urlArguments: Map<String, String>,
) : SelectAddressFormPage(
        page,
        urlPath(urlArguments["propertyOwnershipId"]!!.toLong()),
    ) {
    companion object {
        fun urlPath(propertyOwnershipId: Long): String =
            LandlordUpdateCorrespondenceAddressController.getUpdateCorrespondenceAddressRoute(propertyOwnershipId) +
                "/${SelectAddressStep.ROUTE_SEGMENT}"
    }
}
