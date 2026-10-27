package uk.gov.communities.prsdb.webapp.controllers

import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.constraints.Min
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import uk.gov.communities.prsdb.webapp.annotations.webAnnotations.AllowIfLandlord
import uk.gov.communities.prsdb.webapp.annotations.webAnnotations.AvailableWhenFeatureDisabled
import uk.gov.communities.prsdb.webapp.annotations.webAnnotations.AvailableWhenFeatureEnabled
import uk.gov.communities.prsdb.webapp.annotations.webAnnotations.PrsdbController
import uk.gov.communities.prsdb.webapp.config.managers.FeatureFlagManager
import uk.gov.communities.prsdb.webapp.constants.COMPLIANCE_ACTIONS_PATH_SEGMENT
import uk.gov.communities.prsdb.webapp.constants.DASHBOARD_PATH_SEGMENT
import uk.gov.communities.prsdb.webapp.constants.INCOMPLETE_PROPERTIES_PATH_SEGMENT
import uk.gov.communities.prsdb.webapp.constants.LANDLORD_PATH_SEGMENT
import uk.gov.communities.prsdb.webapp.constants.OUTSTANDING_ACTIONS_PATH_SEGMENT
import uk.gov.communities.prsdb.webapp.constants.PROPERTY_REGISTRATION_PHASE_THREE
import uk.gov.communities.prsdb.webapp.constants.REGISTERED_PROPERTIES_FRAGMENT
import uk.gov.communities.prsdb.webapp.controllers.LandlordController.Companion.LANDLORD_BASE_URL
import uk.gov.communities.prsdb.webapp.models.dataModels.RegistrationNumberDataModel
import uk.gov.communities.prsdb.webapp.models.viewModels.PaginationViewModel
import uk.gov.communities.prsdb.webapp.models.viewModels.summaryModels.ComplianceActionViewModelBuilder
import uk.gov.communities.prsdb.webapp.models.viewModels.summaryModels.LandlordDashboardNotificationBannerViewModel
import uk.gov.communities.prsdb.webapp.models.viewModels.summaryModels.OutstandingActionViewModelBuilder
import uk.gov.communities.prsdb.webapp.services.PropertyComplianceService
import uk.gov.communities.prsdb.webapp.services.PropertyOwnershipService
import uk.gov.communities.prsdb.webapp.services.UserToLandlordService
import uk.gov.communities.prsdb.webapp.services.UsersIncompletePropertyService

