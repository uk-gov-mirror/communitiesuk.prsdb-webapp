package uk.gov.communities.prsdb.webapp.integration

import com.microsoft.playwright.Page
import com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import uk.gov.communities.prsdb.webapp.constants.ASSOCIATED_PARTIES
import uk.gov.communities.prsdb.webapp.constants.PAYMENTS
import uk.gov.communities.prsdb.webapp.integration.pageObjects.components.BaseComponent.Companion.assertThat
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.basePages.BasePage.Companion.assertPageIs
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyRegistrationJourneyPages.CheckAnswersPagePropertyRegistration
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyRegistrationJourneyPages.ConfirmMissingComplianceFormPagePropertyRegistration
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyRegistrationJourneyPages.ConfirmationPagePropertyRegistration
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyRegistrationJourneyPages.PaymentSummaryFormPagePropertyRegistration
import uk.gov.communities.prsdb.webapp.testHelpers.builders.PropertyStateSessionBuilder

class PropertyRegistrationPaymentSinglePageTests : IntegrationTestWithMutableData("data-local.sql") {
    // TODO PDJB-959: remove this override and update these tests for the associated parties journey
    @BeforeEach
    fun disableAssociatedParties() {
        featureFlagManager.disableFeature(ASSOCIATED_PARTIES)
    }

    @BeforeEach
    fun enableFlag() {
        featureFlagManager.enableFeature(PAYMENTS)
    }

    @Test
    fun `submitting check your answers routes to the payment summary step when payments is enabled`(page: Page) {
        val checkAnswersPage = navigator.goToPropertyRegistrationCheckAnswersPageWithPayments()
        assertThat(checkAnswersPage.submitButton).containsText("Submit and pay")

        checkAnswersPage.confirm()

        val paymentSummaryPage = assertPageIs(page, PaymentSummaryFormPagePropertyRegistration::class)
        assertThat(paymentSummaryPage.heading).containsText("Property fee")
        assertThat(paymentSummaryPage.form.submitButton).containsText("Go to payment")
    }

    @Test
    fun `submitting check your answers routes straight to confirmation when payments is disabled`(page: Page) {
        featureFlagManager.disableFeature(PAYMENTS)

        val checkAnswersPage = navigator.goToPropertyRegistrationCheckAnswersPage()
        assertThat(checkAnswersPage.sectionHeader).containsText("Submit your registration")
        assertThat(checkAnswersPage.submitButton).containsText("Complete registration")

        checkAnswersPage.confirm()

        val confirmationPage = assertPageIs(page, ConfirmationPagePropertyRegistration::class)
        assertThat(confirmationPage.paymentSummaryHeading).isHidden()
    }

    @Test
    fun `confirming missing compliance routes straight to confirmation when payments is disabled`(page: Page) {
        featureFlagManager.disableFeature(PAYMENTS)

        val taskListPage =
            navigator.goToPropertyRegistrationTaskList(
                PropertyStateSessionBuilder
                    .beforePropertyRegistrationCheckAnswersOccupied()
                    .withBedrooms(),
            )
        taskListPage.clickSubmitYourRegistrationTaskWithName("Check and submit your answers")
        val checkAnswersPage = assertPageIs(page, CheckAnswersPagePropertyRegistration::class)

        checkAnswersPage.confirm()

        val confirmMissingCompliancePage = assertPageIs(page, ConfirmMissingComplianceFormPagePropertyRegistration::class)
        confirmMissingCompliancePage.form.radios.selectValue("true")
        confirmMissingCompliancePage.form.submit()

        assertPageIs(page, ConfirmationPagePropertyRegistration::class)
    }
}
