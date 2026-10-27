package uk.gov.communities.prsdb.webapp.integration

import com.microsoft.playwright.Page
import com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import uk.gov.communities.prsdb.webapp.constants.ASSOCIATED_PARTIES
import uk.gov.communities.prsdb.webapp.constants.enums.WhoProvidesRentalDetails
import uk.gov.communities.prsdb.webapp.integration.pageObjects.components.BaseComponent
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.basePages.BasePage.Companion.assertPageIs
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyRegistrationJourneyPages.CheckAnswersPagePropertyRegistration
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyRegistrationJourneyPages.LettingAgentEmailPagePropertyRegistration
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyRegistrationJourneyPages.WhoProvidesChangeInterruptionPagePropertyRegistration
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyRegistrationJourneyPages.WhoProvidesRentalDetailsFormPagePropertyRegistration
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps.LettingAgentEmailStep
import uk.gov.communities.prsdb.webapp.models.requestModels.formModels.AllowLettingAgentEmailFormModel
import uk.gov.communities.prsdb.webapp.testHelpers.builders.PropertyStateSessionBuilder
import kotlin.test.assertEquals

class PropertyRegistrationWhoProvidesChangeSinglePageTests : IntegrationTestWithImmutableData("data-local.sql") {
    // TODO PDJB-959: remove this override and update these tests for the associated parties journey
    @BeforeEach
    fun disableAssociatedParties() {
        featureFlagManager.disableFeature(ASSOCIATED_PARTIES)
    }

    companion object {
        const val LETTING_AGENT_EMAIL = "letting.agent@example.com"
    }

    private fun goToCheckAnswersWithLandlordProvidingDetails(page: Page): CheckAnswersPagePropertyRegistration {
        val taskListPage =
            navigator.goToPropertyRegistrationTaskList(
                PropertyStateSessionBuilder
                    .beforePropertyRegistrationCheckAnswersOccupied()
                    .withLandlordProvidesRentalDetails()
                    .withBedrooms(),
            )
        taskListPage.clickSubmitYourRegistrationTaskWithName("Submit and pay")
        return assertPageIs(page, CheckAnswersPagePropertyRegistration::class)
    }

    private fun goToCheckAnswersWithLettingAgentProvidingDetails(page: Page): CheckAnswersPagePropertyRegistration {
        val taskListPage =
            navigator.goToPropertyRegistrationTaskList(
                PropertyStateSessionBuilder
                    .beforePropertyRegistrationCheckAnswersOccupied()
                    .withLettingAgentProvidesRentalDetails()
                    .withSubmittedValue(
                        LettingAgentEmailStep.ROUTE_SEGMENT,
                        AllowLettingAgentEmailFormModel().apply { emailAddress = LETTING_AGENT_EMAIL },
                    ).withBedrooms(),
            )
        taskListPage.clickSubmitYourRegistrationTaskWithName("Submit and pay")
        return assertPageIs(page, CheckAnswersPagePropertyRegistration::class)
    }

    @Test
    fun `the who-provides change link opens the who-provides page showing the previous selection`(page: Page) {
        val checkAnswersPage = goToCheckAnswersWithLandlordProvidingDetails(page)

        checkAnswersPage.summaryList.whoProvidesRentalDetailsRow.clickFirstActionLinkAndWait()

        val whoProvidesPage = assertPageIs(page, WhoProvidesRentalDetailsFormPagePropertyRegistration::class)
        assertEquals(WhoProvidesRentalDetails.LANDLORD.name, whoProvidesPage.form.whoProvidesRadios.selectedValue)
    }

    @Test
    fun `changing from landlord to letting agent shows the interruption listing the details that will be requested`(page: Page) {
        val checkAnswersPage = goToCheckAnswersWithLandlordProvidingDetails(page)
        checkAnswersPage.summaryList.whoProvidesRentalDetailsRow.clickFirstActionLinkAndWait()
        val whoProvidesPage = assertPageIs(page, WhoProvidesRentalDetailsFormPagePropertyRegistration::class)

        whoProvidesPage.submitLettingAgentProvidesDetails()

        val interruptionPage = assertPageIs(page, WhoProvidesChangeInterruptionPagePropertyRegistration::class)
        assertThat(interruptionPage.heading).containsText("Are you sure you want to change this?")
        assertThat(interruptionPage.body).containsText("property licensing")
        assertThat(interruptionPage.body).containsText("compliance certificates")
        assertThat(interruptionPage.body).containsText("the current tenancy")
    }

