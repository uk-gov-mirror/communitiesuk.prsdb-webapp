package uk.gov.communities.prsdb.webapp.journeys.propertyRegistration

import kotlinx.datetime.Clock
import kotlinx.datetime.toJavaInstant
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.junit.jupiter.MockitoSettings
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.mockito.quality.Strictness
import uk.gov.communities.prsdb.webapp.exceptions.UpdateConflictException
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.update.correspondenceAddress.UpdateCorrespondenceAddressCyaConfig
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.update.correspondenceAddress.UpdateCorrespondenceAddressJourneyState
import uk.gov.communities.prsdb.webapp.journeys.shared.stepConfig.LookupAddressStep
import uk.gov.communities.prsdb.webapp.journeys.shared.tasks.CorrespondenceAddressTask
import uk.gov.communities.prsdb.webapp.models.dataModels.AddressDataModel
import uk.gov.communities.prsdb.webapp.services.PropertyOwnershipService
import uk.gov.communities.prsdb.webapp.services.PropertyUpdateEmailService

@ExtendWith(MockitoExtension::class)
@MockitoSettings(strictness = Strictness.LENIENT)
class UpdateCorrespondenceAddressCyaConfigTests {
    @Mock
    private lateinit var mockPropertyOwnershipService: PropertyOwnershipService

    @Mock
    private lateinit var mockPropertyUpdateEmailService: PropertyUpdateEmailService

    @Mock
    private lateinit var mockState: UpdateCorrespondenceAddressJourneyState

    @Mock
    private lateinit var mockAddressTask: CorrespondenceAddressTask

    @Mock
    private lateinit var mockLookupAddressStep: LookupAddressStep

    private lateinit var stepConfig: UpdateCorrespondenceAddressCyaConfig

    private val propertyId = 123L
    private val initialLastModifiedDate = Clock.System.now().toJavaInstant()
    private val addressDataModel =
        AddressDataModel(singleLineAddress = "1 Fictional Road, London, FA1 1AA")

    @BeforeEach
    fun setUp() {
        stepConfig =
            UpdateCorrespondenceAddressCyaConfig(
                propertyOwnershipService = mockPropertyOwnershipService,
                propertyUpdateEmailService = mockPropertyUpdateEmailService,
            )
        whenever(mockState.propertyId).thenReturn(propertyId)
        whenever(mockState.lastModifiedDate).thenReturn(initialLastModifiedDate.toString())
        whenever(mockState.addressTask).thenReturn(mockAddressTask)
        whenever(mockAddressTask.lookupAddressStep).thenReturn(mockLookupAddressStep)
        whenever(mockAddressTask.getAddress()).thenReturn(addressDataModel)
    }

    @Test
    fun `afterStepDataIsAdded calls updateCorrespondenceAddress on propertyOwnershipService`() {
        stepConfig.afterStepDataIsAdded(mockState)

        verify(mockPropertyOwnershipService).updateCorrespondenceAddress(
            id = propertyId,
            address = addressDataModel,
            initialLastModifiedDate = initialLastModifiedDate,
        )
    }

    @Test
    fun `afterStepDataIsAdded sends update emails after a successful save`() {
        stepConfig.afterStepDataIsAdded(mockState)

        verify(mockPropertyUpdateEmailService).sendUpdateEmails(
            propertyId,
            listOf("The postal address the council should contact"),
        )
    }

    @Test
    fun `afterStepDataIsAdded deletes the journey then rethrows when it gets an UpdateConflictException`() {
        whenever(
            mockPropertyOwnershipService.updateCorrespondenceAddress(
                id = propertyId,
                address = addressDataModel,
                initialLastModifiedDate = initialLastModifiedDate,
            ),
        ).thenThrow(UpdateConflictException::class.java)

        assertThrows<UpdateConflictException> { stepConfig.afterStepDataIsAdded(mockState) }

        verify(mockState).deleteJourney()
        verifyNoInteractions(mockPropertyUpdateEmailService)
    }
}
