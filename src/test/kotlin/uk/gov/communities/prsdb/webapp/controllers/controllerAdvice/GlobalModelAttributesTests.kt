package uk.gov.communities.prsdb.webapp.controllers.controllerAdvice

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.context.MessageSource
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken
import org.springframework.security.oauth2.core.oidc.OidcIdToken
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser
import org.springframework.test.util.ReflectionTestUtils
import org.springframework.ui.ExtendedModelMap
import uk.gov.communities.prsdb.webapp.config.security.LettingAgentSecurityConfig.Companion.LETTING_AGENT_ROUTES_PREFIX
import uk.gov.communities.prsdb.webapp.constants.LOCAL_COUNCIL_PATH_SEGMENT
import uk.gov.communities.prsdb.webapp.constants.SYSTEM_OPERATOR_PATH_SEGMENT
import uk.gov.communities.prsdb.webapp.controllers.FeatureFlagOverrideController.Companion.FEATURE_FLAG_OVERRIDES_ROUTE
import uk.gov.communities.prsdb.webapp.controllers.HealthCheckController
import uk.gov.communities.prsdb.webapp.models.viewModels.NavigationLinkViewModel
import uk.gov.communities.prsdb.webapp.services.BackUrlStorageService
import uk.gov.communities.prsdb.webapp.services.DashboardUrlProvider
import uk.gov.communities.prsdb.webapp.services.FeatureFlagOverrideService
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@ExtendWith(MockitoExtension::class)
class GlobalModelAttributesTests {
    @Mock
    private lateinit var backUrlStorageService: BackUrlStorageService

    @Mock
    private lateinit var messageSource: MessageSource

    @Mock
    private lateinit var dashboardUrlProvider: DashboardUrlProvider

    @Mock
    private lateinit var featureFlagOverrideService: FeatureFlagOverrideService

    private val defaultServiceName = "Register your rental property"
    private val customServiceName = "Check a rental property or landlord"

    private fun createGlobalModelAttributes(): GlobalModelAttributes {
        val globalModelAttributes =
            GlobalModelAttributes(backUrlStorageService, messageSource, dashboardUrlProvider, featureFlagOverrideService)
        ReflectionTestUtils.setField(globalModelAttributes, "plausibleSiteId", "test-site-id")
        return globalModelAttributes
    }

    @Test
    fun `addGlobalModelAttributes sets serviceName to custom name for local council routes`() {
        whenever(messageSource.getMessage(eq("localCouncilServiceName"), anyOrNull(), any<String>(), any()))
            .thenReturn(customServiceName)
        val globalModelAttributes = createGlobalModelAttributes()
        val model = ExtendedModelMap()
        val request = MockHttpServletRequest()
        request.requestURI = "/$LOCAL_COUNCIL_PATH_SEGMENT/start"

        globalModelAttributes.addGlobalModelAttributes(model, request)

        assertEquals(customServiceName, model["serviceName"])
        assertTrue(model["showServiceNavigation"] as Boolean)
    }

    @Test
    fun `addGlobalModelAttributes sets serviceName to custom name for system operator routes`() {
        whenever(messageSource.getMessage(eq("localCouncilServiceName"), anyOrNull(), any<String>(), any()))
            .thenReturn(customServiceName)
        val globalModelAttributes = createGlobalModelAttributes()
        val model = ExtendedModelMap()
        val request = MockHttpServletRequest()
        request.requestURI = "/$SYSTEM_OPERATOR_PATH_SEGMENT/dashboard"

        globalModelAttributes.addGlobalModelAttributes(model, request)

        assertEquals(customServiceName, model["serviceName"])
        assertTrue(model["showServiceNavigation"] as Boolean)
    }