@AllowIfLandlord
@PrsdbController
@RequestMapping(LANDLORD_BASE_URL, "/")
class LandlordController(
    private val userToLandlordService: UserToLandlordService,
    private val propertyOwnershipService: PropertyOwnershipService,
    private val propertyComplianceService: PropertyComplianceService,
    private val usersIncompletePropertyService: UsersIncompletePropertyService,
    private val featureFlagManager: FeatureFlagManager,
) {
    @GetMapping
    fun index(): CharSequence = "redirect:$LANDLORD_DASHBOARD_URL"

    @GetMapping("/$DASHBOARD_PATH_SEGMENT")
    fun landlordDashboard(model: Model): String {
        val landlord = userToLandlordService.getCurrentLandlordForUser()
        val numberOfComplianceActions =
            propertyOwnershipService.getNumberOfIncompleteCompliancesForLandlord(landlord) +
                propertyComplianceService.getNumberOfNonCompliantPropertiesForLandlord(landlord)

        val numberOfIncompleteProperties =
            usersIncompletePropertyService.getCurrentUsersIncompletePropertiesCount()

        val landlordDashboardNotificationBannerViewModel =
            LandlordDashboardNotificationBannerViewModel(
                numberOfIncompleteProperties = numberOfIncompleteProperties,
                numberOfComplianceActions = numberOfComplianceActions,
            )

        model.addAttribute("landlordDashboardNotificationBannerViewModel", landlordDashboardNotificationBannerViewModel)
        model.addAttribute("landlordName", landlord.name)
        model.addAttribute("lrn", RegistrationNumberDataModel.fromRegistrationNumber(landlord.registrationNumber))

        model.addAttribute("registerPropertyUrl", RegisterPropertyController.PROPERTY_REGISTRATION_ROUTE)
        model.addAttribute("viewIncompletePropertiesUrl", INCOMPLETE_PROPERTIES_URL)
        model.addAttribute(
            "viewPropertiesUrl",
            "${LandlordDetailsController.LANDLORD_DETAILS_FOR_LANDLORD_ROUTE}#$REGISTERED_PROPERTIES_FRAGMENT",
        )
        model.addAttribute("viewLandlordRecordUrl", LandlordDetailsController.LANDLORD_DETAILS_FOR_LANDLORD_ROUTE)
        // TODO PDJB-2006: Use the outstanding actions URL and button text unconditionally when the
        //  PROPERTY_REGISTRATION_PHASE_THREE flag is removed
        val outstandingActionsEnabled = featureFlagManager.checkFeature(PROPERTY_REGISTRATION_PHASE_THREE)
        model.addAttribute("outstandingActionsUrl", if (outstandingActionsEnabled) OUTSTANDING_ACTIONS_URL else COMPLIANCE_ACTIONS_URL)
        model.addAttribute(
            "outstandingActionsButtonKey",
            if (outstandingActionsEnabled) {
                "landlord.dashboard.outstandingActions.button"
            } else {
                "landlord.dashboard.complianceActions.button"
            },
        )
        return "landlordDashboard"
    }

    // TODO PDJB-2006: Remove this endpoint when the PROPERTY_REGISTRATION_PHASE_THREE flag is removed
    @AvailableWhenFeatureDisabled(PROPERTY_REGISTRATION_PHASE_THREE)
    @GetMapping("/$COMPLIANCE_ACTIONS_PATH_SEGMENT")
    fun getComplianceActions(
        model: Model,
        @RequestParam(value = "page", required = false) @Min(1) page: Int = 1,
        request: HttpServletRequest,
    ): String {
        val landlord = userToLandlordService.getCurrentLandlordForUser()
        val pagedNonCompliantProperties =
            propertyComplianceService.getNonCompliantPropertiesForLandlord(landlord, page - 1)

        if (pagedNonCompliantProperties.totalPages != 0 && pagedNonCompliantProperties.totalPages < page) {
            return "redirect:$COMPLIANCE_ACTIONS_URL"
        }

        val complianceActions =
            pagedNonCompliantProperties.content.map {
                ComplianceActionViewModelBuilder.fromDataModel(it)
            }

        model.addAttribute("complianceActions", complianceActions)
        model.addAttribute("paginationViewModel", PaginationViewModel(page, pagedNonCompliantProperties.totalPages, request))

        model.addAttribute(
            "viewRegisteredPropertiesUrl",
            "${LandlordDetailsController.LANDLORD_DETAILS_FOR_LANDLORD_ROUTE}#$REGISTERED_PROPERTIES_FRAGMENT",
        )
        model.addAttribute("backUrl", LANDLORD_DASHBOARD_URL)

        return "complianceActions"
    }

    @AvailableWhenFeatureEnabled(PROPERTY_REGISTRATION_PHASE_THREE)
    @GetMapping("/$OUTSTANDING_ACTIONS_PATH_SEGMENT")
    fun getOutstandingActions(
        model: Model,
        @RequestParam(value = "page", required = false) @Min(1) page: Int = 1,
        request: HttpServletRequest,
    ): String {
        val landlord = userToLandlordService.getCurrentLandlordForUser()
        val pagedNonCompliantProperties =
            propertyComplianceService.getNonCompliantPropertiesForLandlord(landlord, page - 1)

        if (pagedNonCompliantProperties.totalPages != 0 && pagedNonCompliantProperties.totalPages < page) {
            return "redirect:$OUTSTANDING_ACTIONS_URL"
        }

        val outstandingActions =
            pagedNonCompliantProperties.content.map {
                OutstandingActionViewModelBuilder.fromDataModel(it)
            }

        model.addAttribute("outstandingActions", outstandingActions)
        model.addAttribute("paginationViewModel", PaginationViewModel(page, pagedNonCompliantProperties.totalPages, request))

        model.addAttribute(
            "viewRegisteredPropertiesUrl",
            "${LandlordDetailsController.LANDLORD_DETAILS_FOR_LANDLORD_ROUTE}#$REGISTERED_PROPERTIES_FRAGMENT",
        )
        model.addAttribute("backUrl", LANDLORD_DASHBOARD_URL)

        return "outstandingActions"
    }

    companion object {
        const val LANDLORD_DASHBOARD_URL = "/$LANDLORD_PATH_SEGMENT/$DASHBOARD_PATH_SEGMENT"
        const val LANDLORD_BASE_URL = "/$LANDLORD_PATH_SEGMENT"
        const val INCOMPLETE_PROPERTIES_URL = "/$LANDLORD_PATH_SEGMENT/$INCOMPLETE_PROPERTIES_PATH_SEGMENT"

        // TODO PDJB-2006: Remove when the PROPERTY_REGISTRATION_PHASE_THREE flag is removed
        const val COMPLIANCE_ACTIONS_URL = "/$LANDLORD_PATH_SEGMENT/$COMPLIANCE_ACTIONS_PATH_SEGMENT"

        const val OUTSTANDING_ACTIONS_URL = "/$LANDLORD_PATH_SEGMENT/$OUTSTANDING_ACTIONS_PATH_SEGMENT"
    }
}
