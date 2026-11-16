package uk.gov.communities.prsdb.webapp.integration

import com.microsoft.playwright.Page
import com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.communities.prsdb.webapp.constants.ACTIVE_INACTIVE
import uk.gov.communities.prsdb.webapp.database.repository.LettingAgentAccessRepository
import uk.gov.communities.prsdb.webapp.integration.pageObjects.components.BaseComponent
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.LandlordDashboardPage
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.basePages.BasePage.Companion.assertPageIs
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyDeregistrationJourneyPages.CheckInvitationsPage
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyDeregistrationJourneyPages.ConfirmPagePropertyDeregistration
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyDeregistrationJourneyPages.ConfirmationPagePropertyDeregistration
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class PropertyDeregistrationJourneyTests : IntegrationTestWithMutableData("data-local.sql") {
    @Autowired
    lateinit var lettingAgentAccessRepository: LettingAgentAccessRepository

    // TODO PDJB-2149: Remove these tests along with the property deregistration journey when the ACTIVE_INACTIVE flag is removed
    @BeforeEach
    fun disableActiveInactive() {
        featureFlagManager.disable(ACTIVE_INACTIVE)
    }

    @Test
    fun `User can delete a property record that a letting agent has been delegated to`(page: Page) {
        val propertyOwnershipId = 1
        assertNotNull(lettingAgentAccessRepository.findByPropertyOwnershipId(propertyOwnershipId.toLong()))

        val deregisterPropertyInfoPage = navigator.goToDeregisterPropertyInfoPage(propertyOwnershipId.toLong())
        deregisterPropertyInfoPage.submitContinue()

        val confirmPage =
            assertPageIs(
                page,
                ConfirmPagePropertyDeregistration::class,
                mapOf("propertyOwnershipId" to propertyOwnershipId.toString()),
            )
        confirmPage.submitConfirm()

        assertPageIs(
            page,
            ConfirmationPagePropertyDeregistration::class,
            mapOf("propertyOwnershipId" to propertyOwnershipId.toString()),
        )

        assertNull(lettingAgentAccessRepository.findByPropertyOwnershipId(propertyOwnershipId.toLong()))
    }

    @Test
    fun `User can navigate the whole journey if pages are correctly filled in`(page: Page) {
        val propertyOwnershipId = 1
        val deregisterPropertyInfoPage = navigator.goToDeregisterPropertyInfoPage(propertyOwnershipId.toLong())
        assertThat(deregisterPropertyInfoPage.heading).containsText("1, Example Road, EG")
        deregisterPropertyInfoPage.submitContinue()

        val confirmPage =
            assertPageIs(
                page,
                ConfirmPagePropertyDeregistration::class,
                mapOf("propertyOwnershipId" to propertyOwnershipId.toString()),
            )
        confirmPage.submitConfirm()

        val confirmationPage =
            assertPageIs(
                page,
                ConfirmationPagePropertyDeregistration::class,
                mapOf("propertyOwnershipId" to propertyOwnershipId.toString()),
            )
        BaseComponent.assertThat(confirmationPage.confirmationBanner).containsText("Deregistered 1, Example Road, EG1 1AA")

        confirmationPage.goToDashboardLink.clickAndWait()
        assertPageIs(page, LandlordDashboardPage::class)
    }

    @Test
    fun `User can delete a property record that has compliance information and JL invites`(page: Page) {
        val propertyOwnershipId = 38
        val deregisterPropertyInfoPage = navigator.goToDeregisterPropertyInfoPage(propertyOwnershipId.toLong())
        deregisterPropertyInfoPage.submitContinue()

        val checkInvitationsPage =
            assertPageIs(
                page,
                CheckInvitationsPage::class,
                mapOf("propertyOwnershipId" to propertyOwnershipId.toString()),
            )
        checkInvitationsPage.submitContinue()

        val confirmPage =
            assertPageIs(
                page,
                ConfirmPagePropertyDeregistration::class,
                mapOf("propertyOwnershipId" to propertyOwnershipId.toString()),
            )
        confirmPage.submitConfirm()

        val confirmationPage =
            assertPageIs(
                page,
                ConfirmationPagePropertyDeregistration::class,
                mapOf("propertyOwnershipId" to propertyOwnershipId.toString()),
            )
        BaseComponent.assertThat(confirmationPage.confirmationBanner).containsText("Deregistered 7 Deregister Lane, DR1 1AA")

        confirmationPage.goToDashboardLink.clickAndWait()
        assertPageIs(page, LandlordDashboardPage::class)
    }

    @Nested
    inner class ConfirmStep {
        @Test
        fun `Confirm page deregisters the property and reaches confirmation`(page: Page) {
            val propertyOwnershipId = 1.toLong()
            val confirmPage = navigator.skipToPropertyDeregistrationConfirmPage(propertyOwnershipId)
            confirmPage.submitConfirm()
            assertPageIs(
                page,
                ConfirmationPagePropertyDeregistration::class,
                mapOf("propertyOwnershipId" to propertyOwnershipId.toString()),
            )
        }
    }
}
