package uk.gov.communities.prsdb.webapp.controllers

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.util.ReflectionTestUtils
import org.springframework.test.web.servlet.get
import org.springframework.web.context.WebApplicationContext
import uk.gov.communities.prsdb.webapp.config.managers.FeatureFlagManager
import uk.gov.communities.prsdb.webapp.constants.DELEGATE_TO_LETTING_AGENT
import uk.gov.communities.prsdb.webapp.constants.PROPERTY_REGISTRATION_RESTRUCTURE_AND_SKIPPING
import uk.gov.communities.prsdb.webapp.database.entity.LettingAgentAccess
import uk.gov.communities.prsdb.webapp.models.viewModels.summaryModels.PropertyDetailsNotificationBannerViewModel
import uk.gov.communities.prsdb.webapp.models.viewModels.summaryModels.propertyComplianceViewModels.NotificationBannerViewModelService
import uk.gov.communities.prsdb.webapp.models.viewModels.summaryModels.propertyComplianceViewModels.PropertyComplianceViewModelFactory
import uk.gov.communities.prsdb.webapp.services.JointLandlordInvitationService
import uk.gov.communities.prsdb.webapp.services.PropertyComplianceService
import uk.gov.communities.prsdb.webapp.services.PropertyOwnershipService
import uk.gov.communities.prsdb.webapp.services.UserToLandlordService
import uk.gov.communities.prsdb.webapp.testHelpers.builders.PropertyComplianceBuilder
import uk.gov.communities.prsdb.webapp.testHelpers.mockObjects.MockLandlordData
import uk.gov.communities.prsdb.webapp.testHelpers.mockObjects.MockLandlordData.Companion.createIndividualLandlord
import uk.gov.communities.prsdb.webapp.testHelpers.mockObjects.MockLandlordData.Companion.createOrgLandlord
import uk.gov.communities.prsdb.webapp.testHelpers.mockObjects.MockLandlordData.Companion.createPropertyOwnership
import java.util.UUID
import kotlin.test.Test