    @Test
    fun `addGlobalModelAttributes sets serviceName to default name for other routes`() {
        whenever(messageSource.getMessage(eq("serviceName"), anyOrNull(), any<String>(), any()))
            .thenReturn(defaultServiceName)
        val globalModelAttributes = createGlobalModelAttributes()
        val model = ExtendedModelMap()
        val request = MockHttpServletRequest()
        request.requestURI = "/landlord/dashboard"

        globalModelAttributes.addGlobalModelAttributes(model, request)

        assertEquals(defaultServiceName, model["serviceName"])
        assertNull(model["showServiceNavigation"])
    }

    @Test
    fun `addGlobalModelAttributes shows the service navigation with the default service name on letting agent routes`() {
        whenever(messageSource.getMessage(eq("serviceName"), anyOrNull(), any<String>(), any()))
            .thenReturn(defaultServiceName)
        val globalModelAttributes = createGlobalModelAttributes()
        val model = ExtendedModelMap()
        val request = MockHttpServletRequest()
        request.requestURI = "${LETTING_AGENT_ROUTES_PREFIX}property-details/3334abcd-5678-abcd-1234-567abcd2222a"

        globalModelAttributes.addGlobalModelAttributes(model, request)

        assertTrue(model["showServiceNavigation"] as Boolean)
        assertEquals(defaultServiceName, model["serviceName"])
    }

    @Test
    fun `addGlobalModelAttributes sets privacyUrl with a backUrl query param so the privacy page renders a back link`() {
        whenever(messageSource.getMessage(eq("serviceName"), anyOrNull(), any<String>(), any()))
            .thenReturn(defaultServiceName)
        whenever(backUrlStorageService.storeCurrentUrlReturningKey()).thenReturn(42)
        val globalModelAttributes = createGlobalModelAttributes()
        val model = ExtendedModelMap()
        val request = MockHttpServletRequest()
        request.requestURI = "/landlord/dashboard"

        globalModelAttributes.addGlobalModelAttributes(model, request)

        assertEquals("/privacy-notice?withBackUrl=42", model["privacyUrl"])
    }

    @Test
    fun `addGlobalModelAttributes sets showOneLoginNav to true for one-login users`() {
        whenever(messageSource.getMessage(eq("serviceName"), anyOrNull(), any<String>(), any()))
            .thenReturn(defaultServiceName)
        val globalModelAttributes = createGlobalModelAttributes()
        val model = ExtendedModelMap()
        val request = MockHttpServletRequest()
        request.requestURI = "/landlord/dashboard"
        request.userPrincipal = createOAuth2AuthenticationToken("one-login")

        globalModelAttributes.addGlobalModelAttributes(model, request)

        assertEquals(true, model["showOneLoginNav"])
    }

    @Test
    fun `addGlobalModelAttributes sets showOneLoginNav to false for internal-access users`() {
        whenever(messageSource.getMessage(eq("serviceName"), anyOrNull(), any<String>(), any()))
            .thenReturn(defaultServiceName)
        val globalModelAttributes = createGlobalModelAttributes()
        val model = ExtendedModelMap()
        val request = MockHttpServletRequest()
        request.requestURI = "/cookies"
        request.userPrincipal = createOAuth2AuthenticationToken("internal-access")

        globalModelAttributes.addGlobalModelAttributes(model, request)

        assertEquals(false, model["showOneLoginNav"])
    }

    @Test
    fun `addGlobalModelAttributes sets showOneLoginNav to false for unauthenticated users`() {
        whenever(messageSource.getMessage(eq("serviceName"), anyOrNull(), any<String>(), any()))
            .thenReturn(defaultServiceName)
        val globalModelAttributes = createGlobalModelAttributes()
        val model = ExtendedModelMap()
        val request = MockHttpServletRequest()
        request.requestURI = "/landlord/register-as-a-landlord"

        globalModelAttributes.addGlobalModelAttributes(model, request)

        assertEquals(false, model["showOneLoginNav"])
    }

