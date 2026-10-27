package uk.gov.communities.prsdb.webapp.integration

import com.microsoft.playwright.Page
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import uk.gov.communities.prsdb.webapp.constants.ASSOCIATED_PARTIES
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.basePages.BasePage.Companion.assertPageIs
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyRegistrationJourneyPages.CheckAnswersPagePropertyRegistration
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyRegistrationJourneyPages.OccupancyChangeInterruptionPagePropertyRegistration
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyRegistrationJourneyPages.OccupancyFormPagePropertyRegistration
import uk.gov.communities.prsdb.webapp.testHelpers.builders.PropertyStateSessionBuilder

class PropertyRegistrationOccupancyChangeInterruptionSinglePageTests : IntegrationTestWithImmutableData("data-local.sql") {
    // TODO PDJB-959: remove this override and update these tests for the associated parties journey
    @BeforeEach
    fun disableAssociatedParties() {
        featureFlagManager.disableFeature(ASSOCIATED_PARTIES)
    }

    @Test
    fun `clicking Go back on the interruption page returns to the occupancy page`(page: Page) {
        val interruptionPage = goToOccupancyChangeInterruptionPage(page)

        interruptionPage.goBackLink.clickAndWait()

        assertPageIs(page, OccupancyFormPagePropertyRegistration::class)
    }

    private fun goToOccupancyChangeInterruptionPage(page: Page): OccupancyChangeInterruptionPagePropertyRegistration {
        val taskListPage =
            navigator.goToPropertyRegistrationTaskList(
                PropertyStateSessionBuilder.beforePropertyRegistrationCheckAnswersDelegatedToLettingAgent(),
            )
        taskListPage.clickSubmitYourRegistrationTaskWithName("Submit and pay")
        val checkAnswersPage = assertPageIs(page, CheckAnswersPagePropertyRegistration::class)

        checkAnswersPage.summaryList.occupancyQuestionRow.clickFirstActionLinkAndWait()
        val occupancyPage = assertPageIs(page, OccupancyFormPagePropertyRegistration::class)

        occupancyPage.submitIsVacant()
        return assertPageIs(page, OccupancyChangeInterruptionPagePropertyRegistration::class)
    }
}
