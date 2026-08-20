package uk.gov.communities.prsdb.webapp.controllers

import org.springframework.context.MessageSource
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.servlet.ModelAndView
import org.springframework.web.servlet.mvc.support.RedirectAttributes
import org.springframework.web.util.UriTemplate
import uk.gov.communities.prsdb.webapp.annotations.webAnnotations.PrsdbController
import uk.gov.communities.prsdb.webapp.config.interceptors.BackLinkInterceptor.Companion.overrideBackLinkForUrl
import uk.gov.communities.prsdb.webapp.config.managers.FeatureFlagManager
import uk.gov.communities.prsdb.webapp.constants.COMPLIANCE_INFO_FRAGMENT
import uk.gov.communities.prsdb.webapp.constants.DELEGATE_TO_LETTING_AGENT
import uk.gov.communities.prsdb.webapp.constants.LANDLORD_DETAILS_FRAGMENT
import uk.gov.communities.prsdb.webapp.constants.LANDLORD_PATH_SEGMENT
import uk.gov.communities.prsdb.webapp.constants.LOCAL_COUNCIL_PATH_SEGMENT
import uk.gov.communities.prsdb.webapp.constants.PROPERTY_DETAILS_SEGMENT
import uk.gov.communities.prsdb.webapp.constants.PROPERTY_REGISTRATION_RESTRUCTURE_AND_SKIPPING
import uk.gov.communities.prsdb.webapp.constants.REMOVE_EXPIRED_INVITE_PATH_SEGMENT
import uk.gov.communities.prsdb.webapp.controllers.DelegateToLettingAgentController.Companion.getDelegateToLettingAgentPath
import uk.gov.communities.prsdb.webapp.controllers.LandlordController.Companion.LANDLORD_DASHBOARD_URL
import uk.gov.communities.prsdb.webapp.controllers.LocalCouncilDashboardController.Companion.LOCAL_COUNCIL_DASHBOARD_URL
import uk.gov.communities.prsdb.webapp.database.entity.PropertyCompliance
import uk.gov.communities.prsdb.webapp.database.entity.PropertyOwnership
import uk.gov.communities.prsdb.webapp.exceptions.PrsdbWebException
import uk.gov.communities.prsdb.webapp.models.viewModels.InvitationViewModelBuilder
import uk.gov.communities.prsdb.webapp.models.viewModels.TicketPanelLinkViewModel
import uk.gov.communities.prsdb.webapp.models.viewModels.summaryModels.PropertyDetailsBeforePdjb939ViewModel
import uk.gov.communities.prsdb.webapp.models.viewModels.summaryModels.PropertyDetailsLandlordViewModelBuilder
import uk.gov.communities.prsdb.webapp.models.viewModels.summaryModels.PropertyDetailsNotificationBannerViewModel.NotificationMessage
import uk.gov.communities.prsdb.webapp.models.viewModels.summaryModels.PropertyDetailsViewModel
import uk.gov.communities.prsdb.webapp.models.viewModels.summaryModels.PropertyDetailsViewModelBase
import uk.gov.communities.prsdb.webapp.models.viewModels.summaryModels.propertyComplianceViewModels.NotificationBannerViewModelService
import uk.gov.communities.prsdb.webapp.models.viewModels.summaryModels.propertyComplianceViewModels.PropertyComplianceViewModelFactory
import uk.gov.communities.prsdb.webapp.services.BackUrlStorageService
import uk.gov.communities.prsdb.webapp.services.JointLandlordInvitationService
import uk.gov.communities.prsdb.webapp.services.PropertyComplianceService
import uk.gov.communities.prsdb.webapp.services.PropertyOwnershipService
import uk.gov.communities.prsdb.webapp.services.UserToLandlordService

