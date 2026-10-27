package uk.gov.communities.prsdb.webapp.integration

import com.microsoft.playwright.Page
import com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.communities.prsdb.webapp.constants.ASSOCIATED_PARTIES
import uk.gov.communities.prsdb.webapp.constants.GOV_LEGAL_ADVICE_URL
import uk.gov.communities.prsdb.webapp.integration.pageObjects.components.BaseComponent
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.basePages.BasePage.Companion.assertPageIs
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyRegistrationJourneyPages.CheckJointLandlordsFormPagePropertyRegistration
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyRegistrationJourneyPages.HasJointLandlordsFormBasePagePropertyRegistration
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyRegistrationJourneyPages.InviteAnotherJointLandlordFormPagePropertyRegistration
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyRegistrationJourneyPages.RemoveJointLandlordAreYouSureFormPagePropertyRegistration

class PropertyRegistrationJointLandlordsSinglePageTests : IntegrationTestWithImmutableData("data-local.sql") {
    // TODO PDJB-959: remove this override and update these tests for the associated parties journey
    @BeforeEach
    fun disableAssociatedParties() {
        featureFlagManager.disableFeature(ASSOCIATED_PARTIES)
    }

    @Nested
    inner class HasJointLandlordsStep {
        @Test
        fun `Submitting with no option selected returns an error`(page: Page) {
            val hasJointLandlordsPage = navigator.skipToPropertyRegistrationHasJointLandlordsPage()
            hasJointLandlordsPage.form.submit()
            assertThat(hasJointLandlordsPage.form.getErrorMessage())
                .containsText("Select if there are any other landlords for this property")
        }

        @Test
        fun `The link renders correctly`(page: Page) {
            val hasJointLandlordsPage = navigator.skipToPropertyRegistrationHasJointLandlordsPage()
            BaseComponent.assertThat(hasJointLandlordsPage.legalAdviceLink).hasAttribute("href", GOV_LEGAL_ADVICE_URL)
            BaseComponent.assertThat(hasJointLandlordsPage.legalAdviceLink).hasAttribute("rel", "noreferrer noopener")
            BaseComponent.assertThat(hasJointLandlordsPage.legalAdviceLink).hasAttribute("target", "_blank")
        }
    }

