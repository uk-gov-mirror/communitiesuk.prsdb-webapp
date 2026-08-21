package uk.gov.communities.prsdb.webapp.services

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.whenever
import org.springframework.test.util.ReflectionTestUtils
import uk.gov.communities.prsdb.webapp.database.entity.LettingAgentAccess
import uk.gov.communities.prsdb.webapp.database.entity.PropertyOwnership
import uk.gov.communities.prsdb.webapp.models.viewModels.emailModels.CancelDelegationJointLandlordNotificationEmail
import uk.gov.communities.prsdb.webapp.models.viewModels.emailModels.CancelDelegationLandlordConfirmationEmail
import uk.gov.communities.prsdb.webapp.models.viewModels.emailModels.CancelDelegationLettingAgentNotificationEmail
import uk.gov.communities.prsdb.webapp.testHelpers.mockObjects.MockLandlordData
import java.net.URI
import java.util.UUID
import kotlin.test.assertEquals

class CancelLettingAgentDelegationEmailServiceTests {
    private lateinit var mockUserToLandlordService: UserToLandlordService
    private lateinit var mockAbsoluteUrlProvider: AbsoluteUrlProvider
    private lateinit var mockLandlordConfirmationEmailService: EmailNotificationService<CancelDelegationLandlordConfirmationEmail>
    private lateinit var mockJointLandlordNotificationEmailService: EmailNotificationService<CancelDelegationJointLandlordNotificationEmail>
    private lateinit var mockLettingAgentNotificationEmailService: EmailNotificationService<CancelDelegationLettingAgentNotificationEmail>
    private lateinit var emailService: CancelLettingAgentDelegationEmailService

    @BeforeEach
    fun setup() {
        mockUserToLandlordService = mock()
        mockAbsoluteUrlProvider = mock()
        mockLandlordConfirmationEmailService = mock()
        mockJointLandlordNotificationEmailService = mock()
        mockLettingAgentNotificationEmailService = mock()
        emailService =
            CancelLettingAgentDelegationEmailService(
                mockUserToLandlordService,
                mockAbsoluteUrlProvider,
                mockLandlordConfirmationEmailService,
                mockJointLandlordNotificationEmailService,
                mockLettingAgentNotificationEmailService,
            )
    }

    private fun createPropertyOwnershipWithLettingAgent(
        id: Long = 5,
        landlords: MutableSet<uk.gov.communities.prsdb.webapp.database.entity.Landlord> =
            mutableSetOf(MockLandlordData.createIndividualLandlord()),
        lettingAgentEmail: String = "agent@example.com",
    ): PropertyOwnership {
        val propertyOwnership = MockLandlordData.createPropertyOwnership(id = id, landlords = landlords)
        val lettingAgentAccess = LettingAgentAccess(UUID.randomUUID(), lettingAgentEmail, propertyOwnership)
        ReflectionTestUtils.setField(propertyOwnership, "lettingAgentAccess", lettingAgentAccess)
        return propertyOwnership
    }

    @Test
    fun `sendCancellationEmails sends confirmation email to landlord with correct details`() {
        val landlord = MockLandlordData.createIndividualLandlord(name = "Alice", email = "alice@example.com")
        val propertyOwnership =
            createPropertyOwnershipWithLettingAgent(
                id = 5,
                landlords = mutableSetOf(landlord),
                lettingAgentEmail = "agent@example.com",
            )
        whenever(mockUserToLandlordService.getCurrentLandlordForUser()).thenReturn(landlord)
        whenever(mockAbsoluteUrlProvider.buildPropertyDetailsUri(5)).thenReturn(URI("https://example.com/property/5"))

        emailService.sendCancellationEmails(propertyOwnership)

        val captor = argumentCaptor<CancelDelegationLandlordConfirmationEmail>()
        verify(mockLandlordConfirmationEmailService).sendEmail(eq("alice@example.com"), captor.capture())
        assertEquals("Alice", captor.firstValue.landlordName)
        assertEquals("agent@example.com", captor.firstValue.lettingAgentEmail)
        assertEquals(propertyOwnership.address.toMultiLineAddress(), captor.firstValue.propertyAddress)
        assertEquals("https://example.com/property/5", captor.firstValue.propertyRecordUrl)
    }

