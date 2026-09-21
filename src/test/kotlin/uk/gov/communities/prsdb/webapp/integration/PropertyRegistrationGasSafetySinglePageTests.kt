package uk.gov.communities.prsdb.webapp.integration

import com.microsoft.playwright.Page
import com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import uk.gov.communities.prsdb.webapp.constants.DELEGATE_TO_LETTING_AGENT
import uk.gov.communities.prsdb.webapp.constants.LANDLORD_GAS_SAFETY_URL
import uk.gov.communities.prsdb.webapp.constants.PROPERTY_REGISTRATION_RESTRUCTURE_AND_SKIPPING
import uk.gov.communities.prsdb.webapp.integration.pageObjects.components.BaseComponent.Companion.assertThat
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.basePages.BasePage.Companion.assertPageIs
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyRegistrationJourneyPages.CheckGasCertUploadsFormPagePropertyRegistration
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyRegistrationJourneyPages.CheckGasSafetyAnswersFormPagePropertyRegistration
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyRegistrationJourneyPages.GasCertIssueDateFormPagePropertyRegistration
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyRegistrationJourneyPages.HasGasCertFormPagePropertyRegistration
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyRegistrationJourneyPages.HasGasSupplyFormPagePropertyRegistration
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyRegistrationJourneyPages.ProvideGasCertLaterFormPagePropertyRegistration
import uk.gov.communities.prsdb.webapp.testHelpers.builders.PropertyStateSessionBuilder

class PropertyRegistrationGasSafetySinglePageTests : IntegrationTestWithImmutableData("data-local.sql") {
    @BeforeEach
    fun enableRestructureAndSkippingFlag() {
        featureFlagManager.enableFeature(PROPERTY_REGISTRATION_RESTRUCTURE_AND_SKIPPING)
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
        fun `Submitting with the Continue button with no option selected returns an error`(page: Page) {
            val hasGasSafetyCertPage = navigator.skipToPropertyRegistrationHasGasCertPage()
            hasGasSafetyCertPage.form.submit()
            assertThat(
                hasGasSafetyCertPage.form.getErrorMessage(),
            ).containsText("Select whether you have a gas safety certificate")
        }
    }

