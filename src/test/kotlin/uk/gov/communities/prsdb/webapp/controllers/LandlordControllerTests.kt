package uk.gov.communities.prsdb.webapp.controllers

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.http.HttpStatus
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.get
import org.springframework.web.context.WebApplicationContext
import org.springframework.web.server.ResponseStatusException
import uk.gov.communities.prsdb.webapp.config.managers.FeatureFlagManager
import uk.gov.communities.prsdb.webapp.constants.LANDLORD_PATH_SEGMENT
import uk.gov.communities.prsdb.webapp.constants.PROPERTY_REGISTRATION_PHASE_THREE
import uk.gov.communities.prsdb.webapp.constants.REGISTERED_PROPERTIES_FRAGMENT
import uk.gov.communities.prsdb.webapp.constants.enums.ComplianceCertStatus
import uk.gov.communities.prsdb.webapp.controllers.LandlordController.Companion.COMPLIANCE_ACTIONS_URL
import uk.gov.communities.prsdb.webapp.controllers.LandlordController.Companion.LANDLORD_DASHBOARD_URL
import uk.gov.communities.prsdb.webapp.controllers.LandlordController.Companion.OUTSTANDING_ACTIONS_URL
import uk.gov.communities.prsdb.webapp.models.dataModels.ComplianceStatusDataModel
import uk.gov.communities.prsdb.webapp.models.dataModels.OutstandingActionsDataModel
import uk.gov.communities.prsdb.webapp.models.viewModels.summaryModels.ComplianceActionViewModelBuilder
import uk.gov.communities.prsdb.webapp.models.viewModels.summaryModels.LandlordDashboardNotificationBannerViewModel
import uk.gov.communities.prsdb.webapp.models.viewModels.summaryModels.OutstandingActionViewModelBuilder
import uk.gov.communities.prsdb.webapp.services.LocalCouncilService
import uk.gov.communities.prsdb.webapp.services.PropertyComplianceService
import uk.gov.communities.prsdb.webapp.services.PropertyOwnershipService
import uk.gov.communities.prsdb.webapp.services.UserToLandlordService
import uk.gov.communities.prsdb.webapp.services.UsersIncompletePropertyService
import uk.gov.communities.prsdb.webapp.testHelpers.mockObjects.MockLandlordData.Companion.createIndividualLandlord
import uk.gov.communities.prsdb.webapp.testHelpers.mockObjects.MockLandlordData.Companion.createOrgLandlord
import kotlin.test.assertEquals

