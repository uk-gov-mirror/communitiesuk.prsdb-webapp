package uk.gov.communities.prsdb.webapp.journeys.shared.helpers

import kotlinx.datetime.LocalDate
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import uk.gov.communities.prsdb.webapp.constants.enums.FileUploadStatus
import uk.gov.communities.prsdb.webapp.database.entity.FileUpload
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.states.CertificateUpload
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.states.ElectricalSafetyState
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.states.EpcState
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.states.GasCertOutcome
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.states.GasSafetyState
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.states.GasSupplyOutcome
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps.CheckGasCertUploadsStep
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps.GasCertIssueDateStep
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps.HasElectricalCertMode
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps.HasElectricalCertStep
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps.HasEpcMode
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps.HasEpcStep
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps.HasGasCertStep
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps.HasGasSupplyStep
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps.StartEpcStep
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.tasks.ElectricalSafetyDetailsTask
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.tasks.EpcDetailsTask
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.tasks.GasSafetyDetailsTask
import uk.gov.communities.prsdb.webapp.journeys.shared.states.CheckYourAnswersJourneyState
import uk.gov.communities.prsdb.webapp.models.viewModels.summaryModels.SummaryListRowViewModel
import uk.gov.communities.prsdb.webapp.services.EpcCertificateUrlProvider
import uk.gov.communities.prsdb.webapp.services.UploadService

internal interface TestableEpcState :
    EpcState,
    CheckYourAnswersJourneyState

@ExtendWith(MockitoExtension::class)
class ComplianceDetailsHelperTests {
    @Mock
    lateinit var mockEpcCertificateUrlProvider: EpcCertificateUrlProvider

    private val mockUploadService: UploadService = mock()

    private val helper by lazy { ComplianceDetailsHelper(mockEpcCertificateUrlProvider, mockUploadService) }

    @Nested
    inner class GetGasSafetyCyaContent {
        @Mock
        internal lateinit var mockCyaState: CheckYourAnswersJourneyState

        @Mock
        internal lateinit var mockGasState: GasSafetyState

        @Mock
        internal lateinit var mockGasDetailsTask: GasSafetyDetailsTask

        private val mockHasGasSupplyStep: HasGasSupplyStep = mock()
        private val mockHasGasCertStep: HasGasCertStep = mock()

        @BeforeEach
        fun setUp() {
            whenever(mockGasState.gasSafetyDetailsTask).thenReturn(mockGasDetailsTask)
        }

        @Test
        fun `no gas supply returns gasSupplyRows with 1 row, empty certRows, and noGasSupply inset text key`() {
            whenever(mockGasDetailsTask.gasSupplyOutcomeStep).thenReturn(mockHasGasSupplyStep)
            whenever(mockCyaState.getCyaJourneyId(any())).thenReturn("test-journey-id")
            whenever(mockGasDetailsTask.gasSupplyOutcome).thenReturn(GasSupplyOutcome.NO_SUPPLY)

            val content = helper.getGasSafetyCyaContent(mockCyaState, mockGasState)

            @Suppress("UNCHECKED_CAST")
            val gasSupplyRows = content["gasSupplyRows"] as List<SummaryListRowViewModel>

            @Suppress("UNCHECKED_CAST")
            val gasCertRows = content["gasCertRows"] as List<SummaryListRowViewModel>
            val insetTextKey = content["gasInsetTextKey"]

            assertEquals(1, gasSupplyRows.size)
            assertEquals(0, gasCertRows.size)
            assertEquals("checkGasSafety.noGasSupplyInsetText", insetTextKey)
        }

        @Test
        fun `uploaded certificate returns gasSupplyRows with 1 row, certRows with 3 rows, and null inset text key`() {
            val mockGasCertIssueDateStep: GasCertIssueDateStep = mock()
            val mockCheckGasCertUploadsStep: CheckGasCertUploadsStep = mock()

            whenever(mockGasDetailsTask.gasSupplyOutcomeStep).thenReturn(mockHasGasSupplyStep)
            whenever(mockGasDetailsTask.gasCertOutcomeStep).thenReturn(mockHasGasCertStep)
            whenever(mockCyaState.getCyaJourneyId(any())).thenReturn("test-journey-id")
            whenever(mockGasDetailsTask.gasSupplyOutcome).thenReturn(GasSupplyOutcome.HAS_SUPPLY)
            whenever(mockGasDetailsTask.gasCertOutcome).thenReturn(GasCertOutcome.HAS_CERTIFICATE)
            whenever(mockGasDetailsTask.getGasSafetyCertificateIsOutdated()).thenReturn(false)
            whenever(mockGasDetailsTask.getGasSafetyCertificateIssueDateIfReachable()).thenReturn(LocalDate(2024, 1, 15))
            whenever(mockGasDetailsTask.gasCertIssueDateStep).thenReturn(mockGasCertIssueDateStep)
            whenever(mockGasDetailsTask.checkGasCertUploadsStep).thenReturn(mockCheckGasCertUploadsStep)
            whenever(mockGasDetailsTask.gasUploadMap).thenReturn(mapOf(1 to CertificateUpload(1L, "cert.pdf")))
            val mockFileUpload: FileUpload = mock()
            whenever(mockFileUpload.status).thenReturn(FileUploadStatus.SCANNED)
            whenever(mockUploadService.getFileUploadById(1L)).thenReturn(mockFileUpload)
            whenever(mockUploadService.getDownloadUrlOrNull(any(), any())).thenReturn("/download/cert.pdf")

            val content = helper.getGasSafetyCyaContent(mockCyaState, mockGasState)

            @Suppress("UNCHECKED_CAST")
            val gasSupplyRows = content["gasSupplyRows"] as List<SummaryListRowViewModel>

            @Suppress("UNCHECKED_CAST")
            val gasCertRows = content["gasCertRows"] as List<SummaryListRowViewModel>
            val insetTextKey = content["gasInsetTextKey"]

            assertEquals(1, gasSupplyRows.size)
            assertEquals(3, gasCertRows.size)
            assertNull(insetTextKey)
        }
    }

