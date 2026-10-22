package uk.gov.communities.prsdb.webapp.controllers

import jakarta.servlet.http.HttpServletRequest
import org.apache.commons.fileupload2.core.FileItemInputIterator
import org.springframework.http.MediaType
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.CookieValue
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestAttribute
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.servlet.ModelAndView
import uk.gov.communities.prsdb.webapp.annotations.webAnnotations.AllowIfLandlord
import uk.gov.communities.prsdb.webapp.annotations.webAnnotations.PrsdbController
import uk.gov.communities.prsdb.webapp.config.filters.MultipartFormDataFilter
import uk.gov.communities.prsdb.webapp.constants.LANDLORD_PATH_SEGMENT
import uk.gov.communities.prsdb.webapp.constants.PROPERTY_DETAILS_SEGMENT
import uk.gov.communities.prsdb.webapp.controllers.LandlordUpdateGasSafetyController.Companion.UPDATE_GAS_SAFETY_ROUTE
import uk.gov.communities.prsdb.webapp.exceptions.PrsdbWebException
import uk.gov.communities.prsdb.webapp.helpers.CertificateFilenameHelper
import uk.gov.communities.prsdb.webapp.helpers.CertificateUploadHelper
import uk.gov.communities.prsdb.webapp.journeys.FormData
import uk.gov.communities.prsdb.webapp.journeys.JourneyIdProvider
import uk.gov.communities.prsdb.webapp.journeys.JourneyStepDispatcher
import uk.gov.communities.prsdb.webapp.journeys.StepLifecycleOrchestrator
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps.HasGasSupplyStep
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.update.gasSafety.UpdateGasSafetyJourneyFactory
import uk.gov.communities.prsdb.webapp.services.CollectionKeyParameterService
import uk.gov.communities.prsdb.webapp.services.FileUploadCookieService.Companion.FILE_UPLOAD_COOKIE_NAME
import uk.gov.communities.prsdb.webapp.services.PropertyOwnershipService
import java.security.Principal

@PrsdbController
@RequestMapping(UPDATE_GAS_SAFETY_ROUTE)
@AllowIfLandlord
class LandlordUpdateGasSafetyController(
    private val journeyFactory: UpdateGasSafetyJourneyFactory,
    private val propertyOwnershipService: PropertyOwnershipService,
    private val certificateUploadHelper: CertificateUploadHelper,
) {
    @GetMapping("/{*stepPath}")
    fun getUpdateStep(
        principal: Principal,
        @PathVariable propertyOwnershipId: Long,
        @PathVariable stepPath: String,
    ): ModelAndView {
        propertyOwnershipService.throwIfCurrentUserNotAuthorizedToEdit(propertyOwnershipId)
        return dispatchJourneyStep(stepPath, propertyOwnershipId, principal) { getStepModelAndView() }
    }

    @PostMapping("/{*stepPath}")
    fun postUpdateStep(
        model: Model,
        principal: Principal,
        @PathVariable propertyOwnershipId: Long,
        @PathVariable stepPath: String,
        @RequestParam formData: FormData,
    ): ModelAndView {
        propertyOwnershipService.throwIfCurrentUserNotAuthorizedToEdit(propertyOwnershipId)

        return dispatchJourneyStep(stepPath, propertyOwnershipId, principal) { postStepModelAndView(formData) }
    }

    @PostMapping("/{*stepPath}", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun postFileUploadStep(
        @PathVariable stepPath: String,
        @PathVariable propertyOwnershipId: Long,
        @RequestParam(JourneyIdProvider.PARAMETER_NAME) journeyId: String,
        @RequestParam(CollectionKeyParameterService.PARAMETER_NAME) memberId: String?,
        @RequestAttribute(MultipartFormDataFilter.ITERATOR_ATTRIBUTE) fileInputIterator: FileItemInputIterator,
        @CookieValue(name = FILE_UPLOAD_COOKIE_NAME) token: String,
        principal: Principal,
        request: HttpServletRequest,
    ): ModelAndView {
        propertyOwnershipService.throwIfCurrentUserNotAuthorizedToEdit(propertyOwnershipId)

        val stepName = stepPath.trimStart('/')
        val formData =
            certificateUploadHelper.uploadFileAndReturnFormModel(
                CertificateFilenameHelper.getCertFilename(journeyId, stepName, memberId),
                fileInputIterator,
                token,
                request,
            )

        return dispatchJourneyStep(stepPath, propertyOwnershipId, principal) { postStepModelAndView(formData) }
    }

    private fun dispatchJourneyStep(
        stepPath: String,
        propertyOwnershipId: Long,
        principal: Principal,
        dispatch: StepLifecycleOrchestrator.() -> ModelAndView,
    ): ModelAndView =
        JourneyStepDispatcher.handleInitialisableRequest(
            rawStepPath = stepPath,
            createRoutingMap = {
                journeyFactory.createJourneySteps(
                    propertyOwnershipId,
                    PropertyDetailsController.getPropertyCompliancePath(propertyOwnershipId),
                )
            },
            initialiseJourney = {
                val propertyCompliance =
                    propertyOwnershipService.getPropertyOwnership(propertyOwnershipId).propertyCompliance
                        ?: throw PrsdbWebException("Property ownership $propertyOwnershipId does not have a compliance record")
                journeyFactory.initialiseJourneyState(
                    Pair(propertyOwnershipId, principal),
                    propertyCompliance.getMostRecentlyUpdated(),
                )
            },
            dispatch = dispatch,
        )

    companion object {
        const val UPDATE_GAS_SAFETY_ROUTE = "/$LANDLORD_PATH_SEGMENT/$PROPERTY_DETAILS_SEGMENT/{propertyOwnershipId}/update-gas-safety"

        fun getUpdateGasSafetyFirstStepRoute(propertyOwnershipId: Long): String =
            UPDATE_GAS_SAFETY_ROUTE.replace("{propertyOwnershipId}", propertyOwnershipId.toString()) +
                "/${HasGasSupplyStep.ROUTE_SEGMENT}"
    }
}
