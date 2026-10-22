package uk.gov.communities.prsdb.webapp.controllers

import com.github.dockerjava.zerodep.shaded.org.apache.hc.client5.http.entity.mime.MultipartEntityBuilder
import jakarta.servlet.http.Cookie
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.eq
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import org.springframework.util.ResourceUtils
import org.springframework.web.context.WebApplicationContext
import org.springframework.web.server.ResponseStatusException
import org.springframework.web.servlet.ModelAndView
import uk.gov.communities.prsdb.webapp.helpers.CertificateUploadHelper
import uk.gov.communities.prsdb.webapp.journeys.StepLifecycleOrchestrator
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps.HasGasSupplyStep
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.steps.UploadGasCertStep
import uk.gov.communities.prsdb.webapp.journeys.propertyRegistration.update.gasSafety.UpdateGasSafetyJourneyFactory
import uk.gov.communities.prsdb.webapp.services.FileUploadCookieService.Companion.FILE_UPLOAD_COOKIE_NAME
import uk.gov.communities.prsdb.webapp.services.PropertyOwnershipService
import uk.gov.communities.prsdb.webapp.testHelpers.mockObjects.MockLandlordData.Companion.createOccupiedPropertyOwnership
import uk.gov.communities.prsdb.webapp.testHelpers.mockObjects.MockLettingAgentData
import java.util.UUID

