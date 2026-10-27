package uk.gov.communities.prsdb.webapp.integration

import com.microsoft.playwright.Page
import com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.communities.prsdb.webapp.constants.ASSOCIATED_PARTIES
import uk.gov.communities.prsdb.webapp.integration.pageObjects.components.BaseComponent
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.basePages.BasePage.Companion.assertPageIs
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyRegistrationJourneyPages.EpcExemptionFormPagePropertyRegistration
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyRegistrationJourneyPages.MeesExemptionFormPagePropertyRegistration
import uk.gov.communities.prsdb.webapp.testHelpers.builders.PropertyStateSessionBuilder

class PropertyRegistrationEpcSinglePageTests : IntegrationTestWithImmutableData("data-local.sql") {
    // TODO PDJB-959: remove this override and update these tests for the associated parties journey
    @BeforeEach
    fun disableAssociatedParties() {
        featureFlagManager.disableFeature(ASSOCIATED_PARTIES)
    }

    @Nested
    inner class HasEpcStepTests {
        @Test
        fun `Submitting with the Continue button with no option selected returns an error`(page: Page) {
            val hasEpcPage = navigator.skipToPropertyRegistrationHasEpcPage()
            hasEpcPage.form.submitPrimaryButton()
            assertThat(hasEpcPage.form.getErrorMessage())
                .containsText("Select if you have an EPC for this property")
        }
    }

    @Nested
    inner class FindYourEpcStepTests {
        @Test
        fun `Submitting with no option selected returns an error`(page: Page) {
            val findYourEpcPage = navigator.skipToPropertyRegistrationFindYourEpcPage()
            findYourEpcPage.form.submit()
            assertThat(findYourEpcPage.form.getErrorMessage())
                .containsText("Enter a certificate number")
        }
    }

    @Nested
    inner class ConfirmEpcDetailsRetrievedByCertificateNumberStepTests {
        @Test
        fun `User sees a validation error when they do not select an answer`(page: Page) {
            val confirmEpcDetailsPage =
                navigator.skipToPropertyRegistrationConfirmEpcDetailsRetrievedByCertificateNumberPage()
            confirmEpcDetailsPage.form.submit()
            assertThat(confirmEpcDetailsPage.form.getErrorMessage())
                .containsText("Select if you want to use this EPC")
        }
    }

    @Nested
    inner class IsEpcRequiredStepTests {
        @Test
        fun `Submitting with no option selected returns a validation error`(page: Page) {
            val isEpcRequiredPage = navigator.skipToPropertyRegistrationIsEpcRequiredPage()
            isEpcRequiredPage.form.submit()
            assertThat(isEpcRequiredPage.form.getErrorMessage())
                .containsText("Select if an EPC is required to let this property")
        }
    }

    @Nested
    inner class ConfirmEpcDetailsByUprnStepTests {
        @Test
        fun `User sees a validation error when they do not select an answer`(page: Page) {
            val confirmEpcDetailsPage =
                navigator.skipToPropertyRegistrationConfirmEpcDetailsByUprnPage()
            confirmEpcDetailsPage.form.submit()
            assertThat(confirmEpcDetailsPage.form.getErrorMessage())
                .containsText("Select if you want to use the EPC we found for your property")
        }
    }

    @Nested
    inner class MeesExemptionStepTests {
        @Test
        fun `User sees a validation error when they do not select a MEES exemption reason`(page: Page) {
            val meesExemptionPage = navigator.skipToPropertyRegistrationMeesExemptionPage()

            meesExemptionPage.form.submit()

            assertPageIs(page, MeesExemptionFormPagePropertyRegistration::class)
            assertThat(meesExemptionPage.form.getErrorMessage())
                .containsText("Select the energy efficiency exemption you registered for this property")
        }
    }

    @Nested
    inner class EpcExemptionStepTests {
        @Test
        fun `User sees a validation error when they do not select an EPC exemption reason`(page: Page) {
            val epcExemptionPage = navigator.skipToPropertyRegistrationEpcExemptionPage()

            epcExemptionPage.form.submit()

            assertPageIs(page, EpcExemptionFormPagePropertyRegistration::class)
            assertThat(epcExemptionPage.form.getErrorMessage()).isVisible()
        }
    }

    @Nested
    inner class EpcInDateAtStartOfTenancyCheckStep {
        @Test
        fun `Submitting with no option selected returns an error`() {
            val epcInDateAtStartOfTenancyCheckPage = navigator.skipToPropertyRegistrationEpcInDateAtStartOfTenancyCheckPage()
            epcInDateAtStartOfTenancyCheckPage.form.submit()
            assertThat(epcInDateAtStartOfTenancyCheckPage.form.getErrorMessage())
                .containsText("Select if the EPC was still in date when the current tenancy began")
        }

        @Test
        fun `Page displays the EPC expiry date in the body text and Yes radio hint`() {
            val epcInDateAtStartOfTenancyCheckPage = navigator.skipToPropertyRegistrationEpcInDateAtStartOfTenancyCheckPage()
            assertThat(epcInDateAtStartOfTenancyCheckPage.bodyParagraph).containsText("5 January 2022")
            assertThat(epcInDateAtStartOfTenancyCheckPage.form.yesHint).containsText("5 January 2022")
        }
    }

