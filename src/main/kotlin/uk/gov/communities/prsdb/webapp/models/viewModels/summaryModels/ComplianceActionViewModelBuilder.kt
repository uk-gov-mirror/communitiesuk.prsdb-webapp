package uk.gov.communities.prsdb.webapp.models.viewModels.summaryModels

import uk.gov.communities.prsdb.webapp.constants.GET_NEW_EPC_URL
import uk.gov.communities.prsdb.webapp.constants.TAG_COLOUR_GREY
import uk.gov.communities.prsdb.webapp.constants.TAG_COLOUR_PINK
import uk.gov.communities.prsdb.webapp.constants.enums.ComplianceCertStatus
import uk.gov.communities.prsdb.webapp.controllers.PropertyDetailsController
import uk.gov.communities.prsdb.webapp.exceptions.PrsdbWebException
import uk.gov.communities.prsdb.webapp.helpers.converters.MessageKeyConverter
import uk.gov.communities.prsdb.webapp.helpers.extensions.addRow
import uk.gov.communities.prsdb.webapp.models.dataModels.ComplianceStatusDataModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

// TODO PDJB-2006: Delete this class when the PROPERTY_REGISTRATION_PHASE_THREE flag is removed
class ComplianceActionViewModelBuilder {
    companion object {
        val DATE_FORMATTER = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.UK)
        val OCCUPIED_TAG_COLOUR = TAG_COLOUR_PINK
        val UNOCCUPIED_TAG_COLOUR = TAG_COLOUR_GREY

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
                    addRow(
                        "complianceActions.summaryRow.registrationNumber",
                        dataModel.registrationNumber,
                    )
                    add(
                        SummaryListRowViewModel(
                            fieldHeading = "complianceActions.summaryRow.status",
                            fieldValue =
                                if (dataModel.isOccupied) {
                                    "complianceActions.summaryRow.occupied"
                                } else {
                                    "complianceActions.summaryRow.unoccupied"
                                },
                            tagColour = if (dataModel.isOccupied) OCCUPIED_TAG_COLOUR else UNOCCUPIED_TAG_COLOUR,
                        ),
                    )
                    if (dataModel.shouldShowGasSafetyAction()) {
                        addCertRow(
                            "complianceActions.summaryRow.gasSafety",
                            dataModel.gasSafetyStatus,
                            "gasSafety",
                            dataModel.provideLaterDeadline,
                            dataModel.gasSafetyExpiryDate,
                        )
                    }
                    if (dataModel.shouldShowElectricalSafetyAction()) {
                        addCertRow(
                            "complianceActions.summaryRow.electricalSafety",
                            dataModel.electricalSafetyStatus,
                            "electricalSafety",
                            dataModel.provideLaterDeadline,
                            dataModel.electricalSafetyExpiryDate,
                        )
                    }
                    if (dataModel.shouldShowEpcAction()) {
                        addCertRow(
                            "complianceActions.summaryRow.energyPerformance",
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
                    if (provideLaterDeadline == null) "complianceActions.status.provideLaterNoDeadline" else baseKey
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
                    "complianceActions.action.goToProperty",
                    PropertyDetailsController.getPropertyCompliancePath(dataModel.propertyOwnershipId),
                ),
            )
    }
}
