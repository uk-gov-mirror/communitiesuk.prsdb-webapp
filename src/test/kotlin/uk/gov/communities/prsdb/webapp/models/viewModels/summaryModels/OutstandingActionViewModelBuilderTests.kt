package uk.gov.communities.prsdb.webapp.models.viewModels.summaryModels
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.communities.prsdb.webapp.constants.enums.ComplianceCertStatus
import uk.gov.communities.prsdb.webapp.controllers.PropertyDetailsController
import uk.gov.communities.prsdb.webapp.models.dataModels.ComplianceStatusDataModel
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class OutstandingActionViewModelBuilderTests {
    @Test
    fun `fromDataModel returns a SummaryCardViewModel with the correct title and summary list for an occupied property`() {
        val epcExpiryDate = LocalDate.of(2025, 3, 1)
        val dataModel =
            ComplianceStatusDataModel(
                propertyOwnershipId = 1L,
                singleLineAddress = "123 Test Street",
                registrationNumber = "P-XXXX-XXXX",
                gasSafetyStatus = ComplianceCertStatus.ADDED,
                electricalSafetyStatus = ComplianceCertStatus.HAS_FAULTS,
                epcStatus = ComplianceCertStatus.EXPIRED,
                epcExpiryDate = epcExpiryDate,
                isComplete = true,
                isOccupied = true,
            )

        val viewModel = OutstandingActionViewModelBuilder.fromDataModel(dataModel)

        assertEquals(dataModel.singleLineAddress, viewModel.title)

        val expectedSummaryList =
            listOf(
                SummaryListRowViewModel(
                    fieldHeading = "outstandingActions.summaryRow.electricalSafety",
                    fieldValue = "outstandingActions.status.hasFaults.electricalSafety",
                ),
                SummaryListRowViewModel(
                    fieldHeading = "outstandingActions.summaryRow.energyPerformance",
                    fieldValue = "outstandingActions.status.expired",
                    optionalFieldValueParam = epcExpiryDate.format(OutstandingActionViewModelBuilder.DATE_FORMATTER),
                ),
            )
        assertEquals(expectedSummaryList, viewModel.summaryList)
    }

    @Test
    fun `fromDataModel only shows expired cert rows for vacant properties`() {
        val dataModel =
            ComplianceStatusDataModel(
                propertyOwnershipId = 1L,
                singleLineAddress = "123 Test Street",
                registrationNumber = "P-XXXX-XXXX",
                gasSafetyStatus = ComplianceCertStatus.EXPIRED,
                electricalSafetyStatus = ComplianceCertStatus.HAS_FAULTS,
                epcStatus = ComplianceCertStatus.ADDED,
                isComplete = true,
                isOccupied = false,
            )

        val viewModel = OutstandingActionViewModelBuilder.fromDataModel(dataModel)

        assertEquals(1, viewModel.summaryList.size)
        assertEquals("outstandingActions.summaryRow.gasSafety", viewModel.summaryList[0].fieldHeading)
    }

    @Test
    fun `fromDataModel returns a SummaryCardViewModel with goToProperty action`() {
        val dataModel =
            ComplianceStatusDataModel(
                propertyOwnershipId = 1L,
                singleLineAddress = "123 Test Street",
                registrationNumber = "P-XXXX-XXXX",
                gasSafetyStatus = ComplianceCertStatus.EXPIRED,
                electricalSafetyStatus = ComplianceCertStatus.ADDED,
                epcStatus = ComplianceCertStatus.ADDED,
                isComplete = true,
                isOccupied = true,
            )

        val viewModel = OutstandingActionViewModelBuilder.fromDataModel(dataModel)

        assertEquals(1, viewModel.actions?.size)
        assertEquals("outstandingActions.action.goToProperty", viewModel.actions?.first()?.text)
        assertEquals(
            PropertyDetailsController.getPropertyCompliancePath(dataModel.propertyOwnershipId),
            viewModel.actions?.first()?.url,
        )
    }

    @Nested
    inner class GasSafetyCertRowTests {
        private val provideLaterDeadline = LocalDate.of(2025, 6, 15)
        private val gasSafetyExpiryDate = LocalDate.of(2025, 3, 1)

        private fun buildDataModel(
            gasSafetyStatus: ComplianceCertStatus,
            isOccupied: Boolean,
            provideLaterDeadline: LocalDate? = null,
            gasSafetyExpiryDate: LocalDate? = null,
        ) = ComplianceStatusDataModel(
            propertyOwnershipId = 1L,
            singleLineAddress = "123 Test Street",
            registrationNumber = "P-XXXX-XXXX",
            gasSafetyStatus = gasSafetyStatus,
            electricalSafetyStatus = ComplianceCertStatus.ADDED,
            epcStatus = ComplianceCertStatus.ADDED,
            isComplete = true,
            isOccupied = isOccupied,
            provideLaterDeadline = provideLaterDeadline,
            gasSafetyExpiryDate = gasSafetyExpiryDate,
        )

        private fun getGasSafetyRow(viewModel: SummaryCardViewModel) =
            viewModel.summaryList.find { it.fieldHeading == "outstandingActions.summaryRow.gasSafety" }

        @Test
        fun `occupied property with provide later status shows gas row with provide later message`() {
            val viewModel =
                OutstandingActionViewModelBuilder.fromDataModel(
                    buildDataModel(
                        gasSafetyStatus = ComplianceCertStatus.PROVIDE_LATER,
                        isOccupied = true,
                        provideLaterDeadline = provideLaterDeadline,
                    ),
                )

            val gasSafetyRow = getGasSafetyRow(viewModel)
            assertNotNull(gasSafetyRow)
            assertEquals("outstandingActions.status.provideLater", gasSafetyRow.fieldValue)
            assertEquals(
                provideLaterDeadline.format(OutstandingActionViewModelBuilder.DATE_FORMATTER),
                gasSafetyRow.optionalFieldValueParam,
            )
        }

        @Test
        fun `occupied property with provide later status but no deadline shows gas row with undated provide later message`() {
            val viewModel =
                OutstandingActionViewModelBuilder.fromDataModel(
                    buildDataModel(
                        gasSafetyStatus = ComplianceCertStatus.PROVIDE_LATER,
                        isOccupied = true,
                        provideLaterDeadline = null,
                    ),
                )

            val gasSafetyRow = getGasSafetyRow(viewModel)
            assertNotNull(gasSafetyRow)
            assertEquals("outstandingActions.status.provideLaterNoDeadline", gasSafetyRow.fieldValue)
            assertNull(gasSafetyRow.optionalFieldValueParam)
        }

        @Test
        fun `unoccupied property with provide later status does not show gas row`() {
            val viewModel =
                OutstandingActionViewModelBuilder.fromDataModel(
                    buildDataModel(
                        gasSafetyStatus = ComplianceCertStatus.PROVIDE_LATER,
                        isOccupied = false,
                        provideLaterDeadline = provideLaterDeadline,
                    ),
                )

            assertNull(getGasSafetyRow(viewModel))
        }

        @Test
        fun `occupied property with expired cert shows gas row with expired message and date`() {
            val viewModel =
                OutstandingActionViewModelBuilder.fromDataModel(
                    buildDataModel(
                        gasSafetyStatus = ComplianceCertStatus.EXPIRED,
                        isOccupied = true,
                        gasSafetyExpiryDate = gasSafetyExpiryDate,
                    ),
                )

            val gasSafetyRow = getGasSafetyRow(viewModel)
            assertNotNull(gasSafetyRow)
            assertEquals("outstandingActions.status.expired", gasSafetyRow.fieldValue)
            assertEquals(
                gasSafetyExpiryDate.format(OutstandingActionViewModelBuilder.DATE_FORMATTER),
                gasSafetyRow.optionalFieldValueParam,
            )
        }

        @Test
        fun `unoccupied property with expired cert shows gas row with expired message and date`() {
            val viewModel =
                OutstandingActionViewModelBuilder.fromDataModel(
                    buildDataModel(
                        gasSafetyStatus = ComplianceCertStatus.EXPIRED,
                        isOccupied = false,
                        gasSafetyExpiryDate = gasSafetyExpiryDate,
                    ),
                )

            val gasSafetyRow = getGasSafetyRow(viewModel)
            assertNotNull(gasSafetyRow)
            assertEquals("outstandingActions.status.expired", gasSafetyRow.fieldValue)
            assertEquals(
                gasSafetyExpiryDate.format(OutstandingActionViewModelBuilder.DATE_FORMATTER),
                gasSafetyRow.optionalFieldValueParam,
            )
        }

        @Test
        fun `occupied property with no certificate shows gas row with not added message`() {
            val viewModel =
                OutstandingActionViewModelBuilder.fromDataModel(
                    buildDataModel(
                        gasSafetyStatus = ComplianceCertStatus.HAS_FAULTS,
                        isOccupied = true,
                    ),
                )

            val gasSafetyRow = getGasSafetyRow(viewModel)
            assertNotNull(gasSafetyRow)
            assertEquals("outstandingActions.status.hasFaults.gasSafety", gasSafetyRow.fieldValue)
            assertNull(gasSafetyRow.optionalFieldValueParam)
        }

        @Test
        fun `unoccupied property with no certificate does not show gas row`() {
            val viewModel =
                OutstandingActionViewModelBuilder.fromDataModel(
                    buildDataModel(
                        gasSafetyStatus = ComplianceCertStatus.HAS_FAULTS,
                        isOccupied = false,
                    ),
                )

            assertNull(getGasSafetyRow(viewModel))
        }

        @Test
        fun `occupied property with valid cert does not show gas row`() {
            val viewModel =
                OutstandingActionViewModelBuilder.fromDataModel(
                    buildDataModel(
                        gasSafetyStatus = ComplianceCertStatus.ADDED,
                        isOccupied = true,
                    ),
                )

            assertNull(getGasSafetyRow(viewModel))
        }

        @Test
        fun `unoccupied property with valid cert does not show gas row`() {
            val viewModel =
                OutstandingActionViewModelBuilder.fromDataModel(
                    buildDataModel(
                        gasSafetyStatus = ComplianceCertStatus.ADDED,
                        isOccupied = false,
                    ),
                )

            assertNull(getGasSafetyRow(viewModel))
        }

        @Test
        fun `occupied property with no gas does not show gas row`() {
            val viewModel =
                OutstandingActionViewModelBuilder.fromDataModel(
                    buildDataModel(
                        gasSafetyStatus = ComplianceCertStatus.NOT_REQUIRED,
                        isOccupied = true,
                    ),
                )

            assertNull(getGasSafetyRow(viewModel))
        }

        @Test
        fun `unoccupied property with no gas does not show gas row`() {
            val viewModel =
                OutstandingActionViewModelBuilder.fromDataModel(
                    buildDataModel(
                        gasSafetyStatus = ComplianceCertStatus.NOT_REQUIRED,
                        isOccupied = false,
                    ),
                )

            assertNull(getGasSafetyRow(viewModel))
        }
    }

    @Nested
    inner class ElectricalSafetyCertRowTests {
        private val provideLaterDeadline = LocalDate.of(2025, 6, 15)
        private val eicrExpiryDate = LocalDate.of(2025, 3, 1)

        private fun buildDataModel(
            eicrStatus: ComplianceCertStatus,
            isOccupied: Boolean,
            provideLaterDeadline: LocalDate? = null,
            eicrExpiryDate: LocalDate? = null,
        ) = ComplianceStatusDataModel(
            propertyOwnershipId = 1L,
            singleLineAddress = "123 Test Street",
            registrationNumber = "P-XXXX-XXXX",
            gasSafetyStatus = ComplianceCertStatus.ADDED,
            electricalSafetyStatus = eicrStatus,
            epcStatus = ComplianceCertStatus.ADDED,
            isComplete = true,
            isOccupied = isOccupied,
            provideLaterDeadline = provideLaterDeadline,
            electricalSafetyExpiryDate = eicrExpiryDate,
        )

        private fun getElectricalSafetyRow(viewModel: SummaryCardViewModel) =
            viewModel.summaryList.find { it.fieldHeading == "outstandingActions.summaryRow.electricalSafety" }

        @Test
        fun `occupied property with provide later status shows eicr row with provide later message`() {
            val viewModel =
                OutstandingActionViewModelBuilder.fromDataModel(
                    buildDataModel(
                        eicrStatus = ComplianceCertStatus.PROVIDE_LATER,
                        isOccupied = true,
                        provideLaterDeadline = provideLaterDeadline,
                    ),
                )

            val eicrRow = getElectricalSafetyRow(viewModel)
            assertNotNull(eicrRow)
            assertEquals("outstandingActions.status.provideLater", eicrRow.fieldValue)
            assertEquals(
                provideLaterDeadline.format(OutstandingActionViewModelBuilder.DATE_FORMATTER),
                eicrRow.optionalFieldValueParam,
            )
        }

        @Test
        fun `occupied property with provide later status but no deadline shows eicr row with undated provide later message`() {
            val viewModel =
                OutstandingActionViewModelBuilder.fromDataModel(
                    buildDataModel(
                        eicrStatus = ComplianceCertStatus.PROVIDE_LATER,
                        isOccupied = true,
                        provideLaterDeadline = null,
                    ),
                )

            val eicrRow = getElectricalSafetyRow(viewModel)
            assertNotNull(eicrRow)
            assertEquals("outstandingActions.status.provideLaterNoDeadline", eicrRow.fieldValue)
            assertNull(eicrRow.optionalFieldValueParam)
        }

        @Test
        fun `unoccupied property with provide later status does not show eicr row`() {
            val viewModel =
                OutstandingActionViewModelBuilder.fromDataModel(
                    buildDataModel(
                        eicrStatus = ComplianceCertStatus.PROVIDE_LATER,
                        isOccupied = false,
                        provideLaterDeadline = provideLaterDeadline,
                    ),
                )

            assertNull(getElectricalSafetyRow(viewModel))
        }

        @Test
        fun `occupied property with expired cert shows eicr row with expired message and date`() {
            val viewModel =
                OutstandingActionViewModelBuilder.fromDataModel(
                    buildDataModel(
                        eicrStatus = ComplianceCertStatus.EXPIRED,
                        isOccupied = true,
                        eicrExpiryDate = eicrExpiryDate,
                    ),
                )

            val eicrRow = getElectricalSafetyRow(viewModel)
            assertNotNull(eicrRow)
            assertEquals("outstandingActions.status.expired", eicrRow.fieldValue)
            assertEquals(
                eicrExpiryDate.format(OutstandingActionViewModelBuilder.DATE_FORMATTER),
                eicrRow.optionalFieldValueParam,
            )
        }

        @Test
        fun `unoccupied property with expired cert shows eicr row with expired message and date`() {
            val viewModel =
                OutstandingActionViewModelBuilder.fromDataModel(
                    buildDataModel(
                        eicrStatus = ComplianceCertStatus.EXPIRED,
                        isOccupied = false,
                        eicrExpiryDate = eicrExpiryDate,
                    ),
                )

            val eicrRow = getElectricalSafetyRow(viewModel)
            assertNotNull(eicrRow)
            assertEquals("outstandingActions.status.expired", eicrRow.fieldValue)
            assertEquals(
                eicrExpiryDate.format(OutstandingActionViewModelBuilder.DATE_FORMATTER),
                eicrRow.optionalFieldValueParam,
            )
        }

        @Test
        fun `occupied property with no certificate shows eicr row with not added message`() {
            val viewModel =
                OutstandingActionViewModelBuilder.fromDataModel(
                    buildDataModel(
                        eicrStatus = ComplianceCertStatus.HAS_FAULTS,
                        isOccupied = true,
                    ),
                )

            val eicrRow = getElectricalSafetyRow(viewModel)
            assertNotNull(eicrRow)
            assertEquals("outstandingActions.status.hasFaults.electricalSafety", eicrRow.fieldValue)
            assertNull(eicrRow.optionalFieldValueParam)
        }

        @Test
        fun `unoccupied property with no certificate does not show eicr row`() {
            val viewModel =
                OutstandingActionViewModelBuilder.fromDataModel(
                    buildDataModel(
                        eicrStatus = ComplianceCertStatus.HAS_FAULTS,
                        isOccupied = false,
                    ),
                )

            assertNull(getElectricalSafetyRow(viewModel))
        }

        @Test
        fun `occupied property with valid cert does not show eicr row`() {
            val viewModel =
                OutstandingActionViewModelBuilder.fromDataModel(
                    buildDataModel(
                        eicrStatus = ComplianceCertStatus.ADDED,
                        isOccupied = true,
                    ),
                )

            assertNull(getElectricalSafetyRow(viewModel))
        }

        @Test
        fun `unoccupied property with valid cert does not show eicr row`() {
            val viewModel =
                OutstandingActionViewModelBuilder.fromDataModel(
                    buildDataModel(
                        eicrStatus = ComplianceCertStatus.ADDED,
                        isOccupied = false,
                    ),
                )

            assertNull(getElectricalSafetyRow(viewModel))
        }
    }

    @Nested
    inner class EpcCertRowTests {
        private val provideLaterDeadline = LocalDate.of(2025, 6, 15)
        private val epcExpiryDate = LocalDate.of(2025, 3, 1)

        private fun buildDataModel(
            epcStatus: ComplianceCertStatus,
            isOccupied: Boolean,
            provideLaterDeadline: LocalDate? = null,
            epcExpiryDate: LocalDate? = null,
            tenancyStartedBeforeEpcExpiry: Boolean = false,
        ) = ComplianceStatusDataModel(
            propertyOwnershipId = 1L,
            singleLineAddress = "123 Test Street",
            registrationNumber = "P-XXXX-XXXX",
            gasSafetyStatus = ComplianceCertStatus.ADDED,
            electricalSafetyStatus = ComplianceCertStatus.ADDED,
            epcStatus = epcStatus,
            isComplete = true,
            isOccupied = isOccupied,
            provideLaterDeadline = provideLaterDeadline,
            epcExpiryDate = epcExpiryDate,
            tenancyStartedBeforeEpcExpiry = tenancyStartedBeforeEpcExpiry,
        )

        private fun getEpcRow(viewModel: SummaryCardViewModel) =
            viewModel.summaryList.find { it.fieldHeading == "outstandingActions.summaryRow.energyPerformance" }

        @Test
        fun `occupied property with provide later status shows epc row with provide later message`() {
            val viewModel =
                OutstandingActionViewModelBuilder.fromDataModel(
                    buildDataModel(
                        epcStatus = ComplianceCertStatus.PROVIDE_LATER,
                        isOccupied = true,
                        provideLaterDeadline = provideLaterDeadline,
                    ),
                )

            val epcRow = getEpcRow(viewModel)
            assertNotNull(epcRow)
            assertEquals("outstandingActions.status.provideLater", epcRow.fieldValue)
            assertEquals(
                provideLaterDeadline.format(OutstandingActionViewModelBuilder.DATE_FORMATTER),
                epcRow.optionalFieldValueParam,
            )
        }

        @Test
        fun `occupied property with provide later status but no deadline shows epc row with undated provide later message`() {
            val viewModel =
                OutstandingActionViewModelBuilder.fromDataModel(
                    buildDataModel(
                        epcStatus = ComplianceCertStatus.PROVIDE_LATER,
                        isOccupied = true,
                        provideLaterDeadline = null,
                    ),
                )

            val epcRow = getEpcRow(viewModel)
            assertNotNull(epcRow)
            assertEquals("outstandingActions.status.provideLaterNoDeadline", epcRow.fieldValue)
            assertNull(epcRow.optionalFieldValueParam)
        }

        @Test
        fun `unoccupied property with provide later status does not show epc row`() {
            val viewModel =
                OutstandingActionViewModelBuilder.fromDataModel(
                    buildDataModel(
                        epcStatus = ComplianceCertStatus.PROVIDE_LATER,
                        isOccupied = false,
                        provideLaterDeadline = provideLaterDeadline,
                    ),
                )

            assertNull(getEpcRow(viewModel))
        }

        @Test
        fun `occupied property with expired epc shows epc row with expired message and date`() {
            val viewModel =
                OutstandingActionViewModelBuilder.fromDataModel(
                    buildDataModel(
                        epcStatus = ComplianceCertStatus.EXPIRED,
                        isOccupied = true,
                        epcExpiryDate = epcExpiryDate,
                    ),
                )

            val epcRow = getEpcRow(viewModel)
            assertNotNull(epcRow)
            assertEquals("outstandingActions.status.expired", epcRow.fieldValue)
            assertEquals(
                epcExpiryDate.format(OutstandingActionViewModelBuilder.DATE_FORMATTER),
                epcRow.optionalFieldValueParam,
            )
        }

        @Test
        fun `unoccupied property with expired epc shows epc row with expired message and date`() {
            val viewModel =
                OutstandingActionViewModelBuilder.fromDataModel(
                    buildDataModel(
                        epcStatus = ComplianceCertStatus.EXPIRED,
                        isOccupied = false,
                        epcExpiryDate = epcExpiryDate,
                    ),
                )

            val epcRow = getEpcRow(viewModel)
            assertNotNull(epcRow)
            assertEquals("outstandingActions.status.expired", epcRow.fieldValue)
            assertEquals(
                epcExpiryDate.format(OutstandingActionViewModelBuilder.DATE_FORMATTER),
                epcRow.optionalFieldValueParam,
            )
        }

        @Test
        fun `occupied property with no valid epc shows epc row with not added message`() {
            val viewModel =
                OutstandingActionViewModelBuilder.fromDataModel(
                    buildDataModel(
                        epcStatus = ComplianceCertStatus.HAS_FAULTS,
                        isOccupied = true,
                    ),
                )

            val epcRow = getEpcRow(viewModel)
            assertNotNull(epcRow)
            assertEquals("outstandingActions.status.hasFaults.epc", epcRow.fieldValue)
            assertNull(epcRow.optionalFieldValueParam)
        }

        @Test
        fun `unoccupied property with no valid epc does not show epc row`() {
            val viewModel =
                OutstandingActionViewModelBuilder.fromDataModel(
                    buildDataModel(
                        epcStatus = ComplianceCertStatus.HAS_FAULTS,
                        isOccupied = false,
                    ),
                )

            assertNull(getEpcRow(viewModel))
        }

        @Test
        fun `occupied property with valid epc does not show epc row`() {
            val viewModel =
                OutstandingActionViewModelBuilder.fromDataModel(
                    buildDataModel(
                        epcStatus = ComplianceCertStatus.ADDED,
                        isOccupied = true,
                    ),
                )

            assertNull(getEpcRow(viewModel))
        }

        @Test
        fun `unoccupied property with valid epc does not show epc row`() {
            val viewModel =
                OutstandingActionViewModelBuilder.fromDataModel(
                    buildDataModel(
                        epcStatus = ComplianceCertStatus.ADDED,
                        isOccupied = false,
                    ),
                )

            assertNull(getEpcRow(viewModel))
        }

        @Test
        fun `occupied property with expired epc not in date at tenancy start does not has inset view model with expiry date`() {
            val viewModel =
                OutstandingActionViewModelBuilder.fromDataModel(
                    buildDataModel(
                        epcStatus = ComplianceCertStatus.EXPIRED,
                        isOccupied = true,
                        epcExpiryDate = epcExpiryDate,
                        tenancyStartedBeforeEpcExpiry = false,
                    ),
                )

            assertNull(viewModel.insetViewModel)
        }

        @Test
        fun `occupied property with expired epc in date at tenancy start has inset view model with expiry date`() {
            val viewModel =
                OutstandingActionViewModelBuilder.fromDataModel(
                    buildDataModel(
                        epcStatus = ComplianceCertStatus.EXPIRED,
                        isOccupied = true,
                        epcExpiryDate = epcExpiryDate,
                        tenancyStartedBeforeEpcExpiry = true,
                    ),
                )

            assertNotNull(viewModel.insetViewModel)
            assertEquals(
                epcExpiryDate.format(OutstandingActionViewModelBuilder.DATE_FORMATTER),
                viewModel.insetViewModel!!.expiryDate,
            )
        }

        @Test
        fun `unoccupied property with expired epc does not have inset view model`() {
            val viewModel =
                OutstandingActionViewModelBuilder.fromDataModel(
                    buildDataModel(
                        epcStatus = ComplianceCertStatus.EXPIRED,
                        isOccupied = false,
                        epcExpiryDate = epcExpiryDate,
                    ),
                )

            assertNull(viewModel.insetViewModel)
        }

        @Test
        fun `occupied property with non-expired epc with compliance actions does not have inset view model`() {
            val viewModel =
                OutstandingActionViewModelBuilder.fromDataModel(
                    buildDataModel(
                        epcStatus = ComplianceCertStatus.HAS_FAULTS,
                        isOccupied = true,
                    ),
                )

            assertNull(viewModel.insetViewModel)
        }
    }
}
