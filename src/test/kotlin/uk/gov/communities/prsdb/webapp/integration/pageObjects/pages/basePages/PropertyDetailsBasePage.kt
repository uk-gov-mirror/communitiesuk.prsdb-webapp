package uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.basePages

import com.microsoft.playwright.Page
import uk.gov.communities.prsdb.webapp.integration.pageObjects.components.BackLink
import uk.gov.communities.prsdb.webapp.integration.pageObjects.components.SummaryList
import uk.gov.communities.prsdb.webapp.integration.pageObjects.components.Tabs
import uk.gov.communities.prsdb.webapp.integration.pageObjects.components.TicketPanel

abstract class PropertyDetailsBasePage(
    page: Page,
    urlSegment: String,
) : BasePage(page, urlSegment) {
    val tabs = PropertyDetailsTabs(page)
    val beforePdjb939SummaryList = PropertyDetailsBeforePdjb939SummaryList(page)
    val propertyComplianceSummaryList = PropertyComplianceSummaryList(page)
    val gasSafetyCard = GasSafetySummaryCard(page, "Gas safety certificate")
    val electricalSafetyCard = ElectricalSafetySummaryCard(page, "Electrical safety certificate")
    val epcCard = EpcSummaryCard(page, "Energy performance certificate (EPC)")
    val landlordSummaryList = LandlordSummaryList(page)

    val backLink = BackLink.default(page)

    val lettingAgentPanel = TicketPanel(page)

    val propertyDetailsSummaryList = PropertyDetailsSummaryList(page)

    fun sectionHeading(text: String) =
        page.locator(
            "h3.govuk-heading-s",
            Page.LocatorOptions().setHasText(text),
        )

    fun bodyParagraph(text: String) =
        page.locator(
            "#property-details p.govuk-body",
            Page.LocatorOptions().setHasText(text),
        )

    class PropertyDetailsTabs(
        page: Page,
    ) : Tabs(page) {
        fun goToLandlordDetails() {
            goToTab("Landlords")
        }

        fun goToPropertyDetails() {
            goToTab("Property and tenancy details")
        }

        fun goToComplianceInformation() {
            goToTab("Compliance information")
        }
    }

    class PropertyDetailsBeforePdjb939SummaryList(
        page: Page,
    ) : SummaryList(page) {
        val propertyTypeRow = getRow("Property type")
        val ownershipTypeRow = getRow("Ownership type")
        val occupancyRow = getRow("Occupied by tenants")
        val numberOfHouseholdsRow = getRow("Number of households")
        val numberOfPeopleRow = getRow("Number of tenants")
        val numberOfBedroomsRow = getRow("Number of bedrooms")
        val rentIncludesBillsRow = getRow("Rent includes bills")
        val billsIncludedRow = getRow("Which bills are included")
        val furnishedStatusRow = getRow("Furniture provided")
        val rentFrequencyRow = getRow("When rent is paid")
        val rentAmountRow = getRow("Rent amount")
        val licensingTypeRow = getRow("Licensing type")
        val licensingNumberRow = getRow("Licensing number")
    }

    class PropertyDetailsSummaryList(
        page: Page,
    ) : SummaryList(page.locator("#property-details")) {
        val addressRow = getRow("Address")
        val propertyTypeRow = getRow("Property type")
        val ownershipTypeRow = getRow("How do you own this property?")
        val contactEmailAddressRow = getRow("Contact email address")
        val contactAddressRow = getRow("Contact address")
        val occupancyRow = getRow("Is this property occupied by tenants?")
        val licensingRow = getRow("Licensing")
        val licensingTypeRow = getRow("Licensing type")
        val licensingNumberRow = getRow("Licensing number")
        val tenancyRow = getRow("Tenancy")
        val numberOfHouseholdsRow = getRow("Number of households")
        val numberOfPeopleRow = getRow("Number of tenants")
        val numberOfBedroomsRow = getRow("Number of bedrooms")
        val rentIncludesBillsRow = getRow("Rent includes bills")
        val billsIncludedRow = getRow("Which bills are included")
        val furnishedStatusRow = getRow("Furniture provided")
        val rentFrequencyRow = getRow("When rent is paid")
        val rentAmountRow = getRow("Rent amount")
    }

    class PropertyComplianceSummaryList(
        page: Page,
    ) : SummaryList(page) {
        val gasSafetyRow = getRow("Gas safety certificate")
        val electricalSafetyRow = getRow("Electrical safety certificate")
        val eicrRow = getRow("Electrical Installation Condition Report (EICR)")
        val eicRow = getRow("Electrical Installation Certificate (EIC)")
        val epcRow = getRow("Energy Performance Certificate (EPC)")
        val meesExemptionRow = getRow("MEES exemption")
        val fireSafetyRow = getRow("Fire safety responsibilities")
        val propertySafetyRow = getRow("Health and safety in rental properties")
        val responsibilityToTenantsRow = getRow("Your responsibilities to your tenants")
    }
}