    @Nested
    inner class GetElectricalSafetyCyaContent {
        @Mock
        internal lateinit var mockCyaState: CheckYourAnswersJourneyState

        @Mock
        internal lateinit var mockState: ElectricalSafetyState

        @Mock
        internal lateinit var mockElectricalDetailsTask: ElectricalSafetyDetailsTask

        private val mockHasElectricalCertStep: HasElectricalCertStep = mock()

        @BeforeEach
        fun setUp() {
            whenever(mockState.electricalSafetyDetailsTask).thenReturn(mockElectricalDetailsTask)
        }

        @Test
        fun `provide later for occupied property returns 1 row and null inset text key`() {
            whenever(mockElectricalDetailsTask.hasElectricalCertStep).thenReturn(mockHasElectricalCertStep)
            whenever(mockCyaState.getCyaJourneyId(any())).thenReturn("test-journey-id")
            whenever(mockHasElectricalCertStep.outcome).thenReturn(HasElectricalCertMode.PROVIDE_THIS_LATER)
            whenever(mockElectricalDetailsTask.isOccupied).thenReturn(true)

            val content = helper.getElectricalSafetyCyaContent(mockCyaState, mockState)

            @Suppress("UNCHECKED_CAST")
            val rows = content["electricalRows"] as List<SummaryListRowViewModel>
            val insetTextKey = content["electricalInsetTextKey"]

            assertEquals(1, rows.size)
            assertNull(insetTextKey)
        }

        @Test
        fun `no cert for occupied property returns 1 row and occupiedNoCert inset text key`() {
            whenever(mockElectricalDetailsTask.hasElectricalCertStep).thenReturn(mockHasElectricalCertStep)
            whenever(mockCyaState.getCyaJourneyId(any())).thenReturn("test-journey-id")
            whenever(mockHasElectricalCertStep.outcome).thenReturn(HasElectricalCertMode.NO_CERTIFICATE)
            whenever(mockElectricalDetailsTask.isOccupied).thenReturn(true)

            val content = helper.getElectricalSafetyCyaContent(mockCyaState, mockState)

            @Suppress("UNCHECKED_CAST")
            val rows = content["electricalRows"] as List<SummaryListRowViewModel>
            val insetTextKey = content["electricalInsetTextKey"]

            assertEquals(1, rows.size)
            assertEquals("checkElectricalSafety.occupiedNoCertInsetText", insetTextKey)
        }
    }

    @Nested
    inner class GetEpcCyaContent {
        @Mock
        internal lateinit var mockCyaState: CheckYourAnswersJourneyState

        @Mock
        internal lateinit var mockState: EpcState

        @Mock
        internal lateinit var mockEpcDetailsTask: EpcDetailsTask

        private val mockHasEpcStep: HasEpcStep = mock()

        private val mockStartEpcStep: StartEpcStep = mock()

        @BeforeEach
        fun setUp() {
            whenever(mockState.epcDetailsTask).thenReturn(mockEpcDetailsTask)
        }

        @Test
        fun `skipped occupied returns all expected keys with null epcCardTitle and non-empty nonEpcRows`() {
            whenever(mockEpcDetailsTask.startEpcStep).thenReturn(mockStartEpcStep)
            whenever(mockEpcDetailsTask.hasEpcStep).thenReturn(mockHasEpcStep)
            whenever(mockCyaState.getCyaJourneyId(any())).thenReturn("test-journey-id")
            whenever(mockHasEpcStep.outcome).thenReturn(HasEpcMode.PROVIDE_LATER)
            whenever(mockEpcDetailsTask.isOccupied).thenReturn(true)

            val content = helper.getEpcCyaContent(mockCyaState, mockState)

            assertEquals(
                setOf(
                    "epcCardTitle",
                    "epcCardActions",
                    "epcCardRows",
                    "epcExpiredTextKey",
                    "tenancyCheckRows",
                    "lowRatingTextKey",
                    "exemptionReasonRows",
                    "nonEpcRows",
                    "epcInsetTextKey",
                ),
                content.keys,
            )
            assertNull(content["epcCardTitle"])
            assertNull(content["epcCardActions"])
            assertNull(content["epcCardRows"])

            @Suppress("UNCHECKED_CAST")
            val nonEpcRows = content["nonEpcRows"] as List<SummaryListRowViewModel>
            assertTrue(nonEpcRows.isNotEmpty())
        }

        @Test
        fun `skipped occupied returns provideThisLaterOccupied value`() {
            whenever(mockEpcDetailsTask.startEpcStep).thenReturn(mockStartEpcStep)
            whenever(mockEpcDetailsTask.hasEpcStep).thenReturn(mockHasEpcStep)
            whenever(mockCyaState.getCyaJourneyId(any())).thenReturn("test-journey-id")
            whenever(mockHasEpcStep.outcome).thenReturn(HasEpcMode.PROVIDE_LATER)
            whenever(mockEpcDetailsTask.isOccupied).thenReturn(true)

            val content = helper.getEpcCyaContent(mockCyaState, mockState)

            @Suppress("UNCHECKED_CAST")
            val nonEpcRows = content["nonEpcRows"] as List<SummaryListRowViewModel>
            assertEquals(1, nonEpcRows.size)
            assertEquals(
                "propertyCompliance.epcTask.checkEpcAnswers.hasEpc.provideThisLaterOccupied",
                nonEpcRows.first().fieldValue,
            )
        }
    }
}
