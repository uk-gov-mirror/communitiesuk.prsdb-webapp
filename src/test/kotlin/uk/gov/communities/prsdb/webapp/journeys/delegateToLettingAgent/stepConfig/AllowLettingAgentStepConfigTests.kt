package uk.gov.communities.prsdb.webapp.journeys.delegateToLettingAgent.stepConfig

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.communities.prsdb.webapp.database.entity.Landlord
import uk.gov.communities.prsdb.webapp.journeys.Destination
import uk.gov.communities.prsdb.webapp.journeys.delegateToLettingAgent.DelegateToLettingAgentJourneyState
import uk.gov.communities.prsdb.webapp.services.DelegateToLettingAgentEmailService
import uk.gov.communities.prsdb.webapp.services.LettingAgentAccessService
import uk.gov.communities.prsdb.webapp.services.PropertyOwnershipService
import uk.gov.communities.prsdb.webapp.services.UserToLandlordService
import uk.gov.communities.prsdb.webapp.testHelpers.mockObjects.AlwaysTrueValidator
import uk.gov.communities.prsdb.webapp.testHelpers.mockObjects.MockLandlordData
import uk.gov.communities.prsdb.webapp.testHelpers.mockObjects.MockLettingAgentData
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class AllowLettingAgentStepConfigTests {
    @Mock
    lateinit var mockUserToLandlordService: UserToLandlordService

    @Mock
    lateinit var mockPropertyOwnershipService: PropertyOwnershipService

    @Mock
    lateinit var mockLettingAgentAccessService: LettingAgentAccessService

    @Mock
    lateinit var mockDelegateToLettingAgentEmailService: DelegateToLettingAgentEmailService

    @Mock
    lateinit var mockJourneyState: DelegateToLettingAgentJourneyState

    @Mock
    lateinit var mockLandlord: Landlord

    private fun createStepConfig() =
        AllowLettingAgentStepConfig(
            mockUserToLandlordService,
            mockPropertyOwnershipService,
            mockLettingAgentAccessService,
            mockDelegateToLettingAgentEmailService,
        ).apply {
            urlPath = AllowLettingAgentStep.ROUTE_SEGMENT
            validator = AlwaysTrueValidator()
        }

    @Test
    fun `enrichSubmittedDataBeforeValidation injects the landlord email snapshot at the start of the journey`() {
        val stepConfig = createStepConfig()

        whenever(mockUserToLandlordService.getCurrentLandlordForUser()).thenReturn(mockLandlord)
        whenever(mockLandlord.email).thenReturn("landlord@example.com")

        val result = stepConfig.enrichSubmittedDataBeforeValidation(mockJourneyState, emptyMap())

        assertEquals("landlord@example.com", result["landlordEmailAtStartOfJourney"])
    }

    @Test
    fun `chooseTemplate returns the allow letting agent form template`() {
        val stepConfig = createStepConfig()

        assertEquals("forms/allowLettingAgentForm", stepConfig.chooseTemplate(mockJourneyState))
    }

    @Test
    fun `afterStepDataIsAdded persists the submitted email as a letting agent invitation`() {
        val stepConfig = createStepConfig()
        val landlord = MockLandlordData.createIndividualLandlord()
        val propertyOwnership = MockLandlordData.createPropertyOwnership()
        val lettingAgentAccess = MockLettingAgentData.createLettingAgentAccess(propertyOwnership = propertyOwnership)

        whenever(mockJourneyState.getStepData(AllowLettingAgentStep.ROUTE_SEGMENT))
            .thenReturn(mapOf("emailAddress" to "agent@example.com"))
        whenever(mockJourneyState.propertyOwnershipId).thenReturn(PROPERTY_OWNERSHIP_ID)
        whenever(mockPropertyOwnershipService.getPropertyOwnership(PROPERTY_OWNERSHIP_ID))
            .thenReturn(propertyOwnership)
        whenever(mockUserToLandlordService.getCurrentLandlordForUser()).thenReturn(landlord)
        whenever(mockLettingAgentAccessService.createInvitation(propertyOwnership, "agent@example.com")).thenReturn(lettingAgentAccess)

        stepConfig.afterStepDataIsAdded(mockJourneyState)

        verify(mockLettingAgentAccessService).createInvitation(propertyOwnership, "agent@example.com")
        verify(mockLettingAgentAccessService).addDelegatedPropertyOwnershipToSession(PROPERTY_OWNERSHIP_ID, "agent@example.com")
    }

    @Test
    fun `afterStepDataIsAdded sends delegation emails to landlords and letting agent`() {
        val stepConfig = createStepConfig()
        val landlord = MockLandlordData.createIndividualLandlord(name = "Wallis Smith")
        val propertyOwnership = MockLandlordData.createPropertyOwnership(id = PROPERTY_OWNERSHIP_ID)
        val expectedToken = UUID.randomUUID()
        val lettingAgentAccess =
            MockLettingAgentData.createLettingAgentAccess(token = expectedToken, propertyOwnership = propertyOwnership)

        whenever(mockJourneyState.getStepData(AllowLettingAgentStep.ROUTE_SEGMENT))
            .thenReturn(mapOf("emailAddress" to "agent@example.com"))
        whenever(mockJourneyState.propertyOwnershipId).thenReturn(PROPERTY_OWNERSHIP_ID)
        whenever(mockPropertyOwnershipService.getPropertyOwnership(PROPERTY_OWNERSHIP_ID)).thenReturn(propertyOwnership)
        whenever(mockUserToLandlordService.getCurrentLandlordForUser()).thenReturn(landlord)
        whenever(mockLettingAgentAccessService.createInvitation(propertyOwnership, "agent@example.com")).thenReturn(lettingAgentAccess)

        stepConfig.afterStepDataIsAdded(mockJourneyState)

        verify(mockDelegateToLettingAgentEmailService).sendDelegationEmailToLandlords(PROPERTY_OWNERSHIP_ID, "agent@example.com")
        verify(
            mockDelegateToLettingAgentEmailService,
        ).sendDelegationEmailToLettingAgent(
            propertyOwnership,
            "Wallis Smith",
            "agent@example.com",
            invitationToken = expectedToken,
        )
    }

    @Test
    fun `resolveNextDestination deletes the journey and returns the default destination`() {
        val stepConfig = createStepConfig()
        val defaultDestination = Destination.ExternalUrl("/landlord/property-details/$PROPERTY_OWNERSHIP_ID")

        val result = stepConfig.resolveNextDestination(mockJourneyState, defaultDestination)

        verify(mockJourneyState).deleteJourney()
        assertEquals(defaultDestination, result)
    }

    @Test
    fun `beforeAttemptingToReachStep returns true when the property is not already delegated`() {
        val stepConfig = createStepConfig()

        whenever(mockJourneyState.propertyOwnershipId).thenReturn(PROPERTY_OWNERSHIP_ID)
        whenever(mockPropertyOwnershipService.hasLettingAgent(PROPERTY_OWNERSHIP_ID)).thenReturn(false)

        assertTrue(stepConfig.beforeAttemptingToReachStep(mockJourneyState))
    }

    @Test
    fun `beforeAttemptingToReachStep returns false when the property is already delegated`() {
        val stepConfig = createStepConfig()

        whenever(mockJourneyState.propertyOwnershipId).thenReturn(PROPERTY_OWNERSHIP_ID)
        whenever(mockPropertyOwnershipService.hasLettingAgent(PROPERTY_OWNERSHIP_ID)).thenReturn(true)

        assertFalse(stepConfig.beforeAttemptingToReachStep(mockJourneyState))
    }

    companion object {
        private const val PROPERTY_OWNERSHIP_ID = 1L
    }
}
