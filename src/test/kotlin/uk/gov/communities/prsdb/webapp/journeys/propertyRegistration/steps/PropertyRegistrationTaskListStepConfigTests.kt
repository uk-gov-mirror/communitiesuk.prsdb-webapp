package uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.junit.jupiter.MockitoSettings
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.mockito.quality.Strictness
import org.springframework.mock.web.MockHttpServletRequest
import uk.gov.communities.prsdb.webapp.config.managers.FeatureFlagManager
import uk.gov.communities.prsdb.webapp.constants.PAYMENTS
import uk.gov.communities.prsdb.webapp.constants.enums.TaskStatus
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.PropertyRegistrationJourneyState
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.tasks.CorrespondenceTask
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.tasks.ElectricalSafetyTask
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.tasks.EpcTask
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.tasks.GasSafetyTask
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.tasks.LicensingTask
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.tasks.OwnershipAndLandlordsTaskBeforeAssociatedParties
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.tasks.PropertyDetailsTask
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.tasks.TenancyDetailsTask
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.tasks.WhoProvidesDetailsTask
import uk.gov.communities.prsdb.webapp.services.BackUrlStorageService

@ExtendWith(MockitoExtension::class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PropertyRegistrationTaskListStepConfigTests {
    @Mock
    lateinit var mockFeatureFlagManager: FeatureFlagManager

    @Mock
    lateinit var mockBackUrlStorageService: BackUrlStorageService

    @Mock
    lateinit var mockState: PropertyRegistrationJourneyState

    @Mock
    lateinit var mockOccupiedStep: OccupiedStep

    @Mock
    lateinit var mockCyaStep: PropertyRegistrationCyaStep

    private lateinit var request: MockHttpServletRequest
    private lateinit var stepConfig: PropertyRegistrationTaskListStepConfig

    @BeforeEach
    fun setUp() {
        request = MockHttpServletRequest()
        stepConfig = PropertyRegistrationTaskListStepConfig(mockFeatureFlagManager, request, mockBackUrlStorageService)
    }

    @Test
    fun `chooseTemplate returns taskList`() {
        assertEquals("taskList", stepConfig.chooseTemplate(mockState))
    }

    @Nested
    inner class WhoProvidesDetailsTaskListItemTests {
        @BeforeEach
        fun setUp() {
            stubState()
        }

        @Test
        fun `getTaskListViewModel shows who provides details as NOT_NEEDED_YET for an unoccupied property`() {
            // Arrange
            whenever(mockState.cachedOccupied).thenReturn(false)

            // Act
            val taskListViewModel = stepConfig.getTaskListViewModel(mockState)

            // Assert
            val whoProvidesItem =
                taskListViewModel.taskSections[1].tasks.find {
                    it.nameKey == "registerProperty.taskList.rentedOut.whoProvidesDetails"
                }
            assertEquals("taskList.status.notNeededYet", whoProvidesItem?.status?.textKey)
            assertEquals("registerProperty.taskList.rentedOut.whoProvidesDetailsNotRequiredHint", whoProvidesItem?.hintKey)
            assertNull(whoProvidesItem?.url)
        }

        @Test
        fun `getTaskListViewModel shows who provides details as a task for an occupied property`() {
            // Arrange
            whenever(mockState.cachedOccupied).thenReturn(true)

            // Act
            val taskListViewModel = stepConfig.getTaskListViewModel(mockState)

            // Assert
            val whoProvidesItem =
                taskListViewModel.taskSections[1].tasks.find {
                    it.nameKey == "registerProperty.taskList.rentedOut.whoProvidesDetails"
                }
            assertEquals("taskList.status.cannotStart", whoProvidesItem?.status?.textKey)
        }
    }

    @Test
    fun `getTaskListViewModel shows tenancy details as NOT_NEEDED_YET for an unoccupied property`() {
        // Arrange
        whenever(mockState.cachedOccupied).thenReturn(false)
        stubState()

        // Act
        val taskListViewModel = stepConfig.getTaskListViewModel(mockState)

        // Assert
        val tenancyItem =
            taskListViewModel.taskSections[1].tasks.find {
                it.nameKey == "registerProperty.taskList.rentedOut.tenancyDetails"
            }
        assertEquals("taskList.status.notNeededYet", tenancyItem?.status?.textKey)
        assertEquals("registerProperty.taskList.rentedOut.tenancyDetailsNotNeededYetHint", tenancyItem?.hintKey)
        assertNull(tenancyItem?.url)
    }

    @Test
    fun `getTaskListViewModel shows tenancy details task for an occupied property`() {
        // Arrange
        whenever(mockState.cachedOccupied).thenReturn(true)
        stubState()

        // Act
        val taskListViewModel = stepConfig.getTaskListViewModel(mockState)

        // Assert
        val tenancyItem =
            taskListViewModel.taskSections[1].tasks.find {
                it.nameKey == "registerProperty.taskList.rentedOut.tenancyDetails"
            }
        assertEquals("taskList.status.cannotStart", tenancyItem?.status?.textKey)
        assertNull(tenancyItem?.url)
    }

    @Nested
    inner class SubmitYourRegistrationTaskListItemTests {
        @BeforeEach
        fun setUp() {
            stubState()
        }

        @Test
        fun `getTaskListViewModel labels the submit task 'Check and submit your answers' when PAYMENTS is disabled`() {
            // Arrange
            whenever(mockFeatureFlagManager.checkFeature(PAYMENTS)).thenReturn(false)

            // Act
            val submitSection = stepConfig.getTaskListViewModel(mockState).taskSections[2]

            // Assert
            assertEquals("registerProperty.taskList.submitYourRegistration.heading", submitSection.headingKey)
            assertEquals(
                listOf("registerProperty.taskList.checkAndSubmit.checkAnswers"),
                submitSection.tasks.map { it.nameKey },
            )
        }

        @Test
        fun `getTaskListViewModel labels the submit task 'Submit and pay' but keeps the section heading when PAYMENTS is enabled`() {
            // Arrange
            whenever(mockFeatureFlagManager.checkFeature(PAYMENTS)).thenReturn(true)

            // Act
            val submitSection = stepConfig.getTaskListViewModel(mockState).taskSections[2]

            // Assert
            assertEquals("registerProperty.taskList.submitYourRegistration.heading", submitSection.headingKey)
            assertEquals(
                listOf("registerProperty.taskList.checkAndSubmit.checkAnswersWithPayment"),
                submitSection.tasks.map { it.nameKey },
            )
        }
    }

    private fun stubState() {
        val mockPropertyDetailsTask = mock<PropertyDetailsTask>()
        val mockOwnershipAndLandlordsTaskBeforeAssociatedParties = mock<OwnershipAndLandlordsTaskBeforeAssociatedParties>()
        val mockCorrespondenceTask = mock<CorrespondenceTask>()
        val mockLicensingTask = mock<LicensingTask>()
        val mockTenancyDetailsTask = mock<TenancyDetailsTask>()
        val mockWhoProvidesDetailsTask = mock<WhoProvidesDetailsTask>()
        val mockGasSafetyTask = mock<GasSafetyTask>()
        val mockElectricalSafetyTask = mock<ElectricalSafetyTask>()
        val mockEpcTask = mock<EpcTask>()

        whenever(mockPropertyDetailsTask.taskStatus()).thenReturn(TaskStatus.CANNOT_START)
        whenever(mockOwnershipAndLandlordsTaskBeforeAssociatedParties.taskStatus()).thenReturn(TaskStatus.CANNOT_START)
        whenever(mockCorrespondenceTask.taskStatus()).thenReturn(TaskStatus.CANNOT_START)
        whenever(mockLicensingTask.taskStatus()).thenReturn(TaskStatus.CANNOT_START)
        whenever(mockTenancyDetailsTask.taskStatus()).thenReturn(TaskStatus.CANNOT_START)
        whenever(mockWhoProvidesDetailsTask.taskStatus()).thenReturn(TaskStatus.CANNOT_START)
        whenever(mockGasSafetyTask.taskStatus()).thenReturn(TaskStatus.CANNOT_START)
        whenever(mockElectricalSafetyTask.taskStatus()).thenReturn(TaskStatus.CANNOT_START)
        whenever(mockEpcTask.taskStatus()).thenReturn(TaskStatus.CANNOT_START)
        whenever(mockOccupiedStep.currentJourneyId).thenReturn("test-journey-id")
        whenever(mockCyaStep.currentJourneyId).thenReturn("test-journey-id")

        whenever(mockState.propertyDetailsTask).thenReturn(mockPropertyDetailsTask)
        whenever(
            mockState.ownershipAndLandlordsTaskBeforeAssociatedParties,
        ).thenReturn(mockOwnershipAndLandlordsTaskBeforeAssociatedParties)
        whenever(mockState.correspondenceTask).thenReturn(mockCorrespondenceTask)
        whenever(mockState.occupied).thenReturn(mockOccupiedStep)
        whenever(mockState.licensingTask).thenReturn(mockLicensingTask)
        whenever(mockState.tenancyDetailsTask).thenReturn(mockTenancyDetailsTask)
        whenever(mockState.whoProvidesDetailsTask).thenReturn(mockWhoProvidesDetailsTask)
        whenever(mockState.gasSafetyTask).thenReturn(mockGasSafetyTask)
        whenever(mockState.electricalSafetyTask).thenReturn(mockElectricalSafetyTask)
        whenever(mockState.epcTask).thenReturn(mockEpcTask)
        whenever(mockState.cyaStep).thenReturn(mockCyaStep)
    }
}
