package uk.gov.communities.prsdb.webapp.journeys.propertyRegistration

import kotlinx.datetime.LocalDate
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import uk.gov.communities.prsdb.webapp.constants.enums.FileUploadStatus
import uk.gov.communities.prsdb.webapp.database.entity.FileUpload
import uk.gov.communities.prsdb.webapp.journeys.Destination
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.states.CertificateUpload
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.states.GasCertOutcome
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.states.GasSafetyDetailState
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.states.GasSupplyOutcome
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps.BeforePdjb1022HasGasCertStep
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps.BeforePdjb1022HasGasSupplyStep
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps.CheckGasCertUploadsStep
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps.GasCertIssueDateStep
import uk.gov.communities.prsdb.webapp.models.viewModels.summaryModels.SummaryListRowViewModel
import uk.gov.communities.prsdb.webapp.models.viewModels.summaryModels.UploadedFileUrl
import uk.gov.communities.prsdb.webapp.services.UploadService

@ExtendWith(MockitoExtension::class)
class GasSafetyRegistrationCyaSummaryRowsFactoryTests {
    @Mock
    lateinit var mockState: GasSafetyDetailState

    private val mockHasGasSupplyStep: BeforePdjb1022HasGasSupplyStep = mock()
    private val mockHasGasCertStep: BeforePdjb1022HasGasCertStep = mock()
    private val mockGasCertIssueDateStep: GasCertIssueDateStep = mock()
    private val mockCheckGasCertUploadsStep: CheckGasCertUploadsStep = mock()
    private val mockUploadService: UploadService = mock()

    private fun fileUploadWithStatus(status: FileUploadStatus): FileUpload =
        mock<FileUpload>().also { whenever(it.status).thenReturn(status) }

    private fun stubUpload(
        fileUploadId: Long,
        status: FileUploadStatus,
        downloadUrl: String? = null,
    ) {
        val fileUpload = fileUploadWithStatus(status)
        whenever(mockUploadService.getFileUploadById(fileUploadId)).thenReturn(fileUpload)
        whenever(mockUploadService.getDownloadUrlOrNull(eq(fileUpload), any())).thenReturn(downloadUrl)
    }

    private fun setupCommonStateMocks() {
        whenever(mockState.gasSupplyOutcomeStep).thenReturn(mockHasGasSupplyStep)
        whenever(mockState.gasCertOutcomeStep).thenReturn(mockHasGasCertStep)
        whenever(mockHasGasSupplyStep.currentJourneyId).thenReturn("test-journey-id")
        whenever(mockHasGasCertStep.currentJourneyId).thenReturn("test-journey-id")
    }

