package uk.gov.communities.prsdb.webapp.integration

import com.microsoft.playwright.Browser
import com.microsoft.playwright.Page
import com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.web.util.UriComponentsBuilder
import uk.gov.communities.prsdb.webapp.constants.PAYMENTS
import uk.gov.communities.prsdb.webapp.controllers.RegisterPropertyController
import uk.gov.communities.prsdb.webapp.integration.pageObjects.Navigator
import uk.gov.communities.prsdb.webapp.integration.pageObjects.components.BaseComponent.Companion.assertThat
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.ErrorPage
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.basePages.BasePage.Companion.assertPageIs
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.basePages.BasePage.Companion.createValidPage
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyRegistrationJourneyPages.CheckAnswersPagePropertyRegistration
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyRegistrationJourneyPages.ConfirmMissingComplianceFormPagePropertyRegistration
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyRegistrationJourneyPages.ConfirmationPagePropertyRegistration
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyRegistrationJourneyPages.NumberOfBedroomsFormPagePropertyRegistration
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyRegistrationJourneyPages.PaymentSummaryFormPagePropertyRegistration
import uk.gov.communities.prsdb.webapp.journeys.JourneyIdProvider
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps.BedroomsStep
import uk.gov.communities.prsdb.webapp.testHelpers.builders.PropertyStateSessionBuilder

class PropertyRegistrationPaymentSinglePageTests : IntegrationTestWithMutableData("data-local.sql") {
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
    fun `answers changed in another tab after loading the CYA page stop the property being registered when payments is disabled`(
        browser: Browser,
    ) {
        featureFlagManager.disableFeature(PAYMENTS)

        withJavaScriptDisabled(browser) { page, navigator ->
            val checkAnswersPage = navigator.goToPropertyRegistrationCheckAnswersPage()
            val journeyId =
                UriComponentsBuilder
                    .fromUriString(page.url())
                    .build()
                    .queryParams
                    .getFirst(JourneyIdProvider.PARAMETER_NAME)
            val otherPage = page.context().newPage()
            Navigator(otherPage, port).navigate(
                "${RegisterPropertyController.PROPERTY_REGISTRATION_ROUTE}/${BedroomsStep.ROUTE_SEGMENT}" +
                    "?${JourneyIdProvider.PARAMETER_NAME}=$journeyId",
            )
            createValidPage(otherPage, NumberOfBedroomsFormPagePropertyRegistration::class).submitNumOfBedrooms(7)

            checkAnswersPage.confirm()

            val errorPage = createValidPage(page, ErrorPage::class)
            assertThat(errorPage.heading).containsText("Sorry, there is a problem with the service")
        }
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