    @Nested
    inner class ProvideGasCertLaterStep {
        @BeforeEach
        fun enableLettingAgentsFlag() {
            featureFlagManager.enableFeature(DELEGATE_TO_LETTING_AGENT)
        }

        @Test
        fun `Occupied property shows updated provide later content when letting agents is enabled`(page: Page) {
            val provideLaterPage = navigateToProvideLaterPage(page, isOccupied = true)

            assertThat(provideLaterPage.heading).hasText("Provide these details later")
            assertThat(provideLaterPage.insetText).hasText(
                "To keep the property registered, we need to know about its gas safety within 28 days.",
            )

            provideLaterPage.form.submit()
            assertPageIs(page, CheckGasSafetyAnswersFormPagePropertyRegistration::class)
        }

        @Test
        fun `Unoccupied property shows updated provide later content when letting agents is enabled`(page: Page) {
            val provideLaterPage = navigateToProvideLaterPage(page, isOccupied = false)

            assertThat(provideLaterPage.heading).hasText("Provide these details later")
            assertThat(provideLaterPage.paragraphs.first()).hasText(
                "If your property has a gas supply or any gas appliances, you must get a gas safety certificate before a tenant moves in.",
            )
            assertThat(provideLaterPage.gasSafetyLink).hasAttribute("href", LANDLORD_GAS_SAFETY_URL)

            provideLaterPage.form.submit()
            assertPageIs(page, CheckGasSafetyAnswersFormPagePropertyRegistration::class)
        }

        @Test
        fun `Occupied property keeps legacy provide later content when letting agents is disabled`(page: Page) {
            featureFlagManager.disableFeature(DELEGATE_TO_LETTING_AGENT)
            val provideLaterPage = navigateToProvideLaterPage(page, isOccupied = true)

            assertThat(provideLaterPage.heading).hasText("Provide your gas safety certificate later")
            assertThat(provideLaterPage.insetText).hasText("You must upload your gas safety certificate within 28 days.")
            assertThat(provideLaterPage.gasSafetyLink).hasAttribute("href", LANDLORD_GAS_SAFETY_URL)

            provideLaterPage.form.submit()
            assertPageIs(page, CheckGasSafetyAnswersFormPagePropertyRegistration::class)
        }

        @Test
        fun `Unoccupied property keeps legacy provide later content when letting agents is disabled`(page: Page) {
            featureFlagManager.disableFeature(DELEGATE_TO_LETTING_AGENT)
            val provideLaterPage = navigateToProvideLaterPage(page, isOccupied = false)

            assertThat(provideLaterPage.heading).hasText("Provide your gas safety certificate later")
            assertThat(provideLaterPage.paragraphs.first()).hasText("You must get a gas safety certificate before a tenant moves in.")
            assertThat(provideLaterPage.gasSafetyLink).hasAttribute("href", LANDLORD_GAS_SAFETY_URL)

            provideLaterPage.form.submit()
            assertPageIs(page, CheckGasSafetyAnswersFormPagePropertyRegistration::class)
        }

        private fun navigateToProvideLaterPage(
            page: Page,
            isOccupied: Boolean,
        ): ProvideGasCertLaterFormPagePropertyRegistration {
            val gasSupplyPage = navigator.skipToPropertyRegistrationHasGasSupplyPage(propertyIsOccupied = isOccupied)
            if (featureFlagManager.checkFeature(DELEGATE_TO_LETTING_AGENT)) {
                // With the flag enabled, "Provide this later" is submitted directly from the has-gas-supply page
                gasSupplyPage.submitProvideThisLater()
            } else {
                // With the flag disabled, "Provide this later" is only offered on the (legacy) has-gas-cert page
                gasSupplyPage.submitHasGasSupply()
                val gasCertPage = assertPageIs(page, HasGasCertFormPagePropertyRegistration::class)
                gasCertPage.submitProvideThisLater()
            }
            return assertPageIs(page, ProvideGasCertLaterFormPagePropertyRegistration::class)
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
            @Nested
            inner class WhenLettingAgentsEnabled {
                @BeforeEach
                fun enableLettingAgentsFlag() {
                    featureFlagManager.enableFeature(DELEGATE_TO_LETTING_AGENT)
                }

                @Test
                fun `a single provide this later row is shown against the gas supply question`(page: Page) {
                    val cyaPage =
                        navigator.skipToPropertyRegistrationCheckGasSafetyAnswersPage(
                            PropertyStateSessionBuilder.beforePropertyRegistrationCheckGasSafetyAnswersProvideLater(),
                        )
                    assertThat(cyaPage.gasSupplySummaryList.gasSupplyRow.value).containsText("Provide this later")
                    assertThat(cyaPage.gasSupplySummaryList.gasCertRow.value).hasCount(0)
                }

                @Test
                fun `the gas supply change link navigates to has gas supply page`(page: Page) {
                    val cyaPage =
                        navigator.skipToPropertyRegistrationCheckGasSafetyAnswersPage(
                            PropertyStateSessionBuilder.beforePropertyRegistrationCheckGasSafetyAnswersProvideLater(),
                        )
                    cyaPage.gasSupplySummaryList.gasSupplyRow.clickFirstActionLinkAndWait()
                    assertPageIs(page, HasGasSupplyFormPagePropertyRegistration::class)
                }
            }

            @Nested
            inner class WhenLettingAgentsDisabled {
                @BeforeEach
                fun disableLettingAgentsFlag() {
                    featureFlagManager.disableFeature(DELEGATE_TO_LETTING_AGENT)
                }

                @Test
                fun `separate gas supply and gas cert rows are shown`(page: Page) {
                    val cyaPage =
                        navigator.skipToPropertyRegistrationCheckGasSafetyAnswersPage(
                            PropertyStateSessionBuilder.beforePropertyRegistrationCheckGasSafetyAnswersProvideLaterFromGasCert(),
                        )
                    assertThat(cyaPage.gasSupplySummaryList.gasSupplyRow.value).containsText("Yes")
                    assertThat(cyaPage.gasSupplySummaryList.gasCertRow.value).containsText("Provide this later")
                }

                @Test
                fun `the gas cert change link navigates to has gas cert page`(page: Page) {
                    val cyaPage =
                        navigator.skipToPropertyRegistrationCheckGasSafetyAnswersPage(
                            PropertyStateSessionBuilder.beforePropertyRegistrationCheckGasSafetyAnswersProvideLaterFromGasCert(),
                        )
                    cyaPage.gasSupplySummaryList.gasCertRow.clickFirstActionLinkAndWait()
                    assertPageIs(page, HasGasCertFormPagePropertyRegistration::class)
                }
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