    @Nested
    inner class FlagIndependentTests {
        @Nested
        inner class NoGasSupply {
            @Test
            fun `factory returns a single no-gas-supply row`() {
                whenever(mockState.gasSupplyOutcomeStep).thenReturn(mockHasGasSupplyStep)
                whenever(mockHasGasSupplyStep.currentJourneyId).thenReturn("test-journey-id")
                whenever(mockState.gasSupplyOutcome).thenReturn(GasSupplyOutcome.NO_SUPPLY)

                val factory = GasSafetyRegistrationCyaSummaryRowsFactory(mockState, mockUploadService)

                val gasSupplyRows = factory.createGasSupplyRows()
                assertEquals(1, gasSupplyRows.size)
                assertEquals(false, gasSupplyRows[0].fieldValue)

                val certRows = factory.createCertRows()
                assertEquals(emptyList<SummaryListRowViewModel>(), certRows)

                assertEquals("checkGasSafety.noGasSupplyInsetText", factory.getInsetTextKey())
            }
        }

        @Nested
        inner class NoCertificate {
            @Test
            fun `factory returns correct content when no certificate provided and occupied`() {
                setupCommonStateMocks()
                whenever(mockState.gasSupplyOutcome).thenReturn(GasSupplyOutcome.HAS_SUPPLY)
                whenever(mockState.gasCertOutcome).thenReturn(GasCertOutcome.NO_CERTIFICATE)
                whenever(mockState.isOccupied).thenReturn(true)

                val factory = GasSafetyRegistrationCyaSummaryRowsFactory(mockState, mockUploadService)

                val gasSupplyRows = factory.createGasSupplyRows()
                assertEquals(2, gasSupplyRows.size)
                assertEquals(true, gasSupplyRows[0].fieldValue)
                assertEquals(false, gasSupplyRows[1].fieldValue)

                val certRows = factory.createCertRows()
                assertEquals(emptyList<SummaryListRowViewModel>(), certRows)

                assertEquals("checkGasSafety.occupiedNoCertInsetText", factory.getInsetTextKey())
            }

            @Test
            fun `factory returns correct content when no certificate provided and unoccupied`() {
                setupCommonStateMocks()
                whenever(mockState.gasSupplyOutcome).thenReturn(GasSupplyOutcome.HAS_SUPPLY)
                whenever(mockState.gasCertOutcome).thenReturn(GasCertOutcome.NO_CERTIFICATE)
                whenever(mockState.isOccupied).thenReturn(false)

                val factory = GasSafetyRegistrationCyaSummaryRowsFactory(mockState, mockUploadService)

                val gasSupplyRows = factory.createGasSupplyRows()
                assertEquals(2, gasSupplyRows.size)
                assertEquals(true, gasSupplyRows[0].fieldValue)
                assertEquals("checkGasSafety.provideThisLater.unoccupied", gasSupplyRows[1].fieldValue)

                val certRows = factory.createCertRows()
                assertEquals(emptyList<SummaryListRowViewModel>(), certRows)

                assertNull(factory.getInsetTextKey())
            }

            @Test
            fun `factory returns correct content when certificate expired and occupied`() {
                setupCommonStateMocks()
                whenever(mockState.gasSupplyOutcome).thenReturn(GasSupplyOutcome.HAS_SUPPLY)
                whenever(mockState.gasCertOutcome).thenReturn(GasCertOutcome.HAS_CERTIFICATE)
                whenever(mockState.getGasSafetyCertificateIsOutdated()).thenReturn(true)
                whenever(mockState.isOccupied).thenReturn(true)

                val factory = GasSafetyRegistrationCyaSummaryRowsFactory(mockState, mockUploadService)

                val gasSupplyRows = factory.createGasSupplyRows()
                assertEquals(2, gasSupplyRows.size)
                assertEquals(true, gasSupplyRows[0].fieldValue)
                assertEquals(false, gasSupplyRows[1].fieldValue)

                val certRows = factory.createCertRows()
                assertEquals(emptyList<SummaryListRowViewModel>(), certRows)

                assertEquals("checkGasSafety.occupiedNoCertInsetText", factory.getInsetTextKey())
            }

            @Test
            fun `factory returns correct content when certificate expired and unoccupied`() {
                setupCommonStateMocks()
                whenever(mockState.gasSupplyOutcome).thenReturn(GasSupplyOutcome.HAS_SUPPLY)
                whenever(mockState.gasCertOutcome).thenReturn(GasCertOutcome.HAS_CERTIFICATE)
                whenever(mockState.getGasSafetyCertificateIsOutdated()).thenReturn(true)
                whenever(mockState.isOccupied).thenReturn(false)

                val factory = GasSafetyRegistrationCyaSummaryRowsFactory(mockState, mockUploadService)

                val gasSupplyRows = factory.createGasSupplyRows()
                assertEquals(2, gasSupplyRows.size)
                assertEquals(true, gasSupplyRows[0].fieldValue)
                assertEquals("checkGasSafety.provideThisLater.unoccupied", gasSupplyRows[1].fieldValue)

                val certRows = factory.createCertRows()
                assertEquals(emptyList<SummaryListRowViewModel>(), certRows)

                assertNull(factory.getInsetTextKey())
            }
        }

        @Nested
        inner class ValidCertificate {
            @Test
            fun `factory wires up gas download messageKey and sorts uploads by map index`() {
                setupCommonStateMocks()
                whenever(mockState.gasSupplyOutcome).thenReturn(GasSupplyOutcome.HAS_SUPPLY)
                whenever(mockState.gasCertOutcome).thenReturn(GasCertOutcome.HAS_CERTIFICATE)
                whenever(mockState.getGasSafetyCertificateIsOutdated()).thenReturn(false)

                val issueDate = LocalDate(2024, 6, 15)
                whenever(mockState.getGasSafetyCertificateIssueDateIfReachable()).thenReturn(issueDate)

                whenever(mockState.gasCertIssueDateStep).thenReturn(mockGasCertIssueDateStep)
                whenever(mockState.checkGasCertUploadsStep).thenReturn(mockCheckGasCertUploadsStep)
                whenever(mockGasCertIssueDateStep.currentJourneyId).thenReturn("test-journey-id")
                whenever(mockCheckGasCertUploadsStep.currentJourneyId).thenReturn("test-journey-id")
                whenever(mockState.gasUploadMap).thenReturn(
                    mapOf(
                        2 to CertificateUpload(2L, "second.pdf"),
                        1 to CertificateUpload(1L, "first.pdf"),
                    ),
                )
                stubUpload(1L, FileUploadStatus.SCANNED, downloadUrl = "/download/first.pdf")
                stubUpload(2L, FileUploadStatus.SCANNED, downloadUrl = "/download/second.pdf")

                val factory = GasSafetyRegistrationCyaSummaryRowsFactory(mockState, mockUploadService)

                val gasSupplyRows = factory.createGasSupplyRows()
                assertEquals(1, gasSupplyRows.size)
                assertEquals(true, gasSupplyRows[0].fieldValue)

                val certRows = factory.createCertRows()
                assertEquals(3, certRows.size)
                assertEquals(true, certRows[0].fieldValue)
                assertEquals(issueDate, certRows[1].fieldValue)
                assertEquals(
                    listOf(
                        UploadedFileUrl(
                            messageKey = "propertyDetails.complianceInformation.gasSafety.downloadCertificate",
                            displayName = "first.pdf",
                            url = "/download/first.pdf",
                        ),
                        UploadedFileUrl(
                            messageKey = "propertyDetails.complianceInformation.gasSafety.downloadCertificate",
                            displayName = "second.pdf",
                            url = "/download/second.pdf",
                        ),
                    ),
                    certRows[2].fieldValue,
                )

                assertNull(factory.getInsetTextKey())
            }
        }
    }

