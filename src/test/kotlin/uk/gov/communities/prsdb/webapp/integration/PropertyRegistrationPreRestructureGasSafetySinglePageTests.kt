package uk.gov.communities.prsdb.webapp.integration

import com.microsoft.playwright.Page
import com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import uk.gov.communities.prsdb.webapp.constants.PROPERTY_REGISTRATION_RESTRUCTURE_AND_SKIPPING
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.basePages.BasePage.Companion.assertPageIs
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyRegistrationJourneyPages.CheckGasCertUploadsFormPagePropertyRegistration
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyRegistrationJourneyPages.CheckGasSafetyAnswersFormPagePropertyRegistration
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyRegistrationJourneyPages.GasCertIssueDateFormPagePropertyRegistration
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyRegistrationJourneyPages.HasGasCertFormPagePropertyRegistration
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyRegistrationJourneyPages.HasGasSupplyFormPagePropertyRegistration
import uk.gov.communities.prsdb.webapp.testHelpers.builders.PropertyStateSessionBuilder

// Covers the property registration journey with the restructure-and-skipping feature flag OFF.
// TODO PDJB-1340: delete every PropertyRegistrationPreRestructure*SinglePageTests file when
// PROPERTY_REGISTRATION_RESTRUCTURE_AND_SKIPPING is removed. Every class here has an equivalent
// in the flag-on files, so they can all be removed wholesale.
class PropertyRegistrationPreRestructureGasSafetySinglePageTests : IntegrationTestWithImmutableData("data-local.sql") {
    @BeforeEach
    fun disableRestructureAndSkippingFlag() {
        featureFlagManager.disableFeature(PROPERTY_REGISTRATION_RESTRUCTURE_AND_SKIPPING)
    }

    @Nested
    inner class BeforePdjb1022HasGasSupplyStep {
        @Test
        fun `Submitting with no option selected returns an error`(page: Page) {
            val hasGasSupplyPage = navigator.skipToPropertyRegistrationHasGasSupplyPage()
            hasGasSupplyPage.form.submitPrimaryButton()
            assertThat(
                hasGasSupplyPage.form.getErrorMessage(),
            ).containsText("Select whether you have a gas supply or any gas appliances")
        }

        @Test
        fun `Submitting No navigates to the check you gas answers step`(page: Page) {
            val hasGasSupplyPage = navigator.skipToPropertyRegistrationHasGasSupplyPage()
            hasGasSupplyPage.submitHasNoGasSupply()
            assertPageIs(page, CheckGasSafetyAnswersFormPagePropertyRegistration::class)
        }
    }

    @Nested
    inner class HasGasSafetyCertStep {
        @Test
        fun `Submitting with the Continue button with no option selected returns an error`(page: Page) {
            val hasGasSafetyCertPage = navigator.skipToPropertyRegistrationHasGasCertPage()
            hasGasSafetyCertPage.form.submit()
            assertThat(
                hasGasSafetyCertPage.form.getErrorMessage(),
            ).containsText("Select whether you have a gas safety certificate")
        }
    }

    @Nested
    inner class GasSafetyIssueDateStepTests {
        @ParameterizedTest(name = "{0}")
        @Suppress("ktlint:standard:max-line-length")
        @MethodSource(
            "uk.gov.communities.prsdb.webapp.testHelpers.parameterProviders.TodayOrPastDateValidationTestParameterProvider#provideInvalidDateStrings",
        )
        fun `Submitting returns a corresponding error when`(
            dayMonthYear: Triple<String, String, String>,
            expectedErrorMessage: String,
        ) {
            val (day, month, year) = dayMonthYear
            val gasSafetyIssueDatePage = navigator.skipToPropertyRegistrationGasCertIssueDatePage()
            gasSafetyIssueDatePage.submitDate(day, month, year)
            assertThat(gasSafetyIssueDatePage.form.getErrorMessage()).containsText(expectedErrorMessage)
        }
    }

