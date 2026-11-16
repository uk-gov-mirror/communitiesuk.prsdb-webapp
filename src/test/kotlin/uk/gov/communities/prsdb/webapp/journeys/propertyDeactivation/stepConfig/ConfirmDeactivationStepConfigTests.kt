package uk.gov.communities.prsdb.webapp.journeys.propertyDeactivation.stepConfig

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.communities.prsdb.webapp.controllers.PropertyDetailsController
import uk.gov.communities.prsdb.webapp.journeys.propertyDeactivation.PropertyDeactivationJourneyState
import uk.gov.communities.prsdb.webapp.models.dataModels.RegistrationNumberDataModel
import uk.gov.communities.prsdb.webapp.models.viewModels.emailModels.PropertyDeactivationConfirmationEmail
import uk.gov.communities.prsdb.webapp.services.AbsoluteUrlProvider
import uk.gov.communities.prsdb.webapp.services.ActiveInactiveService
import uk.gov.communities.prsdb.webapp.services.CurrentUserService
import uk.gov.communities.prsdb.webapp.services.EmailNotificationService
import uk.gov.communities.prsdb.webapp.services.PropertyOwnershipService
import uk.gov.communities.prsdb.webapp.services.UserToLandlordService
import uk.gov.communities.prsdb.webapp.testHelpers.mockObjects.AlwaysTrueValidator
import uk.gov.communities.prsdb.webapp.testHelpers.mockObjects.MockLandlordData
import java.net.URI

@ExtendWith(MockitoExtension::class)
class ConfirmDeactivationStepConfigTests {
    @Mock
    lateinit var mockPropertyOwnershipService: PropertyOwnershipService

    @Mock
    lateinit var mockActiveInactiveService: ActiveInactiveService

    @Mock
    lateinit var mockUserToLandlordService: UserToLandlordService

    @Mock
    lateinit var mockCurrentUserService: CurrentUserService

    @Mock
    lateinit var mockAbsoluteUrlProvider: AbsoluteUrlProvider

    @Mock
    lateinit var mockConfirmationEmailSender: EmailNotificationService<PropertyDeactivationConfirmationEmail>

    @Mock
    lateinit var mockState: PropertyDeactivationJourneyState

    private val propertyOwnershipId = 123L
    private val landlord = MockLandlordData.createIndividualLandlord()
    private val propertyOwnership = MockLandlordData.createPropertyOwnership(id = propertyOwnershipId)

    @Test
    fun `getStepSpecificContent shows the landlord account section when this is the landlord's last active property`() {
        arrangeLandlord(hasOtherActiveProperties = false)

        val content = setupStepConfig().getStepSpecificContent(mockState)

        assertEquals(true, content["showLandlordAccountSection"])
        assertEquals(PropertyDetailsController.getPropertyDetailsPath(propertyOwnershipId), content["cancelLinkUrl"])
    }

    @Test
    fun `getStepSpecificContent hides the landlord account section when the landlord has other active properties`() {
        arrangeLandlord(hasOtherActiveProperties = true)

        val content = setupStepConfig().getStepSpecificContent(mockState)

        assertEquals(false, content["showLandlordAccountSection"])
    }

    @Test
    fun `afterStepDataIsAdded deactivates the property`() {
        arrangeAfterStepDataIsAdded(hasOtherActiveProperties = true)

        setupStepConfig().afterStepDataIsAdded(mockState)

        verify(mockActiveInactiveService).deactivateProperty(propertyOwnershipId)
    }

    @Test
    fun `afterStepDataIsAdded emails the current user with landlordDeactivated true when it was their last active property`() {
        arrangeAfterStepDataIsAdded(hasOtherActiveProperties = false)

        setupStepConfig().afterStepDataIsAdded(mockState)

        verify(mockConfirmationEmailSender).sendEmail(
            eq("james@example.com"),
            eq(
                PropertyDeactivationConfirmationEmail(
                    recipientName = "James",
                    multiLineAddress = propertyOwnership.address.toMultiLineAddress(),
                    registrationNumber =
                        RegistrationNumberDataModel.fromRegistrationNumber(propertyOwnership.registrationNumber).toString(),
                    landlordDeactivated = true,
                    dashboardUrl = "https://example.com/dashboard",
                ),
            ),
        )
    }

    @Test
    fun `afterStepDataIsAdded emails the current user with landlordDeactivated false when they have other active properties`() {
        arrangeAfterStepDataIsAdded(hasOtherActiveProperties = true)

        setupStepConfig().afterStepDataIsAdded(mockState)

        verify(mockConfirmationEmailSender).sendEmail(
            eq("james@example.com"),
            argThat<PropertyDeactivationConfirmationEmail> { !landlordDeactivated },
        )
    }

    @Test
    fun `afterStepDataIsAdded does not throw when the confirmation email fails to send`() {
        arrangeAfterStepDataIsAdded(hasOtherActiveProperties = true)
        whenever(mockConfirmationEmailSender.sendEmail(any(), any())).thenThrow(RuntimeException("Notify is down"))

        assertDoesNotThrow { setupStepConfig().afterStepDataIsAdded(mockState) }
        verify(mockActiveInactiveService).deactivateProperty(propertyOwnershipId)
    }

    private fun arrangeLandlord(hasOtherActiveProperties: Boolean) {
        whenever(mockState.propertyOwnershipId).thenReturn(propertyOwnershipId)
        whenever(mockUserToLandlordService.getCurrentLandlordForUser()).thenReturn(landlord)
        whenever(mockActiveInactiveService.hasOtherActiveProperties(landlord.id, propertyOwnershipId))
            .thenReturn(hasOtherActiveProperties)
    }

    private fun arrangeAfterStepDataIsAdded(hasOtherActiveProperties: Boolean) {
        arrangeLandlord(hasOtherActiveProperties)
        whenever(mockPropertyOwnershipService.getPropertyOwnership(propertyOwnershipId)).thenReturn(propertyOwnership)
        whenever(mockCurrentUserService.getRegisteredUserDetails())
            .thenReturn(CurrentUserService.RegisteredUserDetails(name = "James", email = "james@example.com"))
        whenever(mockAbsoluteUrlProvider.buildLandlordDashboardUri()).thenReturn(URI("https://example.com/dashboard"))
    }

    private fun setupStepConfig() =
        ConfirmDeactivationStepConfig(
            mockPropertyOwnershipService,
            mockActiveInactiveService,
            mockUserToLandlordService,
            mockCurrentUserService,
            mockAbsoluteUrlProvider,
            mockConfirmationEmailSender,
        ).apply {
            urlPath = ConfirmDeactivationStep.ROUTE_SEGMENT
            validator = AlwaysTrueValidator()
        }
}
