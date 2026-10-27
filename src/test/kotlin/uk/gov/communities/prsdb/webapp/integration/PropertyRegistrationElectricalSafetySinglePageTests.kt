package uk.gov.communities.prsdb.webapp.integration

import com.microsoft.playwright.Page
import com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import uk.gov.communities.prsdb.webapp.constants.ASSOCIATED_PARTIES
import uk.gov.communities.prsdb.webapp.integration.pageObjects.components.BaseComponent.Companion.assertThat
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.basePages.BasePage.Companion.assertPageIs
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyRegistrationJourneyPages.CheckElectricalCertUploadsFormPagePropertyRegistration
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyRegistrationJourneyPages.ElectricalCertExpiryDateFormPagePropertyRegistration
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyRegistrationJourneyPages.HasElectricalCertFormPagePropertyRegistration
import uk.gov.communities.prsdb.webapp.testHelpers.builders.PropertyStateSessionBuilder
import java.util.regex.Pattern

class PropertyRegistrationElectricalSafetySinglePageTests : IntegrationTestWithImmutableData("data-local.sql") {
    // TODO PDJB-959: remove this override and update these tests for the associated parties journey
    @BeforeEach
    fun disableAssociatedParties() {
        featureFlagManager.disableFeature(ASSOCIATED_PARTIES)
    }

    @Nested
    inner class HasElectricalCertStep {
        @Test
        fun `Submitting with the Continue button with no option selected returns an error`(page: Page) {
            val hasElectricalCertPage = navigator.skipToPropertyRegistrationHasElectricalCertPage()
            hasElectricalCertPage.form.submitPrimaryButton()
            assertThat(
                hasElectricalCertPage.form.getErrorMessage(),
            ).containsText("Select which electrical safety certificate you have")
        }
    }

    @Nested
    inner class CheckElectricalCertUploadsStep {
        @Test
        fun `The actions column header text is visually hidden but the header cell is not`(page: Page) {
            val checkUploadsPage = navigateToCheckUploadsPage(page)

            assertThat(checkUploadsPage.table.actionsHeaderVisuallyHiddenText).hasText("Actions")
            assertThat(checkUploadsPage.table.actionsHeader).not().hasClass(Pattern.compile("govuk-visually-hidden"))
        }

        @Test
        fun `The remove link includes the file name as visually hidden text`(page: Page) {
            val checkUploadsPage = navigateToCheckUploadsPage(page)

            assertThat(checkUploadsPage.table.getClickableCell(0, 2).link).hasText("Remove electrical-safety-cert.pdf")
            assertThat(checkUploadsPage.table.getVisuallyHiddenText(0, 2)).hasText("electrical-safety-cert.pdf")
        }

        private fun navigateToCheckUploadsPage(page: Page): CheckElectricalCertUploadsFormPagePropertyRegistration {
            val cyaPage =
                navigator.skipToPropertyRegistrationCheckElectricalSafetyAnswersPage(
                    PropertyStateSessionBuilder.beforePropertyRegistrationCheckElectricalSafetyAnswersUploadedEic(),
                )
            cyaPage.summaryList.yourCertificateRow.clickFirstActionLinkAndWait()
            return assertPageIs(page, CheckElectricalCertUploadsFormPagePropertyRegistration::class)
        }
    }

