package uk.gov.communities.prsdb.webapp.models.viewModels.summaryModels.propertyComplianceViewModels

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Named.named
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments.arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.context.MessageSource
import uk.gov.communities.prsdb.webapp.config.managers.FeatureFlagManager
import uk.gov.communities.prsdb.webapp.constants.DELEGATE_TO_LETTING_AGENT
import uk.gov.communities.prsdb.webapp.constants.PROPERTY_REGISTRATION_RESTRUCTURE_AND_SKIPPING
import uk.gov.communities.prsdb.webapp.constants.PROVIDE_LATER_DEADLINE_DAYS
import uk.gov.communities.prsdb.webapp.constants.enums.FileUploadStatus
import uk.gov.communities.prsdb.webapp.database.entity.FileUpload
import uk.gov.communities.prsdb.webapp.database.entity.PropertyCompliance
import uk.gov.communities.prsdb.webapp.helpers.DateTimeHelper
import uk.gov.communities.prsdb.webapp.models.viewModels.summaryModels.SummaryListRowViewModel
import uk.gov.communities.prsdb.webapp.models.viewModels.summaryModels.TagValue
import uk.gov.communities.prsdb.webapp.models.viewModels.summaryModels.UploadedFileUrl
import uk.gov.communities.prsdb.webapp.services.UploadService
import uk.gov.communities.prsdb.webapp.testHelpers.builders.PropertyComplianceBuilder
import uk.gov.communities.prsdb.webapp.testHelpers.mockObjects.MockLandlordData
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

class GasSafetyViewModelFactoryTests : ComplianceViewModelFactoryTests() {
    private val gasSafetyViewModelFactory = GasSafetyViewModelFactory(mock(), mock(), mock())

