package uk.gov.communities.prsdb.webapp.controllers

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.springframework.data.domain.PageImpl
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.get
import uk.gov.communities.prsdb.webapp.config.featureFlags.FeatureFlagTestCallingEndpoints
import uk.gov.communities.prsdb.webapp.constants.PROPERTY_REGISTRATION_PHASE_THREE
import uk.gov.communities.prsdb.webapp.controllers.LandlordController.Companion.COMPLIANCE_ACTIONS_URL
import uk.gov.communities.prsdb.webapp.controllers.LandlordController.Companion.OUTSTANDING_ACTIONS_URL
import uk.gov.communities.prsdb.webapp.services.PropertyComplianceService
import uk.gov.communities.prsdb.webapp.services.PropertyOwnershipService
import uk.gov.communities.prsdb.webapp.services.UserToLandlordService
import uk.gov.communities.prsdb.webapp.services.UsersIncompletePropertyService
import uk.gov.communities.prsdb.webapp.testHelpers.mockObjects.MockLandlordData.Companion.createIndividualLandlord

// TODO PDJB-2006: Delete this class when the PROPERTY_REGISTRATION_PHASE_THREE flag is removed
class LandlordControllerFeatureFlagTests : FeatureFlagTestCallingEndpoints() {
    @MockitoBean
    private lateinit var userToLandlordService: UserToLandlordService

    @MockitoBean
    private lateinit var propertyOwnershipService: PropertyOwnershipService

    @MockitoBean
    private lateinit var propertyComplianceService: PropertyComplianceService

    @MockitoBean
    private lateinit var usersIncompletePropertyService: UsersIncompletePropertyService

    @BeforeEach
    fun setUpMocks() {
        whenever(userToLandlordService.getCurrentLandlordForUser()).thenReturn(createIndividualLandlord())
        whenever(propertyComplianceService.getNonCompliantPropertiesForLandlord(any(), any())).thenReturn(PageImpl(emptyList()))
        whenever(propertyOwnershipService.getOutstandingActionsForLandlord(any(), any())).thenReturn(PageImpl(emptyList()))
    }

    @WithMockUser(roles = ["INDIVIDUAL_LANDLORD"])
    @Test
    fun `outstanding actions page is unavailable if the property registration phase three feature flag is disabled`() {
        featureFlagManager.disableFeature(PROPERTY_REGISTRATION_PHASE_THREE)

        mvc
            .get(OUTSTANDING_ACTIONS_URL)
            .andExpect { status { isNotFound() } }
    }

    @WithMockUser(roles = ["INDIVIDUAL_LANDLORD"])
    @Test
    fun `outstanding actions page is available if the property registration phase three feature flag is enabled`() {
        featureFlagManager.enableFeature(PROPERTY_REGISTRATION_PHASE_THREE)

        mvc
            .get(OUTSTANDING_ACTIONS_URL)
            .andExpect { status { isOk() } }
    }

    @WithMockUser(roles = ["INDIVIDUAL_LANDLORD"])
    @Test
    fun `compliance actions page is available if the property registration phase three feature flag is disabled`() {
        featureFlagManager.disableFeature(PROPERTY_REGISTRATION_PHASE_THREE)

        mvc
            .get(COMPLIANCE_ACTIONS_URL)
            .andExpect { status { isOk() } }
    }

    @WithMockUser(roles = ["INDIVIDUAL_LANDLORD"])
    @Test
    fun `compliance actions page is unavailable if the property registration phase three feature flag is enabled`() {
        featureFlagManager.enableFeature(PROPERTY_REGISTRATION_PHASE_THREE)

        mvc
            .get(COMPLIANCE_ACTIONS_URL)
            .andExpect { status { isNotFound() } }
    }
}