@WebMvcTest(LandlordController::class)
class LandlordControllerTests(
    @Autowired val webContext: WebApplicationContext,
) : ControllerTest(webContext) {
    @MockitoBean
    private lateinit var userToLandlordService: UserToLandlordService

    @MockitoBean
    private lateinit var localCouncilService: LocalCouncilService

    @MockitoBean
    private lateinit var propertyOwnershipService: PropertyOwnershipService

    @MockitoBean
    private lateinit var propertyComplianceService: PropertyComplianceService

    @MockitoBean
    private lateinit var usersIncompletePropertyService: UsersIncompletePropertyService

    @MockitoBean
    private lateinit var featureFlagManager: FeatureFlagManager

    @BeforeEach
    fun enableFeatureFlag() {
        whenever(featureFlagManager.checkFeature(PROPERTY_REGISTRATION_PHASE_THREE)).thenReturn(true)
    }

    @Test
    fun `index returns a redirect for unauthenticated user`() {
        mvc
            .get("/$LANDLORD_PATH_SEGMENT")
            .andExpect {
                status { is3xxRedirection() }
            }
    }

    @WithMockUser
    @Test
    fun `index returns 403 for unauthorized user`() {
        mvc
            .get("/$LANDLORD_PATH_SEGMENT")
            .andExpect {
                status { isForbidden() }
            }
    }

    @Test
    @WithMockUser(roles = ["INDIVIDUAL_LANDLORD"])
    fun `index returns a redirect for authorised user`() {
        mvc
            .get("/$LANDLORD_PATH_SEGMENT")
            .andExpect {
                status { is3xxRedirection() }
                redirectedUrl(LANDLORD_DASHBOARD_URL)
            }
    }

    @Test
    @WithMockUser(roles = ["ORG_ADMIN"])
    fun `index returns a redirect for an org admin`() {
        mvc
            .get("/$LANDLORD_PATH_SEGMENT")
            .andExpect {
                status { is3xxRedirection() }
                redirectedUrl(LANDLORD_DASHBOARD_URL)
            }
    }

    @Test
    @WithMockUser(roles = ["ORG_EDITOR"])
    fun `index returns a redirect for an org editor`() {
        mvc
            .get("/$LANDLORD_PATH_SEGMENT")
            .andExpect {
                status { is3xxRedirection() }
                redirectedUrl(LANDLORD_DASHBOARD_URL)
            }
    }

    @Test
    fun `landlordDashboard returns a redirect for unauthenticated user`() {
        mvc
            .get(LANDLORD_DASHBOARD_URL)
            .andExpect {
                status { is3xxRedirection() }
            }
    }

    @Test
    @WithMockUser
    fun `landlordDashboard returns 403 for unauthorized user`() {
        mvc
            .get(LANDLORD_DASHBOARD_URL)
            .andExpect {
                status { isForbidden() }
            }
    }

    @Test
    @WithMockUser(roles = ["ORG_EDITOR"], username = "removed-member")
    fun `landlordDashboard forwards forbidden exception from userToLandlordService`() {
        whenever(userToLandlordService.getCurrentLandlordForUser())
            .thenThrow(ResponseStatusException(HttpStatus.FORBIDDEN))

        mvc
            .get(LANDLORD_DASHBOARD_URL)
            .andExpect {
                status { isForbidden() }
            }
    }

    @Test
    @WithMockUser(roles = ["INDIVIDUAL_LANDLORD"])
    fun `landlordDashboard returns 200 for authorised landlord user`() {
        val landlord = createIndividualLandlord()
        whenever(userToLandlordService.getCurrentLandlordForUser()).thenReturn(landlord)
        mvc
            .get(LANDLORD_DASHBOARD_URL)
            .andExpect {
                status { isOk() }
            }
    }

    @Test
    @WithMockUser(roles = ["INDIVIDUAL_LANDLORD"])
    fun `landlordDashboard links to outstanding actions`() {
        whenever(userToLandlordService.getCurrentLandlordForUser()).thenReturn(createIndividualLandlord())

        mvc
            .get(LANDLORD_DASHBOARD_URL)
            .andExpect {
                status { isOk() }
                model {
                    attribute("outstandingActionsUrl", OUTSTANDING_ACTIONS_URL)
                    attribute("outstandingActionsButtonKey", "landlord.dashboard.outstandingActions.button")
                }
            }
    }

    @Test
    @WithMockUser(roles = ["INDIVIDUAL_LANDLORD"])
    fun `landlordDashboard returns 200 for an org landlord user`() {
        val landlord = createOrgLandlord()
        whenever(userToLandlordService.getCurrentLandlordForUser()).thenReturn(landlord)
        mvc
            .get(LANDLORD_DASHBOARD_URL)
            .andExpect {
                status { isOk() }
                model { attribute("landlordName", landlord.name) }
            }
    }

    @Test
    @WithMockUser(roles = ["INDIVIDUAL_LANDLORD"], username = "user-123")
    fun `landlordDashboard shows the number of incomplete properties for the requesting user, for an individual landlord`() {
        val landlord = createIndividualLandlord()
        whenever(userToLandlordService.getCurrentLandlordForUser()).thenReturn(landlord)
        whenever(usersIncompletePropertyService.getCurrentUsersIncompletePropertiesCount()).thenReturn(3)

        val result =
            mvc
                .get(LANDLORD_DASHBOARD_URL)
                .andExpect { status { isOk() } }
                .andReturn()

        val bannerViewModel =
            result.modelAndView
                ?.model
                ?.get("landlordDashboardNotificationBannerViewModel") as LandlordDashboardNotificationBannerViewModel
        assertEquals(3, bannerViewModel.numberOfIncompleteProperties)
    }

    @Test
    @WithMockUser(roles = ["INDIVIDUAL_LANDLORD"], username = "org-user-123")
    fun `landlordDashboard shows the number of incomplete properties for the requesting user, for an org landlord`() {
        val landlord = createOrgLandlord()
        whenever(userToLandlordService.getCurrentLandlordForUser()).thenReturn(landlord)
        whenever(usersIncompletePropertyService.getCurrentUsersIncompletePropertiesCount()).thenReturn(2)

        val result =
            mvc
                .get(LANDLORD_DASHBOARD_URL)
                .andExpect { status { isOk() } }
                .andReturn()

        val bannerViewModel =
            result.modelAndView
                ?.model
                ?.get("landlordDashboardNotificationBannerViewModel") as LandlordDashboardNotificationBannerViewModel
        assertEquals(2, bannerViewModel.numberOfIncompleteProperties)
    }

    @Test
    fun `getOutstandingActions returns a redirect for unauthenticated user`() {
        mvc
            .get(OUTSTANDING_ACTIONS_URL)
            .andExpect {
                status { is3xxRedirection() }
            }
    }

    @Test
    @WithMockUser
    fun `getOutstandingActions returns 403 for unauthorized user`() {
        mvc
            .get(OUTSTANDING_ACTIONS_URL)
            .andExpect {
                status { isForbidden() }
            }
    }

    @Test
    @WithMockUser(roles = ["INDIVIDUAL_LANDLORD"], username = "user")
    fun `getOutstandingActions returns 200 for authorised landlord user`() {
        val nonCompliantDataModel =
            ComplianceStatusDataModel(
                propertyOwnershipId = 2,
                singleLineAddress = "456 Example Avenue, EX",
                registrationNumber = "P-YYYY-YYYY",
                gasSafetyStatus = ComplianceCertStatus.EXPIRED,
                electricalSafetyStatus = ComplianceCertStatus.ADDED,
                epcStatus = ComplianceCertStatus.HAS_FAULTS,
                isComplete = false,
                isOccupied = true,
            )
        val outstandingActionsDataModel =
            OutstandingActionsDataModel(
                propertyOwnershipId = 2,
                singleLineAddress = "456 Example Avenue, EX",
                isOccupied = true,
                isLicensingProvideLater = true,
                isTenancyProvideLater = false,
                provideLaterDeadline = null,
                complianceStatus = nonCompliantDataModel,
            )
        whenever(userToLandlordService.getCurrentLandlordForUser()).thenReturn(createIndividualLandlord())
        whenever(propertyOwnershipService.getOutstandingActionsForLandlord(any(), any())).thenReturn(
            PageImpl(listOf(outstandingActionsDataModel)),
        )

        // Act and Assert
        val expectedOutstandingActions =
            listOf(
                OutstandingActionViewModelBuilder.fromDataModel(outstandingActionsDataModel),
            )

        mvc
            .get(OUTSTANDING_ACTIONS_URL)
            .andExpect {
                status { isOk() }
                model {
                    attribute("outstandingActions", expectedOutstandingActions)
                    attribute(
                        "viewRegisteredPropertiesUrl",
                        "${LandlordDetailsController.LANDLORD_DETAILS_FOR_LANDLORD_ROUTE}#$REGISTERED_PROPERTIES_FRAGMENT",
                    )
                    attribute("backUrl", LANDLORD_DASHBOARD_URL)
                }
            }
    }

    @Test
    @WithMockUser(roles = ["INDIVIDUAL_LANDLORD"], username = "user")
    fun `getOutstandingActions returns outstandingActions view`() {
        whenever(userToLandlordService.getCurrentLandlordForUser()).thenReturn(createIndividualLandlord())
        whenever(propertyOwnershipService.getOutstandingActionsForLandlord(any(), any())).thenReturn(
            PageImpl(emptyList()),
        )

        mvc
            .get(OUTSTANDING_ACTIONS_URL)
            .andExpect {
                status { isOk() }
                view { name("outstandingActions") }
            }
    }

    @Test
    @WithMockUser(roles = ["INDIVIDUAL_LANDLORD"], username = "user")
    fun `getOutstandingActions redirects to first page when requested page exceeds total pages`() {
        whenever(userToLandlordService.getCurrentLandlordForUser()).thenReturn(createIndividualLandlord())
        whenever(propertyOwnershipService.getOutstandingActionsForLandlord(any(), any())).thenReturn(
            PageImpl(emptyList(), PageRequest.of(5, 10), 10),
        )

        mvc
            .get("$OUTSTANDING_ACTIONS_URL?page=6")
            .andExpect {
                status { is3xxRedirection() }
            }
    }

    @Test
    @WithMockUser(roles = ["INDIVIDUAL_LANDLORD"], username = "user")
    fun `getOutstandingActions includes paginationViewModel`() {
        whenever(userToLandlordService.getCurrentLandlordForUser()).thenReturn(createIndividualLandlord())
        whenever(propertyOwnershipService.getOutstandingActionsForLandlord(any(), any())).thenReturn(
            PageImpl(emptyList(), PageRequest.of(0, 10), 20),
        )

        mvc
            .get(OUTSTANDING_ACTIONS_URL)
            .andExpect {
                status { isOk() }
                model { attributeExists("paginationViewModel") }
            }
    }

    // TODO PDJB-2006: Delete this class when the PROPERTY_REGISTRATION_PHASE_THREE flag is removed
    @Nested
    inner class LegacyComplianceActions {
        @Test
        @WithMockUser(roles = ["INDIVIDUAL_LANDLORD"])
        fun `landlordDashboard links to compliance actions when PROPERTY_REGISTRATION_PHASE_THREE is disabled`() {
            whenever(featureFlagManager.checkFeature(PROPERTY_REGISTRATION_PHASE_THREE)).thenReturn(false)
            whenever(userToLandlordService.getCurrentLandlordForUser()).thenReturn(createIndividualLandlord())

            mvc
                .get(LANDLORD_DASHBOARD_URL)
                .andExpect {
                    status { isOk() }
                    model {
                        attribute("outstandingActionsUrl", COMPLIANCE_ACTIONS_URL)
                        attribute("outstandingActionsButtonKey", "landlord.dashboard.complianceActions.button")
                    }
                }
        }

        @Test
        fun `getComplianceActions returns a redirect for unauthenticated user`() {
            mvc
                .get(COMPLIANCE_ACTIONS_URL)
                .andExpect {
                    status { is3xxRedirection() }
                }
        }

        @Test
        @WithMockUser
        fun `getComplianceActions returns 403 for unauthorized user`() {
            mvc
                .get(COMPLIANCE_ACTIONS_URL)
                .andExpect {
                    status { isForbidden() }
                }
        }

        @Test
        @WithMockUser(roles = ["INDIVIDUAL_LANDLORD"], username = "user")
        fun `getComplianceActions returns 200 for authorised landlord user`() {
            val nonCompliantDataModel =
                ComplianceStatusDataModel(
                    propertyOwnershipId = 2,
                    singleLineAddress = "456 Example Avenue, EX",
                    registrationNumber = "P-YYYY-YYYY",
                    gasSafetyStatus = ComplianceCertStatus.EXPIRED,
                    electricalSafetyStatus = ComplianceCertStatus.ADDED,
                    epcStatus = ComplianceCertStatus.HAS_FAULTS,
                    isComplete = false,
                    isOccupied = true,
                )
            whenever(userToLandlordService.getCurrentLandlordForUser()).thenReturn(createIndividualLandlord())
            whenever(propertyComplianceService.getNonCompliantPropertiesForLandlord(any(), any())).thenReturn(
                PageImpl(listOf(nonCompliantDataModel)),
            )

            // Act and Assert
            val expectedComplianceActions =
                listOf(
                    ComplianceActionViewModelBuilder.fromDataModel(nonCompliantDataModel),
                )

            mvc
                .get(COMPLIANCE_ACTIONS_URL)
                .andExpect {
                    status { isOk() }
                    model {
                        attribute("complianceActions", expectedComplianceActions)
                        attribute(
                            "viewRegisteredPropertiesUrl",
                            "${LandlordDetailsController.LANDLORD_DETAILS_FOR_LANDLORD_ROUTE}#$REGISTERED_PROPERTIES_FRAGMENT",
                        )
                        attribute("backUrl", LANDLORD_DASHBOARD_URL)
                    }
                }
        }

        @Test
        @WithMockUser(roles = ["INDIVIDUAL_LANDLORD"], username = "user")
        fun `getComplianceActions returns complianceActions view`() {
            whenever(userToLandlordService.getCurrentLandlordForUser()).thenReturn(createIndividualLandlord())
            whenever(propertyComplianceService.getNonCompliantPropertiesForLandlord(any(), any())).thenReturn(
                PageImpl(emptyList()),
            )

            mvc
                .get(COMPLIANCE_ACTIONS_URL)
                .andExpect {
                    status { isOk() }
                    view { name("complianceActions") }
                }
        }

        @Test
        @WithMockUser(roles = ["INDIVIDUAL_LANDLORD"], username = "user")
        fun `getComplianceActions redirects to first page when requested page exceeds total pages`() {
            whenever(userToLandlordService.getCurrentLandlordForUser()).thenReturn(createIndividualLandlord())
            whenever(propertyComplianceService.getNonCompliantPropertiesForLandlord(any(), any())).thenReturn(
                PageImpl(emptyList(), PageRequest.of(5, 10), 10),
            )

            mvc
                .get("$COMPLIANCE_ACTIONS_URL?page=6")
                .andExpect {
                    status { is3xxRedirection() }
                }
        }

        @Test
        @WithMockUser(roles = ["INDIVIDUAL_LANDLORD"], username = "user")
        fun `getComplianceActions includes paginationViewModel`() {
            whenever(userToLandlordService.getCurrentLandlordForUser()).thenReturn(createIndividualLandlord())
            whenever(propertyComplianceService.getNonCompliantPropertiesForLandlord(any(), any())).thenReturn(
                PageImpl(emptyList(), PageRequest.of(0, 10), 20),
            )

            mvc
                .get(COMPLIANCE_ACTIONS_URL)
                .andExpect {
                    status { isOk() }
                    model { attributeExists("paginationViewModel") }
                }
        }
    }
}
