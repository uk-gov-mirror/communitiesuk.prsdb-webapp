package uk.gov.communities.prsdb.webapp.controllers

import org.springframework.context.MessageSource
import org.springframework.http.HttpStatus
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.server.ResponseStatusException
import org.springframework.web.util.UriTemplate
import uk.gov.communities.prsdb.webapp.annotations.webAnnotations.AvailableWhenFeatureEnabled
import uk.gov.communities.prsdb.webapp.annotations.webAnnotations.PrsdbController
import uk.gov.communities.prsdb.webapp.constants.DELEGATE_TO_LETTING_AGENT
import uk.gov.communities.prsdb.webapp.constants.LANDLORD_PATH_SEGMENT
import uk.gov.communities.prsdb.webapp.constants.LETTING_AGENT_PATH_SEGMENT
import uk.gov.communities.prsdb.webapp.constants.LETTING_AGENT_PROPERTY_DETAILS_SURVEY_URL
import uk.gov.communities.prsdb.webapp.constants.PROPERTY_DETAILS_SEGMENT
import uk.gov.communities.prsdb.webapp.controllers.LettingAgentPropertyDetailsController.Companion.LETTING_AGENT_PROPERTY_DETAILS_ROUTE
import uk.gov.communities.prsdb.webapp.exceptions.PrsdbWebException
import uk.gov.communities.prsdb.webapp.models.viewModels.summaryModels.LettingAgentPropertyDetailsViewModel
import uk.gov.communities.prsdb.webapp.models.viewModels.summaryModels.PropertyDetailsViewType
import uk.gov.communities.prsdb.webapp.models.viewModels.summaryModels.propertyComplianceViewModels.PropertyComplianceViewModelFactory
import uk.gov.communities.prsdb.webapp.services.LettingAgentAccessService
import uk.gov.communities.prsdb.webapp.services.PropertyComplianceService
import uk.gov.communities.prsdb.webapp.services.PropertyOwnershipService
import java.util.UUID

@PrsdbController
@RequestMapping(LETTING_AGENT_PROPERTY_DETAILS_ROUTE)
class LettingAgentPropertyDetailsController(
    private val lettingAgentAccessService: LettingAgentAccessService,
    private val propertyOwnershipService: PropertyOwnershipService,
    private val propertyComplianceService: PropertyComplianceService,
    private val propertyComplianceViewModelFactory: PropertyComplianceViewModelFactory,
    private val messageSource: MessageSource,
) {
    @AvailableWhenFeatureEnabled(DELEGATE_TO_LETTING_AGENT)
    @GetMapping
    fun getLettingAgentPropertyDetails(
        @PathVariable token: UUID,
        model: Model,
    ): String {
        val lettingAgentAccess =
            lettingAgentAccessService.getInvitationByTokenOrNull(token)
                ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "No letting agent access found for token $token")

        val propertyOwnership = propertyOwnershipService.getPropertyOwnership(lettingAgentAccess.propertyOwnership.id)

        if (!propertyOwnershipService.hasLettingAgent(propertyOwnership.id)) {
            throw ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Property ownership ${propertyOwnership.id} does not have a letting agent",
            )
        }

        val propertyCompliance =
            propertyComplianceService.getComplianceForPropertyOrNull(propertyOwnership.id)
                ?: throw PrsdbWebException("Property ownership ${propertyOwnership.id} does not have a compliance record")

        model.addAttribute(
            "propertyDetails",
            LettingAgentPropertyDetailsViewModel(propertyOwnership, propertyCompliance, messageSource, token),
        )
        model.addAttribute(
            "complianceDetails",
            propertyComplianceViewModelFactory.create(
                propertyCompliance = propertyCompliance,
                viewType = PropertyDetailsViewType.LETTING_AGENT,
                propertyOwnershipId = propertyOwnership.id,
                lettingAgentAccessToken = token,
            ),
        )
        model.addAttribute("lettingAgentPropertyDetailsSurveyUrl", LETTING_AGENT_PROPERTY_DETAILS_SURVEY_URL)

        return "propertyDetailsLettingAgentView"
    }

    companion object {
        const val LETTING_AGENT_PROPERTY_DETAILS_ROUTE =
            "/$LANDLORD_PATH_SEGMENT/$LETTING_AGENT_PATH_SEGMENT/$PROPERTY_DETAILS_SEGMENT/{token}"

        fun getLettingAgentPropertyDetailsPath(token: UUID): String =
            UriTemplate(LETTING_AGENT_PROPERTY_DETAILS_ROUTE).expand(token).toASCIIString()
    }
}