    @Nested
    inner class HasMeesExemptionStep {
        @Test
        fun `Submitting with no option selected returns an error`() {
            val hasMeesExemptionPage = navigator.skipToPropertyRegistrationHasMeesExemptionPage()
            hasMeesExemptionPage.form.submit()
            assertThat(hasMeesExemptionPage.form.getErrorMessage())
                .containsText("Select if you have a registered energy efficiency exemption for this property")
        }
    }

    @Nested
    inner class EpcMissingStep {
        @Test
        fun `The page renders the occupied variant for an occupied property`(page: Page) {
            val epcMissingPage = navigator.skipToPropertyRegistrationEpcMissingPage(propertyIsOccupied = true)
            BaseComponent.assertThat(epcMissingPage.heading).containsText("Your property is missing an EPC")
            BaseComponent.assertThat(epcMissingPage.warning).isVisible()
            BaseComponent.assertThat(epcMissingPage.continueAnywayButton).hasText("Continue anyway")
        }

        @Test
        fun `The page renders the unoccupied variant for an unoccupied property`(page: Page) {
            val epcMissingPage = navigator.skipToPropertyRegistrationEpcMissingPage(propertyIsOccupied = false)
            BaseComponent.assertThat(epcMissingPage.heading).containsText("Your property is missing an EPC")
            BaseComponent.assertThat(epcMissingPage.warning).isHidden()
            BaseComponent.assertThat(epcMissingPage.continueButton).hasText("Continue")
        }
    }

    @Nested
    inner class EpcExpiredStep {
        @Test
        fun `The page renders the occupied variant for an occupied property`(page: Page) {
            val epcExpiredPage = navigator.skipToPropertyRegistrationEpcExpiredPage(propertyIsOccupied = true)
            BaseComponent.assertThat(epcExpiredPage.heading).containsText("This property’s EPC has expired")
            BaseComponent.assertThat(epcExpiredPage.warning).isVisible()
            BaseComponent.assertThat(epcExpiredPage.submitButton).hasText("Continue anyway")
        }

        @Test
        fun `The page renders the unoccupied variant for an unoccupied property`(page: Page) {
            val epcExpiredPage = navigator.skipToPropertyRegistrationEpcExpiredPage(propertyIsOccupied = false)
            BaseComponent.assertThat(epcExpiredPage.heading).containsText("This property’s EPC has expired")
            BaseComponent.assertThat(epcExpiredPage.warning).isHidden()
            BaseComponent.assertThat(epcExpiredPage.submitButton).hasText("Continue")
        }

        @Test
        fun `The expiry date is displayed in bold on the occupied variant`(page: Page) {
            val epcExpiredPage = navigator.skipToPropertyRegistrationEpcExpiredPage(propertyIsOccupied = true)
            assertThat(epcExpiredPage.expiryDateParagraph.locator("strong")).hasText("5 January 2022")
        }

        @Test
        fun `The expiry date is displayed in bold on the unoccupied variant`(page: Page) {
            val epcExpiredPage = navigator.skipToPropertyRegistrationEpcExpiredPage(propertyIsOccupied = false)
            assertThat(epcExpiredPage.expiryDateParagraph.locator("strong")).hasText("5 January 2022")
        }
    }

    @Nested
    inner class LowEnergyRatingStep {
        @Test
        fun `The page renders the occupied variant for an occupied property`(page: Page) {
            val lowEnergyRatingPage = navigator.skipToPropertyRegistrationLowEnergyRatingPage(propertyIsOccupied = true)
            BaseComponent.assertThat(lowEnergyRatingPage.heading).containsText(
                "This property does not meet energy efficiency requirements for letting",
            )
            BaseComponent.assertThat(lowEnergyRatingPage.continueAnywayButton).containsText("Continue anyway")
        }

        @Test
        fun `The page renders the unoccupied variant for an unoccupied property`(page: Page) {
            val lowEnergyRatingPage = navigator.skipToPropertyRegistrationLowEnergyRatingPage(propertyIsOccupied = false)
            BaseComponent.assertThat(lowEnergyRatingPage.heading).containsText(
                "You’ll need to get a new EPC before letting this property",
            )
            BaseComponent.assertThat(lowEnergyRatingPage.continueButton).containsText("Continue")
        }
    }