@WebMvcTest(PropertyDetailsController::class)
class PropertyDetailsControllerTests(
    @Autowired val webContext: WebApplicationContext,
) : ControllerTest(webContext) {
    @MockitoBean
    private lateinit var propertyOwnershipService: PropertyOwnershipService

    @MockitoBean
    private lateinit var propertyComplianceService: PropertyComplianceService

    @MockitoBean
    private lateinit var viewModelFactory: PropertyComplianceViewModelFactory

    @MockitoBean
    private lateinit var notificationBannerViewModelService: NotificationBannerViewModelService

    @MockitoBean
    private lateinit var jointLandlordInvitationService: JointLandlordInvitationService

    @MockitoBean
    private lateinit var userToLandlordService: UserToLandlordService

    @MockitoBean
    private lateinit var featureFlagManager: FeatureFlagManager

    @BeforeEach
    fun setUp() {
        whenever(featureFlagManager.checkFeature(PROPERTY_REGISTRATION_RESTRUCTURE_AND_SKIPPING)).thenReturn(false)
        whenever(propertyComplianceService.getComplianceForPropertyOrNull(any()))
            .thenReturn(PropertyComplianceBuilder.createWithInDateCerts())
        whenever(
            notificationBannerViewModelService.getPropertyDetailsNotificationBanner(
                anyOrNull(),
                any(),
                any(),
                any(),
                any(),
            ),
        ).thenReturn(PropertyDetailsNotificationBannerViewModel.fromState(true, false, false, false, emptyList()))
        whenever(notificationBannerViewModelService.getBeforePdjb939NotificationBanner(anyOrNull(), any()))
            .thenReturn(emptyList())
    }

    @Nested
    inner class GetPropertyDetailsLandlordViewTests {
        @BeforeEach
        fun setUpLandlord() {
            whenever(userToLandlordService.getCurrentLandlordForUser()).thenReturn(createIndividualLandlord())
        }

        @Test
        fun `getPropertyDetails returns a redirect for an unauthenticated user`() {
            mvc.get(PropertyDetailsController.getPropertyDetailsPath(1L, isLocalCouncilView = false)).andExpect {
                status { is3xxRedirection() }
            }
        }

        @Test
        @WithMockUser
        fun `getPropertyDetails returns 403 for an unauthorized user`() {
            mvc.get(PropertyDetailsController.getPropertyDetailsPath(1L, isLocalCouncilView = false)).andExpect {
                status { status { isForbidden() } }
            }
        }

        @Test
        @WithMockUser(roles = ["LOCAL_COUNCIL_ADMIN"])
        fun `getPropertyDetails returns 403 for an unauthorized user with local council admin role`() {
            mvc.get(PropertyDetailsController.getPropertyDetailsPath(1L, isLocalCouncilView = false)).andExpect {
                status { status { isForbidden() } }
            }
        }

        @Test
        @WithMockUser(roles = ["LOCAL_COUNCIL_USER"])
        fun `getPropertyDetails returns 403 for an unauthorized user with local council user role`() {
            mvc.get(PropertyDetailsController.getPropertyDetailsPath(1L, isLocalCouncilView = false)).andExpect {
                status { status { isForbidden() } }
            }
        }

        @Test
        @WithMockUser(roles = ["LANDLORD"])
        fun `getPropertyDetails returns 200 for a valid request from a landlord`() {
            val propertyOwnership = createPropertyOwnership()

            whenever(propertyOwnershipService.getPropertyOwnershipIfCurrentUserAuthorized(eq(propertyOwnership.id)))
                .thenReturn(
                    propertyOwnership,
                )
            whenever(jointLandlordInvitationService.getPendingAndExpiredInvitations(propertyOwnership))
                .thenReturn(Pair(emptyList(), emptyList()))

            mvc
                .get(PropertyDetailsController.getPropertyDetailsPath(propertyOwnership.id, isLocalCouncilView = false))
                .andExpect {
                    status { status { isOk() } }
                }
        }

        @Test
        @WithMockUser(roles = ["LANDLORD"])
        fun `getPropertyDetails fetches invitations`() {
            val propertyOwnership = createPropertyOwnership()

            whenever(propertyOwnershipService.getPropertyOwnershipIfCurrentUserAuthorized(eq(propertyOwnership.id)))
                .thenReturn(propertyOwnership)
            whenever(jointLandlordInvitationService.getPendingAndExpiredInvitations(propertyOwnership))
                .thenReturn(Pair(emptyList(), emptyList()))

            mvc
                .get(PropertyDetailsController.getPropertyDetailsPath(propertyOwnership.id, isLocalCouncilView = false))
                .andExpect {
                    status { isOk() }
                    model { attributeExists("pendingInvitations") }
                    model { attributeExists("expiredInvitations") }
                }

            verify(jointLandlordInvitationService).getPendingAndExpiredInvitations(propertyOwnership)
        }

        @ParameterizedTest(name = "when the provide later feature is {0}")
        @ValueSource(booleans = [true, false])
        @WithMockUser(roles = ["LANDLORD"])
        fun `getPropertyDetails selects the view matching the provide later feature`(isFeatureEnabled: Boolean) {
            val propertyOwnership = createPropertyOwnership()

            whenever(propertyOwnershipService.getPropertyOwnershipIfCurrentUserAuthorized(eq(propertyOwnership.id)))
                .thenReturn(propertyOwnership)
            whenever(jointLandlordInvitationService.getPendingAndExpiredInvitations(propertyOwnership))
                .thenReturn(Pair(emptyList(), emptyList()))
            whenever(featureFlagManager.checkFeature(PROPERTY_REGISTRATION_RESTRUCTURE_AND_SKIPPING)).thenReturn(
                isFeatureEnabled,
            )

            val expectedView =
                if (isFeatureEnabled) {
                    PropertyDetailsController.PROPERTY_DETAILS_VIEW
                } else {
                    PropertyDetailsController.PROPERTY_DETAILS_BEFORE_PDJB939_VIEW
                }

            mvc
                .get(PropertyDetailsController.getPropertyDetailsPath(propertyOwnership.id, isLocalCouncilView = false))
                .andExpect {
                    status { isOk() }
                    view { name(expectedView) }
                }
        }

        @Test
        @WithMockUser(roles = ["LANDLORD"])
        fun `getPropertyDetails shows invite joint landlord button`() {
            val propertyOwnership = createPropertyOwnership()

            whenever(propertyOwnershipService.getPropertyOwnershipIfCurrentUserAuthorized(eq(propertyOwnership.id)))
                .thenReturn(propertyOwnership)
            whenever(jointLandlordInvitationService.getPendingAndExpiredInvitations(propertyOwnership))
                .thenReturn(Pair(emptyList(), emptyList()))

            mvc
                .get(PropertyDetailsController.getPropertyDetailsPath(propertyOwnership.id, isLocalCouncilView = false))
                .andExpect {
                    status { isOk() }
                    model { attributeExists("inviteJointLandlordUrl") }
                }
        }

        @Test
        @WithMockUser(roles = ["LANDLORD"])
        fun `getPropertyDetails passes markedJointLandlord false when property is individual`() {
            val propertyOwnership = createPropertyOwnership(markedJointLandlord = false)

            whenever(propertyOwnershipService.getPropertyOwnershipIfCurrentUserAuthorized(eq(propertyOwnership.id)))
                .thenReturn(propertyOwnership)
            whenever(jointLandlordInvitationService.getPendingAndExpiredInvitations(propertyOwnership))
                .thenReturn(Pair(emptyList(), emptyList()))

            mvc
                .get(PropertyDetailsController.getPropertyDetailsPath(propertyOwnership.id, isLocalCouncilView = false))
                .andExpect {
                    status { isOk() }
                    model { attribute("markedJointLandlord", false) }
                }
        }

        @Test
        @WithMockUser(roles = ["LANDLORD"])
        fun `getPropertyDetails passes markedJointLandlord true when property is joint`() {
            val propertyOwnership = createPropertyOwnership(markedJointLandlord = true)

            whenever(propertyOwnershipService.getPropertyOwnershipIfCurrentUserAuthorized(eq(propertyOwnership.id)))
                .thenReturn(propertyOwnership)
            whenever(jointLandlordInvitationService.getPendingAndExpiredInvitations(propertyOwnership))
                .thenReturn(Pair(emptyList(), emptyList()))

            mvc
                .get(PropertyDetailsController.getPropertyDetailsPath(propertyOwnership.id, isLocalCouncilView = false))
                .andExpect {
                    status { isOk() }
                    model { attribute("markedJointLandlord", true) }
                }
        }

        @Test
        @WithMockUser(roles = ["LANDLORD"])
        fun `getPropertyDetails shows switch to individual inset if the property is marked as JL and there is only one landlord`() {
            val propertyOwnership = createPropertyOwnership(markedJointLandlord = true)

            whenever(propertyOwnershipService.getPropertyOwnershipIfCurrentUserAuthorized(eq(propertyOwnership.id)))
                .thenReturn(propertyOwnership)
            whenever(jointLandlordInvitationService.getPendingAndExpiredInvitations(propertyOwnership))
                .thenReturn(Pair(emptyList(), emptyList()))

            mvc
                .get(PropertyDetailsController.getPropertyDetailsPath(propertyOwnership.id, isLocalCouncilView = false))
                .andExpect {
                    status { isOk() }
                    model {
                        attribute(
                            "switchToIndividualLink",
                            SwitchToIndividualController.getSwitchToIndividualFirstStepPath(propertyOwnership.id),
                        )
                    }
                }
        }

        @Test
        @WithMockUser(roles = ["LANDLORD"])
        fun `getPropertyDetails does not show switch to individual inset when property is not marked as joint landlord`() {
            val propertyOwnership = createPropertyOwnership(markedJointLandlord = false)

            whenever(propertyOwnershipService.getPropertyOwnershipIfCurrentUserAuthorized(eq(propertyOwnership.id)))
                .thenReturn(propertyOwnership)
            whenever(jointLandlordInvitationService.getPendingAndExpiredInvitations(propertyOwnership))
                .thenReturn(Pair(emptyList(), emptyList()))

            mvc
                .get(PropertyDetailsController.getPropertyDetailsPath(propertyOwnership.id, isLocalCouncilView = false))
                .andExpect {
                    status { isOk() }
                    model { attributeDoesNotExist("switchToIndividualLink") }
                }
        }

        @Test
        @WithMockUser(roles = ["LANDLORD"])
        fun `getPropertyDetails does not show switch to individual inset when property has multiple landlords`() {
            val propertyOwnership =
                createPropertyOwnership(
                    markedJointLandlord = true,
                    landlords =
                        mutableSetOf(
                            createIndividualLandlord(name = "Landlord 1"),
                            createIndividualLandlord(name = "Landlord 2"),
                        ),
                )

            whenever(propertyOwnershipService.getPropertyOwnershipIfCurrentUserAuthorized(eq(propertyOwnership.id)))
                .thenReturn(propertyOwnership)
            whenever(jointLandlordInvitationService.getPendingAndExpiredInvitations(propertyOwnership))
                .thenReturn(Pair(emptyList(), emptyList()))

            mvc
                .get(PropertyDetailsController.getPropertyDetailsPath(propertyOwnership.id, isLocalCouncilView = false))
                .andExpect {
                    status { isOk() }
                    model { attributeDoesNotExist("switchToIndividualLink") }
                }
        }

        @Test
        @WithMockUser(roles = ["LANDLORD"])
        fun `getPropertyDetails adds landlordSummaryCards to model`() {
            val propertyOwnership = createPropertyOwnership()

            whenever(propertyOwnershipService.getPropertyOwnershipIfCurrentUserAuthorized(eq(propertyOwnership.id)))
                .thenReturn(propertyOwnership)
            whenever(jointLandlordInvitationService.getPendingAndExpiredInvitations(propertyOwnership))
                .thenReturn(Pair(emptyList(), emptyList()))

            mvc
                .get(PropertyDetailsController.getPropertyDetailsPath(propertyOwnership.id, isLocalCouncilView = false))
                .andExpect {
                    status { isOk() }
                    model { attributeExists("landlordSummaryCards") }
                    model { attributeExists("landlordCount") }
                }
        }

        @Test
        @WithMockUser(roles = ["LANDLORD"])
        fun `getPropertyDetails includes correct landlord count`() {
            val landlord1 = createIndividualLandlord(baseUser = MockLandlordData.createPrsdbUser("user-1"))
            val landlord2 = createIndividualLandlord(baseUser = MockLandlordData.createPrsdbUser("user-2"))
            val propertyOwnership = createPropertyOwnership(landlords = mutableSetOf(landlord1, landlord2))

            whenever(propertyOwnershipService.getPropertyOwnershipIfCurrentUserAuthorized(eq(propertyOwnership.id)))
                .thenReturn(propertyOwnership)
            whenever(jointLandlordInvitationService.getPendingAndExpiredInvitations(propertyOwnership))
                .thenReturn(Pair(emptyList(), emptyList()))

            mvc
                .get(PropertyDetailsController.getPropertyDetailsPath(propertyOwnership.id, isLocalCouncilView = false))
                .andExpect {
                    status { isOk() }
                    model { attribute("landlordCount", 2) }
                }
        }

        @Test
        @WithMockUser(roles = ["LANDLORD"])
        fun `getPropertyDetails returns 200 for an org landlord`() {
            val orgLandlord = createOrgLandlord()
            val propertyOwnership = createPropertyOwnership(landlords = mutableSetOf(orgLandlord))

            whenever(userToLandlordService.getCurrentLandlordForUser()).thenReturn(orgLandlord)
            whenever(propertyOwnershipService.getPropertyOwnershipIfCurrentUserAuthorized(eq(propertyOwnership.id)))
                .thenReturn(propertyOwnership)
            whenever(jointLandlordInvitationService.getPendingAndExpiredInvitations(propertyOwnership))
                .thenReturn(Pair(emptyList(), emptyList()))

            mvc
                .get(PropertyDetailsController.getPropertyDetailsPath(propertyOwnership.id, isLocalCouncilView = false))
                .andExpect {
                    status { isOk() }
                    model { attributeExists("landlordSummaryCards") }
                }
        }

        @Test
        @WithMockUser(roles = ["LANDLORD"])
        fun `getPropertyDetails returns 200 for a property with mixed individual and org landlords`() {
            val individualLandlord = createIndividualLandlord()
            val orgLandlord = createOrgLandlord()
            val propertyOwnership = createPropertyOwnership(landlords = mutableSetOf(individualLandlord, orgLandlord))

            whenever(propertyOwnershipService.getPropertyOwnershipIfCurrentUserAuthorized(eq(propertyOwnership.id)))
                .thenReturn(propertyOwnership)
            whenever(jointLandlordInvitationService.getPendingAndExpiredInvitations(propertyOwnership))
                .thenReturn(Pair(emptyList(), emptyList()))

            mvc
                .get(PropertyDetailsController.getPropertyDetailsPath(propertyOwnership.id, isLocalCouncilView = false))
                .andExpect {
                    status { isOk() }
                    model { attribute("landlordCount", 2) }
                    model { attributeExists("landlordSummaryCards") }
                }
        }

        @Test
        @WithMockUser(roles = ["LANDLORD"])
        fun `getPropertyDetails includes letting agent panel when flag enabled and agent exists`() {
            val propertyOwnership = createPropertyOwnership()
            val lettingAgentAccess = LettingAgentAccess(UUID.randomUUID(), "agent@example.com", propertyOwnership)
            ReflectionTestUtils.setField(propertyOwnership, "lettingAgentAccess", lettingAgentAccess)

            whenever(featureFlagManager.checkFeature(DELEGATE_TO_LETTING_AGENT)).thenReturn(true)
            whenever(propertyOwnershipService.getPropertyOwnershipIfCurrentUserAuthorized(eq(propertyOwnership.id)))
                .thenReturn(propertyOwnership)
            whenever(jointLandlordInvitationService.getPendingAndExpiredInvitations(propertyOwnership))
                .thenReturn(Pair(emptyList(), emptyList()))

            mvc
                .get(PropertyDetailsController.getPropertyDetailsPath(propertyOwnership.id, isLocalCouncilView = false))
                .andExpect {
                    status { isOk() }
                    model { attribute("showLettingAgentPanel", true) }
                    model { attribute("delegatesToLettingAgent", true) }
                    model { attribute("lettingAgentEmail", "agent@example.com") }
                    model { attributeExists("lettingAgentPanelLink") }
                }
        }

        @Test
        @WithMockUser(roles = ["LANDLORD"])
        fun `getPropertyDetails does not include letting agent panel when flag disabled`() {
            val propertyOwnership = createPropertyOwnership()

            whenever(featureFlagManager.checkFeature(DELEGATE_TO_LETTING_AGENT)).thenReturn(false)
            whenever(propertyOwnershipService.getPropertyOwnershipIfCurrentUserAuthorized(eq(propertyOwnership.id)))
                .thenReturn(propertyOwnership)
            whenever(jointLandlordInvitationService.getPendingAndExpiredInvitations(propertyOwnership))
                .thenReturn(Pair(emptyList(), emptyList()))

            mvc
                .get(PropertyDetailsController.getPropertyDetailsPath(propertyOwnership.id, isLocalCouncilView = false))
                .andExpect {
                    status { isOk() }
                    model { attributeDoesNotExist("showLettingAgentPanel") }
                    model { attributeDoesNotExist("delegatesToLettingAgent") }
                    model { attributeDoesNotExist("lettingAgentEmail") }
                }
        }

        @Test
        @WithMockUser(roles = ["LANDLORD"])
        fun `getPropertyDetails shows delegate panel when flag enabled and no agent assigned`() {
            val propertyOwnership = createPropertyOwnership()

            whenever(featureFlagManager.checkFeature(DELEGATE_TO_LETTING_AGENT)).thenReturn(true)
            whenever(propertyOwnershipService.getPropertyOwnershipIfCurrentUserAuthorized(eq(propertyOwnership.id)))
                .thenReturn(propertyOwnership)
            whenever(jointLandlordInvitationService.getPendingAndExpiredInvitations(propertyOwnership))
                .thenReturn(Pair(emptyList(), emptyList()))

            mvc
                .get(PropertyDetailsController.getPropertyDetailsPath(propertyOwnership.id, isLocalCouncilView = false))
                .andExpect {
                    status { isOk() }
                    model { attribute("showLettingAgentPanel", true) }
                    model { attribute("delegatesToLettingAgent", false) }
                    model { attributeDoesNotExist("lettingAgentEmail") }
                    model { attributeExists("lettingAgentPanelLink") }
                }
        }

        @Test
        @WithMockUser(roles = ["LANDLORD"])
        fun `getPropertyDetails shows the delegate to letting agent link when the feature flag is enabled`() {
            val propertyOwnership = createPropertyOwnership()

            whenever(propertyOwnershipService.getPropertyOwnershipIfCurrentUserAuthorized(eq(propertyOwnership.id)))
                .thenReturn(propertyOwnership)
            whenever(jointLandlordInvitationService.getPendingAndExpiredInvitations(propertyOwnership))
                .thenReturn(Pair(emptyList(), emptyList()))
            whenever(featureFlagManager.checkFeature(DELEGATE_TO_LETTING_AGENT)).thenReturn(true)

            mvc.get(PropertyDetailsController.getPropertyDetailsPath(propertyOwnership.id, isLocalCouncilView = false)).andExpect {
                status { isOk() }
                model { attributeExists("lettingAgentPanelLink") }
            }
        }

        @Test
        @WithMockUser(roles = ["LANDLORD"])
        fun `getPropertyDetails hides the delegate to letting agent link when the feature flag is disabled`() {
            val propertyOwnership = createPropertyOwnership()

            whenever(propertyOwnershipService.getPropertyOwnershipIfCurrentUserAuthorized(eq(propertyOwnership.id)))
                .thenReturn(propertyOwnership)
            whenever(jointLandlordInvitationService.getPendingAndExpiredInvitations(propertyOwnership))
                .thenReturn(Pair(emptyList(), emptyList()))
            whenever(featureFlagManager.checkFeature(DELEGATE_TO_LETTING_AGENT)).thenReturn(false)

            mvc.get(PropertyDetailsController.getPropertyDetailsPath(propertyOwnership.id, isLocalCouncilView = false)).andExpect {
                status { isOk() }
                model { attributeDoesNotExist("lettingAgentPanelLink") }
            }
        }
    }

    @Nested
    inner class GetPropertyDetailsLocalCouncilViewTests {
        @Test
        fun `getPropertyDetailsLocalCouncilView returns a redirect for an unauthenticated user`() {
            mvc.get(PropertyDetailsController.getPropertyDetailsPath(1L, isLocalCouncilView = true)).andExpect {
                status { is3xxRedirection() }
            }
        }

        @Test
        @WithMockUser
        fun `getPropertyDetailsLocalCouncilView returns 403 for an unauthorized user`() {
            mvc.get(PropertyDetailsController.getPropertyDetailsPath(1L, isLocalCouncilView = true)).andExpect {
                status { status { isForbidden() } }
            }
        }

        @Test
        @WithMockUser(roles = ["LANDLORD"])
        fun `getPropertyDetailsLocalCouncilView returns 403 for an unauthorized user with only the landlord role`() {
            mvc.get(PropertyDetailsController.getPropertyDetailsPath(1L, isLocalCouncilView = true)).andExpect {
                status { status { isForbidden() } }
            }
        }

        @Test
        @WithMockUser(roles = ["LOCAL_COUNCIL_USER"])
        fun `getPropertyDetailsLocalCouncilView returns 200 for a valid request from an LocalCouncil user`() {
            val propertyOwnership = createPropertyOwnership()

            whenever(propertyOwnershipService.getPropertyOwnershipIfCurrentUserAuthorized(eq(1)))
                .thenReturn(
                    propertyOwnership,
                )
            whenever(jointLandlordInvitationService.getPendingAndExpiredInvitations(propertyOwnership))
                .thenReturn(Pair(emptyList(), emptyList()))

            mvc.get(PropertyDetailsController.getPropertyDetailsPath(1L, isLocalCouncilView = true)).andExpect {
                status { status { isOk() } }
            }
        }

        @Test
        @WithMockUser(roles = ["LOCAL_COUNCIL_ADMIN"])
        fun `getPropertyDetailsLocalCouncilView returns 200 for a valid request from an LocalCouncil admin`() {
            val propertyOwnership = createPropertyOwnership()

            whenever(propertyOwnershipService.getPropertyOwnershipIfCurrentUserAuthorized(eq(1)))
                .thenReturn(
                    propertyOwnership,
                )
            whenever(jointLandlordInvitationService.getPendingAndExpiredInvitations(propertyOwnership))
                .thenReturn(Pair(emptyList(), emptyList()))

            mvc.get(PropertyDetailsController.getPropertyDetailsPath(1L, isLocalCouncilView = true)).andExpect {
                status { status { isOk() } }
            }
        }

        @Test
        @WithMockUser(roles = ["LOCAL_COUNCIL_USER"])
        fun `getPropertyDetailsLocalCouncilView fetches invitations`() {
            val propertyOwnership = createPropertyOwnership()

            whenever(propertyOwnershipService.getPropertyOwnershipIfCurrentUserAuthorized(eq(1)))
                .thenReturn(propertyOwnership)
            whenever(jointLandlordInvitationService.getPendingAndExpiredInvitations(propertyOwnership))
                .thenReturn(Pair(emptyList(), emptyList()))

            mvc.get(PropertyDetailsController.getPropertyDetailsPath(1L, isLocalCouncilView = true)).andExpect {
                status { isOk() }
                model { attributeExists("pendingInvitations") }
                model { attributeExists("expiredInvitations") }
            }

            verify(jointLandlordInvitationService).getPendingAndExpiredInvitations(propertyOwnership)
        }

        @Test
        @WithMockUser(roles = ["LOCAL_COUNCIL_USER"])
        fun `getPropertyDetailsLocalCouncilView never shows the delegate to letting agent link`() {
            val propertyOwnership = createPropertyOwnership()

            whenever(propertyOwnershipService.getPropertyOwnershipIfCurrentUserAuthorized(eq(1)))
                .thenReturn(propertyOwnership)
            whenever(jointLandlordInvitationService.getPendingAndExpiredInvitations(propertyOwnership))
                .thenReturn(Pair(emptyList(), emptyList()))
            whenever(featureFlagManager.checkFeature(DELEGATE_TO_LETTING_AGENT)).thenReturn(true)

            mvc.get(PropertyDetailsController.getPropertyDetailsPath(1L, isLocalCouncilView = true)).andExpect {
                status { isOk() }
                model { attributeDoesNotExist("lettingAgentPanelLink") }
            }
        }
    }

    @Nested
    inner class RemoveExpiredInviteTests {
        @Test
        fun `removeExpiredInvite returns a redirect for an unauthenticated user`() {
            mvc.get(PropertyDetailsController.getRemoveExpiredInvitePath(1L, 1L)).andExpect {
                status { is3xxRedirection() }
            }
        }

        @Test
        @WithMockUser
        fun `removeExpiredInvite returns 403 for an unauthorized user`() {
            mvc.get(PropertyDetailsController.getRemoveExpiredInvitePath(1L, 1L)).andExpect {
                status { status { isForbidden() } }
            }
        }

        @Test
        @WithMockUser(roles = ["LANDLORD"])
        fun `removeExpiredInvite redirects to property details with flash attribute on success`() {
            mvc.get(PropertyDetailsController.getRemoveExpiredInvitePath(1L, 1L)).andExpect {
                status { is3xxRedirection() }
                flash { attribute("inviteRemoved", true) }
            }

            verify(jointLandlordInvitationService).hideExpiredInvitation(eq(1L))
        }
    }
}