    @Test
    fun `sendCancellationEmails sends notification to joint landlords`() {
        val actingLandlord = MockLandlordData.createIndividualLandlord(name = "Alice", email = "alice@example.com")
        val jointLandlord = MockLandlordData.createIndividualLandlord(name = "Bob", email = "bob@example.com")
        val propertyOwnership =
            createPropertyOwnershipWithLettingAgent(
                id = 5,
                landlords = mutableSetOf(actingLandlord, jointLandlord),
                lettingAgentEmail = "agent@example.com",
            )
        whenever(mockUserToLandlordService.getCurrentLandlordForUser()).thenReturn(actingLandlord)
        whenever(mockAbsoluteUrlProvider.buildPropertyDetailsUri(5)).thenReturn(URI("https://example.com/property/5"))

        emailService.sendCancellationEmails(propertyOwnership)

        val captor = argumentCaptor<CancelDelegationJointLandlordNotificationEmail>()
        verify(mockJointLandlordNotificationEmailService).sendEmail(eq("bob@example.com"), captor.capture())
        assertEquals("Bob", captor.firstValue.jointLandlordName)
        assertEquals("agent@example.com", captor.firstValue.lettingAgentEmail)
    }

    @Test
    fun `sendCancellationEmails sends notification to letting agent`() {
        val landlord = MockLandlordData.createIndividualLandlord(name = "Alice", email = "alice@example.com")
        val propertyOwnership =
            createPropertyOwnershipWithLettingAgent(
                id = 5,
                landlords = mutableSetOf(landlord),
                lettingAgentEmail = "agent@example.com",
            )
        whenever(mockUserToLandlordService.getCurrentLandlordForUser()).thenReturn(landlord)
        whenever(mockAbsoluteUrlProvider.buildPropertyDetailsUri(5)).thenReturn(URI("https://example.com/property/5"))

        emailService.sendCancellationEmails(propertyOwnership)

        val captor = argumentCaptor<CancelDelegationLettingAgentNotificationEmail>()
        verify(mockLettingAgentNotificationEmailService).sendEmail(eq("agent@example.com"), captor.capture())
        assertEquals(propertyOwnership.address.toMultiLineAddress(), captor.firstValue.propertyAddress)
        assertEquals(propertyOwnership.address.singleLineAddress, captor.firstValue.singleLineAddress)
    }

    @Test
    fun `sendCancellationEmails does not send any emails when letting agent access is null`() {
        val landlord = MockLandlordData.createIndividualLandlord(name = "Alice", email = "alice@example.com")
        val propertyOwnership =
            MockLandlordData.createPropertyOwnership(
                id = 5,
                landlords = mutableSetOf(landlord),
            )
        whenever(mockUserToLandlordService.getCurrentLandlordForUser()).thenReturn(landlord)

        emailService.sendCancellationEmails(propertyOwnership)

        verify(mockLandlordConfirmationEmailService, never()).sendEmail(any(), any())
        verify(mockJointLandlordNotificationEmailService, never()).sendEmail(any(), any())
        verify(mockLettingAgentNotificationEmailService, never()).sendEmail(any(), any())
    }

    @Test
    fun `sendCancellationEmails does not send joint landlord email when there are no other landlords`() {
        val landlord = MockLandlordData.createIndividualLandlord(name = "Alice", email = "alice@example.com")
        val propertyOwnership =
            createPropertyOwnershipWithLettingAgent(
                id = 5,
                landlords = mutableSetOf(landlord),
                lettingAgentEmail = "agent@example.com",
            )
        whenever(mockUserToLandlordService.getCurrentLandlordForUser()).thenReturn(landlord)
        whenever(mockAbsoluteUrlProvider.buildPropertyDetailsUri(5)).thenReturn(URI("https://example.com/property/5"))

        emailService.sendCancellationEmails(propertyOwnership)

        verify(mockJointLandlordNotificationEmailService, never()).sendEmail(any(), any())
    }
}