    @Nested
    inner class ManagingJointLandlords {
        @Test
        fun `Joint landlord change and remove links are rendered in an actions list`(page: Page) {
            val inviteJointLandlordsPage = navigator.skipToPropertyRegistrationInviteJointLandlordPage()
            inviteJointLandlordsPage.submitEmail("alpha@example.com")

            val checkJointLandlordPage = assertPageIs(page, CheckJointLandlordsFormPagePropertyRegistration::class)

            val actions = checkJointLandlordPage.summaryList.firstRow.actions
            assertThat(actions.actionsList).hasCount(1)
            assertThat(actions.actionsListItems).hasCount(2)
            assertThat(actions.actionsListItems.nth(0)).containsText("Change")
            assertThat(actions.actionsListItems.nth(1)).containsText("Remove")
        }

        @Test
        fun `Submitting remove a joint landlord with no option selected returns an error`(page: Page) {
            val inviteJointLandlordsPage = navigator.skipToPropertyRegistrationInviteJointLandlordPage()
            inviteJointLandlordsPage.submitEmail("alpha@example.com")

            val checkJointLandlordPage = assertPageIs(page, CheckJointLandlordsFormPagePropertyRegistration::class)
            checkJointLandlordPage.summaryList.firstRow.clickNamedActionLinkAndWait("Remove")

            val removeJointLandlordPage = assertPageIs(page, RemoveJointLandlordAreYouSureFormPagePropertyRegistration::class)
            removeJointLandlordPage.form.submit()
            assertThat(removeJointLandlordPage.form.getErrorMessage())
                .containsText("Select if you want to remove this joint landlord")
        }

        @Test
        fun `Submitting remove a joint landlord with No selected returns to the check page without removing the landlord`(page: Page) {
            val inviteJointLandlordsPage = navigator.skipToPropertyRegistrationInviteJointLandlordPage()
            inviteJointLandlordsPage.submitEmail("alpha@example.com")

            var checkJointLandlordPage = assertPageIs(page, CheckJointLandlordsFormPagePropertyRegistration::class)
            checkJointLandlordPage.summaryList.firstRow.clickNamedActionLinkAndWait("Remove")

            val removeJointLandlordPage = assertPageIs(page, RemoveJointLandlordAreYouSureFormPagePropertyRegistration::class)
            removeJointLandlordPage.submitDoesNotWantToProceed()

            checkJointLandlordPage = assertPageIs(page, CheckJointLandlordsFormPagePropertyRegistration::class)
            BaseComponent.assertThat(checkJointLandlordPage.title).containsText("You’ve added 1 joint landlord")
            assertThat(checkJointLandlordPage.summaryList.firstRow.value).containsText("alpha@example.com")
        }

        @Test
        fun `Clicking cancel returns to the check page regardless of selected remove answer`(page: Page) {
            val inviteJointLandlordsPage = navigator.skipToPropertyRegistrationInviteJointLandlordPage()
            inviteJointLandlordsPage.submitEmail("alpha@example.com")

            var checkJointLandlordPage = assertPageIs(page, CheckJointLandlordsFormPagePropertyRegistration::class)
            checkJointLandlordPage.form.addAnotherButton.clickAndWait()

            val inviteAnotherJointLandlordPage = assertPageIs(page, InviteAnotherJointLandlordFormPagePropertyRegistration::class)
            inviteAnotherJointLandlordPage.submitEmail("beta@example.com")

            checkJointLandlordPage = assertPageIs(page, CheckJointLandlordsFormPagePropertyRegistration::class)
            checkJointLandlordPage.summaryList.firstRow.clickNamedActionLinkAndWait("Remove")

            var removeJointLandlordPage = assertPageIs(page, RemoveJointLandlordAreYouSureFormPagePropertyRegistration::class)
            removeJointLandlordPage.form.areYouSureRadios.selectValue("true")
            removeJointLandlordPage.cancelLink.clickAndWait()

            checkJointLandlordPage = assertPageIs(page, CheckJointLandlordsFormPagePropertyRegistration::class)
            BaseComponent.assertThat(checkJointLandlordPage.title).containsText("You’ve added 2 joint landlords")
            assertThat(checkJointLandlordPage.summaryList.firstRow.value).containsText("alpha@example.com")
            assertThat(checkJointLandlordPage.summaryList.getRowByIndex(1).value).containsText("beta@example.com")
            checkJointLandlordPage.summaryList.firstRow.clickNamedActionLinkAndWait("Remove")

            removeJointLandlordPage = assertPageIs(page, RemoveJointLandlordAreYouSureFormPagePropertyRegistration::class)
            removeJointLandlordPage.form.areYouSureRadios.selectValue("false")
            removeJointLandlordPage.cancelLink.clickAndWait()

            checkJointLandlordPage = assertPageIs(page, CheckJointLandlordsFormPagePropertyRegistration::class)
            BaseComponent.assertThat(checkJointLandlordPage.title).containsText("You’ve added 2 joint landlords")
            assertThat(checkJointLandlordPage.summaryList.firstRow.value).containsText("alpha@example.com")
            assertThat(checkJointLandlordPage.summaryList.getRowByIndex(1).value).containsText("beta@example.com")
        }

        @Test
        fun `Removing joint landlords works as expected`(page: Page) {
            val inviteJointLandlordsPage = navigator.skipToPropertyRegistrationInviteJointLandlordPage()
            inviteJointLandlordsPage.submitEmail("alpha@example.com")

            var checkJointLandlordPage = assertPageIs(page, CheckJointLandlordsFormPagePropertyRegistration::class)
            checkJointLandlordPage.form.addAnotherButton.clickAndWait()

            val inviteAnotherJointLandlordPage = assertPageIs(page, InviteAnotherJointLandlordFormPagePropertyRegistration::class)
            inviteAnotherJointLandlordPage.submitEmail("beta@example.com")

            checkJointLandlordPage = assertPageIs(page, CheckJointLandlordsFormPagePropertyRegistration::class)
            assertThat(checkJointLandlordPage.summaryList.firstRow.value).containsText("alpha@example.com")
            checkJointLandlordPage.summaryList.firstRow.clickNamedActionLinkAndWait("Remove")

            var removeJointLandlordPage = assertPageIs(page, RemoveJointLandlordAreYouSureFormPagePropertyRegistration::class)
            removeJointLandlordPage.submitWantsToProceed()

            checkJointLandlordPage = assertPageIs(page, CheckJointLandlordsFormPagePropertyRegistration::class)
            assertThat(checkJointLandlordPage.summaryList.firstRow.value).containsText("beta@example.com")
            checkJointLandlordPage.summaryList.firstRow.clickNamedActionLinkAndWait("Remove")

            removeJointLandlordPage = assertPageIs(page, RemoveJointLandlordAreYouSureFormPagePropertyRegistration::class)
            removeJointLandlordPage.submitWantsToProceed()

            assertPageIs(page, HasJointLandlordsFormBasePagePropertyRegistration::class)
        }

        @Test
        fun `Editing joint landlords works as expected`(page: Page) {
            val inviteJointLandlordsPage = navigator.skipToPropertyRegistrationInviteJointLandlordPage()
            inviteJointLandlordsPage.submitEmail("alpha@example.com")

            var checkJointLandlordPage = assertPageIs(page, CheckJointLandlordsFormPagePropertyRegistration::class)
            checkJointLandlordPage.form.addAnotherButton.clickAndWait()

            var inviteAnotherJointLandlordPage = assertPageIs(page, InviteAnotherJointLandlordFormPagePropertyRegistration::class)
            inviteAnotherJointLandlordPage.submitEmail("beta@example.com")

            checkJointLandlordPage = assertPageIs(page, CheckJointLandlordsFormPagePropertyRegistration::class)
            assertThat(checkJointLandlordPage.summaryList.firstRow.value).containsText("alpha@example.com")
            checkJointLandlordPage.summaryList.firstRow.clickNamedActionLinkAndWait("Change")

            inviteAnotherJointLandlordPage = assertPageIs(page, InviteAnotherJointLandlordFormPagePropertyRegistration::class)
            BaseComponent.assertThat(inviteAnotherJointLandlordPage.form.emailInput).hasValue("alpha@example.com")
            inviteAnotherJointLandlordPage.submitEmail("gamma@example.com")

            checkJointLandlordPage = assertPageIs(page, CheckJointLandlordsFormPagePropertyRegistration::class)
            assertThat(checkJointLandlordPage.summaryList.firstRow.value).containsText("gamma@example.com")
        }

        @Test
        fun `Numbering on page and tables is correct`(page: Page) {
            val inviteJointLandlordsPage = navigator.skipToPropertyRegistrationInviteJointLandlordPage()
            inviteJointLandlordsPage.submitEmail("alpha@example.com")

            var checkJointLandlordPage = assertPageIs(page, CheckJointLandlordsFormPagePropertyRegistration::class)
            BaseComponent.assertThat(checkJointLandlordPage.title).containsText("You’ve added 1 joint landlord")
            assertThat(checkJointLandlordPage.summaryList.firstRow.key).containsText("Joint landlord 1")
            assertThat(checkJointLandlordPage.summaryList.firstRow.value).containsText("alpha@example.com")
            checkJointLandlordPage.form.addAnotherButton.clickAndWait()

            val inviteAnotherJointLandlordPage = assertPageIs(page, InviteAnotherJointLandlordFormPagePropertyRegistration::class)
            inviteAnotherJointLandlordPage.submitEmail("beta@example.com")

            checkJointLandlordPage = assertPageIs(page, CheckJointLandlordsFormPagePropertyRegistration::class)
            BaseComponent.assertThat(checkJointLandlordPage.title).containsText("You’ve added 2 joint landlords")
            assertThat(checkJointLandlordPage.summaryList.getRowByIndex(1).key).containsText("Joint landlord 2")
            assertThat(checkJointLandlordPage.summaryList.getRowByIndex(1).value).containsText("beta@example.com")
            checkJointLandlordPage.summaryList.firstRow.clickNamedActionLinkAndWait("Remove")

            val removeJointLandlordPage = assertPageIs(page, RemoveJointLandlordAreYouSureFormPagePropertyRegistration::class)
            removeJointLandlordPage.submitWantsToProceed()

            checkJointLandlordPage = assertPageIs(page, CheckJointLandlordsFormPagePropertyRegistration::class)
            BaseComponent.assertThat(checkJointLandlordPage.title).containsText("You’ve added 1 joint landlord")
            assertThat(checkJointLandlordPage.summaryList.firstRow.key).containsText("Joint landlord 1")
            assertThat(checkJointLandlordPage.summaryList.firstRow.value).containsText("beta@example.com")
        }
    }

