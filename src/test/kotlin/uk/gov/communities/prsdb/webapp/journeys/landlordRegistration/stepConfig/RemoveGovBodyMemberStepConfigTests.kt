package uk.gov.communities.prsdb.webapp.journeys.landlordRegistration.stepConfig

import kotlinx.datetime.LocalDate
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.inOrder
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.communities.prsdb.webapp.constants.enums.GoverningBodyMemberType
import uk.gov.communities.prsdb.webapp.journeys.landlordRegistration.states.OrgGovBodyMembersState
import uk.gov.communities.prsdb.webapp.models.dataModels.AddressDataModel
import uk.gov.communities.prsdb.webapp.models.dataModels.GoverningBodyMemberDataModel
import uk.gov.communities.prsdb.webapp.services.CollectionKeyParameterService
import uk.gov.communities.prsdb.webapp.testHelpers.mockObjects.AlwaysTrueValidator

@ExtendWith(MockitoExtension::class)
class RemoveGovBodyMemberStepConfigTests {
    @Mock
    lateinit var mockState: OrgGovBodyMembersState

    @Mock
    lateinit var collectionKeyParameterService: CollectionKeyParameterService

    @Test
    fun `beforeAttemptingToReachStep returns false when valid key would take list to minimum size`() {
        val stepConfig = setupStepConfig()
        whenever(collectionKeyParameterService.getParameterOrNull()).thenReturn(1)
        whenever(mockState.governingBodyMembersMap).thenReturn(mapOf(1 to createMember("Alex Example")))
        whenever(mockState.allowRemovingLastMember).thenReturn(false)

        assertFalse(stepConfig.beforeAttemptingToReachStep(mockState))
    }

    @Test
    fun `beforeAttemptingToReachStep returns true when valid key leaves list above minimum size`() {
        val stepConfig = setupStepConfig()
        whenever(collectionKeyParameterService.getParameterOrNull()).thenReturn(1)
        whenever(
            mockState.governingBodyMembersMap,
        ).thenReturn(mapOf(1 to createMember("Alex Example"), 2 to createMember("Jamie Example")))

        assertTrue(stepConfig.beforeAttemptingToReachStep(mockState))
    }

    @Test
    fun `beforeAttemptingToReachStep returns false when key is null`() {
        val stepConfig = setupStepConfig()
        whenever(collectionKeyParameterService.getParameterOrNull()).thenReturn(null)

        assertFalse(stepConfig.beforeAttemptingToReachStep(mockState))
    }

    @Test
    fun `beforeAttemptingToReachStep returns false when key is not in map`() {
        val stepConfig = setupStepConfig()
        whenever(collectionKeyParameterService.getParameterOrNull()).thenReturn(2)
        whenever(mockState.governingBodyMembersMap).thenReturn(mapOf(1 to createMember("Alex Example")))

        assertFalse(stepConfig.beforeAttemptingToReachStep(mockState))
    }

    @Test
    fun `beforeAttemptingToReachStep returns true for registration behaviour when minimum is zero`() {
        val stepConfig = setupStepConfig()
        whenever(collectionKeyParameterService.getParameterOrNull()).thenReturn(1)
        whenever(mockState.governingBodyMembersMap).thenReturn(mapOf(1 to createMember("Alex Example")))
        whenever(mockState.allowRemovingLastMember).thenReturn(true)

        assertTrue(stepConfig.beforeAttemptingToReachStep(mockState))
    }

    @Test
    fun `afterStepIsReached removes the member and marks answers changed`() {
        // Arrange
        val stepConfig = setupStepConfig()
        whenever(collectionKeyParameterService.getParameterOrNull()).thenReturn(1)
        whenever(
            mockState.governingBodyMembersMap,
        ).thenReturn(mapOf(1 to createMember("Alex Example"), 2 to createMember("Jamie Example")))

        // Act
        stepConfig.afterStepIsReached(mockState)

        // Assert
        val inOrder = inOrder(mockState)
        inOrder.verify(mockState).governingBodyMembersMap = mapOf(2 to createMember("Jamie Example"))
        inOrder.verify(mockState).markAnswersChanged()
    }

    @Test
    fun `afterStepIsReached does not mark answers changed when there is no key to remove`() {
        // Arrange
        val stepConfig = setupStepConfig()
        whenever(collectionKeyParameterService.getParameterOrNull()).thenReturn(null)

        // Act
        stepConfig.afterStepIsReached(mockState)

        // Assert
        verify(mockState, never()).markAnswersChanged()
    }

    private fun setupStepConfig(): RemoveGovBodyMemberStepConfig {
        val stepConfig = RemoveGovBodyMemberStepConfig(collectionKeyParameterService)
        stepConfig.urlPath = RemoveGovBodyMemberStep.ROUTE_SEGMENT
        stepConfig.validator = AlwaysTrueValidator()
        return stepConfig
    }

    private fun createMember(name: String) =
        GoverningBodyMemberDataModel(
            name = name,
            type = GoverningBodyMemberType.DIRECTOR,
            dateOfBirth = LocalDate(1980, 1, 2),
            address = AddressDataModel(singleLineAddress = "1 Test Street, Test Town, TT1 1TT", postcode = "TT1 1TT"),
        )
}
