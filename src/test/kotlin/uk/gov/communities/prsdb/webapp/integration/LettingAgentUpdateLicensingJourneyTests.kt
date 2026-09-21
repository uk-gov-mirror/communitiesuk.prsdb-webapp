package uk.gov.communities.prsdb.webapp.integration

import com.microsoft.playwright.Page
import com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import uk.gov.communities.prsdb.webapp.constants.DELEGATE_TO_LETTING_AGENT
import uk.gov.communities.prsdb.webapp.constants.enums.LicensingType
import uk.gov.communities.prsdb.webapp.controllers.LettingAgentUpdateLicensingController
import uk.gov.communities.prsdb.webapp.integration.pageObjects.components.BaseComponent.Companion.assertThat
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.ErrorPage
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.PropertyDetailsPageLettingAgentView
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.basePages.BasePage.Companion.assertPageIs
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.basePages.BasePage.Companion.createValidPage
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.lettingAgentUpdateLicensingJourneyPages.CheckLicensingAnswersPageLettingAgentUpdate
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.lettingAgentUpdateLicensingJourneyPages.LicensingTypeFormPageLettingAgentUpdate
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.lettingAgentUpdateLicensingJourneyPages.SelectiveLicenceFormPageLettingAgentUpdate
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps.LicensingTypeStep
import java.util.UUID
import kotlin.test.assertContains

class LettingAgentUpdateLicensingJourneyTests : IntegrationTestWithMutableData("data-local.sql") {
    // Property 40 has an existing licence for the removal scenario.
    private val token = UUID.fromString("3334abcd-5678-abcd-1234-567abcd2222d")
    private val urlArguments = mapOf("token" to token.toString())

    @BeforeEach
    fun enableFeatureFlag() {
        featureFlagManager.enable(DELEGATE_TO_LETTING_AGENT)
    }

    @Test
    fun `A letting agent can update a property's licensing`(page: Page) {
        val newLicenceNumber = "SL123"

        var propertyDetailsPage = navigator.goToPropertyDetailsLettingAgentView(token)
        propertyDetailsPage.summaryList.licensingTypeRow.clickFirstActionLinkAndWait()
        val licensingTypePage = assertPageIs(page, LicensingTypeFormPageLettingAgentUpdate::class, urlArguments)

        licensingTypePage.submitLicensingType(LicensingType.SELECTIVE_LICENCE)
        val licenceNumberPage = assertPageIs(page, SelectiveLicenceFormPageLettingAgentUpdate::class, urlArguments)

        licenceNumberPage.submitLicenseNumber(newLicenceNumber)
        val checkYourAnswersPage = assertPageIs(page, CheckLicensingAnswersPageLettingAgentUpdate::class, urlArguments)

        assertContains(checkYourAnswersPage.summaryName.getText(), "You have updated the property licence")
        assertThat(checkYourAnswersPage.summaryList.licensingTypeRow.value).containsText("Selective licence")
        assertThat(checkYourAnswersPage.summaryList.licensingNumberRow.value).containsText(newLicenceNumber)
        checkYourAnswersPage.confirm()

        propertyDetailsPage = assertPageIs(page, PropertyDetailsPageLettingAgentView::class, urlArguments)
        assertThat(propertyDetailsPage.summaryList.licensingTypeRow.value).containsText("Selective licence")
        assertThat(propertyDetailsPage.summaryList.licensingNumberRow.value).containsText(newLicenceNumber)
    }

    @Test
    fun `A letting agent sees an update-success banner after completing the licensing journey, which clears on refresh or revisit`(
        page: Page,
    ) {
        var propertyDetailsPage = navigator.goToPropertyDetailsLettingAgentView(token)
        assertThat(propertyDetailsPage.updateSuccessBanner).isHidden()

        propertyDetailsPage.summaryList.licensingTypeRow.clickFirstActionLinkAndWait()
        val licensingTypePage = assertPageIs(page, LicensingTypeFormPageLettingAgentUpdate::class, urlArguments)

        licensingTypePage.submitLicensingType(LicensingType.SELECTIVE_LICENCE)
        val licenceNumberPage = assertPageIs(page, SelectiveLicenceFormPageLettingAgentUpdate::class, urlArguments)

        licenceNumberPage.submitLicenseNumber("SL999")
        val checkYourAnswersPage = assertPageIs(page, CheckLicensingAnswersPageLettingAgentUpdate::class, urlArguments)
        checkYourAnswersPage.confirm()

        propertyDetailsPage = assertPageIs(page, PropertyDetailsPageLettingAgentView::class, urlArguments)
        assertThat(propertyDetailsPage.updateSuccessBanner).isVisible()
        assertThat(propertyDetailsPage.updateSuccessBanner).containsText("Property licensing updated.")

        page.reload()
        assertThat(propertyDetailsPage.updateSuccessBanner).isHidden()

        propertyDetailsPage = navigator.goToPropertyDetailsLettingAgentView(token)
        assertThat(propertyDetailsPage.updateSuccessBanner).isHidden()
    }

