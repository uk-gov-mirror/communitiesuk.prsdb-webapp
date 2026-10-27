package uk.gov.communities.prsdb.webapp.integration.pageObjects.pages

import com.microsoft.playwright.Locator
import com.microsoft.playwright.Page
import uk.gov.communities.prsdb.webapp.controllers.LandlordController.Companion.OUTSTANDING_ACTIONS_URL
import uk.gov.communities.prsdb.webapp.integration.pageObjects.components.Heading
import uk.gov.communities.prsdb.webapp.integration.pageObjects.components.InsetText
import uk.gov.communities.prsdb.webapp.integration.pageObjects.components.Pagination
import uk.gov.communities.prsdb.webapp.integration.pageObjects.components.SummaryCard
import uk.gov.communities.prsdb.webapp.integration.pageObjects.components.SummaryList
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.basePages.BasePage

class OutstandingActionsPage(
    page: Page,
) : BasePage(page, OUTSTANDING_ACTIONS_URL) {
    val heading = Heading.default(page)
    val hintText = Heading(page.locator("p.govuk-hint"))
    val bodyText = Heading(page.locator("header p.govuk-body"))
    val insetText = InsetText(page)
    val pagination = Pagination(page)

    /**
     * Will use the pagination buttons if the property isn't immediately visible
     */
    fun findSummaryCard(propertyAddress: String): OutstandingActionSummaryCard {
        val cardLocator = page.locator(".govuk-summary-card", Page.LocatorOptions().setHasText(propertyAddress))
        if (cardLocator.isVisible) return getSummaryCard(propertyAddress)

        while (page.locator(".govuk-pagination__next").isVisible) {
            page.locator(".govuk-pagination__next a").click()
            page.waitForLoadState()
            if (cardLocator.isVisible) return getSummaryCard(propertyAddress)
        }

        return getSummaryCard(propertyAddress)
    }

    private fun getSummaryCard(propertyAddress: String) = OutstandingActionSummaryCard(page, propertyAddress)

    class OutstandingActionSummaryCard(
        page: Page,
        title: String,
    ) : SummaryCard(page, title) {
        override val summaryList = OutstandingActionSummaryList(locator)
        val epcInsetText = InsetText(locator)
    }

    class OutstandingActionSummaryList(
        locator: Locator,
    ) : SummaryList(locator) {
        val licensingRow = getRow("Licensing details")
        val tenancyRow = getRow("Tenancy details")
        val gasSafetyRow = getRow("Gas safety certificate")
        val electricalSafetyRow = getRow("Electrical safety certificate")
        val energyPerformanceRow = getRow("Energy performance certificate")
    }
}