@WebMvcTest(LettingAgentUpdateGasSafetyController::class)
class LettingAgentUpdateGasSafetyControllerTests(
    @Autowired webContext: WebApplicationContext,
) : LettingAgentAccessControllerTest(webContext) {
    @MockitoBean
    private lateinit var journeyFactory: UpdateGasSafetyJourneyFactory

    @MockitoBean
    private lateinit var propertyOwnershipService: PropertyOwnershipService

    @MockitoBean
    private lateinit var certificateUploadHelper: CertificateUploadHelper

    @MockitoBean
    private lateinit var stepLifecycleOrchestrator: StepLifecycleOrchestrator.VisitableStepLifecycleOrchestrator

    private val token: UUID = UUID.randomUUID()

    private val updateStepRoute =
        LettingAgentUpdateGasSafetyController.getUpdateGasSafetyRoute(token) +
            "/${HasGasSupplyStep.ROUTE_SEGMENT}"

    private val formContent = "hasGasSupply=true"

    private fun stubValidTokenForProperty() {
        val propertyOwnership = createOccupiedPropertyOwnership()
        whenever(lettingAgentAccessService.getInvitationByTokenOrNull(eq(token)))
            .thenReturn(MockLettingAgentData.createLettingAgentAccess(token = token, propertyOwnership = propertyOwnership))
        whenever(journeyFactory.createJourneySteps(eq(propertyOwnership.id), any()))
            .thenReturn(mapOf(HasGasSupplyStep.ROUTE_SEGMENT to stepLifecycleOrchestrator))
    }

    @Test
    fun `getUpdateStep returns 200 when the token maps to a property the user can edit`() {
        stubValidTokenForProperty()
        whenever(stepLifecycleOrchestrator.getStepModelAndView())
            .thenReturn(ModelAndView("placeholder", mapOf("title" to "placeholder")))

        mvc.get(updateStepRoute).andExpect {
            status { isOk() }
        }
    }

    @Test
    fun `getUpdateStep returns 404 when the token is not recognised`() {
        whenever(lettingAgentAccessService.getInvitationByTokenOrNull(eq(token))).thenReturn(null)

        mvc.get(updateStepRoute).andExpect {
            status { isNotFound() }
        }
    }

    @Test
    fun `getUpdateStep returns 404 when the current user is not authorised to edit the property`() {
        val propertyOwnership = createOccupiedPropertyOwnership()
        whenever(lettingAgentAccessService.getInvitationByTokenOrNull(eq(token)))
            .thenReturn(MockLettingAgentData.createLettingAgentAccess(token = token, propertyOwnership = propertyOwnership))
        whenever(propertyOwnershipService.throwIfCurrentUserNotAuthorizedToEdit(eq(propertyOwnership.id)))
            .thenThrow(ResponseStatusException(HttpStatus.NOT_FOUND))

        mvc.get(updateStepRoute).andExpect {
            status { isNotFound() }
        }
    }

    @Test
    @WithMockUser(roles = ["INDIVIDUAL_LANDLORD"], username = "landlord-user")
    fun `getUpdateStep returns 403 when a landlord is logged in`() {
        mvc.get(updateStepRoute).andExpect {
            status { isForbidden() }
        }
    }

    @Test
    @WithMockUser(roles = ["LOCAL_COUNCIL_USER"], username = "council-user")
    fun `getUpdateStep returns 403 when a local council user is logged in`() {
        mvc.get(updateStepRoute).andExpect {
            status { isForbidden() }
        }
    }

    @Test
    fun `postUpdateStep redirects for a valid token`() {
        stubValidTokenForProperty()
        val redirectUrl = LettingAgentPropertyDetailsController.getLettingAgentPropertyDetailsPath(token)
        whenever(stepLifecycleOrchestrator.postStepModelAndView(any()))
            .thenReturn(ModelAndView("redirect:$redirectUrl"))

        mvc
            .post(updateStepRoute) {
                contentType = MediaType.APPLICATION_FORM_URLENCODED
                content = formContent
                with(csrf())
            }.andExpect {
                status { is3xxRedirection() }
                redirectedUrl(redirectUrl)
            }
    }

    @Test
    fun `postUpdateStep returns 404 when the token is not recognised`() {
        whenever(lettingAgentAccessService.getInvitationByTokenOrNull(eq(token))).thenReturn(null)

        mvc
            .post(updateStepRoute) {
                contentType = MediaType.APPLICATION_FORM_URLENCODED
                content = formContent
                with(csrf())
            }.andExpect {
                status { isNotFound() }
            }
    }

    @Test
    fun `postUpdateStep returns 404 when the current user is not authorised to edit the property`() {
        val propertyOwnership = createOccupiedPropertyOwnership()
        whenever(lettingAgentAccessService.getInvitationByTokenOrNull(eq(token)))
            .thenReturn(MockLettingAgentData.createLettingAgentAccess(token = token, propertyOwnership = propertyOwnership))
        whenever(propertyOwnershipService.throwIfCurrentUserNotAuthorizedToEdit(eq(propertyOwnership.id)))
            .thenThrow(ResponseStatusException(HttpStatus.NOT_FOUND))

        mvc
            .post(updateStepRoute) {
                contentType = MediaType.APPLICATION_FORM_URLENCODED
                content = formContent
                with(csrf())
            }.andExpect {
                status { isNotFound() }
            }
    }

    @Nested
    inner class PostFileUploadStep {
        private val journeyId = "test-journey-id"
        private val redirectUrl = "any-url"

        private val validFileUploadUrl =
            LettingAgentUpdateGasSafetyController.getUpdateGasSafetyRoute(token) +
                "/${UploadGasCertStep.ROUTE_SEGMENT}?journeyId=$journeyId"

        private val validFileUploadCookie = Cookie(FILE_UPLOAD_COOKIE_NAME, "valid-token")

        private val httpEntity =
            MultipartEntityBuilder
                .create()
                .addTextBody("_csrf", "any-csrf-token")
                .addBinaryBody("certificate", ResourceUtils.getFile("classpath:data/certificates/validFile.png"))
                .build()

        @Test
        fun `postFileUploadStep delegates to the certificate upload helper and redirects for a valid token`() {
            val propertyOwnership = createOccupiedPropertyOwnership()
            whenever(lettingAgentAccessService.getInvitationByTokenOrNull(eq(token)))
                .thenReturn(MockLettingAgentData.createLettingAgentAccess(token = token, propertyOwnership = propertyOwnership))
            whenever(journeyFactory.createJourneySteps(eq(propertyOwnership.id), any()))
                .thenReturn(mapOf(UploadGasCertStep.ROUTE_SEGMENT to stepLifecycleOrchestrator))
            whenever(certificateUploadHelper.uploadFileAndReturnFormModel(any(), any(), any(), any()))
                .thenReturn(mapOf<String, Any>())
            whenever(stepLifecycleOrchestrator.postStepModelAndView(any()))
                .thenReturn(ModelAndView("redirect:$redirectUrl"))

            mvc
                .post(validFileUploadUrl) {
                    contentType = MediaType.parseMediaType(httpEntity.contentType)
                    content = httpEntity.content.readAllBytes()
                    with(csrf().asHeader())
                    cookie(validFileUploadCookie)
                }.andExpect {
                    status { is3xxRedirection() }
                    redirectedUrl(redirectUrl)
                }
        }

        @Test
        fun `postFileUploadStep returns 400 for a valid token without a cookie`() {
            stubValidTokenForProperty()

            mvc
                .post(validFileUploadUrl) {
                    contentType = MediaType.parseMediaType(httpEntity.contentType)
                    content = httpEntity.content.readAllBytes()
                    with(csrf().asHeader())
                }.andExpect {
                    status { isBadRequest() }
                }
        }

        @Test
        fun `postFileUploadStep returns 404 when the current user is not authorised to edit the property`() {
            val propertyOwnership = createOccupiedPropertyOwnership()
            whenever(lettingAgentAccessService.getInvitationByTokenOrNull(eq(token)))
                .thenReturn(MockLettingAgentData.createLettingAgentAccess(token = token, propertyOwnership = propertyOwnership))
            doThrow(ResponseStatusException(HttpStatus.NOT_FOUND))
                .whenever(propertyOwnershipService)
                .throwIfCurrentUserNotAuthorizedToEdit(eq(propertyOwnership.id))

            mvc
                .post(validFileUploadUrl) {
                    contentType = MediaType.parseMediaType(httpEntity.contentType)
                    content = httpEntity.content.readAllBytes()
                    with(csrf().asHeader())
                    cookie(validFileUploadCookie)
                }.andExpect {
                    status { isNotFound() }
                }
        }
    }
}
