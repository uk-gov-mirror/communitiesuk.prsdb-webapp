package uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyDetailsUpdateJourneyPages

import com.microsoft.playwright.Page
import uk.gov.communities.prsdb.webapp.controllers.LandlordUpdateCorrespondenceAddressController
import uk.gov.communities.prsdb.webapp.integration.pageObjects.components.Form
import uk.gov.communities.prsdb.webapp.integration.pageObjects.components.Heading
import uk.gov.communities.prsdb.webapp.integration.pageObjects.components.SummaryList
import uk.gov.communities.prsdb.webapp.integration.pageObjects.components.Warning
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.basePages.BasePage
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.update.correspondenceAddress.UpdateCorrespondenceAddressCyaStep

class CheckCorrespondenceAddressAnswersPagePropertyDetailsUpdate(
    page: Page,
    urlArguments: Map<String, String>,
) : BasePage(
        page,
        LandlordUpdateCorrespondenceAddressController.getUpdateCorrespondenceAddressRoute(
            urlArguments["propertyOwnershipId"]!!.toLong(),
        ) + "/${UpdateCorrespondenceAddressCyaStep.ROUTE_SEGMENT}",
    ) {
    val heading = Heading.default(page)
    val form = Form(page)
    val summaryName = Heading(page.locator("#summary-name"))
    val summaryList = CheckCorrespondenceAddressAnswersSummaryList(page)
    val warning = Warning.default(page)

    fun confirm() = form.submit()

    class CheckCorrespondenceAddressAnswersSummaryList(
        page: Page,
    ) : SummaryList(page) {
        val postalAddressRow = getRow("Postal address")
    }
}
