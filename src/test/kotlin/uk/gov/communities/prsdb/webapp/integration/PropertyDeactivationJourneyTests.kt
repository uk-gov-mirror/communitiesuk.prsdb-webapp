package uk.gov.communities.prsdb.webapp.integration

import com.microsoft.playwright.Page
import com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.mockito.kotlin.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.bean.override.mockito.MockitoBean
import uk.gov.communities.prsdb.webapp.controllers.DeactivatePropertyController
import uk.gov.communities.prsdb.webapp.database.repository.LandlordRepository
import uk.gov.communities.prsdb.webapp.database.repository.LettingAgentAccessRepository
import uk.gov.communities.prsdb.webapp.database.repository.PropertyOwnershipRepository
import uk.gov.communities.prsdb.webapp.integration.pageObjects.components.BaseComponent
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.PropertyDetailsPageLandlordView
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.basePages.BasePage.Companion.assertPageIs
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.leavePropertyJourneyPages.ConfirmPageLeaveProperty
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyDeactivationJourneyPages.CannotDeactivatePropertyJointLandlordsPage
import uk.gov.communities.prsdb.webapp.integration.pageObjects.pages.propertyDeactivationJourneyPages.ConfirmPagePropertyDeactivation
import uk.gov.communities.prsdb.webapp.models.viewModels.emailModels.PropertyDeactivationConfirmationEmail
import uk.gov.communities.prsdb.webapp.services.ActiveInactiveService
import uk.gov.communities.prsdb.webapp.services.EmailNotificationService

class PropertyDeactivationJourneyTests : IntegrationTestWithMutableData("data-local.sql") {
    private val propertyOwnershipId = 1L

    @Autowired
    private lateinit var propertyOwnershipRepository: PropertyOwnershipRepository

    @Autowired
    private lateinit var lettingAgentAccessRepository: LettingAgentAccessRepository

    @MockitoBean
    private lateinit var confirmationEmailSender: EmailNotificationService<PropertyDeactivationConfirmationEmail>

    @Test
    fun `landlord can deactivate a single-landlord property from the property record`(page: Page) {
        val detailsPage = navigator.goToPropertyDetailsLandlordView(propertyOwnershipId)
        BaseComponent.assertThat(detailsPage.deregisterPropertyLink).isHidden()
        detailsPage.deactivatePropertyLink.clickAndWait()

        val confirmPage =
            assertPageIs(
                page,
                ConfirmPagePropertyDeactivation::class,
                mapOf("propertyOwnershipId" to propertyOwnershipId.toString()),
            )
        assertThat(confirmPage.heading).containsText("Are you sure you want to deactivate this property?")
        confirmPage.submitConfirm()

        assertFalse(propertyOwnershipRepository.findById(propertyOwnershipId).get().isActive)
        assertNull(lettingAgentAccessRepository.findByPropertyOwnershipId(propertyOwnershipId))
        verify(confirmationEmailSender).sendEmail(any(), argThat { !landlordDeactivated })
        // TODO PDJB-2035: Assert landing on PropertyDetailsPageLandlordView with the "Property deactivated." banner
        //  once inactive property records render
    }

    @Test
    fun `cancel returns to the property record without deactivating`(page: Page) {
        val confirmPage = navigator.goToPropertyDeactivationConfirmPage(propertyOwnershipId)
        confirmPage.cancelLink.clickAndWait()

        assertPageIs(page, PropertyDetailsPageLandlordView::class, mapOf("propertyOwnershipId" to propertyOwnershipId.toString()))
        assertTrue(propertyOwnershipRepository.findById(propertyOwnershipId).get().isActive)
    }

    @Test
    fun `confirm page hides the landlord account section when the landlord has other active properties`(page: Page) {
        val confirmPage = navigator.goToPropertyDeactivationConfirmPage(propertyOwnershipId)

        assertThat(confirmPage.landlordAccountHeading).isHidden()
    }
}

class PropertyDeactivationJointLandlordJourneyTests :
    IntegrationTestWithMutableData("data-mockuser-landlord-with-sole-and-joint-properties.sql") {
    private val solePropertyOwnershipId = 1L
    private val jointPropertyOwnershipId = 2L
    private val landlordId = 1L

    @Autowired
    private lateinit var landlordRepository: LandlordRepository

    @Autowired
    private lateinit var activeInactiveService: ActiveInactiveService

    @MockitoBean
    private lateinit var confirmationEmailSender: EmailNotificationService<PropertyDeactivationConfirmationEmail>

    @Test
    fun `joint landlord property shows the cannot deactivate page linking to leave property`(page: Page) {
        navigator.navigate(DeactivatePropertyController.getPropertyDeactivationPath(jointPropertyOwnershipId))

        val cannotDeactivatePage =
            assertPageIs(
                page,
                CannotDeactivatePropertyJointLandlordsPage::class,
                mapOf("propertyOwnershipId" to jointPropertyOwnershipId.toString()),
            )
        cannotDeactivatePage.leavePropertyLink.clickAndWait()

        assertPageIs(page, ConfirmPageLeaveProperty::class, mapOf("propertyOwnershipId" to jointPropertyOwnershipId.toString()))
    }

    @Test
    fun `deactivating the landlord's last active property shows the account section and deactivates the landlord`(page: Page) {
        activeInactiveService.deactivateProperty(jointPropertyOwnershipId)

        val confirmPage = navigator.goToPropertyDeactivationConfirmPage(solePropertyOwnershipId)
        assertThat(confirmPage.landlordAccountHeading).isVisible()
        confirmPage.submitConfirm()

        assertFalse(landlordRepository.findById(landlordId).get().isActive)
        verify(confirmationEmailSender).sendEmail(any(), argThat { landlordDeactivated })
    }
}
