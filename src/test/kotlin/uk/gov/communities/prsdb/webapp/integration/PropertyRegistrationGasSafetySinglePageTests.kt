package uk.gov.communities.prsdb.webapp.integration

import com.microsoft.playwright.Page
import com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import uk.gov.communities.prsdb.webapp.constants.ASSOCIATED_PARTIES
import uk.gov.communities.prsdb.webapp.constants.GAS_CERTIFICATE_14_MONTH_EXPIRY
import uk.gov.communities.prsdb.webapp.constants.LANDLORD_GAS_SAFETY_URL
import uk.gov.communities.prsdb.webapp.integration.pageObjects.components.BaseComponent.Companion.assertThat
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.basePages.BasePage.Companion.assertPageIs
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyRegistrationJourneyPages.CheckGasCertUploadsFormPagePropertyRegistration
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyRegistrationJourneyPages.CheckGasSafetyAnswersFormPagePropertyRegistration
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyRegistrationJourneyPages.GasCertIssueDateFormPagePropertyRegistration
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyRegistrationJourneyPages.HasGasCertFormPagePropertyRegistration
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyRegistrationJourneyPages.HasGasSupplyFormPagePropertyRegistration
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyRegistrationJourneyPages.ProvideGasCertLaterFormPagePropertyRegistration
import uk.gov.communities.prsdb.webapp.testHelpers.builders.PropertyStateSessionBuilder
import java.util.regex.Pattern

class PropertyRegistrationGasSafetySinglePageTests : IntegrationTestWithImmutableData("data-local.sql") {
    // TODO PDJB-959: remove this override and update these tests for the associated parties journey
    @BeforeEach
    fun disableAssociatedParties() {
        featureFlagManager.disableFeature(ASSOCIATED_PARTIES)
    }

