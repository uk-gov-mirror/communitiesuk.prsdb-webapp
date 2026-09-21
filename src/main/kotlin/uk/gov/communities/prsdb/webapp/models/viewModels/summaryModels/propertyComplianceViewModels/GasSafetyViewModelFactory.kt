package uk.gov.communities.prsdb.webapp.models.viewModels.summaryModels.propertyComplianceViewModels

import org.springframework.context.MessageSource
import uk.gov.communities.prsdb.webapp.annotations.webAnnotations.PrsdbWebService
import uk.gov.communities.prsdb.webapp.config.managers.FeatureFlagManager
import uk.gov.communities.prsdb.webapp.constants.DELEGATE_TO_LETTING_AGENT
import uk.gov.communities.prsdb.webapp.constants.enums.ComplianceCertStatus
import uk.gov.communities.prsdb.webapp.constants.enums.FileUploadStatus
import uk.gov.communities.prsdb.webapp.database.entity.PropertyCompliance
import uk.gov.communities.prsdb.webapp.helpers.extensions.addRow
import uk.gov.communities.prsdb.webapp.models.dataModels.ComplianceStatusDataModel
import uk.gov.communities.prsdb.webapp.models.viewModels.summaryModels.SummaryListRowViewModel
import uk.gov.communities.prsdb.webapp.models.viewModels.summaryModels.TagValue
import uk.gov.communities.prsdb.webapp.services.UploadService

@PrsdbWebService
class GasSafetyViewModelFactory(
    private val uploadService: UploadService,
    messageSource: MessageSource,
    featureFlagManager: FeatureFlagManager,
) : ComplianceViewModelFactoryBase(messageSource, featureFlagManager) {
    override val provideLaterUnoccupiedKey = "checkGasSafety.provideThisLater.unoccupied"
    override val provideLaterNoDeadlineKey = "checkGasSafety.provideThisLater.occupiedNoDeadline"
    override val provideLaterWithDeadlineKey = "checkGasSafety.provideThisLater.occupiedWithDeadline"
    override val missingCertOccupiedValue = "commonText.no"
    override val occupiedNoCertInsetKey = "checkGasSafety.occupiedNoCertInsetText"

    override fun getStatus(propertyCompliance: PropertyCompliance): ComplianceCertStatus =
        ComplianceStatusDataModel.fromPropertyCompliance(propertyCompliance).gasSafetyStatus

    override fun getInsetTextKey(propertyCompliance: PropertyCompliance): String? {
        val status = getStatus(propertyCompliance)
        return when (status) {
            ComplianceCertStatus.NOT_REQUIRED -> "checkGasSafety.noGasSupplyInsetText"
            else -> getCouncilWillSeeInsetKey(status, propertyCompliance)
        }
    }

    fun fromEntity(propertyCompliance: PropertyCompliance): List<SummaryListRowViewModel> =
        mutableListOf<SummaryListRowViewModel>()
            .apply {
                val status = getStatus(propertyCompliance)

                when (status) {
                    ComplianceCertStatus.NOT_REQUIRED -> {
                        addRow(
                            key = "propertyDetails.complianceInformation.gasSafety.hasGasSupply",
                            value = "commonText.no",
                        )
                        return@apply
                    }

                    in ComplianceCertStatus.NEEDS_COMPLIANCE_IF_OCCUPIED_STATUSES -> {
                        if (status == ComplianceCertStatus.PROVIDE_LATER &&
                            featureFlagManager.checkFeature(DELEGATE_TO_LETTING_AGENT)
                        ) {
                            addRow(
                                key = "propertyDetails.complianceInformation.gasSafety.hasGasSupply",
                                value = getMissingCertValue(status, propertyCompliance),
                            )
                            return@apply
                        }

                        addRow(
                            key = "propertyDetails.complianceInformation.gasSafety.hasGasSupply",
                            value = "commonText.yes",
                        )
                        addRow(
                            key = "propertyDetails.complianceInformation.gasSafety.hasCert",
                            value = getMissingCertValue(status, propertyCompliance),
                        )
                        return@apply
                    }

                    else -> {}
                }

                val addedValidCertificate = status == ComplianceCertStatus.ADDED

                addRow(
                    key = "propertyDetails.complianceInformation.certificateStatus",
                    value =
                        if (addedValidCertificate) {
                            TagValue.VALID
                        } else {
                            TagValue.EXPIRED
                        },
                )
                addRow(
                    key = "propertyDetails.complianceInformation.gasSafety.hasValidCert",
                    value = "commonText.yes",
                )

                propertyCompliance.gasSafetyCertIssueDate?.let { issueDate ->
                    addRow(
                        key = "propertyDetails.complianceInformation.issueDate",
                        value = issueDate,
                    )
                }

                val (uploadsDeletedByVirusScan, visibleUploads) =
                    propertyCompliance.gasSafetyFileUploads.partition { it.status == FileUploadStatus.DELETED }

                if (visibleUploads.isNotEmpty()) {
                    addFileUploadRows(
                        visibleUploads = visibleUploads,
                        certificateKey = "propertyDetails.complianceInformation.gasSafety.yourCertificate",
                        downloadMessageKey =
                            if (addedValidCertificate) {
                                "propertyDetails.complianceInformation.gasSafety.downloadCertificate"
                            } else {
                                "propertyDetails.complianceInformation.gasSafety.downloadExpiredCertificate"
                            },
                        noUploadMessageKey = "",
                        fallbackFileName = "gas_safety_certificate",
                        uploadService = uploadService,
                    )
                } else if (uploadsDeletedByVirusScan.isNotEmpty()) {
                    addRow(
                        key = "propertyDetails.complianceInformation.gasSafety.yourCertificate",
                        value = "propertyCompliance.uploadedFile.virusScanFailed",
                    )
                }
            }.toList()
}
