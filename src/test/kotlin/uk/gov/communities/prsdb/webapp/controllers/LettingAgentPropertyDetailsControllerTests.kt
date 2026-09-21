package uk.gov.communities.prsdb.webapp.controllers

import jakarta.servlet.ServletException
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.context.MessageSource
import org.springframework.context.annotation.Import
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.get
import org.springframework.web.context.WebApplicationContext
import uk.gov.communities.prsdb.webapp.config.MessageSourceConfig
import uk.gov.communities.prsdb.webapp.constants.LETTING_AGENT_PROPERTY_DETAILS_SURVEY_URL
import uk.gov.communities.prsdb.webapp.models.viewModels.summaryModels.PropertyDetailsViewType
import uk.gov.communities.prsdb.webapp.models.viewModels.summaryModels.propertyComplianceViewModels.ElectricalSafetyViewModelFactory
import uk.gov.communities.prsdb.webapp.models.viewModels.summaryModels.propertyComplianceViewModels.EpcViewModelFactory
import uk.gov.communities.prsdb.webapp.models.viewModels.summaryModels.propertyComplianceViewModels.GasSafetyViewModelFactory
import uk.gov.communities.prsdb.webapp.models.viewModels.summaryModels.propertyComplianceViewModels.PropertyComplianceViewModel
import uk.gov.communities.prsdb.webapp.models.viewModels.summaryModels.propertyComplianceViewModels.PropertyComplianceViewModelFactory
import uk.gov.communities.prsdb.webapp.services.PropertyComplianceService
import uk.gov.communities.prsdb.webapp.services.PropertyOwnershipService
import uk.gov.communities.prsdb.webapp.testHelpers.builders.PropertyComplianceBuilder
import uk.gov.communities.prsdb.webapp.testHelpers.mockObjects.MockLandlordData.Companion.createOccupiedPropertyOwnership
import uk.gov.communities.prsdb.webapp.testHelpers.mockObjects.MockLandlordData.Companion.createUnoccupiedPropertyOwnership
import uk.gov.communities.prsdb.webapp.testHelpers.mockObjects.MockLettingAgentData
import java.util.UUID