    override fun createRows(
        uploadService: UploadService,
        propertyCompliance: PropertyCompliance,
    ): List<SummaryListRowViewModel> {
        val messageSource = mock<MessageSource>()
        whenever(messageSource.getMessage(eq(PROVIDE_LATER_WITH_DEADLINE_KEY), any(), any<Locale>()))
            .thenAnswer { invocation ->
                val args = invocation.getArgument<Array<Any>>(1)
                "Provide this later (before ${args[0]})"
            }
        return GasSafetyViewModelFactory(
            uploadService,
            messageSource,
            mockFeatureFlagManager(registrationDateDeadlineEnabled = true, delegateToLettingAgentEnabled = false),
        ).fromEntity(propertyCompliance)
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class FlagIndependentTests {
        @ParameterizedTest(name = "{0}")
        @MethodSource("provideInsetTextKeys")
        fun `getInsetTextKey returns the correct key`(
            propertyCompliance: PropertyCompliance,
            expectedKey: String?,
        ) {
            val insetTextKey = gasSafetyViewModelFactory.getInsetTextKey(propertyCompliance)

            assertEquals(expectedKey, insetTextKey)
        }

        private fun provideInsetTextKeys() =
            arrayOf(
                arguments(named("with compliant gas cert", compliant), null),
                arguments(named("without gas cert and unoccupied", missingUnoccupied), null),
                arguments(
                    named("without gas cert and occupied (no cert)", missingOccupiedNoCert),
                    "checkGasSafety.occupiedNoCertInsetText",
                ),
                arguments(named("without gas cert and occupied (provide later)", missingOccupiedProvideLater), null),
                arguments(named("with no gas supply", noGasSupply), "checkGasSafety.noGasSupplyInsetText"),
                arguments(
                    named("with expired gas cert and occupied", expiredOccupied),
                    "checkGasSafety.occupiedNoCertInsetText",
                ),
                arguments(named("with expired gas cert and unoccupied", expiredBeforeUpload), null),
            )
    }

    @Test
    fun `occupied provide-later renders a single gas-supply row with the deadline value and no cert row`() {
        val messageSource = mock<MessageSource>()
        whenever(messageSource.getMessage(eq(PROVIDE_LATER_WITH_DEADLINE_KEY), any(), any<Locale>()))
            .thenAnswer { invocation ->
                val args = invocation.getArgument<Array<Any>>(1)
                "Provide this later (before ${args[0]})"
            }
        val factory =
            GasSafetyViewModelFactory(
                mock(),
                messageSource,
                mockFeatureFlagManager(registrationDateDeadlineEnabled = false, delegateToLettingAgentEnabled = true),
            )
        val rows = factory.fromEntity(missingOccupiedAfterRegistrationProvideLater)

        val expectedDeadline =
            occupiedAtRegistrationDate
                .plusDays(30)
                .plusDays(PROVIDE_LATER_DEADLINE_DAYS.toLong())
                .format(DATE_FORMATTER)
        assertEquals(
            listOf(
                SummaryListRowViewModel(
                    "propertyDetails.complianceInformation.gasSafety.hasGasSupply",
                    "Provide this later (before $expectedDeadline)",
                ),
            ),
            rows,
        )
    }

    @Test
    fun `unoccupied provide-later renders a single gas-supply row with the no-date value and no cert row`() {
        val factory =
            GasSafetyViewModelFactory(
                mock(),
                mock(),
                mockFeatureFlagManager(registrationDateDeadlineEnabled = true, delegateToLettingAgentEnabled = true),
            )
        val rows = factory.fromEntity(missingUnoccupiedProvideLater)

        assertEquals(
            listOf(
                SummaryListRowViewModel(
                    "propertyDetails.complianceInformation.gasSafety.hasGasSupply",
                    "checkGasSafety.provideThisLater.unoccupied",
                ),
            ),
            rows,
        )
    }

    @Test
    fun `occupied no-cert (HAS_FAULTS) is unaffected and still renders both rows`() {
        val factory =
            GasSafetyViewModelFactory(
                mock(),
                mock(),
                mockFeatureFlagManager(registrationDateDeadlineEnabled = true, delegateToLettingAgentEnabled = true),
            )
        val rows = factory.fromEntity(missingOccupiedNoCert)

        assertEquals(
            listOf(
                SummaryListRowViewModel(
                    "propertyDetails.complianceInformation.gasSafety.hasGasSupply",
                    "commonText.yes",
                ),
                SummaryListRowViewModel(
                    "propertyDetails.complianceInformation.gasSafety.hasCert",
                    "commonText.no",
                ),
            ),
            rows,
        )
    }

    // TODO PDJB-1617: delete this class when the DELEGATE_TO_LETTING_AGENT feature flag is removed. Flag off
    @Nested
    inner class WhenDelegateToLettingAgentDisabled {
        @Test
        fun `occupied provide-later still renders both gas-supply and cert rows`() {
            val messageSource = mock<MessageSource>()
            whenever(messageSource.getMessage(eq(PROVIDE_LATER_WITH_DEADLINE_KEY), any(), any<Locale>()))
                .thenAnswer { invocation ->
                    val args = invocation.getArgument<Array<Any>>(1)
                    "Provide this later (before ${args[0]})"
                }
            val factory =
                GasSafetyViewModelFactory(
                    mock(),
                    messageSource,
                    mockFeatureFlagManager(registrationDateDeadlineEnabled = false, delegateToLettingAgentEnabled = false),
                )
            val rows = factory.fromEntity(missingOccupiedAfterRegistrationProvideLater)

            val expectedDeadline =
                occupiedAtRegistrationDate
                    .plusDays(30)
                    .plusDays(PROVIDE_LATER_DEADLINE_DAYS.toLong())
                    .format(DATE_FORMATTER)
            assertEquals(
                listOf(
                    SummaryListRowViewModel(
                        "propertyDetails.complianceInformation.gasSafety.hasGasSupply",
                        "commonText.yes",
                    ),
                    SummaryListRowViewModel(
                        "propertyDetails.complianceInformation.gasSafety.hasCert",
                        "Provide this later (before $expectedDeadline)",
                    ),
                ),
                rows,
            )
        }

        @Test
        fun `unoccupied provide-later still renders both gas-supply and cert rows`() {
            val factory =
                GasSafetyViewModelFactory(
                    mock(),
                    mock(),
                    mockFeatureFlagManager(registrationDateDeadlineEnabled = true, delegateToLettingAgentEnabled = false),
                )
            val rows = factory.fromEntity(missingUnoccupiedProvideLater)

            assertEquals(
                listOf(
                    SummaryListRowViewModel(
                        "propertyDetails.complianceInformation.gasSafety.hasGasSupply",
                        "commonText.yes",
                    ),
                    SummaryListRowViewModel(
                        "propertyDetails.complianceInformation.gasSafety.hasCert",
                        "checkGasSafety.provideThisLater.unoccupied",
                    ),
                ),
                rows,
            )
        }
    }

    companion object {
        private val DATE_FORMATTER = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.UK)
        private const val PROVIDE_LATER_WITH_DEADLINE_KEY = "checkGasSafety.provideThisLater.occupiedWithDeadline"

        private fun mockFeatureFlagManager(
            registrationDateDeadlineEnabled: Boolean,
            delegateToLettingAgentEnabled: Boolean,
        ): FeatureFlagManager =
            mock {
                on { checkFeature(PROPERTY_REGISTRATION_RESTRUCTURE_AND_SKIPPING) } doReturn registrationDateDeadlineEnabled
                on { checkFeature(DELEGATE_TO_LETTING_AGENT) } doReturn delegateToLettingAgentEnabled
            }

        // A property "occupied when registered" has a lastOccupiedDate matching its registration (created) date.
        private val occupiedAtRegistrationDate = LocalDate.of(2025, 1, 1)
        private val occupiedAtRegistrationInstant = occupiedAtRegistrationDate.atStartOfDay(DateTimeHelper.UK_ZONE).toInstant()

        private val compliant =
            PropertyComplianceBuilder()
                .withPropertyOwnershipWithOccupancy(false)
                .withHasGasSupply(true)
                .withGasSafetyCert()
                .withElectricalSafety()
                .withElectricalCertType()
                .withEpc()
                .build()
        private val compliantViaPluralUploads =
            PropertyComplianceBuilder()
                .withPropertyOwnershipWithOccupancy(false)
                .withHasGasSupply(true)
                .withGasSafetyCert(fileUpload = null)
                .withGasSafetyFileUploads()
                .withElectricalSafety()
                .withElectricalCertType()
                .withEpc()
                .build()
        private val expiredAfterUpload =
            PropertyComplianceBuilder()
                .withPropertyOwnershipWithOccupancy(false)
                .withHasGasSupply(true)
                .withGasSafetyCert()
                .withExpiredGasSafetyCert()
                .withElectricalSafety()
                .withElectricalCertType()
                .withEpc()
                .build()
        private val expiredBeforeUpload =
            PropertyComplianceBuilder()
                .withPropertyOwnershipWithOccupancy(false)
                .withHasGasSupply(true)
                .withExpiredGasSafetyCert()
                .withElectricalSafety()
                .withElectricalCertType()
                .withEpc()
                .build()
        private val noGasSupply =
            PropertyComplianceBuilder()
                .withPropertyOwnershipWithOccupancy(false)
                .withHasGasSupply(false)
                .build()
        private val missingUnoccupied =
            PropertyComplianceBuilder()
                .withPropertyOwnershipWithOccupancy(false)
                .withHasGasSupply(true)
                .build()
        private val missingUnoccupiedProvideLater =
            PropertyComplianceBuilder()
                .withPropertyOwnershipWithOccupancy(false)
                .withHasGasSupply(true)
                .withGasSafetyCertProvideLater()
                .build()
        private val missingOccupiedProvideLater =
            PropertyComplianceBuilder()
                .withPropertyOwnership(
                    MockLandlordData.createOccupiedPropertyOwnership(
                        createdDate = occupiedAtRegistrationInstant,
                        lastOccupiedDate = occupiedAtRegistrationDate,
                    ),
                ).withHasGasSupply(true)
                .withGasSafetyCertProvideLater()
                .build()
        private val missingOccupiedAfterRegistrationProvideLater =
            PropertyComplianceBuilder()
                .withPropertyOwnership(
                    MockLandlordData.createOccupiedPropertyOwnership(
                        createdDate = occupiedAtRegistrationInstant,
                        lastOccupiedDate = occupiedAtRegistrationDate.plusDays(30),
                    ),
                ).withHasGasSupply(true)
                .withGasSafetyCertProvideLater()
                .build()
        private val missingOccupiedNoCert =
            PropertyComplianceBuilder()
                .withOccupiedPropertyOwnership()
                .withHasGasSupply(true)
                .build()
        private val compliantWithFileName =
            PropertyComplianceBuilder()
                .withPropertyOwnershipWithOccupancy(false)
                .withHasGasSupply(true)
                .withGasSafetyCert(
                    fileUpload =
                        FileUpload(
                            FileUploadStatus.SCANNED,
                            "property_1_gas.pdf",
                            "pdf",
                            "etag",
                            "versionId",
                        ).apply { fileName = "my_gas_certificate.pdf" },
                ).withElectricalSafety()
                .withElectricalCertType()
                .withEpc()
                .build()
        private val quarantinedUpload =
            PropertyComplianceBuilder()
                .withPropertyOwnershipWithOccupancy(false)
                .withHasGasSupply(true)
                .withGasSafetyCert(
                    fileUpload =
                        FileUpload(
                            FileUploadStatus.QUARANTINED,
                            "property_1_gas.pdf",
                            "pdf",
                            "etag",
                            "versionId",
                        ).apply { fileName = "pending_gas.pdf" },
                ).withElectricalSafety()
                .withElectricalCertType()
                .withEpc()
                .build()
        private val virusScanFailedUpload =
            PropertyComplianceBuilder()
                .withPropertyOwnershipWithOccupancy(false)
                .withHasGasSupply(true)
                .withGasSafetyCert(
                    fileUpload =
                        FileUpload(
                            FileUploadStatus.DELETED,
                            "property_1_gas.pdf",
                            "pdf",
                            "etag",
                            "versionId",
                        ).apply { fileName = "infected_gas.pdf" },
                ).withElectricalSafety()
                .withElectricalCertType()
                .withEpc()
                .build()
        private val expiredOccupied =
            PropertyComplianceBuilder()
                .withOccupiedPropertyOwnership()
                .withHasGasSupply(true)
                .withExpiredGasSafetyCert()
                .withElectricalSafety()
                .withElectricalCertType()
                .withEpc()
                .build()

        @JvmStatic
        private fun provideRows() =
            arrayOf(
                arguments(
                    named(
                        "with compliant gas safety certificate",
                        compliant,
                    ),
                    listOf(
                        SummaryListRowViewModel(
                            "propertyDetails.complianceInformation.certificateStatus",
                            TagValue.VALID,
                        ),
                        SummaryListRowViewModel(
                            "propertyDetails.complianceInformation.gasSafety.hasValidCert",
                            "commonText.yes",
                        ),
                        SummaryListRowViewModel(
                            "propertyDetails.complianceInformation.issueDate",
                            compliant.gasSafetyCertIssueDate,
                        ),
                        SummaryListRowViewModel(
                            "propertyDetails.complianceInformation.gasSafety.yourCertificate",
                            listOf(
                                UploadedFileUrl(
                                    messageKey = "propertyDetails.complianceInformation.gasSafety.downloadCertificate",
                                    displayName = "gas_safety_certificate.pdf",
                                    url = DOWNLOAD_URL,
                                ),
                            ),
                        ),
                    ),
                ),
                arguments(
                    named(
                        "with compliant gas safety certificate via plural uploads",
                        compliantViaPluralUploads,
                    ),
                    listOf(
                        SummaryListRowViewModel(
                            "propertyDetails.complianceInformation.certificateStatus",
                            TagValue.VALID,
                        ),
                        SummaryListRowViewModel(
                            "propertyDetails.complianceInformation.gasSafety.hasValidCert",
                            "commonText.yes",
                        ),
                        SummaryListRowViewModel(
                            "propertyDetails.complianceInformation.issueDate",
                            compliantViaPluralUploads.gasSafetyCertIssueDate,
                        ),
                        SummaryListRowViewModel(
                            "propertyDetails.complianceInformation.gasSafety.yourCertificate",
                            listOf(
                                UploadedFileUrl(
                                    messageKey = "propertyDetails.complianceInformation.gasSafety.downloadCertificate",
                                    displayName = "gas_safety_certificate.pdf",
                                    url = DOWNLOAD_URL,
                                ),
                            ),
                        ),
                    ),
                ),
                arguments(
                    named(
                        "with compliant gas safety certificate with file name",
                        compliantWithFileName,
                    ),
                    listOf(
                        SummaryListRowViewModel(
                            "propertyDetails.complianceInformation.certificateStatus",
                            TagValue.VALID,
                        ),
                        SummaryListRowViewModel(
                            "propertyDetails.complianceInformation.gasSafety.hasValidCert",
                            "commonText.yes",
                        ),
                        SummaryListRowViewModel(
                            "propertyDetails.complianceInformation.issueDate",
                            compliantWithFileName.gasSafetyCertIssueDate,
                        ),
                        SummaryListRowViewModel(
                            "propertyDetails.complianceInformation.gasSafety.yourCertificate",
                            listOf(
                                UploadedFileUrl(
                                    messageKey = "propertyDetails.complianceInformation.gasSafety.downloadCertificate",
                                    displayName = "my_gas_certificate.pdf",
                                    url = DOWNLOAD_URL,
                                ),
                            ),
                        ),
                    ),
                ),
                arguments(
                    named(
                        "with quarantined gas safety upload",
                        quarantinedUpload,
                    ),
                    listOf(
                        SummaryListRowViewModel(
                            "propertyDetails.complianceInformation.certificateStatus",
                            TagValue.VALID,
                        ),
                        SummaryListRowViewModel(
                            "propertyDetails.complianceInformation.gasSafety.hasValidCert",
                            "commonText.yes",
                        ),
                        SummaryListRowViewModel(
                            "propertyDetails.complianceInformation.issueDate",
                            quarantinedUpload.gasSafetyCertIssueDate,
                        ),
                        SummaryListRowViewModel(
                            "propertyDetails.complianceInformation.gasSafety.yourCertificate",
                            listOf(
                                UploadedFileUrl(
                                    messageKey = VIRUS_SCAN_PENDING_WITH_NAME_KEY,
                                    displayName = "pending_gas.pdf",
                                ),
                            ),
                        ),
                    ),
                ),
                arguments(
                    named(
                        "with virus scan failed gas safety certificate",
                        virusScanFailedUpload,
                    ),
                    listOf(
                        SummaryListRowViewModel(
                            "propertyDetails.complianceInformation.certificateStatus",
                            TagValue.VALID,
                        ),
                        SummaryListRowViewModel(
                            "propertyDetails.complianceInformation.gasSafety.hasValidCert",
                            "commonText.yes",
                        ),
                        SummaryListRowViewModel(
                            "propertyDetails.complianceInformation.issueDate",
                            virusScanFailedUpload.gasSafetyCertIssueDate,
                        ),
                        SummaryListRowViewModel(
                            "propertyDetails.complianceInformation.gasSafety.yourCertificate",
                            "propertyCompliance.uploadedFile.virusScanFailed",
                        ),
                    ),
                ),
                arguments(
                    named(
                        "with expired after upload gas safety certificate",
                        expiredAfterUpload,
                    ),
                    listOf(
                        SummaryListRowViewModel(
                            "propertyDetails.complianceInformation.certificateStatus",
                            TagValue.EXPIRED,
                        ),
                        SummaryListRowViewModel(
                            "propertyDetails.complianceInformation.gasSafety.hasValidCert",
                            "commonText.yes",
                        ),
                        SummaryListRowViewModel(
                            "propertyDetails.complianceInformation.issueDate",
                            expiredAfterUpload.gasSafetyCertIssueDate,
                        ),
                        SummaryListRowViewModel(
                            "propertyDetails.complianceInformation.gasSafety.yourCertificate",
                            listOf(
                                UploadedFileUrl(
                                    messageKey = "propertyDetails.complianceInformation.gasSafety.downloadExpiredCertificate",
                                    displayName = "gas_safety_certificate.pdf",
                                    url = DOWNLOAD_URL,
                                ),
                            ),
                        ),
                    ),
                ),
                arguments(
                    named(
                        "with expired before upload gas safety certificate",
                        expiredBeforeUpload,
                    ),
                    listOf(
                        SummaryListRowViewModel(
                            "propertyDetails.complianceInformation.certificateStatus",
                            TagValue.EXPIRED,
                        ),
                        SummaryListRowViewModel(
                            "propertyDetails.complianceInformation.gasSafety.hasValidCert",
                            "commonText.yes",
                        ),
                        SummaryListRowViewModel(
                            "propertyDetails.complianceInformation.issueDate",
                            expiredBeforeUpload.gasSafetyCertIssueDate,
                        ),
                    ),
                ),
                arguments(
                    named(
                        "without gas safety certificate and unoccupied",
                        missingUnoccupied,
                    ),
                    listOf(
                        SummaryListRowViewModel(
                            "propertyDetails.complianceInformation.gasSafety.hasGasSupply",
                            "commonText.yes",
                        ),
                        SummaryListRowViewModel(
                            "propertyDetails.complianceInformation.gasSafety.hasCert",
                            "checkGasSafety.provideThisLater.unoccupied",
                        ),
                    ),
                ),
                arguments(
                    named(
                        "without gas safety certificate, occupied at registration, and provide later",
                        missingOccupiedProvideLater,
                    ),
                    listOf(
                        SummaryListRowViewModel(
                            "propertyDetails.complianceInformation.gasSafety.hasGasSupply",
                            "commonText.yes",
                        ),
                        SummaryListRowViewModel(
                            "propertyDetails.complianceInformation.gasSafety.hasCert",
                            "Provide this later (before ${
                                occupiedAtRegistrationDate
                                    .plusDays(PROVIDE_LATER_DEADLINE_DAYS.toLong())
                                    .format(DATE_FORMATTER)
                            })",
                        ),
                    ),
                ),
                arguments(
                    named(
                        "without gas safety certificate, occupied after registration, and provide later",
                        missingOccupiedAfterRegistrationProvideLater,
                    ),
                    listOf(
                        SummaryListRowViewModel(
                            "propertyDetails.complianceInformation.gasSafety.hasGasSupply",
                            "commonText.yes",
                        ),
                        SummaryListRowViewModel(
                            "propertyDetails.complianceInformation.gasSafety.hasCert",
                            "checkGasSafety.provideThisLater.occupiedNoDeadline",
                        ),
                    ),
                ),
                arguments(
                    named(
                        "without gas safety certificate and occupied",
                        missingOccupiedNoCert,
                    ),
                    listOf(
                        SummaryListRowViewModel(
                            "propertyDetails.complianceInformation.gasSafety.hasGasSupply",
                            "commonText.yes",
                        ),
                        SummaryListRowViewModel(
                            "propertyDetails.complianceInformation.gasSafety.hasCert",
                            "commonText.no",
                        ),
                    ),
                ),
                arguments(
                    named(
                        "with no gas supply",
                        noGasSupply,
                    ),
                    listOf(
                        SummaryListRowViewModel(
                            "propertyDetails.complianceInformation.gasSafety.hasGasSupply",
                            "commonText.no",
                        ),
                    ),
                ),
            )
    }
}