    @Test
    fun `addGlobalModelAttributes adds a dashboard nav link pointing at the current user's dashboard`() {
        whenever(messageSource.getMessage(eq("serviceName"), anyOrNull(), any<String>(), any()))
            .thenReturn(defaultServiceName)
        whenever(dashboardUrlProvider.getDashboardUrlForCurrentUser()).thenReturn("/landlord/dashboard")
        val globalModelAttributes = createGlobalModelAttributes()
        val model = ExtendedModelMap()
        val request = MockHttpServletRequest()
        request.requestURI = "/landlord/dashboard"

        globalModelAttributes.addGlobalModelAttributes(model, request)

        @Suppress("UNCHECKED_CAST")
        val navLinks = model["navLinks"] as List<NavigationLinkViewModel>
        assertEquals(1, navLinks.size)
        assertEquals("/landlord/dashboard", navLinks[0].href)
        assertEquals("navLink.dashboard.title", navLinks[0].messageProperty)
        assertTrue(navLinks[0].isActive)
    }

    @Test
    fun `addGlobalModelAttributes marks the dashboard link unselected when not on the dashboard`() {
        whenever(messageSource.getMessage(eq("serviceName"), anyOrNull(), any<String>(), any()))
            .thenReturn(defaultServiceName)
        whenever(dashboardUrlProvider.getDashboardUrlForCurrentUser()).thenReturn("/landlord/dashboard")
        val globalModelAttributes = createGlobalModelAttributes()
        val model = ExtendedModelMap()
        val request = MockHttpServletRequest()
        request.requestURI = "/landlord/incomplete-properties"

        globalModelAttributes.addGlobalModelAttributes(model, request)

        @Suppress("UNCHECKED_CAST")
        val navLinks = model["navLinks"] as List<NavigationLinkViewModel>
        assertEquals(1, navLinks.size)
        assertEquals("/landlord/dashboard", navLinks[0].href)
        assertFalse(navLinks[0].isActive)
    }

    @Test
    fun `addGlobalModelAttributes adds the dashboard nav link on pages without a service-specific route`() {
        whenever(messageSource.getMessage(eq("serviceName"), anyOrNull(), any<String>(), any()))
            .thenReturn(defaultServiceName)
        whenever(dashboardUrlProvider.getDashboardUrlForCurrentUser()).thenReturn("/landlord/dashboard")
        val globalModelAttributes = createGlobalModelAttributes()
        val model = ExtendedModelMap()
        val request = MockHttpServletRequest()
        request.requestURI = "/cookies"

        globalModelAttributes.addGlobalModelAttributes(model, request)

        @Suppress("UNCHECKED_CAST")
        val navLinks = model["navLinks"] as List<NavigationLinkViewModel>
        assertEquals(1, navLinks.size)
        assertEquals("/landlord/dashboard", navLinks[0].href)
        assertFalse(navLinks[0].isActive)
    }

    @Test
    fun `addGlobalModelAttributes points the dashboard link at the local council dashboard for a local council user`() {
        whenever(messageSource.getMessage(eq("localCouncilServiceName"), anyOrNull(), any<String>(), any()))
            .thenReturn(customServiceName)
        whenever(dashboardUrlProvider.getDashboardUrlForCurrentUser()).thenReturn("/local-council/dashboard")
        val globalModelAttributes = createGlobalModelAttributes()
        val model = ExtendedModelMap()
        val request = MockHttpServletRequest()
        request.requestURI = "/local-council/dashboard"

        globalModelAttributes.addGlobalModelAttributes(model, request)

        @Suppress("UNCHECKED_CAST")
        val navLinks = model["navLinks"] as List<NavigationLinkViewModel>
        assertEquals(1, navLinks.size)
        assertEquals("/local-council/dashboard", navLinks[0].href)
        assertEquals("navLink.dashboard.title", navLinks[0].messageProperty)
        assertTrue(navLinks[0].isActive)
    }