    @Nested
    inner class HasGasSupplyStepTests {
        @Test
        fun `Submitting with no option selected returns an error`() {
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

        @Test
        fun `Submitting Provide this later navigates to the provide gas cert later page`(page: Page) {
            val hasGasSupplyPage = navigator.skipToPropertyRegistrationHasGasSupplyPage()
            hasGasSupplyPage.submitProvideThisLater()
            assertPageIs(page, ProvideGasCertLaterFormPagePropertyRegistration::class)
        }
    }

    @Nested
    inner class HasGasSafetyCertStep {
        @Test
        fun `Submitting with the Continue button with no option selected returns an error`() {
            val hasGasSafetyCertPage = navigator.skipToPropertyRegistrationHasGasCertPage()
            hasGasSafetyCertPage.form.submit()
            assertThat(
                hasGasSafetyCertPage.form.getErrorMessage(),
            ).containsText("Select whether you have a gas safety certificate")
        }
    }

    @Nested
    inner class ProvideGasCertLaterStepTests {
        @Test
        fun `Occupied property shows the provide later content`(page: Page) {
            val provideLaterPage = navigateToProvideLaterPage(page, isOccupied = true)

            assertThat(provideLaterPage.heading).hasText("Provide these details later")
            assertThat(provideLaterPage.insetText).hasText(
                "To keep the property registered, we need to know about its gas safety within 28 days.",
            )

            provideLaterPage.form.submit()
            assertPageIs(page, CheckGasSafetyAnswersFormPagePropertyRegistration::class)
        }

        @Test
        fun `Unoccupied property shows the provide later content`(page: Page) {
            val provideLaterPage = navigateToProvideLaterPage(page, isOccupied = false)

            assertThat(provideLaterPage.heading).hasText("Provide these details later")
            assertThat(provideLaterPage.paragraphs.first()).hasText(
                "If your property has a gas supply or any gas appliances, you must get a gas safety certificate before a tenant moves in.",
            )
            assertThat(provideLaterPage.gasSafetyLink).hasAttribute("href", LANDLORD_GAS_SAFETY_URL)

            provideLaterPage.form.submit()
            assertPageIs(page, CheckGasSafetyAnswersFormPagePropertyRegistration::class)
        }

        private fun navigateToProvideLaterPage(
            page: Page,
            isOccupied: Boolean,
        ): ProvideGasCertLaterFormPagePropertyRegistration {
            val gasSupplyPage = navigator.skipToPropertyRegistrationHasGasSupplyPage(propertyIsOccupied = isOccupied)
            gasSupplyPage.submitProvideThisLater()
            return assertPageIs(page, ProvideGasCertLaterFormPagePropertyRegistration::class)
        }
    }

    @Nested
    inner class GasSafetyIssueDateStepTests {
        @Test
        fun `Issue date page displays heading`() {
            // TODO PDJB-1926: Remove the feature flag setup when GAS_CERTIFICATE_14_MONTH_EXPIRY is removed.
            featureFlagManager.enableFeature(GAS_CERTIFICATE_14_MONTH_EXPIRY)

            val issueDatePage = navigator.skipToPropertyRegistrationGasCertIssueDatePage()

            assertThat(issueDatePage.heading).containsText("When was this certificate issued?")
        }

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

    // TODO PDJB-1926: Remove this test class when GAS_CERTIFICATE_14_MONTH_EXPIRY is removed.
    @Nested
    inner class BeforePdjb1898GasSafetyIssueDateStepTests {
        @Test
        fun `Issue date page retains existing content without 14 month expiry enabled`() {
            featureFlagManager.disableFeature(GAS_CERTIFICATE_14_MONTH_EXPIRY)

            val issueDatePage = navigator.skipToPropertyRegistrationGasCertIssueDatePage()

            assertThat(issueDatePage.heading).containsText("What’s the issue date on the gas safety certificate?")
        }
    }

    @Nested
    inner class CheckGasCertUploadsStep {
        @Test
        fun `The actions column header text is visually hidden but the header cell is not`(page: Page) {
            val checkUploadsPage = navigateToCheckUploadsPage(page)

            assertThat(checkUploadsPage.table.actionsHeaderVisuallyHiddenText).hasText("Actions")
            assertThat(checkUploadsPage.table.actionsHeader).not().hasClass(Pattern.compile("govuk-visually-hidden"))
        }

        @Test
        fun `The remove link includes the file name as visually hidden text`(page: Page) {
            val checkUploadsPage = navigateToCheckUploadsPage(page)

            assertThat(checkUploadsPage.table.getClickableCell(0, 2).link).hasText("Remove gas-safety-cert.pdf")
            assertThat(checkUploadsPage.table.getVisuallyHiddenText(0, 2)).hasText("gas-safety-cert.pdf")
        }

        private fun navigateToCheckUploadsPage(page: Page): CheckGasCertUploadsFormPagePropertyRegistration {
            val cyaPage =
                navigator.skipToPropertyRegistrationCheckGasSafetyAnswersPage(
                    PropertyStateSessionBuilder.beforePropertyRegistrationCheckGasSafetyAnswersUploadedCert(),
                )
            cyaPage.certSummaryList.yourCertificateRow.clickFirstActionLinkAndWait()
            return assertPageIs(page, CheckGasCertUploadsFormPagePropertyRegistration::class)
        }
    }

    @Nested
    inner class CheckGasSafetyAnswersStep {
        @Test
        fun `Uploaded cert - issue date change link includes the row heading as visually hidden text`() {
            val cyaPage =
                navigator.skipToPropertyRegistrationCheckGasSafetyAnswersPage(
                    PropertyStateSessionBuilder.beforePropertyRegistrationCheckGasSafetyAnswersUploadedCert(),
                )

            assertThat(cyaPage.certSummaryList.issueDateRow.actions.firstActionLink).hasText("Change Issue date")
            assertThat(cyaPage.certSummaryList.issueDateRow.actions.firstActionLinkVisuallyHiddenText).hasText("Issue date")
        }

        @Test
        fun `Uploaded cert - issue date change link is not wrapped in an actions list`() {
            val cyaPage =
                navigator.skipToPropertyRegistrationCheckGasSafetyAnswersPage(
                    PropertyStateSessionBuilder.beforePropertyRegistrationCheckGasSafetyAnswersUploadedCert(),
                )

            assertThat(cyaPage.certSummaryList.issueDateRow.actions.firstActionLink).isVisible()
            assertThat(cyaPage.certSummaryList.issueDateRow.actions.actionsList).hasCount(0)
        }

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
        fun `No cert - gas cert change link navigates to has gas cert page`(page: Page) {
            val cyaPage =
                navigator.skipToPropertyRegistrationCheckGasSafetyAnswersPage(
                    PropertyStateSessionBuilder.beforePropertyRegistrationCheckGasSafetyAnswersNoCert(),
                )
            cyaPage.gasSupplySummaryList.gasCertRow.clickFirstActionLinkAndWait()
            assertPageIs(page, HasGasCertFormPagePropertyRegistration::class)
        }

        @Nested
        inner class ProvideLater {
            @Test
            fun `A single provide this later row is shown against the gas supply question`() {
                val cyaPage =
                    navigator.skipToPropertyRegistrationCheckGasSafetyAnswersPage(
                        PropertyStateSessionBuilder.beforePropertyRegistrationCheckGasSafetyAnswersProvideLater(),
                    )
                assertThat(cyaPage.gasSupplySummaryList.gasSupplyRow.value).containsText("Provide this later")
                assertThat(cyaPage.gasSupplySummaryList.gasCertRow.value).hasCount(0)
            }

            @Test
            fun `The gas supply change link navigates to has gas supply page`(page: Page) {
                val cyaPage =
                    navigator.skipToPropertyRegistrationCheckGasSafetyAnswersPage(
                        PropertyStateSessionBuilder.beforePropertyRegistrationCheckGasSafetyAnswersProvideLater(),
                    )
                cyaPage.gasSupplySummaryList.gasSupplyRow.clickFirstActionLinkAndWait()
                assertPageIs(page, HasGasSupplyFormPagePropertyRegistration::class)
            }
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
