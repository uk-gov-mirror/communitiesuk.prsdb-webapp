package uk.gov.communities.prsdb.webapp.integration

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.communities.prsdb.webapp.constants.ASSOCIATED_PARTIES
import uk.gov.communities.prsdb.webapp.constants.enums.CorrespondenceEmailOption
import uk.gov.communities.prsdb.webapp.integration.IntegrationTestWithImmutableData.NestedIntegrationTestWithImmutableData
import uk.gov.communities.prsdb.webapp.integration.pageObjects.components.BaseComponent.Companion.assertThat

class PropertyRegistrationCorrespondenceEmailSinglePageTests : IntegrationTestWithImmutableData("data-local.sql") {
    // TODO PDJB-959: remove this override and update these tests for the associated parties journey
    @BeforeEach
    fun disableAssociatedParties() {
        featureFlagManager.disableFeature(ASSOCIATED_PARTIES)
    }

    @Test
    fun `the account email option displays the landlord email address`() {
        val correspondenceEmailPage = navigator.skipToPropertyRegistrationCorrespondenceEmailPage()

        assertThat(correspondenceEmailPage.form.whichEmailRadios).containsText("alex.surname@example.com")
        assertThat(correspondenceEmailPage.form.submitButton).containsText("Save and continue")
    }

    @Test
    fun `submitting without selecting an email option returns an error`() {
        val correspondenceEmailPage = navigator.skipToPropertyRegistrationCorrespondenceEmailPage()

        correspondenceEmailPage.form.submit()

        assertThat(correspondenceEmailPage.errorSummary)
            .containsText("Select which email address the council should send emails to")
    }

    @Test
    fun `selecting a different email address reveals the email input`() {
        val correspondenceEmailPage = navigator.skipToPropertyRegistrationCorrespondenceEmailPage()

        correspondenceEmailPage.form.whichEmailRadios.selectValue(CorrespondenceEmailOption.DIFFERENT_EMAIL)

        assertThat(correspondenceEmailPage.form.differentEmailInput).isVisible()
    }

    @Test
    fun `submitting a different email option without an email address returns an error`() {
        val correspondenceEmailPage = navigator.skipToPropertyRegistrationCorrespondenceEmailPage()
        correspondenceEmailPage.form.whichEmailRadios.selectValue(CorrespondenceEmailOption.DIFFERENT_EMAIL)

        correspondenceEmailPage.form.submit()

        assertThat(correspondenceEmailPage.errorSummary).containsText("Enter an email address")
    }

    @Test
    fun `submitting an invalid different email address returns an error`() {
        val correspondenceEmailPage = navigator.skipToPropertyRegistrationCorrespondenceEmailPage()

        correspondenceEmailPage.submitDifferentEmail("not-an-email")

        assertThat(correspondenceEmailPage.errorSummary)
            .containsText("Enter an email address in the correct format")
    }

    @Nested
    inner class OrganisationLandlord :
        NestedIntegrationTestWithImmutableData("data-mockuser-org-landlord-with-incomplete-properties.sql") {
        @Test
        fun `the account email option displays the logged-in user's email`() {
            val correspondenceEmailPage = navigator.skipToPropertyRegistrationCorrespondenceEmailPage()

            assertThat(correspondenceEmailPage.form.whichEmailRadios).containsText("logged.in.user@example.com")
            assertThat(correspondenceEmailPage.form.whichEmailRadios).not().containsText("contact@example.com")
        }
    }
}
