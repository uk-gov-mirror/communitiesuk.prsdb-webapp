package uk.gov.communities.prsdb.webapp.models.viewModels.summaryModels

import uk.gov.communities.prsdb.webapp.constants.GET_NEW_EPC_URL
import uk.gov.communities.prsdb.webapp.constants.enums.ComplianceCertStatus
import uk.gov.communities.prsdb.webapp.controllers.PropertyDetailsController
import uk.gov.communities.prsdb.webapp.exceptions.PrsdbWebException
import uk.gov.communities.prsdb.webapp.helpers.converters.MessageKeyConverter
import uk.gov.communities.prsdb.webapp.models.dataModels.ComplianceStatusDataModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

class OutstandingActionViewModelBuilder {
    companion object {
        val DATE_FORMATTER = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.UK)

        fun fromDataModel(dataModel: ComplianceStatusDataModel): SummaryCardViewModel =
            SummaryCardViewModel(
                title = dataModel.singleLineAddress,
                summaryList = getSummaryList(dataModel),
                actions = getActions(dataModel),
                insetViewModel = getInsetViewModel(dataModel),
            )

        private fun getSummaryList(dataModel: ComplianceStatusDataModel): List<SummaryListRowViewModel> =
            mutableListOf<SummaryListRowViewModel>()
                .apply {
                    if (dataModel.shouldShowGasSafetyAction()) {
                        addCertRow(
                            "outstandingActions.summaryRow.gasSafety",
                            dataModel.gasSafetyStatus,
                            "gasSafety",
                            dataModel.provideLaterDeadline,
                            dataModel.gasSafetyExpiryDate,
                        )
                    }
                    if (dataModel.shouldShowElectricalSafetyAction()) {
                        addCertRow(
                            "outstandingActions.summaryRow.electricalSafety",
                            dataModel.electricalSafetyStatus,
                            "electricalSafety",
                            dataModel.provideLaterDeadline,
                            dataModel.electricalSafetyExpiryDate,
                        )
                    }
                    if (dataModel.shouldShowEpcAction()) {
                        addCertRow(
                            "outstandingActions.summaryRow.energyPerformance",
                            dataModel.epcStatus,
                            "epc",
                            dataModel.provideLaterDeadline,
                            dataModel.epcExpiryDate,
                        )
                    }
                }.toList()

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
        ): String {
            val baseKey = MessageKeyConverter.convert(status)
            return when (status) {
                ComplianceCertStatus.HAS_FAULTS -> {
                    "$baseKey.$certTypeKey"
                }

                ComplianceCertStatus.PROVIDE_LATER -> {
                    if (provideLaterDeadline == null) "outstandingActions.status.provideLaterNoDeadline" else baseKey
                }

                ComplianceCertStatus.EXPIRED -> {
                    baseKey
                }

                else -> {
                    baseKey
                }
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

        private fun getActions(dataModel: ComplianceStatusDataModel): List<SummaryCardActionViewModel> =
            listOf(
                SummaryCardActionViewModel(
                    "outstandingActions.action.goToProperty",
                    PropertyDetailsController.getPropertyCompliancePath(dataModel.propertyOwnershipId),
                ),
            )
    }
}