    @Test
    fun `addGlobalModelAttributes points the dashboard link at the system operator dashboard for a system operator`() {
        whenever(messageSource.getMessage(eq("localCouncilServiceName"), anyOrNull(), any<String>(), any()))
            .thenReturn(customServiceName)
        whenever(dashboardUrlProvider.getDashboardUrlForCurrentUser()).thenReturn("/system-operator/dashboard")
        val globalModelAttributes = createGlobalModelAttributes()
        val model = ExtendedModelMap()
        val request = MockHttpServletRequest()
        request.requestURI = "/system-operator/dashboard"

        globalModelAttributes.addGlobalModelAttributes(model, request)

        @Suppress("UNCHECKED_CAST")
        val navLinks = model["navLinks"] as List<NavigationLinkViewModel>
        assertEquals(1, navLinks.size)
        assertEquals("/system-operator/dashboard", navLinks[0].href)
        assertEquals("navLink.dashboard.title", navLinks[0].messageProperty)
        assertTrue(navLinks[0].isActive)
    }

    @Test
    fun `addGlobalModelAttributes adds no nav link when the user has no dashboard`() {
        whenever(messageSource.getMessage(eq("serviceName"), anyOrNull(), any<String>(), any()))
            .thenReturn(defaultServiceName)
        whenever(dashboardUrlProvider.getDashboardUrlForCurrentUser()).thenReturn(null)
        val globalModelAttributes = createGlobalModelAttributes()
        val model = ExtendedModelMap()
        val request = MockHttpServletRequest()
        request.requestURI = "/landlord/register-as-a-landlord"

        globalModelAttributes.addGlobalModelAttributes(model, request)

        assertNull(model["navLinks"])
    }

    @Test
    fun `addGlobalModelAttributes does no page setup work for the healthcheck route`() {
        val globalModelAttributes = createGlobalModelAttributes()
        val model = ExtendedModelMap()
        val request = MockHttpServletRequest()
        request.requestURI = HealthCheckController.HEALTHCHECK_ROUTE

        globalModelAttributes.addGlobalModelAttributes(model, request)

        verify(backUrlStorageService, never()).storeCurrentUrlReturningKey()
        verify(dashboardUrlProvider, never()).getDashboardUrlForCurrentUser()
        verifyNoInteractions(messageSource)
        assertTrue(model.asMap().isEmpty())
    }

    @Test
    fun `addGlobalModelAttributes advertises the overrides page when overrides are active`() {
        whenever(featureFlagOverrideService.hasActiveOverrides()).thenReturn(true)
        val globalModelAttributes = createGlobalModelAttributes()
        val model = ExtendedModelMap()
        val request = MockHttpServletRequest()
        request.requestURI = "/"

        globalModelAttributes.addGlobalModelAttributes(model, request)

        assertTrue(model["featureFlagOverridesActive"] as Boolean)
        assertEquals(FEATURE_FLAG_OVERRIDES_ROUTE, model["featureFlagOverridesUrl"])
    }

    @Test
    fun `addGlobalModelAttributes does not advertise the overrides page when no overrides are active`() {
        whenever(featureFlagOverrideService.hasActiveOverrides()).thenReturn(false)
        val globalModelAttributes = createGlobalModelAttributes()
        val model = ExtendedModelMap()
        val request = MockHttpServletRequest()
        request.requestURI = "/"

        globalModelAttributes.addGlobalModelAttributes(model, request)

        assertNull(model["featureFlagOverridesActive"])
        assertNull(model["featureFlagOverridesUrl"])
    }

    private fun createOAuth2AuthenticationToken(registrationId: String): OAuth2AuthenticationToken {
        val idToken =
            OidcIdToken.withTokenValue("mock-token")
                .subject("mock-user")
                .issuer("http://localhost")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(300))
                .build()
        val oidcUser = DefaultOidcUser(emptyList(), idToken)
        return OAuth2AuthenticationToken(oidcUser, emptyList(), registrationId)
    }
}