    @Nested
    inner class CheckElectricalSafetyAnswersStep {
        @Test
        fun `Cert uploaded EIC - cert type change link navigates to has electrical cert page`(page: Page) {
            val cyaPage =
                navigator.skipToPropertyRegistrationCheckElectricalSafetyAnswersPage(
                    PropertyStateSessionBuilder.beforePropertyRegistrationCheckElectricalSafetyAnswersUploadedEic(),
                )
            cyaPage.summaryList.electricalCertRow.clickFirstActionLinkAndWait()
            assertPageIs(page, HasElectricalCertFormPagePropertyRegistration::class)
        }

        @Test
        fun `Cert uploaded EIC - expiry date change link navigates to expiry date page`(page: Page) {
            val cyaPage =
                navigator.skipToPropertyRegistrationCheckElectricalSafetyAnswersPage(
                    PropertyStateSessionBuilder.beforePropertyRegistrationCheckElectricalSafetyAnswersUploadedEic(),
                )
            cyaPage.summaryList.expiryDateRow.clickFirstActionLinkAndWait()
            assertPageIs(page, ElectricalCertExpiryDateFormPagePropertyRegistration::class)
        }

        @Test
        fun `Cert uploaded EIC - certificate change link navigates to check uploads page`(page: Page) {
            val cyaPage =
                navigator.skipToPropertyRegistrationCheckElectricalSafetyAnswersPage(
                    PropertyStateSessionBuilder.beforePropertyRegistrationCheckElectricalSafetyAnswersUploadedEic(),
                )
            cyaPage.summaryList.yourCertificateRow.clickFirstActionLinkAndWait()
            assertPageIs(page, CheckElectricalCertUploadsFormPagePropertyRegistration::class)
        }

        @Test
        fun `Cert uploaded EICR - cert type change link navigates to has electrical cert page`(page: Page) {
            val cyaPage =
                navigator.skipToPropertyRegistrationCheckElectricalSafetyAnswersPage(
                    PropertyStateSessionBuilder.beforePropertyRegistrationCheckElectricalSafetyAnswersUploadedEicr(),
                )
            cyaPage.summaryList.electricalCertRow.clickFirstActionLinkAndWait()
            assertPageIs(page, HasElectricalCertFormPagePropertyRegistration::class)
        }

        @Test
        fun `Provide later - cert type change link navigates to has electrical cert page`(page: Page) {
            val cyaPage =
                navigator.skipToPropertyRegistrationCheckElectricalSafetyAnswersPage(
                    PropertyStateSessionBuilder.beforePropertyRegistrationCheckElectricalSafetyAnswersProvideLater(),
                )
            cyaPage.summaryList.electricalCertRow.clickFirstActionLinkAndWait()
            assertPageIs(page, HasElectricalCertFormPagePropertyRegistration::class)
        }

        @Test
        fun `No cert - cert type change link navigates to has electrical cert page`(page: Page) {
            val cyaPage =
                navigator.skipToPropertyRegistrationCheckElectricalSafetyAnswersPage(
                    PropertyStateSessionBuilder.beforePropertyRegistrationCheckElectricalSafetyAnswersNoCert(),
                )
            cyaPage.summaryList.electricalCertRow.clickFirstActionLinkAndWait()
            assertPageIs(page, HasElectricalCertFormPagePropertyRegistration::class)
        }

        @Test
        fun `Cert expired - cert type change link navigates to has electrical cert page`(page: Page) {
            val cyaPage =
                navigator.skipToPropertyRegistrationCheckElectricalSafetyAnswersPage(
                    PropertyStateSessionBuilder.beforePropertyRegistrationCheckElectricalSafetyAnswersCertExpired(),
                )
            cyaPage.summaryList.electricalCertRow.clickFirstActionLinkAndWait()
            assertPageIs(page, HasElectricalCertFormPagePropertyRegistration::class)
        }
    }

    @Nested
    inner class ElectricalCertExpiryDateStepTests {
        @ParameterizedTest(name = "{0}")
        @Suppress("ktlint:standard:max-line-length")
        @MethodSource(
            "uk.gov.communities.prsdb.webapp.testHelpers.parameterProviders.AnyDateValidationTestParameterProvider#provideInvalidDateStrings",
        )
        fun `Submitting returns a corresponding error when`(
            dayMonthYear: Triple<String, String, String>,
            expectedErrorMessage: String,
        ) {
            val (day, month, year) = dayMonthYear
            val electricalCertExpiryDatePage = navigator.skipToPropertyRegistrationElectricalCertExpiryDatePage()
            electricalCertExpiryDatePage.submitDate(day, month, year)
            assertThat(electricalCertExpiryDatePage.form.getErrorMessage()).containsText(expectedErrorMessage)
        }
    }
}