    @Test
    fun `A letting agent can remove a property's licensing`(page: Page) {
        var propertyDetailsPage = navigator.goToPropertyDetailsLettingAgentView(token)
        propertyDetailsPage.summaryList.licensingTypeRow.clickFirstActionLinkAndWait()
        val licensingTypePage = assertPageIs(page, LicensingTypeFormPageLettingAgentUpdate::class, urlArguments)

        licensingTypePage.submitLicensingType(LicensingType.NO_LICENSING)
        val checkYourAnswersPage = assertPageIs(page, CheckLicensingAnswersPageLettingAgentUpdate::class, urlArguments)

        assertContains(checkYourAnswersPage.summaryName.getText(), "You have removed this property’s licence")
        assertThat(checkYourAnswersPage.summaryList.licensingTypeRow.value).containsText("None")
        checkYourAnswersPage.confirm()

        propertyDetailsPage = assertPageIs(page, PropertyDetailsPageLettingAgentView::class, urlArguments)
        assertThat(propertyDetailsPage.summaryList.licensingTypeRow.value).containsText("None")
    }

    @Test
    fun `Changing the licence number from the CYA page updates the property with the correct value`(page: Page) {
        val firstNewLicenceNumber = "SL456"
        val secondNewLicenceNumber = "SL789"

        val propertyDetailsPage = navigator.goToPropertyDetailsLettingAgentView(token)
        propertyDetailsPage.summaryList.licensingTypeRow.clickFirstActionLinkAndWait()
        val licensingTypePage = assertPageIs(page, LicensingTypeFormPageLettingAgentUpdate::class, urlArguments)

        licensingTypePage.submitLicensingType(LicensingType.SELECTIVE_LICENCE)
        var licenceNumberPage = assertPageIs(page, SelectiveLicenceFormPageLettingAgentUpdate::class, urlArguments)

        licenceNumberPage.submitLicenseNumber(firstNewLicenceNumber)
        var checkYourAnswersPage = assertPageIs(page, CheckLicensingAnswersPageLettingAgentUpdate::class, urlArguments)

        checkYourAnswersPage.summaryList.licensingNumberRow
            .clickFirstActionLinkAndWait()
        licenceNumberPage = assertPageIs(page, SelectiveLicenceFormPageLettingAgentUpdate::class, urlArguments)

        licenceNumberPage.submitLicenseNumber(secondNewLicenceNumber)
        checkYourAnswersPage = assertPageIs(page, CheckLicensingAnswersPageLettingAgentUpdate::class, urlArguments)

        assertThat(checkYourAnswersPage.summaryList.licensingNumberRow.value).containsText(secondNewLicenceNumber)
    }

    @Test
    fun `The licensing pages show a Continue button rather than Save and continue`(page: Page) {
        val propertyDetailsPage = navigator.goToPropertyDetailsLettingAgentView(token)
        propertyDetailsPage.summaryList.licensingTypeRow.clickFirstActionLinkAndWait()
        val licensingTypePage = assertPageIs(page, LicensingTypeFormPageLettingAgentUpdate::class, urlArguments)

        assertThat(licensingTypePage.form.submitButton).hasText("Continue")

        licensingTypePage.submitLicensingType(LicensingType.SELECTIVE_LICENCE)
        val licenceNumberPage = assertPageIs(page, SelectiveLicenceFormPageLettingAgentUpdate::class, urlArguments)

        assertThat(licenceNumberPage.form.submitButton).hasText("Continue")
    }

    @Test
    fun `a not found page is returned when the delegate to letting agent flag is disabled`(page: Page) {
        featureFlagManager.disable(DELEGATE_TO_LETTING_AGENT)

        navigator.navigate(
            LettingAgentUpdateLicensingController.getUpdateLicensingRoute(token) +
                "/${LicensingTypeStep.ROUTE_SEGMENT}",
        )

        val errorPage = createValidPage(page, ErrorPage::class)
        assertThat(errorPage.heading).containsText("Page not found")
    }
}
