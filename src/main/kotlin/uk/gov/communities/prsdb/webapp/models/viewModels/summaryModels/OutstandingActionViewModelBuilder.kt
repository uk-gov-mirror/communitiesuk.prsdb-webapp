package uk.gov.communities.prsdb.webapp.models.viewModels.summaryModels

import uk.gov.communities.prsdb.webapp.constants.GET_NEW_EPC_URL
import uk.gov.communities.prsdb.webapp.constants.enums.ComplianceCertStatus
import uk.gov.communities.prsdb.webapp.controllers.PropertyDetailsController
import uk.gov.communities.prsdb.webapp.exceptions.PrsdbWebException
import uk.gov.communities.prsdb.webapp.models.dataModels.ComplianceStatusDataModel
import uk.gov.communities.prsdb.webapp.models.dataModels.OutstandingActionsDataModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

class OutstandingActionViewModelBuilder {
    companion object {
        val DATE_FORMATTER = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.UK)

        fun fromDataModel(dataModel: OutstandingActionsDataModel): SummaryCardViewModel =
            SummaryCardViewModel(
                title = dataModel.singleLineAddress,
                summaryList = getSummaryList(dataModel),
                actions = getActions(dataModel),
                insetViewModel = dataModel.complianceStatus?.let { getInsetViewModel(it) },
            )

        private fun getSummaryList(dataModel: OutstandingActionsDataModel): List<SummaryListRowViewModel> =
            mutableListOf<SummaryListRowViewModel>()
                .apply {
                    if (dataModel.shouldShowLicensingAction) {
                        addProvideLaterRow("outstandingActions.summaryRow.licensing", dataModel.provideLaterDeadline)
                    }
                    if (dataModel.shouldShowTenancyAction) {
                        addProvideLaterRow("outstandingActions.summaryRow.tenancy", dataModel.provideLaterDeadline)
                    }
                    dataModel.complianceStatus?.let { addCertRows(it) }
                }.toList()

        private fun MutableList<SummaryListRowViewModel>.addProvideLaterRow(
            label: String,
            provideLaterDeadline: LocalDate?,
        ) {
            add(
                SummaryListRowViewModel(
                    fieldHeading = label,
                    fieldValue =
                        if (provideLaterDeadline == null) {
                            "outstandingActions.status.provideLaterNoDeadline"
                        } else {
                            "outstandingActions.status.provideLater"
                        },
                    optionalFieldValueParam = provideLaterDeadline?.format(DATE_FORMATTER),
                ),
            )
        }

        private fun MutableList<SummaryListRowViewModel>.addCertRows(complianceStatus: ComplianceStatusDataModel) {
            if (complianceStatus.shouldShowGasSafetyAction()) {
                addCertRow(
                    "outstandingActions.summaryRow.gasSafety",
                    complianceStatus.gasSafetyStatus,
                    "gasSafety",
                    complianceStatus.provideLaterDeadline,
                    complianceStatus.gasSafetyExpiryDate,
                )
            }
            if (complianceStatus.shouldShowElectricalSafetyAction()) {
                addCertRow(
                    "outstandingActions.summaryRow.electricalSafety",
                    complianceStatus.electricalSafetyStatus,
                    "electricalSafety",
                    complianceStatus.provideLaterDeadline,
                    complianceStatus.electricalSafetyExpiryDate,
                )
            }
            if (complianceStatus.shouldShowEpcAction()) {
                addCertRow(
                    "outstandingActions.summaryRow.energyPerformance",
                    complianceStatus.epcStatus,
                    "epc",
                    complianceStatus.provideLaterDeadline,
                    complianceStatus.epcExpiryDate,
                )
            }
        }

        private fun MutableList<SummaryListRowViewModel>.addCertRow(
            label: String,
            status: ComplianceCertStatus,
            certTypeKey: String,
            provideLaterDeadline: LocalDate?,
            expiryDate: LocalDate?,
        ) {
            add(
                SummaryListRowViewModel(
                    fieldHeading = label,
                    fieldValue = getCertStatusValue(status, certTypeKey, provideLaterDeadline),
                    optionalFieldValueParam = getCertStatusValueParam(status, provideLaterDeadline, expiryDate),
                ),
            )
        }

        private fun getCertStatusValue(
            status: ComplianceCertStatus,
            certTypeKey: String,
            provideLaterDeadline: LocalDate?,
        ): String =
            when (status) {
                ComplianceCertStatus.HAS_FAULTS -> {
                    "outstandingActions.status.hasFaults.$certTypeKey"
                }

                ComplianceCertStatus.PROVIDE_LATER -> {
                    if (provideLaterDeadline == null) {
                        "outstandingActions.status.provideLaterNoDeadline"
                    } else {
                        "outstandingActions.status.provideLater"
                    }
                }

                ComplianceCertStatus.EXPIRED -> {
                    "outstandingActions.status.expired"
                }

                else -> {
                    throw IllegalStateException("${status.name} should not be shown on the outstanding actions page")
                }
            }

        private fun getCertStatusValueParam(
            status: ComplianceCertStatus,
            provideLaterDeadline: LocalDate?,
            expiryDate: LocalDate?,
        ): String? =
            when (status) {
                ComplianceCertStatus.PROVIDE_LATER -> {
                    provideLaterDeadline?.format(DATE_FORMATTER)
                }

                ComplianceCertStatus.EXPIRED -> {
                    expiryDate?.format(DATE_FORMATTER)
                }

                else -> {
                    null
                }
            }

        private fun getInsetViewModel(dataModel: ComplianceStatusDataModel): ComplianceActionInsetViewModel? =
            if (dataModel.epcStatus == ComplianceCertStatus.EXPIRED && dataModel.isOccupied &&
                dataModel.tenancyStartedBeforeEpcExpiry
            ) {
                if (dataModel.epcExpiryDate == null) throw PrsdbWebException("epcExpiryDate was null for an expired certificate")
                ComplianceActionInsetViewModel(
                    expiryDate = dataModel.epcExpiryDate.format(DATE_FORMATTER),
                    linkUrl = GET_NEW_EPC_URL,
                )
            } else {
                null
            }

        private fun getActions(dataModel: OutstandingActionsDataModel): List<SummaryCardActionViewModel> =
            listOf(
                SummaryCardActionViewModel(
                    "outstandingActions.action.goToProperty",
                    if (dataModel.hasPropertyDetailsAction) {
                        PropertyDetailsController.getPropertyDetailsPath(dataModel.propertyOwnershipId)
                    } else {
                        PropertyDetailsController.getPropertyCompliancePath(dataModel.propertyOwnershipId)
                    },
                ),
            )
    }
}
