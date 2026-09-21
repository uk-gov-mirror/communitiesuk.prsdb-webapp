package uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyRegistrationJourneyPages

import com.microsoft.playwright.Page
import uk.gov.communities.prsdb.webapp.controllers.RegisterPropertyController
import uk.gov.communities.prsdb.webapp.integration.pageObjects.components.Button
import uk.gov.communities.prsdb.webapp.integration.pageObjects.components.FormWithSectionHeader.SectionHeader
import uk.gov.communities.prsdb.webapp.integration.pageObjects.components.Heading
import uk.gov.communities.prsdb.webapp.integration.pageObjects.components.Paragraph
import uk.gov.communities.prsdb.webapp.integration.pageObjects.components.PostForm
import uk.gov.communities.prsdb.webapp.integration.pageObjects.components.SummaryList
import uk.gov.communities.prsdb.webapp.integration.pageObjects.components.TicketPanel
import uk.gov.communities.prsdb.webapp.integration.pageObjects.components.Warning
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.basePages.BasePage
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps.PropertyRegistrationCyaStep
import java.util.regex.Pattern

class CheckAnswersPagePropertyRegistration(
    page: Page,
) : BasePage(page, "${RegisterPropertyController.PROPERTY_REGISTRATION_ROUTE}/${PropertyRegistrationCyaStep.ROUTE_SEGMENT}") {
    fun confirm() = form.submit()

    val form = PostForm(page)

    val sectionHeader = SectionHeader(page.locator("main"))

    val heading = Heading(page.locator("h1"))

    val summaryList = CheckAnswersPropertyRegistrationSummaryList(page)

    // TODO PDJB-1340: Delete these before-restructure heading locators when PROPERTY_REGISTRATION_RESTRUCTURE_AND_SKIPPING is removed.
    val beforePropertyRegistrationRestructuredHeading = Heading(page.locator("h1.govuk-heading-l"))

    val beforePropertyRegistrationRestructuredPropertyDetailsHeading =
        Heading(page.locator("h2.govuk-heading-m", Page.LocatorOptions().setHasText("Property details")))

    val beforePropertyRegistrationRestructuredGasSafetyHeading =
        Heading(page.locator("h3.govuk-heading-s", Page.LocatorOptions().setHasText("Gas safety certificate")))

    val restructuredHeading = Heading(page.locator("h1.govuk-heading-xl"))

    val aboutYourPropertyHeading =
        Heading(page.locator("h2.govuk-heading-l", Page.LocatorOptions().setHasText("About your property")))

    val propertyDetailsHeading =
        Heading(page.locator("h3.govuk-heading-m", Page.LocatorOptions().setHasText("Property details")))

    private val correspondenceHeadingLocator =
        page.locator("h3.govuk-heading-m", Page.LocatorOptions().setHasText(Pattern.compile("^Who the council should contact$")))

    val correspondenceHeading = Heading(correspondenceHeadingLocator)

    val correspondenceRowKeys =
        correspondenceHeadingLocator.locator("xpath=following-sibling::dl[1]//dt[contains(@class,'govuk-summary-list__key')]")

    val occupancyHeading =
        Heading(page.locator("h3.govuk-heading-m", Page.LocatorOptions().setHasText("Tell us if your property’s occupied")))

    val rentedOutHeading =
        Heading(page.locator("h2.govuk-heading-l", Page.LocatorOptions().setHasText("How your property’s rented out")))

    val lettingAgentDelegationSubheading =
        Heading(
            page.locator(
                "h3.govuk-heading-m",
                Page.LocatorOptions().setHasText("Who will provide these details"),
            ),
        )

    val lettingAgentDelegationBodyText =
        Paragraph.byText(
            page,
            "After you’ve paid, we’ll ask your letting agent or property manager to provide the remaining details:",
        )

    val lettingAgentDelegationUnoccupiedPanel = TicketPanel(page)

    val restructuredSectionHeadings: List<String>
        get() =
            page
                .locator("main h2.govuk-heading-l, main h3.govuk-heading-m")
                .allInnerTexts()
                .map { it.trim() }

    val warning = Warning.default(page)

    val submitButton = Button.default(page)

    val complianceSummaryList = ComplianceSummaryList(page)

    val tenancyHeading =
        Heading(page.locator("h2.govuk-heading-m", Page.LocatorOptions().setHasText("Tenancy and rental information")))

    val restructuredTenancyHeading =
        Heading(page.locator("h3.govuk-heading-m", Page.LocatorOptions().setHasText("Tenancy details")))
    val restructuredTenancyUnoccupiedBodyText =
        Paragraph.byText(
            page,
            "We’ll ask for tenancy details when your property becomes occupied.",
        )
    private val restructuredTenancyRowKeys =
        page
            .locator("h3.govuk-heading-m", Page.LocatorOptions().setHasText("Tenancy details"))
            .locator("xpath=following-sibling::dl[1]//dt[contains(@class,'govuk-summary-list__key')]")

    fun restructuredTenancyRowHeadings(): List<String> {
        val rowCount = restructuredTenancyRowKeys.count()
        return (0 until rowCount).map { index -> restructuredTenancyRowKeys.nth(index).innerText().trim() }
    }

    val complianceCertificatesHeading =
        Heading(page.locator("h2.govuk-heading-m", Page.LocatorOptions().setHasText("Compliance certificates")))

    val gasSafetyHeading =
        Heading(page.locator("h3", Page.LocatorOptions().setHasText("Gas safety certificate")))

    val electricalSafetyHeading =
        Heading(page.locator("h3", Page.LocatorOptions().setHasText("Electrical safety certificate")))

    val epcHeading =
        Heading(page.locator("h3", Page.LocatorOptions().setHasText("Energy performance certificate (EPC)")))

    class CheckAnswersPropertyRegistrationSummaryList(
        page: Page,
    ) : SummaryList(page) {
        val propertyAddressRow = getRow(Pattern.compile("^Address$"))
        val ownershipRow = getRow("How do you own this property?")
        val correspondenceEmailRow = getRow(Pattern.compile("^Email address$"))
        val correspondencePostalAddressRow = getRow(Pattern.compile("^Postal address$"))

        // TODO PDJB-1340: Delete beforePropertyRegistrationRestructuredOwnershipRow when PROPERTY_REGISTRATION_RESTRUCTURE_AND_SKIPPING is removed.
        val beforePropertyRegistrationRestructuredOwnershipRow = getRow("Ownership type")
        val licensingRow = getRow("Licensing type")
        val licensingNumberRow = getRow("Licensing number")
        val occupancyQuestionRow = getRow("Is this property occupied by tenants?")
        val whoProvidesRentalDetailsRow = getRow("Who will provide this property’s rental details?")
        val lettingAgentEmailRow = getRow("Letting agent or property manager’s email address")
        val occupiedByTenantsRow = getRow(Pattern.compile("^Occupied by tenants$"))
        val tenancyDetailsRow = getRow("Tenancy details")
        val numberOfHouseholdsRow = getRow("Number of households")
        val numberOfTenantsRow = getRow("Number of tenants")
        val numberOfBedroomsRow = getRow("Number of bedrooms")
        val rentAmountRow = getRow("Rent amount")
        val billsIncludedRow = getRow("Which bills are included")

        val jointLandlordsInvitationsRow = getRow("Joint landlord invitations")

        // TODO PDJB-1340: Delete beforePropertyRegistrationRestructuredJointLandlordsInvitationsRow when PROPERTY_REGISTRATION_RESTRUCTURE_AND_SKIPPING is removed.
        val beforePropertyRegistrationRestructuredJointLandlordsInvitationsRow = getRow("Invitations")
        val jointLandlordsAreThereRow = getRow("Are there any other landlords for this property?")
    }

    class ComplianceSummaryList(
        page: Page,
    ) : SummaryList(page) {
        val gasSupplyRow = getRow("Does the property have a gas supply or any gas appliances?")
        val validGasCertRow = getRow("Do you have a valid gas safety certificate for this property?")
        val gasCertIssueDateRow = getRow("Issue date")
        val gasCertUploadRow = getRow("Your certificate")
        val electricalCertRow = getRow("Which electrical safety certificate do you have for this property?")
        val electricalCertExpiryDateRow = getRow("Expiry date")
        val electricalCertUploadRow = getRow("Your certificate")
        val hasEpcRow = getRow("Do you have an EPC for this property?")
        val epcTenancyCheckRow = getRow("Was the EPC still in date when the current tenancy began?")
        val isEpcRequiredRow = getRow("Is an EPC required to let this property?")
        val epcExemptionRow = getRow("Why does this property not need an EPC?")
        val hasMeesExemptionRow = getRow("Do you have a registered energy efficiency exemption for this property?")
        val meesExemptionRow = getRow("Registered exemption")
    }
}
