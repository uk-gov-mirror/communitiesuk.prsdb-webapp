package uk.gov.communities.prsdb.webapp.integration

import com.microsoft.playwright.Page
import com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.communities.prsdb.webapp.constants.ASSOCIATED_PARTIES
import uk.gov.communities.prsdb.webapp.integration.pageObjects.components.BaseComponent
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.basePages.BasePage.Companion.assertPageIs
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyRegistrationJourneyPages.NumberOfPeopleFormPagePropertyRegistration

class PropertyRegistrationOccupancySinglePageTests : IntegrationTestWithImmutableData("data-local.sql") {
    // TODO PDJB-959: remove this override and update these tests for the associated parties journey
    @BeforeEach
    fun disableAssociatedParties() {
        featureFlagManager.disableFeature(ASSOCIATED_PARTIES)
    }

    @Nested
    inner class OccupancyStep {
        @Test
        fun `Submitting with no occupancy option selected returns an error`(page: Page) {
            val occupancyPage = navigator.skipToPropertyRegistrationOccupancyPage()
            occupancyPage.form.submit()
            assertThat(occupancyPage.form.getErrorMessage()).containsText("Select whether the property is occupied")
        }
    }

    @Nested
    inner class NumberOfHouseholdsStep {
        @Test
        fun `Submitting with a blank numberOfHouseholds field returns an error`() {
            val householdsPage = navigator.skipToTenancyDetailsHouseholdsPage()
            householdsPage.form.submitPrimaryButton()
            assertThat(householdsPage.form.getErrorMessage()).containsText("Enter how many separate households, like 1 or 2")
        }

        @Test
        fun `Submitting with a non-numerical value in the numberOfHouseholds field returns an error`() {
            val householdsPage = navigator.skipToTenancyDetailsHouseholdsPage()
            householdsPage.submitNumberOfHouseholds("not-a-number")
            assertThat(householdsPage.form.getErrorMessage())
                .containsText("Enter how many separate households, like 1 or 2")
        }

        @Test
        fun `Submitting with a non-integer number in the numberOfHouseholds field returns an error`() {
            val householdsPage = navigator.skipToTenancyDetailsHouseholdsPage()
            householdsPage.submitNumberOfHouseholds("2.3")
            assertThat(householdsPage.form.getErrorMessage())
                .containsText("Enter how many separate households, like 1 or 2")
        }

        @Test
        fun `Submitting with a negative integer in the numberOfHouseholds field returns an error`() {
            val householdsPage = navigator.skipToTenancyDetailsHouseholdsPage()
            householdsPage.submitNumberOfHouseholds(-2)
            assertThat(householdsPage.form.getErrorMessage())
                .containsText("Enter how many separate households, like 1 or 2")
        }

        @Test
        fun `Submitting with a zero integer in the numberOfHouseholds field returns an error`() {
            val householdsPage = navigator.skipToTenancyDetailsHouseholdsPage()
            householdsPage.submitNumberOfHouseholds(0)
            assertThat(householdsPage.form.getErrorMessage())
                .containsText("Enter how many separate households, like 1 or 2")
        }
    }

    @Nested
    inner class NumberOfPeopleStep {
        @Test
        fun `Submitting with a blank numberOfPeople field returns an error`(page: Page) {
            val peoplePage = navigator.skipToTenancyDetailsPeoplePage()
            peoplePage.form.submit()
            assertThat(peoplePage.form.getErrorMessage()).containsText("Enter how many people, like 2 or 5")
        }

        @Test
        fun `Submitting with a non-numerical value in the numberOfPeople field returns an error`(page: Page) {
            val peoplePage = navigator.skipToTenancyDetailsPeoplePage()
            peoplePage.submitNumOfPeople("not-a-number")
            assertThat(peoplePage.form.getErrorMessage())
                .containsText("Enter how many people, like 2 or 5")
        }

        @Test
        fun `Submitting with a non-integer number in the numberOfPeople field returns an error`(page: Page) {
            val peoplePage = navigator.skipToTenancyDetailsPeoplePage()
            peoplePage.submitNumOfPeople("2.3")
            assertThat(peoplePage.form.getErrorMessage())
                .containsText("Enter how many people, like 2 or 5")
        }

        @Test
        fun `Submitting with a negative integer in the numberOfPeople field returns an error`(page: Page) {
            val peoplePage = navigator.skipToTenancyDetailsPeoplePage()
            peoplePage.submitNumOfPeople("-2")
            assertThat(peoplePage.form.getErrorMessage())
                .containsText("Enter how many people, like 2 or 5")
        }

        @Test
        fun `Submitting with a zero integer in the numberOfPeople field returns an error`(page: Page) {
            val peoplePage = navigator.skipToTenancyDetailsPeoplePage()
            peoplePage.submitNumOfPeople(0)
            assertThat(peoplePage.form.getErrorMessage())
                .containsText("Enter how many people, like 2 or 5")
        }

        @Test
        fun `Submitting with an integer in the numberOfPeople field that is less than the numberOfHouseholds returns an error`(
            page: Page,
        ) {
            val householdsPage = navigator.skipToTenancyDetailsHouseholdsPage()
            householdsPage.submitNumberOfHouseholds(3)
            val peoplePage = assertPageIs(page, NumberOfPeopleFormPagePropertyRegistration::class)
            peoplePage.submitNumOfPeople(2)
            assertThat(peoplePage.form.getErrorMessage())
                .containsText(
                    "The number of people in the property must be the same as or higher than the number of households in the property",
                )
        }
    }

    @Nested
    inner class ProvideTenancyDetailsLaterStep {
        @Test
        fun `The page renders the occupied variant for an occupied property`() {
            val provideTenancyDetailsLaterPage = navigator.skipToTenancyDetailsProvideTenancyDetailsLaterPage()
            BaseComponent.assertThat(provideTenancyDetailsLaterPage.heading).containsText("Provide tenancy details later")
        }
    }
}