    @Nested
    inner class CheckGasSafetyAnswersStep {
        @Test
        fun `No gas supply - gas supply change link navigates to has gas supply page`(page: Page) {
            val cyaPage =
                navigator.skipToPropertyRegistrationCheckGasSafetyAnswersPage(
                    PropertyStateSessionBuilder.beforePropertyRegistrationCheckGasSafetyAnswersNoGasSupply(),
                )
            cyaPage.gasSupplySummaryList.gasSupplyRow.clickFirstActionLinkAndWait()
            assertPageIs(page, HasGasSupplyFormPagePropertyRegistration::class)
        }

        @Test
        fun `Uploaded cert - gas supply change link navigates to has gas supply page`(page: Page) {
            val cyaPage =
                navigator.skipToPropertyRegistrationCheckGasSafetyAnswersPage(
                    PropertyStateSessionBuilder.beforePropertyRegistrationCheckGasSafetyAnswersUploadedCert(),
                )
            cyaPage.gasSupplySummaryList.gasSupplyRow.clickFirstActionLinkAndWait()
            assertPageIs(page, HasGasSupplyFormPagePropertyRegistration::class)
        }

        @Test
        fun `Uploaded cert - valid gas cert change link navigates to has gas cert page`(page: Page) {
            val cyaPage =
                navigator.skipToPropertyRegistrationCheckGasSafetyAnswersPage(
                    PropertyStateSessionBuilder.beforePropertyRegistrationCheckGasSafetyAnswersUploadedCert(),
                )
            cyaPage.certSummaryList.validGasCertRow.clickFirstActionLinkAndWait()
            assertPageIs(page, HasGasCertFormPagePropertyRegistration::class)
        }

        @Test
        fun `Uploaded cert - issue date change link navigates to issue date page`(page: Page) {
            val cyaPage =
                navigator.skipToPropertyRegistrationCheckGasSafetyAnswersPage(
                    PropertyStateSessionBuilder.beforePropertyRegistrationCheckGasSafetyAnswersUploadedCert(),
                )
            cyaPage.certSummaryList.issueDateRow.clickFirstActionLinkAndWait()
            assertPageIs(page, GasCertIssueDateFormPagePropertyRegistration::class)
        }

        @Test
        fun `Uploaded cert - certificate change link navigates to check uploads page`(page: Page) {
            val cyaPage =
                navigator.skipToPropertyRegistrationCheckGasSafetyAnswersPage(
                    PropertyStateSessionBuilder.beforePropertyRegistrationCheckGasSafetyAnswersUploadedCert(),
                )
            cyaPage.certSummaryList.yourCertificateRow.clickFirstActionLinkAndWait()
            assertPageIs(page, CheckGasCertUploadsFormPagePropertyRegistration::class)
        }

        @Test
        fun `Provide later - gas supply change link navigates to has gas supply page`(page: Page) {
            val cyaPage =
                navigator.skipToPropertyRegistrationCheckGasSafetyAnswersPage(
                    PropertyStateSessionBuilder.beforePropertyRegistrationCheckGasSafetyAnswersProvideLater(),
                )
            cyaPage.gasSupplySummaryList.gasSupplyRow.clickFirstActionLinkAndWait()
            assertPageIs(page, HasGasSupplyFormPagePropertyRegistration::class)
        }

        @Test
        fun `No cert - gas cert change link navigates to has gas cert page`(page: Page) {
            val cyaPage =
                navigator.skipToPropertyRegistrationCheckGasSafetyAnswersPage(
                    PropertyStateSessionBuilder.beforePropertyRegistrationCheckGasSafetyAnswersNoCert(),
                )
            cyaPage.gasSupplySummaryList.gasCertRow.clickFirstActionLinkAndWait()
            assertPageIs(page, HasGasCertFormPagePropertyRegistration::class)
        }

        @Test
        fun `Cert expired - gas cert change link navigates to has gas cert page`(page: Page) {
            val cyaPage =
                navigator.skipToPropertyRegistrationCheckGasSafetyAnswersPage(
                    PropertyStateSessionBuilder.beforePropertyRegistrationCheckGasSafetyAnswersCertExpired(),
                )
            cyaPage.gasSupplySummaryList.gasCertRow.clickFirstActionLinkAndWait()
            assertPageIs(page, HasGasCertFormPagePropertyRegistration::class)
        }
    }
}
