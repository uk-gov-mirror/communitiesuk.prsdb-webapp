package uk.gov.communities.prsdb.webapp.config.security

import org.springframework.context.annotation.Bean
import org.springframework.core.annotation.Order
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.header.HeaderWriterFilter
import uk.gov.communities.prsdb.webapp.annotations.webAnnotations.PrsdbWebConfiguration
import uk.gov.communities.prsdb.webapp.config.filters.CSPNonceFilter
import uk.gov.communities.prsdb.webapp.config.security.DefaultSecurityConfig.Companion.CONTENT_SECURITY_POLICY_DIRECTIVES
import uk.gov.communities.prsdb.webapp.config.security.DefaultSecurityConfig.Companion.PERMISSIONS_POLICY_DIRECTIVES
import uk.gov.communities.prsdb.webapp.constants.INVALID_LINK_PAGE_PATH_SEGMENT
import uk.gov.communities.prsdb.webapp.constants.LETTING_AGENT_PATH_SEGMENT
import uk.gov.communities.prsdb.webapp.controllers.LettingAgentInvitationController.Companion.LETTING_AGENT_INVITATION_ROUTE
import uk.gov.communities.prsdb.webapp.journeys.lettingAgentInvitation.steps.ConfirmationStep
import uk.gov.communities.prsdb.webapp.journeys.lettingAgentInvitation.steps.EnterPasswordStep
import uk.gov.communities.prsdb.webapp.journeys.lettingAgentInvitation.steps.SetPasswordStep
import uk.gov.communities.prsdb.webapp.journeys.lettingAgentInvitation.steps.StartStep
import uk.gov.communities.prsdb.webapp.journeys.lettingAgentInvitation.steps.StoreAccessStep
import uk.gov.communities.prsdb.webapp.journeys.lettingAgentInvitation.steps.ValidateTokenStep

@PrsdbWebConfiguration
@EnableMethodSecurity
class LettingAgentSecurityConfig {
    @Bean
    @Order(4)
    fun lettingAgentSecurityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .securityMatcher("/$LETTING_AGENT_PATH_SEGMENT/**")
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.ALWAYS) }
            .authorizeHttpRequests { requests ->
                requests
                    .requestMatchers(LETTING_AGENT_INVITATION_ROUTE)
                    .anonymous()
                    .requestMatchers(
                        "$LETTING_AGENT_INVITATION_ROUTE/${StartStep.ROUTE_SEGMENT}",
                    ).anonymous()
                    .requestMatchers(
                        // TODO: PDJB-1660: Remove when validate token step becomes an internal step
                        "$LETTING_AGENT_INVITATION_ROUTE/${ValidateTokenStep.ROUTE_SEGMENT}",
                    ).anonymous()
                    .requestMatchers(
                        "$LETTING_AGENT_INVITATION_ROUTE/${SetPasswordStep.ROUTE_SEGMENT}",
                    ).anonymous()
                    .requestMatchers(
                        "$LETTING_AGENT_INVITATION_ROUTE/${ConfirmationStep.ROUTE_SEGMENT}",
                    ).anonymous()
                    .requestMatchers(
                        "$LETTING_AGENT_INVITATION_ROUTE/${EnterPasswordStep.ROUTE_SEGMENT}",
                    ).anonymous()
                    .requestMatchers(
                        // TODO: PDJB-1659: Remove when store access set step becomes an internal step
                        "$LETTING_AGENT_INVITATION_ROUTE/${StoreAccessStep.ROUTE_SEGMENT}",
                    ).anonymous()
                    .requestMatchers(
                        "$LETTING_AGENT_INVITATION_ROUTE/$INVALID_LINK_PAGE_PATH_SEGMENT",
                    ).anonymous()
                    .anyRequest()
                    .authenticated()
            }.headers { headers ->
                headers
                    .contentSecurityPolicy { csp ->
                        csp
                            .policyDirectives(CONTENT_SECURITY_POLICY_DIRECTIVES)
                    }.permissionsPolicyHeader { permissions ->
                        permissions
                            .policy(PERMISSIONS_POLICY_DIRECTIVES)
                    }
            }.addFilterBefore(CSPNonceFilter(), HeaderWriterFilter::class.java)

        return http.build()
    }
}
