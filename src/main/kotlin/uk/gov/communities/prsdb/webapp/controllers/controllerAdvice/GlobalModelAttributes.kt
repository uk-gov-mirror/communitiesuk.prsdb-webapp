package uk.gov.communities.prsdb.webapp.controllers.controllerAdvice

import jakarta.servlet.http.HttpServletRequest
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.MessageSource
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes
import uk.gov.communities.prsdb.webapp.annotations.webAnnotations.PrsdbControllerAdvice
import uk.gov.communities.prsdb.webapp.config.filters.CSPNonceFilter.Companion.CSP_NONCE_ATTRIBUTE
import uk.gov.communities.prsdb.webapp.config.interceptors.BackLinkInterceptor.Companion.overrideBackLinkForUrl
import uk.gov.communities.prsdb.webapp.config.security.LettingAgentSecurityConfig.Companion.LETTING_AGENT_ROUTES_PREFIX
import uk.gov.communities.prsdb.webapp.constants.CONFIRM_SIGN_OUT_PATH_SEGMENT
import uk.gov.communities.prsdb.webapp.constants.CROWN_COPYRIGHT_URL
import uk.gov.communities.prsdb.webapp.constants.GOV_LICENCE_URL
import uk.gov.communities.prsdb.webapp.constants.LOCAL_COUNCIL_PATH_SEGMENT
import uk.gov.communities.prsdb.webapp.constants.MHCLG_URL
import uk.gov.communities.prsdb.webapp.constants.PLAUSIBLE_URL
import uk.gov.communities.prsdb.webapp.constants.PRIVACY_NOTICE_PATH_SEGMENT
import uk.gov.communities.prsdb.webapp.constants.PRSD_EMAIL
import uk.gov.communities.prsdb.webapp.constants.RENTERS_RIGHTS_BILL_URL
import uk.gov.communities.prsdb.webapp.constants.SYSTEM_OPERATOR_PATH_SEGMENT
import uk.gov.communities.prsdb.webapp.controllers.BetaFeedbackController.Companion.FEEDBACK_URL
import uk.gov.communities.prsdb.webapp.controllers.CookiesController.Companion.COOKIES_ROUTE
import uk.gov.communities.prsdb.webapp.controllers.FeatureFlagOverrideController.Companion.FEATURE_FLAG_OVERRIDES_ROUTE
import uk.gov.communities.prsdb.webapp.controllers.HealthCheckController.Companion.HEALTHCHECK_ROUTE
import uk.gov.communities.prsdb.webapp.models.viewModels.NavigationLinkViewModel
import uk.gov.communities.prsdb.webapp.services.BackUrlStorageService
import uk.gov.communities.prsdb.webapp.services.DashboardUrlProvider
import uk.gov.communities.prsdb.webapp.services.FeatureFlagOverrideService
import java.util.Locale

@PrsdbControllerAdvice
class GlobalModelAttributes(
    private val backUrlStorageService: BackUrlStorageService,
    private val messageSource: MessageSource,
    private val dashboardUrlProvider: DashboardUrlProvider,
    private val featureFlagOverrideService: FeatureFlagOverrideService,
) {
    @Value("\${plausible.site-id}")
    private lateinit var plausibleSiteId: String

    @ModelAttribute
    fun addGlobalModelAttributes(
        model: Model,
        request: HttpServletRequest,
    ) {
        if (request.requestURI == HEALTHCHECK_ROUTE) {
            return
        }

        model.addAttribute("cookiesUrl", COOKIES_ROUTE.overrideBackLinkForUrl(backUrlStorageService.storeCurrentUrlReturningKey()))
        model.addAttribute("plausibleUrl", "$PLAUSIBLE_URL/js/pa-$plausibleSiteId.js")
        model.addAttribute("serverGeneratedNonce", getCurrentNonce())

        // Feedback banner attributes
        model.addAttribute("feedbackBannerUrl", FEEDBACK_URL)

        if (featureFlagOverrideService.hasActiveOverrides()) {
            model.addAttribute("featureFlagOverridesActive", true)
            model.addAttribute("featureFlagOverridesUrl", FEATURE_FLAG_OVERRIDES_ROUTE)
        }

        // Authenticated header attributes
        model.addAttribute("confirmSignOutUrl", "/$CONFIRM_SIGN_OUT_PATH_SEGMENT")
        val principal = request.userPrincipal
        val isOneLoginUser =
            principal is OAuth2AuthenticationToken && principal.authorizedClientRegistrationId == "one-login"
        model.addAttribute("showOneLoginNav", isOneLoginUser)

        // Footer attributes
        model.addAttribute("prsdbEmail", PRSD_EMAIL)
        model.addAttribute(
            "privacyUrl",
            "/$PRIVACY_NOTICE_PATH_SEGMENT".overrideBackLinkForUrl(backUrlStorageService.storeCurrentUrlReturningKey()),
        )
        model.addAttribute("rentersRightsBillUrl", RENTERS_RIGHTS_BILL_URL)
        model.addAttribute("mhclgUrl", MHCLG_URL)
        model.addAttribute("licenceUrl", GOV_LICENCE_URL)
        model.addAttribute("copyrightUrl", CROWN_COPYRIGHT_URL)

        // Service name — LC/system operator routes use a different name from the default
        val uri = request.requestURI
        val isCustomServiceName =
            uri.isServicePage(LOCAL_COUNCIL_PATH_SEGMENT) || uri.isServicePage(SYSTEM_OPERATOR_PATH_SEGMENT)
        val serviceNameKey = if (isCustomServiceName) "localCouncilServiceName" else "serviceName"
        val serviceName = messageSource.getMessage(serviceNameKey, null, serviceNameKey, Locale.getDefault())
        model.addAttribute("serviceName", serviceName)
        if (isCustomServiceName || uri.startsWith(LETTING_AGENT_ROUTES_PREFIX)) {
            model.addAttribute("showServiceNavigation", true)
        }

        val dashboardUrl = dashboardUrlProvider.getDashboardUrlForCurrentUser()
        if (dashboardUrl != null) {
            model.addAttribute(
                "navLinks",
                listOf(NavigationLinkViewModel(dashboardUrl, "navLink.dashboard.title", uri == dashboardUrl)),
            )
        }
    }

    private fun String.isServicePage(pathSegment: String): Boolean = this.startsWith("/$pathSegment/")

    private fun getCurrentNonce(): String {
        val context = RequestContextHolder.getRequestAttributes() as ServletRequestAttributes?
        if (context?.request is HttpServletRequest) {
            val nonce = context.request.getAttribute(CSP_NONCE_ATTRIBUTE)
            if (nonce != null) return nonce.toString()
        }
        return ""
    }
}