@WebMvcTest(LettingAgentPropertyDetailsController::class)
@Import(MessageSourceConfig::class)
class LettingAgentPropertyDetailsControllerTests(
    @Autowired val webContext: WebApplicationContext,
) : LettingAgentAccessControllerTest(webContext) {
    @MockitoBean
    private lateinit var propertyOwnershipService: PropertyOwnershipService

    @MockitoBean
    private lateinit var propertyComplianceService: PropertyComplianceService

    @MockitoBean
    private lateinit var propertyComplianceViewModelFactory: PropertyComplianceViewModelFactory

    // Access to this endpoint is restricted by the LettingAGentAccessInterceptor
    @Test
    fun `getLettingAgentPropertyDetails renders the letting agent view for a valid token`() {
        val token = UUID.randomUUID()
        val propertyOwnership = createOccupiedPropertyOwnership()

        whenever(lettingAgentAccessService.getInvitationByTokenOrNull(eq(token)))
            .thenReturn(MockLettingAgentData.createLettingAgentAccess(token = token, propertyOwnership = propertyOwnership))
        whenever(propertyOwnershipService.getPropertyOwnership(eq(propertyOwnership.id)))
            .thenReturn(propertyOwnership)
        whenever(propertyOwnershipService.hasLettingAgent(any()))
            .thenReturn(true)
        whenever(propertyComplianceService.getComplianceForPropertyOrNull(eq(propertyOwnership.id)))
            .thenReturn(PropertyComplianceBuilder.createWithInDateCerts())
        val complianceViewModel = createComplianceViewModel()
        whenever(propertyComplianceViewModelFactory.create(any(), any(), any(), anyOrNull()))
            .thenReturn(complianceViewModel)

        mvc
            .get(LettingAgentPropertyDetailsController.getLettingAgentPropertyDetailsPath(token))
            .andExpect {
                status { isOk() }
                view { name("propertyDetailsLettingAgentView") }
                model { attributeExists("propertyDetails") }
            }
    }

    @Test
    fun `getLettingAgentPropertyDetails includes the survey URL in the model`() {
        val token = UUID.randomUUID()
        val propertyOwnership = createOccupiedPropertyOwnership()

        whenever(lettingAgentAccessService.getInvitationByTokenOrNull(eq(token)))
            .thenReturn(MockLettingAgentData.createLettingAgentAccess(token = token, propertyOwnership = propertyOwnership))
        whenever(propertyOwnershipService.getPropertyOwnership(eq(propertyOwnership.id)))
            .thenReturn(propertyOwnership)
        whenever(propertyOwnershipService.hasLettingAgent(any()))
            .thenReturn(true)
        whenever(propertyComplianceService.getComplianceForPropertyOrNull(eq(propertyOwnership.id)))
            .thenReturn(PropertyComplianceBuilder.createWithInDateCerts())
        val complianceViewModel = createComplianceViewModel()
        whenever(propertyComplianceViewModelFactory.create(any(), any(), any(), anyOrNull()))
            .thenReturn(complianceViewModel)

        mvc
            .get(LettingAgentPropertyDetailsController.getLettingAgentPropertyDetailsPath(token))
            .andExpect {
                status { isOk() }
                model {
                    attribute("lettingAgentPropertyDetailsSurveyUrl", LETTING_AGENT_PROPERTY_DETAILS_SURVEY_URL)
                }
            }
    }

    private fun createComplianceViewModel(): PropertyComplianceViewModel {
        val messageSource = mock<MessageSource>()
        whenever(messageSource.getMessage(any(), any(), any())).thenReturn("")
        return PropertyComplianceViewModelFactory(
            GasSafetyViewModelFactory(mock(), messageSource, mock()),
            ElectricalSafetyViewModelFactory(mock(), messageSource, mock()),
            EpcViewModelFactory(messageSource, mock()),
        ).create(
            propertyCompliance = PropertyComplianceBuilder.createWithInDateCerts(),
            viewType = PropertyDetailsViewType.LETTING_AGENT,
            propertyOwnershipId = 1L,
        )
    }

    @Test
    fun `getLettingAgentPropertyDetails returns not found when the token is not recognised`() {
        val token = UUID.randomUUID()

        whenever(lettingAgentAccessService.getInvitationByTokenOrNull(eq(token)))
            .thenReturn(null)

        mvc
            .get(LettingAgentPropertyDetailsController.getLettingAgentPropertyDetailsPath(token))
            .andExpect {
                status { isNotFound() }
            }
    }

    @Test
    fun `getLettingAgentPropertyDetails returns not found when the property does not have a letting agent`() {
        val token = UUID.randomUUID()
        val propertyOwnership = createUnoccupiedPropertyOwnership()

        whenever(lettingAgentAccessService.getInvitationByTokenOrNull(eq(token)))
            .thenReturn(MockLettingAgentData.createLettingAgentAccess(token = token, propertyOwnership = propertyOwnership))
        whenever(propertyOwnershipService.getPropertyOwnership(eq(propertyOwnership.id)))
            .thenReturn(propertyOwnership)
        whenever(propertyOwnershipService.hasLettingAgent(any()))
            .thenReturn(false)

        mvc
            .get(LettingAgentPropertyDetailsController.getLettingAgentPropertyDetailsPath(token))
            .andExpect {
                status { isNotFound() }
            }
    }

    @Test
    fun `getLettingAgentPropertyDetails throws when the property has no compliance record`() {
        val token = UUID.randomUUID()
        val propertyOwnership = createOccupiedPropertyOwnership()

        whenever(lettingAgentAccessService.getInvitationByTokenOrNull(eq(token)))
            .thenReturn(MockLettingAgentData.createLettingAgentAccess(token = token, propertyOwnership = propertyOwnership))
        whenever(propertyOwnershipService.getPropertyOwnership(eq(propertyOwnership.id)))
            .thenReturn(propertyOwnership)
        whenever(propertyOwnershipService.hasLettingAgent(any()))
            .thenReturn(true)
        whenever(propertyComplianceService.getComplianceForPropertyOrNull(any()))
            .thenReturn(null)

        assertThrows<ServletException> {
            mvc.get(LettingAgentPropertyDetailsController.getLettingAgentPropertyDetailsPath(token))
        }
    }
}