@PrsdbController
@RequestMapping
class PropertyDetailsController(
    private val propertyOwnershipService: PropertyOwnershipService,
    private val backLinkStorageService: BackUrlStorageService,
    private val propertyComplianceService: PropertyComplianceService,
    private val propertyComplianceViewModelFactory: PropertyComplianceViewModelFactory,
    private val notificationBannerViewModelService: NotificationBannerViewModelService,
    private val messageSource: MessageSource,
    private val jointLandlordInvitationService: JointLandlordInvitationService,
    private val userToLandlordService: UserToLandlordService,
    private val featureFlagManager: FeatureFlagManager,
) {
    @PreAuthorize("hasRole('LANDLORD')")
    @GetMapping(LANDLORD_PROPERTY_DETAILS_ROUTE)
    fun getPropertyDetails(
        @PathVariable propertyOwnershipId: Long,
    ): ModelAndView {
        val propertyOwnership = propertyOwnershipService.getPropertyOwnershipIfCurrentUserAuthorized(propertyOwnershipId)

        val propertyCompliance =
            propertyComplianceService.getComplianceForPropertyOrNull(propertyOwnershipId)
                ?: throw PrsdbWebException("Property ownership $propertyOwnershipId does not have a compliance record")

        val provideLaterEnabled = featureFlagManager.checkFeature(PROPERTY_REGISTRATION_RESTRUCTURE_AND_SKIPPING)

        val (propertyDetails, viewName) = getPropertyDetailsViewModelAndView(propertyOwnership, provideLaterEnabled, isLandlordView = true)

        val propertyComplianceDetails =
            propertyComplianceViewModelFactory.create(
                propertyCompliance = propertyCompliance,
                landlordView = true,
                propertyOwnershipId = propertyOwnershipId,
            )

        val modelAndView = ModelAndView(viewName)
        modelAndView.addObject("propertyDetails", propertyDetails)
        modelAndView.addObject("complianceDetails", propertyComplianceDetails)
        modelAndView.addObject("complianceInfoTabId", COMPLIANCE_INFO_FRAGMENT)
        modelAndView.addObject(
            "notificationBanner",
            buildNotificationBanner(provideLaterEnabled, isLandlordView = true, propertyDetails, propertyCompliance),
        )

        val landlord = userToLandlordService.getCurrentLandlordForUser()
        val landlordSummaryCards =
            PropertyDetailsLandlordViewModelBuilder.buildSummaryCards(
                propertyOwnership.landlords,
                landlord,
                propertyOwnership.id,
            )
        modelAndView.addObject("landlordSummaryCards", landlordSummaryCards)
        modelAndView.addObject("landlordCount", propertyOwnership.landlords.size)
        val deregisterPropertyLink = DeregisterPropertyController.getPropertyDeregistrationPath(propertyOwnershipId)
        modelAndView.addObject("deregisterPropertyLink", deregisterPropertyLink)
        modelAndView.addObject("isLandlordView", true)
        if (featureFlagManager.checkFeature(DELEGATE_TO_LETTING_AGENT)) {
            modelAndView.addObject("showLettingAgentPanel", true)
            modelAndView.addObject("delegatesToLettingAgent", propertyOwnership.delegatesToLettingAgent)
            if (propertyOwnership.delegatesToLettingAgent) {
                modelAndView.addObject("lettingAgentEmail", propertyOwnership.lettingAgentAccess!!.invitedEmail)
                modelAndView.addObject(
                    "lettingAgentPanelLink",
                    TicketPanelLinkViewModel(
                        text = "propertyDetails.lettingAgentPanel.cancelDelegation.link",
                        url = CancelLettingAgentDelegationController.getRemoveLettingAgentPath(propertyOwnershipId),
                    ),
                )
            } else {
                modelAndView.addObject(
                    "lettingAgentPanelLink",
                    TicketPanelLinkViewModel(
                        text = "propertyDetails.lettingAgentPanel.delegateToLettingAgent.link",
                        url = getDelegateToLettingAgentPath(propertyOwnershipId),
                    ),
                )
            }
        }
        if (propertyOwnership.markedJointLandlord && propertyOwnership.landlords.size == 1) {
            modelAndView.addObject(
                "switchToIndividualLink",
                SwitchToIndividualController.getSwitchToIndividualFirstStepPath(propertyOwnershipId),
            )
        }

        modelAndView.addObject(
            "inviteJointLandlordUrl",
            InviteJointLandlordController.getInviteJointLandlordFirstStepPath(propertyOwnershipId),
        )

        modelAndView.addObject("markedJointLandlord", propertyOwnership.markedJointLandlord)

        val (pendingInvitations, expiredInvitations) =
            jointLandlordInvitationService
                .getPendingAndExpiredInvitations(propertyOwnership)
                .let { (pending, expired) ->
                    Pair(
                        pending.map { InvitationViewModelBuilder.buildPendingViewModel(it) },
                        expired.map { InvitationViewModelBuilder.buildExpiredViewModel(it) },
                    )
                }
        modelAndView.addObject("pendingInvitations", pendingInvitations)
        modelAndView.addObject("expiredInvitations", expiredInvitations)
        modelAndView.addObject("backUrl", LANDLORD_DASHBOARD_URL)

        return modelAndView
    }

    // TODO: PDJB-1060: We should not be using a GET for editing actions. Replace with a confirmation page.PDJB
    @PreAuthorize("hasRole('LANDLORD')")
    @GetMapping(REMOVE_EXPIRED_INVITE_ROUTE)
    fun removeExpiredInvite(
        @PathVariable propertyOwnershipId: Long,
        @PathVariable invitationId: Long,
        redirectAttributes: RedirectAttributes,
    ): String {
        jointLandlordInvitationService.hideExpiredInvitation(invitationId)
        redirectAttributes.addFlashAttribute("inviteRemoved", true)
        return "redirect:${getPropertyDetailsPath(propertyOwnershipId)}#$LANDLORD_DETAILS_FRAGMENT"
    }

    @PreAuthorize("hasAnyRole('LOCAL_COUNCIL_USER', 'LOCAL_COUNCIL_ADMIN')")
    @GetMapping(LOCAL_COUNCIL_PROPERTY_DETAILS_ROUTE)
    fun getPropertyDetailsLocalCouncilView(
        @PathVariable propertyOwnershipId: Long,
        model: Model,
    ): String {
        val propertyOwnership =
            propertyOwnershipService.getPropertyOwnershipIfCurrentUserAuthorized(propertyOwnershipId)

        val backUrlKey = backLinkStorageService.storeCurrentUrlReturningKey(LANDLORD_DETAILS_FRAGMENT)

        val propertyCompliance =
            propertyComplianceService.getComplianceForPropertyOrNull(propertyOwnershipId)
                ?: throw PrsdbWebException("Property ownership $propertyOwnershipId does not have a compliance record")

        val provideLaterEnabled = featureFlagManager.checkFeature(PROPERTY_REGISTRATION_RESTRUCTURE_AND_SKIPPING)

        val (propertyDetails, viewName) = getPropertyDetailsViewModelAndView(propertyOwnership, provideLaterEnabled, isLandlordView = false)

        val landlordSummaryCards =
            PropertyDetailsLandlordViewModelBuilder.buildLocalCouncilSummaryCards(
                propertyOwnership.landlords,
                landlordDetailsUrlProvider = { landlord ->
                    LandlordDetailsController
                        .getLandlordDetailsForLocalCouncilUserPath(landlord.id)
                        .overrideBackLinkForUrl(backUrlKey)
                },
            )
        model.addAttribute("landlordSummaryCards", landlordSummaryCards)
        model.addAttribute("landlordCount", propertyOwnership.landlords.size)

        val (pendingInvitations, expiredInvitations) =
            jointLandlordInvitationService
                .getPendingAndExpiredInvitations(propertyOwnership)
                .let { (pending, expired) ->
                    Pair(
                        pending.map { InvitationViewModelBuilder.buildPendingViewModel(it) },
                        expired.map { InvitationViewModelBuilder.buildExpiredViewModel(it) },
                    )
                }
        model.addAttribute("pendingInvitations", pendingInvitations)
        model.addAttribute("expiredInvitations", expiredInvitations)

        val propertyComplianceDetails =
            propertyComplianceViewModelFactory.create(
                propertyCompliance = propertyCompliance,
                landlordView = false,
                propertyOwnershipId = propertyOwnershipId,
            )

        model.addAttribute("propertyDetails", propertyDetails)
        model.addAttribute("complianceDetails", propertyComplianceDetails)
        model.addAttribute("complianceInfoTabId", COMPLIANCE_INFO_FRAGMENT)
        model.addAttribute(
            "notificationBanner",
            buildNotificationBanner(provideLaterEnabled, isLandlordView = false, propertyDetails, propertyCompliance),
        )
        model.addAttribute("isLandlordView", false)

        model.addAttribute("backUrl", LOCAL_COUNCIL_DASHBOARD_URL)

        return viewName
    }

    private fun buildNotificationBanner(
        provideLaterEnabled: Boolean,
        isLandlordView: Boolean,
        propertyDetails: PropertyDetailsViewModelBase,
        propertyCompliance: PropertyCompliance,
    ): List<NotificationMessage> =
        if (provideLaterEnabled) {
            val provideLaterDetails = propertyDetails as PropertyDetailsViewModel
            notificationBannerViewModelService
                .getPropertyDetailsNotificationBanner(
                    propertyCompliance = propertyCompliance,
                    isLandlordView = isLandlordView,
                    isOccupied = provideLaterDetails.isOccupied,
                    isLicensingProvideLater = provideLaterDetails.isLicensingProvideLater,
                    isTenancyProvideLater = provideLaterDetails.isTenancyProvideLater,
                ).messages
        } else {
            notificationBannerViewModelService.getBeforePdjb939NotificationBanner(propertyCompliance, isLandlordView)
        }

    private fun getPropertyDetailsViewModelAndView(
        propertyOwnership: PropertyOwnership,
        provideLaterEnabled: Boolean,
        isLandlordView: Boolean,
    ): Pair<PropertyDetailsViewModelBase, String> =
        if (provideLaterEnabled) {
            Pair(
                PropertyDetailsViewModel(propertyOwnership, isLandlordView, messageSource),
                PROPERTY_DETAILS_VIEW,
            )
        } else {
            Pair(
                PropertyDetailsBeforePdjb939ViewModel(propertyOwnership, isLandlordView, messageSource),
                PROPERTY_DETAILS_BEFORE_PDJB939_VIEW,
            )
        }

    companion object {
        const val PROPERTY_DETAILS_VIEW = "propertyDetailsView"

        const val PROPERTY_DETAILS_BEFORE_PDJB939_VIEW = "propertyDetailsViewBeforePdjb939"

        const val LANDLORD_PROPERTY_DETAILS_ROUTE = "/$LANDLORD_PATH_SEGMENT/$PROPERTY_DETAILS_SEGMENT/{propertyOwnershipId}"
        const val REMOVE_EXPIRED_INVITE_ROUTE = "$LANDLORD_PROPERTY_DETAILS_ROUTE/$REMOVE_EXPIRED_INVITE_PATH_SEGMENT/{invitationId}"

        const val LOCAL_COUNCIL_PROPERTY_DETAILS_ROUTE = "/$LOCAL_COUNCIL_PATH_SEGMENT/$PROPERTY_DETAILS_SEGMENT/{propertyOwnershipId}"

        fun getPropertyDetailsPath(
            propertyOwnershipId: Long,
            isLocalCouncilView: Boolean = false,
        ): String =
            UriTemplate(if (isLocalCouncilView) LOCAL_COUNCIL_PROPERTY_DETAILS_ROUTE else LANDLORD_PROPERTY_DETAILS_ROUTE)
                .expand(propertyOwnershipId)
                .toASCIIString()

        fun getPropertyCompliancePath(propertyOwnershipId: Long): String =
            UriTemplate("$LANDLORD_PROPERTY_DETAILS_ROUTE#$COMPLIANCE_INFO_FRAGMENT").expand(propertyOwnershipId).toASCIIString()

        fun getRemoveExpiredInvitePath(
            propertyOwnershipId: Long,
            invitationId: Long,
        ): String = UriTemplate(REMOVE_EXPIRED_INVITE_ROUTE).expand(propertyOwnershipId, invitationId).toASCIIString()
    }
}
