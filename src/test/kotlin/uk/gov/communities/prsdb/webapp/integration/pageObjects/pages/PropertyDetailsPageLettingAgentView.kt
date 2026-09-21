package uk.gov.communities.prsdb.webapp.integration.pageObjects.pages

import com.microsoft.playwright.Locator
import com.microsoft.playwright.Page
import uk.gov.communities.prsdb.webapp.controllers.LettingAgentPropertyDetailsController
import uk.gov.communities.prsdb.webapp.integration.pageObjects.components.Button
import uk.gov.communities.prsdb.webapp.integration.pageObjects.components.ServiceNavigation
import uk.gov.communities.prsdb.webapp.integration.pageObjects.components.SummaryCard
import uk.gov.communities.prsdb.webapp.integration.pageObjects.components.SummaryList
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.basePages.BasePage
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.basePages.GasSafetySummaryCard
import java.util.UUID

class PropertyDetailsPageLettingAgentView(
    page: Page,
    urlArguments: Map<String, String>,
) : BasePage(
        page,
        LettingAgentPropertyDetailsController.getLettingAgentPropertyDetailsPath(
            UUID.fromString(urlArguments["token"]!!),
        ),
    ) {
    val summaryList = LettingAgentSummaryList(page)

    val serviceNavigation = ServiceNavigation(page)

    val gasSafetyCard = GasSafetySummaryCard(page, "Gas safety certificate")
    val electricalSafetyCard = SummaryCard(page, "Electrical safety certificate")
    val epcCard = SummaryCard(page, "Energy performance certificate (EPC)")

    val surveyLink = Button.byText(page, "Go to survey")

    val provideDetailsInset: Locator
        get() = page.locator("#provide-details-inset")

    val updateSuccessBanner: Locator
        get() = page.locator("#update-success-banner")

    val complianceCertificates: Locator
        get() = page.locator("#compliance-certificates")

    fun sectionHeading(text: String): Locator =
        page.locator(
            "h2.govuk-heading-m",
            Page.LocatorOptions().setHasText(text),
        )

    class LettingAgentSummaryList(
        page: Page,
    ) : SummaryList(page) {
        val licensingRow = getRow("Licensing")
        val licensingTypeRow = getRow("Licensing type")
        val licensingNumberRow = getRow("Licensing number")
        val tenancyRow = getRow("Tenancy")
        val numberOfHouseholdsRow = getRow("Number of households")
        val numberOfTenantsRow = getRow("Number of tenants")
        val rentIncludesBillsRow = getRow("Rent includes bills")
        val billsIncludedRow = getRow("Which bills are included")
        val furnishedStatusRow = getRow("Furniture provided")
        val rentFrequencyRow = getRow("When rent is paid")
        val rentAmountRow = getRow("Rent amount")
    }
}