    @Test
    fun `going back from the interruption returns to the who-provides page keeping the letting agent selection`(page: Page) {
        val checkAnswersPage = goToCheckAnswersWithLandlordProvidingDetails(page)
        checkAnswersPage.summaryList.whoProvidesRentalDetailsRow.clickFirstActionLinkAndWait()
        assertPageIs(page, WhoProvidesRentalDetailsFormPagePropertyRegistration::class)
            .submitLettingAgentProvidesDetails()
        val interruptionPage = assertPageIs(page, WhoProvidesChangeInterruptionPagePropertyRegistration::class)

        interruptionPage.goBackLink.clickAndWait()

        val whoProvidesPage = assertPageIs(page, WhoProvidesRentalDetailsFormPagePropertyRegistration::class)
        assertEquals(WhoProvidesRentalDetails.LETTING_AGENT.name, whoProvidesPage.form.whoProvidesRadios.selectedValue)
    }

    @Test
    fun `continuing through the interruption and entering a valid email returns to the CYA showing the letting agent details`(page: Page) {
        val checkAnswersPage = goToCheckAnswersWithLandlordProvidingDetails(page)
        checkAnswersPage.summaryList.whoProvidesRentalDetailsRow.clickFirstActionLinkAndWait()
        assertPageIs(page, WhoProvidesRentalDetailsFormPagePropertyRegistration::class)
            .submitLettingAgentProvidesDetails()
        assertPageIs(page, WhoProvidesChangeInterruptionPagePropertyRegistration::class).submit()

        val emailPage = assertPageIs(page, LettingAgentEmailPagePropertyRegistration::class)
        emailPage.submitEmail(LETTING_AGENT_EMAIL)

        val updatedCheckAnswersPage = assertPageIs(page, CheckAnswersPagePropertyRegistration::class)
        assertThat(updatedCheckAnswersPage.summaryList.whoProvidesRentalDetailsRow.value)
            .containsText("My letting agent or property manager")
        assertThat(updatedCheckAnswersPage.summaryList.lettingAgentEmailRow.value).containsText(LETTING_AGENT_EMAIL)
    }

    @Test
    fun `entering an invalid email on the letting agent email page returns a validation error`(page: Page) {
        val checkAnswersPage = goToCheckAnswersWithLandlordProvidingDetails(page)
        checkAnswersPage.summaryList.whoProvidesRentalDetailsRow.clickFirstActionLinkAndWait()
        assertPageIs(page, WhoProvidesRentalDetailsFormPagePropertyRegistration::class)
            .submitLettingAgentProvidesDetails()
        assertPageIs(page, WhoProvidesChangeInterruptionPagePropertyRegistration::class).submit()
        val emailPage = assertPageIs(page, LettingAgentEmailPagePropertyRegistration::class)

        emailPage.submitEmail("notAnEmail")

        assertThat(emailPage.form.getErrorMessage())
            .containsText("Enter an email address in the correct format, like name@example.com")
    }

    @Test
    fun `submitting a blank email on the letting agent email page returns a validation error`(page: Page) {
        val checkAnswersPage = goToCheckAnswersWithLandlordProvidingDetails(page)
        checkAnswersPage.summaryList.whoProvidesRentalDetailsRow.clickFirstActionLinkAndWait()
        assertPageIs(page, WhoProvidesRentalDetailsFormPagePropertyRegistration::class)
            .submitLettingAgentProvidesDetails()
        assertPageIs(page, WhoProvidesChangeInterruptionPagePropertyRegistration::class).submit()
        val emailPage = assertPageIs(page, LettingAgentEmailPagePropertyRegistration::class)

        emailPage.submitEmail("")

        assertThat(emailPage.form.getErrorMessage())
            .containsText("Enter your letting agent or property manager’s email address")
    }

    @Test
    fun `going back from the letting agent email page returns to the interruption`(page: Page) {
        val checkAnswersPage = goToCheckAnswersWithLandlordProvidingDetails(page)
        checkAnswersPage.summaryList.whoProvidesRentalDetailsRow.clickFirstActionLinkAndWait()
        assertPageIs(page, WhoProvidesRentalDetailsFormPagePropertyRegistration::class)
            .submitLettingAgentProvidesDetails()
        assertPageIs(page, WhoProvidesChangeInterruptionPagePropertyRegistration::class).submit()
        val emailPage = assertPageIs(page, LettingAgentEmailPagePropertyRegistration::class)

        emailPage.backLink.clickAndWait()

        assertPageIs(page, WhoProvidesChangeInterruptionPagePropertyRegistration::class)
    }