    @Nested
    inner class InviteJointLandlordsStep {
        @Test
        fun `Submitting with no email returns an error`(page: Page) {
            val inviteJointLandlordsPage = navigator.skipToPropertyRegistrationInviteJointLandlordPage()
            inviteJointLandlordsPage.submitEmail("")
            assertThat(inviteJointLandlordsPage.form.getErrorMessage())
                .containsText("Enter an email address in the correct format, like name@example.com")
        }

        @Test
        fun `Submitting with an invalid email returns an error`(page: Page) {
            val inviteJointLandlordsPage = navigator.skipToPropertyRegistrationInviteJointLandlordPage()
            inviteJointLandlordsPage.submitEmail("not-an-email")
            assertThat(inviteJointLandlordsPage.form.getErrorMessage())
                .containsText("Enter an email address in the correct format, like name@example.com")
        }
    }

    @Nested
    inner class InviteAnotherJointLandlordsStep {
        @Test
        fun `Submitting with an already invited email returns an error`(page: Page) {
            val alreadyInvitedEmail = "already@invited.com"
            val inviteJointLandlordsPage =
                navigator.skipToPropertyRegistrationInviteAnotherJointLandlordPage(mutableListOf(alreadyInvitedEmail))
            inviteJointLandlordsPage.submitEmail(alreadyInvitedEmail)
            assertThat(inviteJointLandlordsPage.form.getErrorMessage())
                .containsText("You have already invited this email address")
        }

        @Test
        fun `Submitting with an invited email in edit mode is permitted`(page: Page) {
            val alreadyInvitedEmail = "already@invited.com"

            val inviteJointLandlordsPage = navigator.skipToPropertyRegistrationInviteJointLandlordPage()
            inviteJointLandlordsPage.submitEmail(alreadyInvitedEmail)

            var checkJointLandlordPage = assertPageIs(page, CheckJointLandlordsFormPagePropertyRegistration::class)
            checkJointLandlordPage.summaryList.firstRow.clickNamedActionLinkAndWait("Change")

            val editJointLandlordPage = assertPageIs(page, InviteAnotherJointLandlordFormPagePropertyRegistration::class)
            BaseComponent.assertThat(editJointLandlordPage.form.emailInput).hasValue(alreadyInvitedEmail)
            inviteJointLandlordsPage.submitEmail(alreadyInvitedEmail)

            checkJointLandlordPage = assertPageIs(page, CheckJointLandlordsFormPagePropertyRegistration::class)
            assertThat(checkJointLandlordPage.summaryList.firstRow.value).containsText(alreadyInvitedEmail)
        }
    }
}
