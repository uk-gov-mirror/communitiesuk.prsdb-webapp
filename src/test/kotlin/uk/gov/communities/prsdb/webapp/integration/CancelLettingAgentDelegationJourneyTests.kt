package uk.gov.communities.prsdb.webapp.integration

import com.microsoft.playwright.Page
import org.junit.jupiter.api.Test
import uk.gov.communities.prsdb.webapp.constants.DELEGATE_TO_LETTING_AGENT
import uk.gov.communities.prsdb.webapp.controllers.CancelLettingAgentDelegationController
import uk.gov.communities.prsdb.webapp.integration.pageObjects.components.BaseComponent
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.PropertyDetailsPageLandlordView
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.basePages.BasePage.Companion.assertPageIs
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.cancelLettingAgentDelegationJourneyPages.AreYouSurePageCancelLettingAgentDelegation
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.cancelLettingAgentDelegationJourneyPages.ConfirmationPageCancelLettingAgentDelegation
import kotlin.test.assertEquals

class CancelLettingAgentDelegationJourneyTests : IntegrationTestWithImmutableData("data-local.sql") {
    private val propertyOwnershipId = 1L

    @Test
    fun `a landlord can walk the remove letting agent journey and reach the confirmation page`(page: Page) {
        featureFlagManager.enable(DELEGATE_TO_LETTING_AGENT)

        val propertyDetailsPage = navigator.goToPropertyDetailsLandlordView(propertyOwnershipId)
        propertyDetailsPage.removeLettingAgentLink.clickAndWait()

        // "Are you sure" page
        val areYouSurePage = assertPageIs(page, AreYouSurePageCancelLettingAgentDelegation::class)
        // TODO PDJB-1413: assert the real "are you sure" page content and the yes/no decision
        areYouSurePage.continueButton.clickAndWait()

        // Confirmation page
        val confirmationPage = assertPageIs(page, ConfirmationPageCancelLettingAgentDelegation::class)
        BaseComponent.assertThat(confirmationPage.confirmationBanner).containsText(
            "Letting agent or property manager can no longer provide details",
        )
        // TODO: PDJB-1560: Make sure the email is shown on this page
        confirmationPage.continueButton.clickAndWait()

        // Back to the property record page
        assertPageIs(
            page,
            PropertyDetailsPageLandlordView::class,
            mapOf("propertyOwnershipId" to propertyOwnershipId.toString()),
        )
    }

    @Test
    fun `the remove letting agent journey is unavailable when the flag is disabled`() {
        featureFlagManager.disable(DELEGATE_TO_LETTING_AGENT)

        val response =
            navigator.navigate(
                CancelLettingAgentDelegationController.getRemoveLettingAgentPath(propertyOwnershipId),
            )

        assertEquals(404, response?.status())

        val confirmationResponse =
            navigator.navigate(
                CancelLettingAgentDelegationController.getRemoveLettingAgentConfirmationPath(propertyOwnershipId),
            )

        assertEquals(404, confirmationResponse?.status())
    }
}