    @Test
    fun `factory shows a single deferred gas supply row when occupied`() {
        whenever(mockState.gasSupplyOutcomeStep).thenReturn(mockHasGasSupplyStep)
        whenever(mockHasGasSupplyStep.currentJourneyId).thenReturn("test-journey-id")
        whenever(mockState.gasSupplyOutcome).thenReturn(GasSupplyOutcome.PROVIDE_LATER)
        whenever(mockState.isOccupied).thenReturn(true)

        val destinationSteps = mutableListOf<Any>()
        val factory =
            GasSafetyRegistrationCyaSummaryRowsFactory(mockState, mockUploadService, destinationProvider = {
                destinationSteps.add(it)
                Destination(it)
            })

        val gasSupplyRows = factory.createGasSupplyRows()
        assertEquals(1, gasSupplyRows.size)
        assertEquals("checkGasSafety.provideThisLater.occupied", gasSupplyRows[0].fieldValue)
        assertEquals(mockHasGasSupplyStep, destinationSteps[0])

        val certRows = factory.createCertRows()
        assertEquals(emptyList<SummaryListRowViewModel>(), certRows)

        assertNull(factory.getInsetTextKey())
    }

    @Test
    fun `factory shows a single deferred gas supply row when unoccupied`() {
        whenever(mockState.gasSupplyOutcomeStep).thenReturn(mockHasGasSupplyStep)
        whenever(mockHasGasSupplyStep.currentJourneyId).thenReturn("test-journey-id")
        whenever(mockState.gasSupplyOutcome).thenReturn(GasSupplyOutcome.PROVIDE_LATER)
        whenever(mockState.isOccupied).thenReturn(false)

        val factory = GasSafetyRegistrationCyaSummaryRowsFactory(mockState, mockUploadService)

        val gasSupplyRows = factory.createGasSupplyRows()
        assertEquals(1, gasSupplyRows.size)
        assertEquals("checkGasSafety.provideThisLater.unoccupied", gasSupplyRows[0].fieldValue)

        val certRows = factory.createCertRows()
        assertEquals(emptyList<SummaryListRowViewModel>(), certRows)

        assertNull(factory.getInsetTextKey())
    }

    // TODO PDJB-1617: delete this class when the DELEGATE_TO_LETTING_AGENT feature flag is removed. Flag off
    @Nested
    inner class WhenDelegateToLettingAgentDisabled {
        @Test
        fun `factory shows a gas supply row plus a deferred gas cert row when occupied`() {
            setupCommonStateMocks()
            whenever(mockState.gasSupplyOutcome).thenReturn(GasSupplyOutcome.HAS_SUPPLY)
            whenever(mockState.gasCertOutcome).thenReturn(GasCertOutcome.PROVIDE_LATER)
            whenever(mockState.isOccupied).thenReturn(true)

            val destinationSteps = mutableListOf<Any>()
            val factory =
                GasSafetyRegistrationCyaSummaryRowsFactory(mockState, mockUploadService, destinationProvider = {
                    destinationSteps.add(it)
                    Destination(it)
                })

            val gasSupplyRows = factory.createGasSupplyRows()
            assertEquals(2, gasSupplyRows.size)
            assertEquals(true, gasSupplyRows[0].fieldValue)
            assertEquals("checkGasSafety.provideThisLater.occupied", gasSupplyRows[1].fieldValue)
            assertEquals(mockHasGasCertStep, destinationSteps[1])

            val certRows = factory.createCertRows()
            assertEquals(emptyList<SummaryListRowViewModel>(), certRows)

            assertNull(factory.getInsetTextKey())
        }

        @Test
        fun `factory shows a gas supply row plus a deferred gas cert row when unoccupied`() {
            setupCommonStateMocks()
            whenever(mockState.gasSupplyOutcome).thenReturn(GasSupplyOutcome.HAS_SUPPLY)
            whenever(mockState.gasCertOutcome).thenReturn(GasCertOutcome.PROVIDE_LATER)
            whenever(mockState.isOccupied).thenReturn(false)

            val factory = GasSafetyRegistrationCyaSummaryRowsFactory(mockState, mockUploadService)

            val gasSupplyRows = factory.createGasSupplyRows()
            assertEquals(2, gasSupplyRows.size)
            assertEquals(true, gasSupplyRows[0].fieldValue)
            assertEquals("checkGasSafety.provideThisLater.unoccupied", gasSupplyRows[1].fieldValue)

            val certRows = factory.createCertRows()
            assertEquals(emptyList<SummaryListRowViewModel>(), certRows)

            assertNull(factory.getInsetTextKey())
        }
    }
}