    @Test
    fun `re-selecting letting agent when already delegated returns to the CYA without the interruption`(page: Page) {
        val checkAnswersPage = goToCheckAnswersWithLettingAgentProvidingDetails(page)
        checkAnswersPage.summaryList.whoProvidesRentalDetailsRow.clickFirstActionLinkAndWait()

        assertPageIs(page, WhoProvidesRentalDetailsFormPagePropertyRegistration::class)
            .submitLettingAgentProvidesDetails()

        val updatedCheckAnswersPage = assertPageIs(page, CheckAnswersPagePropertyRegistration::class)
        assertThat(updatedCheckAnswersPage.summaryList.whoProvidesRentalDetailsRow.value)
            .containsText("My letting agent or property manager")
    }

    @Test
    fun `switching from letting agent to landlord removes the letting agent email row from the CYA`(page: Page) {
        val checkAnswersPage = goToCheckAnswersWithLettingAgentProvidingDetails(page)
        assertThat(checkAnswersPage.summaryList.lettingAgentEmailRow.value).containsText(LETTING_AGENT_EMAIL)

        checkAnswersPage.summaryList.whoProvidesRentalDetailsRow.clickFirstActionLinkAndWait()
        assertPageIs(page, WhoProvidesRentalDetailsFormPagePropertyRegistration::class)
            .submitLandlordProvidesDetails()

        val updatedCheckAnswersPage = assertPageIs(page, CheckAnswersPagePropertyRegistration::class)
        assertThat(updatedCheckAnswersPage.summaryList.whoProvidesRentalDetailsRow.value)
            .containsText("I will provide these details")
        assertThat(updatedCheckAnswersPage.summaryList.lettingAgentEmailRow.key).hasCount(0)
    }

    @Test
    fun `switching to landlord then back to letting agent restores the previously entered email`(page: Page) {
        val checkAnswersPage = goToCheckAnswersWithLettingAgentProvidingDetails(page)

        // Change from letting agent to landlord and return to the CYA.
        checkAnswersPage.summaryList.whoProvidesRentalDetailsRow.clickFirstActionLinkAndWait()
        assertPageIs(page, WhoProvidesRentalDetailsFormPagePropertyRegistration::class)
            .submitLandlordProvidesDetails()
        val landlordCheckAnswersPage = assertPageIs(page, CheckAnswersPagePropertyRegistration::class)
        assertThat(landlordCheckAnswersPage.summaryList.whoProvidesRentalDetailsRow.value)
            .containsText("I will provide these details")
        assertThat(landlordCheckAnswersPage.summaryList.lettingAgentEmailRow.key).hasCount(0)

        // Change back to letting agent; the previously entered email should be restored, not cleared.
        landlordCheckAnswersPage.summaryList.whoProvidesRentalDetailsRow.clickFirstActionLinkAndWait()
        assertPageIs(page, WhoProvidesRentalDetailsFormPagePropertyRegistration::class)
            .submitLettingAgentProvidesDetails()
        assertPageIs(page, WhoProvidesChangeInterruptionPagePropertyRegistration::class).submit()

        val emailPage = assertPageIs(page, LettingAgentEmailPagePropertyRegistration::class)
        BaseComponent.assertThat(emailPage.form.emailInput).hasValue(LETTING_AGENT_EMAIL)
    }

    @Test
    fun `re-selecting landlord when already providing details returns to the CYA without changes`(page: Page) {
        val checkAnswersPage = goToCheckAnswersWithLandlordProvidingDetails(page)
        checkAnswersPage.summaryList.whoProvidesRentalDetailsRow.clickFirstActionLinkAndWait()

        assertPageIs(page, WhoProvidesRentalDetailsFormPagePropertyRegistration::class)
            .submitLandlordProvidesDetails()

        val updatedCheckAnswersPage = assertPageIs(page, CheckAnswersPagePropertyRegistration::class)
        assertThat(updatedCheckAnswersPage.summaryList.whoProvidesRentalDetailsRow.value)
            .containsText("I will provide these details")
    }
}
