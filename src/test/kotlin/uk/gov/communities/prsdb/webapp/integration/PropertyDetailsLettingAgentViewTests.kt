package uk.gov.communities.prsdb.webapp.integration

import com.microsoft.playwright.Page
import com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import uk.gov.communities.prsdb.webapp.constants.DELEGATE_TO_LETTING_AGENT
import uk.gov.communities.prsdb.webapp.constants.LETTING_AGENT_PROPERTY_DETAILS_SURVEY_URL
import uk.gov.communities.prsdb.webapp.controllers.LettingAgentPropertyDetailsController
import uk.gov.communities.prsdb.webapp.controllers.LettingAgentUpdateEpcController
import uk.gov.communities.prsdb.webapp.integration.pageObjects.components.BaseComponent.Companion.assertThat
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.ErrorPage
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.basePages.BasePage.Companion.createValidPage
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps.StartEpcStep
import java.util.UUID

class PropertyDetailsLettingAgentViewTests : IntegrationTestWithImmutableData("data-local.sql") {
    private val allDetailsDelegatedToken = UUID.fromString("3334abcd-5678-abcd-1234-567abcd2222a")

    private val licensingAndTenancyOutstandingToken = UUID.fromString("3334abcd-5678-abcd-1234-567abcd2222c")

    private val allDetailsProvidedToken = UUID.fromString("3334abcd-5678-abcd-1234-567abcd2222d")

    @BeforeEach
    fun enableFeatureFlag() {
        featureFlagManager.enable(DELEGATE_TO_LETTING_AGENT)
    }

    @Test
    fun `the service navigation banner is shown with the service name`(page: Page) {
        val detailsPage = navigator.goToPropertyDetailsLettingAgentView(allDetailsDelegatedToken)

        assertThat(detailsPage.serviceNavigation).isVisible()
        assertThat(detailsPage.serviceNavigation.serviceName).hasText("Register your rental property")
    }

    @Test
    fun `the feedback survey link is shown with the configured URL`(page: Page) {
        val detailsPage = navigator.goToPropertyDetailsLettingAgentView(allDetailsDelegatedToken)

        assertThat(detailsPage.surveyLink).isVisible()
        assertThat(detailsPage.surveyLink).hasAttribute("href", LETTING_AGENT_PROPERTY_DETAILS_SURVEY_URL)
    }

    @Test
    fun `when all details are delegated the provide-details inset and single provide-later rows are shown`(page: Page) {
        val detailsPage = navigator.goToPropertyDetailsLettingAgentView(allDetailsDelegatedToken)

        assertThat(detailsPage.provideDetailsInset).isVisible()
        assertThat(detailsPage.provideDetailsInset).containsText("Provide all details")

        assertThat(detailsPage.sectionHeading("Property licensing")).isVisible()
        assertThat(detailsPage.summaryList.licensingRow).isVisible()
        assertThat(detailsPage.summaryList.licensingTypeRow).isHidden()

        assertThat(detailsPage.sectionHeading("Tenancy details")).isVisible()
        assertThat(detailsPage.summaryList.tenancyRow).isVisible()

        assertThat(detailsPage.sectionHeading("Compliance certificates")).isVisible()
    }

    @Test
    fun `when all details are delegated every outstanding field shows provide-later text`(page: Page) {
        val detailsPage = navigator.goToPropertyDetailsLettingAgentView(allDetailsDelegatedToken)

        assertThat(detailsPage.provideDetailsInset).containsText("Provide all details")
        assertThat(detailsPage.summaryList.licensingRow.value).containsText("Provide this later")
        assertThat(detailsPage.summaryList.tenancyRow.value).containsText("Provide this later")
        assertThat(detailsPage.gasSafetyCard.summaryList.hasGasSupplyRow.value).containsText("Provide this later")
        assertThat(detailsPage.gasSafetyCard.summaryList.hasCertRow).isHidden()
        assertThat(detailsPage.electricalSafetyCard).containsText("Provide this later")
        assertThat(detailsPage.epcCard).containsText("Provide this later")
        assertThat(detailsPage.epcCard.getAction("Change").link).hasAttribute(
            "href",
            LettingAgentUpdateEpcController.getUpdateEpcRoute(allDetailsDelegatedToken) + "/${StartEpcStep.ROUTE_SEGMENT}",
        )
    }

    @Test
    fun `when only licensing and tenancy are outstanding the inset is shown and compliance is valid`(page: Page) {
        val detailsPage = navigator.goToPropertyDetailsLettingAgentView(licensingAndTenancyOutstandingToken)

        assertThat(detailsPage.provideDetailsInset).isVisible()
        assertThat(detailsPage.summaryList.licensingRow).isVisible()
        assertThat(detailsPage.summaryList.tenancyRow).isVisible()
        assertThat(detailsPage.complianceCertificates).isVisible()
    }

    @Test
    fun `when all details are provided the inset is hidden and the full detail rows are shown`(page: Page) {
        val detailsPage = navigator.goToPropertyDetailsLettingAgentView(allDetailsProvidedToken)

        assertThat(detailsPage.provideDetailsInset).not().isVisible()

        assertThat(detailsPage.summaryList.licensingTypeRow).isVisible()
        assertThat(detailsPage.summaryList.licensingNumberRow).isVisible()
        assertThat(detailsPage.summaryList.numberOfHouseholdsRow).isVisible()
        assertThat(detailsPage.summaryList.numberOfTenantsRow).isVisible()
        assertThat(detailsPage.summaryList.furnishedStatusRow).isVisible()
        assertThat(detailsPage.summaryList.rentAmountRow).isVisible()

        assertThat(detailsPage.sectionHeading("Compliance certificates")).isVisible()
        assertThat(detailsPage.epcCard.getAction("Change").link).hasAttribute(
            "href",
            LettingAgentUpdateEpcController.getUpdateEpcRoute(allDetailsProvidedToken) + "/${StartEpcStep.ROUTE_SEGMENT}",
        )
        assertThat(detailsPage.epcCard.getAction("View full EPC").link).hasAttribute(
            "href",
            "https://find-energy-certificate-staging.digital.communities.gov.uk/energy-certificate/0000-0000-0000-0961-0832",
        )
    }

    @Test
    fun `a not found page is returned when the delegate to letting agent flag is disabled`(page: Page) {
        featureFlagManager.disable(DELEGATE_TO_LETTING_AGENT)

        navigator.navigate(
            LettingAgentPropertyDetailsController.getLettingAgentPropertyDetailsPath(allDetailsProvidedToken),
        )

        val errorPage = createValidPage(page, ErrorPage::class)
        assertThat(errorPage.heading).containsText("Page not found")
    }
}