    @Nested
    inner class ProvideEpcLaterStep {
        @Test
        fun `The page renders the occupied variant for an occupied property`(page: Page) {
            val provideEpcLaterPage = navigator.skipToPropertyRegistrationProvideEpcLaterPage(propertyIsOccupied = true)
            BaseComponent.assertThat(provideEpcLaterPage.heading).containsText("Provide your EPC details later")
            BaseComponent.assertThat(provideEpcLaterPage.insetText).containsText(
                "To keep the property registered, we need all its compliance certificates within 28 days.",
            )
        }

        @Test
        fun `The page renders the unoccupied variant for an unoccupied property`(page: Page) {
            val provideEpcLaterPage = navigator.skipToPropertyRegistrationProvideEpcLaterPage(propertyIsOccupied = false)
            BaseComponent.assertThat(provideEpcLaterPage.heading).containsText("Provide your EPC details later")
            BaseComponent.assertThat(provideEpcLaterPage.insetText).isHidden()
        }
    }

    @Nested
    inner class CheckEpcAnswersStep {
        @Test
        fun `Shows EPC card with meets requirements inset for a compliant unexpired EPC`(page: Page) {
            val cyaPage =
                navigator.skipToPropertyRegistrationCheckEpcAnswers(
                    PropertyStateSessionBuilder.beforePropertyRegistrationCheckEpcAnswersCompliantEpc(),
                )

            BaseComponent.assertThat(cyaPage.epcCard).isVisible()
            assertThat(cyaPage.meetsRequirementsInset).isVisible()
            assertThat(cyaPage.epcExpiredText).isHidden()
            assertThat(cyaPage.lowRatingText).isHidden()
            assertThat(cyaPage.lowRatingOccupiedInset).isHidden()
            assertThat(cyaPage.occupiedNoEpcInset).isHidden()
        }

        @Test
        fun `Shows EPC card and MEES exemption rows for an unexpired EPC with low rating and exemption`(page: Page) {
            val cyaPage =
                navigator.skipToPropertyRegistrationCheckEpcAnswers(
                    PropertyStateSessionBuilder.beforePropertyRegistrationCheckEpcAnswersLowRatingWithExemption(),
                )

            BaseComponent.assertThat(cyaPage.epcCard).isVisible()
            assertThat(cyaPage.lowRatingText).isVisible()
            assertThat(cyaPage.rows.hasMeesExemptionRow.value).containsText("Yes")
            assertThat(cyaPage.rows.meesExemptionRow.value).isVisible()
            assertThat(cyaPage.meetsRequirementsInset).isHidden()
            assertThat(cyaPage.lowRatingOccupiedInset).isHidden()
        }

        @Test
        fun `Shows EPC card, expired text, tenancy check row, and meets requirements inset for expired but valid EPC`(page: Page) {
            val cyaPage =
                navigator.skipToPropertyRegistrationCheckEpcAnswers(
                    PropertyStateSessionBuilder.beforePropertyRegistrationCheckEpcAnswersExpiredEpcInDateAtTenancyStart(),
                )

            BaseComponent.assertThat(cyaPage.epcCard).isVisible()
            assertThat(cyaPage.epcExpiredText).isVisible()
            assertThat(cyaPage.rows.tenancyCheckRow.value).containsText("Yes")
            assertThat(cyaPage.meetsRequirementsInset).isVisible()
            assertThat(cyaPage.lowRatingText).isHidden()
        }

        @Test
        fun `Shows EPC card, expired text, tenancy check, low rating text, and MEES rows for expired EPC with low rating and exemption`(
            page: Page,
        ) {
            val cyaPage =
                navigator.skipToPropertyRegistrationCheckEpcAnswers(
                    PropertyStateSessionBuilder.beforePropertyRegistrationCheckEpcAnswersExpiredEpcLowRatingWithExemption(),
                )

            BaseComponent.assertThat(cyaPage.epcCard).isVisible()
            assertThat(cyaPage.epcExpiredText).isVisible()
            assertThat(cyaPage.rows.tenancyCheckRow.value).containsText("Yes")
            assertThat(cyaPage.lowRatingText).isVisible()
            assertThat(cyaPage.rows.hasMeesExemptionRow.value).containsText("Yes")
            assertThat(cyaPage.rows.meesExemptionRow.value).isVisible()
            assertThat(cyaPage.meetsRequirementsInset).isHidden()
        }

        @Test
        fun `Shows hasEpc row with occupied provide-later text for occupied property choosing to provide EPC later`() {
            val cyaPage =
                navigator.skipToPropertyRegistrationCheckEpcAnswers(
                    PropertyStateSessionBuilder.beforePropertyRegistrationCheckEpcAnswersProvideLaterOccupied(),
                )

            assertThat(cyaPage.rows.hasEpcRow.value).containsText("Provide this later")
            BaseComponent.assertThat(cyaPage.epcCard).isHidden()
            assertThat(cyaPage.meetsRequirementsInset).isHidden()
        }

        @Test
        fun `Shows hasEpc row with unoccupied provide-later text for unoccupied property choosing to provide EPC later`(page: Page) {
            val cyaPage =
                navigator.skipToPropertyRegistrationCheckEpcAnswers(
                    PropertyStateSessionBuilder.beforePropertyRegistrationCheckEpcAnswersProvideLaterUnoccupied(),
                )

            assertThat(cyaPage.rows.hasEpcRow.value).containsText("within 28 days of the property being occupied")
            BaseComponent.assertThat(cyaPage.epcCard).isHidden()
        }

        @Suppress("ktlint:standard:max-line-length")
        @Test
        fun `Shows EPC card, low rating text, no exemption row, and council inset for occupied property with low rating and no MEES exemption`(
            page: Page,
        ) {
            val cyaPage =
                navigator.skipToPropertyRegistrationCheckEpcAnswers(
                    PropertyStateSessionBuilder.beforePropertyRegistrationCheckEpcAnswersLowRatingNoExemptionOccupied(),
                )

            BaseComponent.assertThat(cyaPage.epcCard).isVisible()
            assertThat(cyaPage.lowRatingText).isVisible()
            assertThat(cyaPage.rows.hasMeesExemptionRow.value).containsText("No")
            assertThat(cyaPage.lowRatingOccupiedInset).isVisible()
            assertThat(cyaPage.meetsRequirementsInset).isHidden()
        }

        @Test
        fun `Shows provide EPC later row for unoccupied property with low rating and no MEES exemption`(page: Page) {
            val cyaPage =
                navigator.skipToPropertyRegistrationCheckEpcAnswers(
                    PropertyStateSessionBuilder.beforePropertyRegistrationCheckEpcAnswersLowRatingNoExemptionUnoccupied(),
                )

            BaseComponent.assertThat(cyaPage.epcCard).isHidden()
            assertThat(cyaPage.rows.hasEpcRow.value)
                .containsText("Provide this later (within 28 days of the property being occupied)")
            assertThat(cyaPage.lowRatingText).isHidden()
            assertThat(cyaPage.lowRatingOccupiedInset).isHidden()
        }

        @Test
        fun `Shows hasEpc, isEpcRequired, and exemption reason rows for property with no EPC that is exempt`(page: Page) {
            val cyaPage =
                navigator.skipToPropertyRegistrationCheckEpcAnswers(
                    PropertyStateSessionBuilder.beforePropertyRegistrationCheckEpcAnswersNoEpcExempt(),
                )

            assertThat(cyaPage.rows.hasEpcRow.value).containsText("No")
            assertThat(cyaPage.rows.isEpcRequiredRow.value).containsText("No")
            assertThat(cyaPage.rows.epcExemptionRow.value).isVisible()
            BaseComponent.assertThat(cyaPage.epcCard).isHidden()
            assertThat(cyaPage.occupiedNoEpcInset).isHidden()
        }

        @Suppress("ktlint:standard:max-line-length")
        @Test
        fun `Shows EPC card, expired text, tenancy check, low rating text, no exemption row, and council inset for occupied property with expired low-rating EPC in date at tenancy start and no exemption`() {
            val cyaPage =
                navigator.skipToPropertyRegistrationCheckEpcAnswers(
                    PropertyStateSessionBuilder.beforePropertyRegistrationCheckEpcAnswersExpiredEpcLowRatingNoExemptionOccupied(),
                )

            BaseComponent.assertThat(cyaPage.epcCard).isVisible()
            assertThat(cyaPage.epcExpiredText).isVisible()
            assertThat(cyaPage.rows.tenancyCheckRow.value).containsText("Yes")
            assertThat(cyaPage.lowRatingText).isVisible()
            assertThat(cyaPage.rows.hasMeesExemptionRow.value).containsText("No")
            assertThat(cyaPage.lowRatingOccupiedInset).isVisible()
            assertThat(cyaPage.meetsRequirementsInset).isHidden()
        }

        @Test
        fun `Shows hasEpc and isEpcRequired rows with council inset for occupied property with no EPC that is required`(page: Page) {
            val cyaPage =
                navigator.skipToPropertyRegistrationCheckEpcAnswers(
                    PropertyStateSessionBuilder.beforePropertyRegistrationCheckEpcAnswersNoEpcOccupiedNotExempt(),
                )

            assertThat(cyaPage.rows.hasEpcRow.value).containsText("No")
            assertThat(cyaPage.rows.isEpcRequiredRow.value).containsText("Yes")
            assertThat(cyaPage.occupiedNoEpcInset).isVisible()
            BaseComponent.assertThat(cyaPage.epcCard).isHidden()
            assertThat(cyaPage.rows.epcExemptionRow.key).isHidden()
        }
    }
}
