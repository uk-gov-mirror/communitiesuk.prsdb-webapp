package uk.gov.communities.prsdb.webapp.models.viewModels.summaryModels.propertyComplianceViewModels

import org.junit.jupiter.api.Nested
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.context.MessageSource
import uk.gov.communities.prsdb.webapp.controllers.LandlordUpdateElectricalSafetyController
import uk.gov.communities.prsdb.webapp.controllers.LandlordUpdateEpcController
import uk.gov.communities.prsdb.webapp.controllers.LandlordUpdateGasSafetyController
import uk.gov.communities.prsdb.webapp.controllers.LettingAgentUpdateElectricalSafetyController
import uk.gov.communities.prsdb.webapp.controllers.LettingAgentUpdateEpcController
import uk.gov.communities.prsdb.webapp.controllers.LettingAgentUpdateGasSafetyController
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps.HasElectricalCertStep
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps.HasGasSupplyStep
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps.StartEpcStep
import uk.gov.communities.prsdb.webapp.models.viewModels.summaryModels.PropertyDetailsViewType
import uk.gov.communities.prsdb.webapp.models.viewModels.summaryModels.SummaryCardActionViewModel
import uk.gov.communities.prsdb.webapp.testHelpers.builders.PropertyComplianceBuilder
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PropertyComplianceViewModelFactoryTests {
    private val mockMessageSource: MessageSource = mock()

    init {
        whenever(mockMessageSource.getMessage(any(), any(), any())).thenReturn("")
    }

    private val gasSafetyViewModelFactory = GasSafetyViewModelFactory(mock(), mockMessageSource)
    private val electricalSafetyViewModelFactory = ElectricalSafetyViewModelFactory(mock(), mockMessageSource)
    private val propertyComplianceViewModelFactory =
        PropertyComplianceViewModelFactory(
            gasSafetyViewModelFactory,
            electricalSafetyViewModelFactory,
            EpcViewModelFactory(mockMessageSource),
        )

    private val propertyOwnershipId = 1L

    @Test
    fun `isAllValid is true when the property is compliant`() {
        val propertyCompliance = PropertyComplianceBuilder.createWithInDateCerts()

        val result = propertyComplianceViewModelFactory.create(propertyCompliance, propertyOwnershipId = propertyOwnershipId)

        assertTrue(result.isAllValid)
    }

    @Test
    fun `isAllValid is false when certificates are missing`() {
        val propertyCompliance = PropertyComplianceBuilder.createWithMissingCerts()

        val result = propertyComplianceViewModelFactory.create(propertyCompliance, propertyOwnershipId = propertyOwnershipId)

        assertFalse(result.isAllValid)
    }

    @Nested
    inner class CardActions {
        @Test
        fun `cards have change actions for the landlord view`() {
            val propertyCompliance = PropertyComplianceBuilder.createWithInDateCerts()
            val propertyOwnershipId = propertyCompliance.propertyOwnership.id

            val result =
                propertyComplianceViewModelFactory.create(
                    propertyCompliance,
                    viewType = PropertyDetailsViewType.LANDLORD,
                    propertyOwnershipId = propertyOwnershipId,
                )

            val expectedGasSafetyActions =
                listOf(
                    SummaryCardActionViewModel(
                        "forms.links.change",
                        LandlordUpdateGasSafetyController.getUpdateGasSafetyFirstStepRoute(propertyOwnershipId),
                    ),
                )

            val expectedElectricalSafetyActions =
                listOf(
                    SummaryCardActionViewModel(
                        "forms.links.change",
                        LandlordUpdateElectricalSafetyController.getUpdateElectricalSafetyFirstStepRoute(propertyOwnershipId),
                    ),
                )

            val expectedEpcActions =
                listOf(
                    SummaryCardActionViewModel(
                        "propertyCompliance.epcTask.checkEpcAnswers.epc.viewFullEpc",
                        "${PropertyComplianceBuilder.TEST_EPC_BASE_URL}/0000-0000-0000-0000-0000",
                        opensInNewTab = true,
                    ),
                    SummaryCardActionViewModel(
                        "forms.links.change",
                        LandlordUpdateEpcController.getUpdateEpcRouteFirstStep(propertyOwnershipId),
                    ),
                )
            assertEquals(expectedGasSafetyActions, result.gasSafetySummaryCard.actions)
            assertEquals(expectedElectricalSafetyActions, result.electricalSafetySummaryCard.actions)
            assertEquals(expectedEpcActions, result.epcSummaryCard.actions)
        }

        @Test
        fun `the gas safety card has a letting agent change action when a token is supplied`() {
            val propertyCompliance = PropertyComplianceBuilder.createWithInDateCerts()
            val propertyOwnershipId = propertyCompliance.propertyOwnership.id
            val token = UUID.randomUUID()

            val result =
                propertyComplianceViewModelFactory.create(
                    propertyCompliance,
                    viewType = PropertyDetailsViewType.LETTING_AGENT,
                    propertyOwnershipId = propertyOwnershipId,
                    lettingAgentAccessToken = token,
                )

            val expectedGasSafetyActions =
                listOf(
                    SummaryCardActionViewModel(
                        "forms.links.change",
                        LettingAgentUpdateGasSafetyController.getUpdateGasSafetyRoute(token) +
                            "/${HasGasSupplyStep.ROUTE_SEGMENT}",
                    ),
                )

            assertEquals(expectedGasSafetyActions, result.gasSafetySummaryCard.actions)
        }

        @Test
        fun `the gas safety card has no change action for the letting agent view without a token`() {
            val propertyCompliance = PropertyComplianceBuilder.createWithInDateCerts()
            val propertyOwnershipId = propertyCompliance.propertyOwnership.id

            val result =
                propertyComplianceViewModelFactory.create(
                    propertyCompliance,
                    viewType = PropertyDetailsViewType.LETTING_AGENT,
                    propertyOwnershipId = propertyOwnershipId,
                )

            assertNull(result.gasSafetySummaryCard.actions)
        }

        @Test
        fun `the electrical safety card links to the letting agent update journey when a token is provided`() {
            val propertyCompliance = PropertyComplianceBuilder.createWithInDateCerts()
            val propertyOwnershipId = propertyCompliance.propertyOwnership.id
            val token = UUID.randomUUID()

            val result =
                propertyComplianceViewModelFactory.create(
                    propertyCompliance,
                    viewType = PropertyDetailsViewType.LETTING_AGENT,
                    propertyOwnershipId = propertyOwnershipId,
                    lettingAgentAccessToken = token,
                )

            val expectedElectricalSafetyActions =
                listOf(
                    SummaryCardActionViewModel(
                        "forms.links.change",
                        LettingAgentUpdateElectricalSafetyController.getUpdateElectricalSafetyRoute(token) +
                            "/${HasElectricalCertStep.ROUTE_SEGMENT}",
                    ),
                )

            assertEquals(expectedElectricalSafetyActions, result.electricalSafetySummaryCard.actions)
        }

        @Test
        fun `the electrical safety card has no change action for the letting agent view without a token`() {
            val propertyCompliance = PropertyComplianceBuilder.createWithInDateCerts()
            val propertyOwnershipId = propertyCompliance.propertyOwnership.id

            val result =
                propertyComplianceViewModelFactory.create(
                    propertyCompliance,
                    viewType = PropertyDetailsViewType.LETTING_AGENT,
                    propertyOwnershipId = propertyOwnershipId,
                )

            assertNull(result.electricalSafetySummaryCard.actions)
        }

        @Test
        fun `the EPC card includes view and change actions for the letting agent with a token`() {
            val propertyCompliance = PropertyComplianceBuilder.createWithInDateCerts()
            val token = UUID.randomUUID()

            val result =
                propertyComplianceViewModelFactory.create(
                    propertyCompliance,
                    viewType = PropertyDetailsViewType.LETTING_AGENT,
                    propertyOwnershipId = propertyCompliance.propertyOwnership.id,
                    lettingAgentAccessToken = token,
                )

            assertEquals(
                listOf(
                    SummaryCardActionViewModel(
                        "propertyCompliance.epcTask.checkEpcAnswers.epc.viewFullEpc",
                        requireNotNull(propertyCompliance.epcUrl),
                        opensInNewTab = true,
                    ),
                    SummaryCardActionViewModel(
                        "forms.links.change",
                        LettingAgentUpdateEpcController.getUpdateEpcRoute(token) + "/${StartEpcStep.ROUTE_SEGMENT}",
                    ),
                ),
                result.epcSummaryCard.actions,
            )
        }

        @Test
        fun `the EPC card retains the view action without a change action for the letting agent without a token`() {
            val propertyCompliance = PropertyComplianceBuilder.createWithInDateCerts()

            val result =
                propertyComplianceViewModelFactory.create(
                    propertyCompliance,
                    viewType = PropertyDetailsViewType.LETTING_AGENT,
                    propertyOwnershipId = propertyCompliance.propertyOwnership.id,
                )

            assertEquals(
                listOf(
                    SummaryCardActionViewModel(
                        "propertyCompliance.epcTask.checkEpcAnswers.epc.viewFullEpc",
                        requireNotNull(propertyCompliance.epcUrl),
                        opensInNewTab = true,
                    ),
                ),
                result.epcSummaryCard.actions,
            )
        }

        @Test
        fun `the EPC card has a change action for the letting agent when the certificate is missing`() {
            val propertyCompliance = PropertyComplianceBuilder.createWithMissingCerts()
            val token = UUID.randomUUID()

            val result =
                propertyComplianceViewModelFactory.create(
                    propertyCompliance,
                    viewType = PropertyDetailsViewType.LETTING_AGENT,
                    propertyOwnershipId = propertyCompliance.propertyOwnership.id,
                    lettingAgentAccessToken = token,
                )

            assertEquals(
                SummaryCardActionViewModel.changeAction(
                    LettingAgentUpdateEpcController.getUpdateEpcRoute(token) + "/${StartEpcStep.ROUTE_SEGMENT}",
                ),
                result.epcSummaryCard.actions,
            )
        }

        @Test
        fun `cards have no change actions for the local council view`() {
            val propertyCompliance = PropertyComplianceBuilder.createWithInDateCerts()

            val result =
                propertyComplianceViewModelFactory.create(
                    propertyCompliance,
                    viewType = PropertyDetailsViewType.LOCAL_COUNCIL,
                    propertyOwnershipId = propertyOwnershipId,
                )

            assertNull(result.gasSafetySummaryCard.actions)
            assertNull(result.electricalSafetySummaryCard.actions)
            assertEquals(
                listOf(
                    SummaryCardActionViewModel(
                        "propertyCompliance.epcTask.checkEpcAnswers.epc.viewFullEpc",
                        "${PropertyComplianceBuilder.TEST_EPC_BASE_URL}/0000-0000-0000-0000-0000",
                        opensInNewTab = true,
                    ),
                ),
                result.epcSummaryCard.actions,
            )
        }
    }
}
